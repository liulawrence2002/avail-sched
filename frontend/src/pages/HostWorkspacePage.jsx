import { useParams, useNavigate } from 'react-router-dom';
import { useEffect, useState, useCallback } from 'react';
import {
  getHostResults,
  getEvent,
  finalizeEvent,
  getIcsUrl,
  updateEvent,
  deleteEvent,
  getEventNotes,
  saveEventNotes,
  getGoogleAuthUrl,
  addEventToCalendar,
  nudgeNonRespondents,
} from '../api.js';
import { useLocalStorage } from '../hooks';
import { useAppState } from '../hooks/useAppState';
import Card from '../components/Card';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';
import EmptyState from '../components/EmptyState';
import EventCommentsPanel from '../components/EventCommentsPanel';
import SmartSuggestModal from '../components/SmartSuggestModal';

function formatSlotLocal(isoString, timezone) {
  const d = new Date(isoString);
  const dateStr = d.toLocaleDateString('en-US', {
    timeZone: timezone,
    weekday: 'long',
    month: 'long',
    day: 'numeric',
  });
  const timeStr = d.toLocaleTimeString('en-US', {
    timeZone: timezone,
    hour: '2-digit',
    minute: '2-digit',
  });
  return { dateStr, timeStr };
}

/**
 * Private host workspace.
 * /host/:hostToken
 */
export default function HostWorkspacePage() {
  const { hostToken } = useParams();
  const navigate = useNavigate();
  const { showError } = useAppState();

  const [event, setEvent] = useState(null);
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [finalizing, setFinalizing] = useState(false);
  const [googleUserId, setGoogleUserId] = useLocalStorage('goblin_google_user_id', null);
  const [addingToCalendar, setAddingToCalendar] = useState(false);
  const [calendarLink, setCalendarLink] = useState(null);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [nudging, setNudging] = useState(false);
  const [nudgeSent, setNudgeSent] = useState(0);
  const [finalizeError, setFinalizeError] = useState(null);
  const [copied, setCopied] = useState(false);
  const [editing, setEditing] = useState(false);
  const [editData, setEditData] = useState(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [notes, setNotes] = useState(null);
  const [notesOpen, setNotesOpen] = useState(false);
  const [notesDraft, setNotesDraft] = useState('');
  const [savingNotes, setSavingNotes] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);

    const resultsRes = await getHostResults(hostToken);
    if (!resultsRes.ok) {
      setError(resultsRes.error?.message || 'Failed to load host workspace');
      showError(resultsRes.error?.message || 'Failed to load host workspace');
      setLoading(false);
      return;
    }

    setResults(resultsRes.data);

    // Fetch event details for title
    if (resultsRes.data?.publicId) {
      const [eventRes, notesRes] = await Promise.all([
        getEvent(resultsRes.data.publicId),
        getEventNotes(resultsRes.data.publicId),
      ]);
      if (eventRes.ok) {
        setEvent(eventRes.data);
      }
      if (notesRes.ok && notesRes.data) {
        setNotes(notesRes.data);
        setNotesDraft(notesRes.data.content);
      } else {
        setNotes(null);
        setNotesDraft('');
      }
    }

    setLoading(false);
  }, [hostToken, showError]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleCopyLink = async () => {
    if (!event?.publicId) return;
    const url = `${window.location.origin}/e/${event.publicId}`;
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback ignored
    }
  };

  const handleCopyResults = async () => {
    if (!event?.publicId) return;
    const url = `${window.location.origin}/e/${event.publicId}/results`;
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback ignored
    }
  };

  const handleFinalize = async (slotStartUtc) => {
    if (!results?.publicId || !hostToken) return;
    if (!window.confirm('Are you sure? This will lock in the final time for everyone.')) return;

    setFinalizing(true);
    setFinalizeError(null);

    const res = await finalizeEvent(results.publicId, slotStartUtc, hostToken);
    setFinalizing(false);

    if (!res.ok) {
      setFinalizeError(res.error?.message || 'Failed to finalize');
      showError(res.error?.message || 'Failed to finalize');
      return;
    }

    // Refresh data
    loadData();
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12">
        <LoadingState message="Preparing your command center..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12">
        <ErrorMessage
          title="Host workspace not found"
          message={error}
          onRetry={loadData}
        />
      </div>
    );
  }

  if (!results) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12">
        <EmptyState
          icon="🦎"
          title="Nothing to see here"
          description="Your host workspace appears to be empty."
        />
      </div>
    );
  }

  const { topSlots, finalSelection, participantCount, respondentCount, timezone, publicId } = results;
  const icsUrl = getIcsUrl(publicId);

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 sm:py-12">
      {/* Header card */}
      <Card padding="lg" border="gold" shadow="stage" className="mb-6">
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-gold/15 text-gold uppercase tracking-widest border border-gold/20">
                Host
              </span>
              {finalSelection && (
                <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-emerald/15 text-jade uppercase tracking-widest border border-emerald/20">
                  Finalized
                </span>
              )}
            </div>
            <h1 className="font-display text-2xl sm:text-3xl text-cream">{event?.title || 'Your Event'}</h1>
            {event?.description && (
              <p className="text-sm text-silver mt-1">{event.description}</p>
            )}
            {(event?.location || event?.meetingUrl) && (
              <div className="flex flex-wrap items-center gap-3 mt-2">
                {event.location && (
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-charcoal/40 border border-white/5 text-xs text-cream-muted">
                    <svg className="w-3.5 h-3.5 text-silver" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                    {event.location}
                  </span>
                )}
                {event.meetingUrl && (
                  <a
                    href={event.meetingUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-sapphire/10 border border-sapphire/20 text-xs text-sapphire-bright hover:bg-sapphire/20 transition-colors"
                  >
                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                    </svg>
                    Join Meeting
                  </a>
                )}
              </div>
            )}
            <div className="flex items-center gap-3 mt-2 text-xs text-silver-dim">
              <span>{participantCount} invited</span>
              <span>•</span>
              <span>{respondentCount} responded</span>
              {!finalSelection && respondentCount < participantCount && (
                <>
                  <span>•</span>
                  <button
                    onClick={async () => {
                      setNudging(true);
                      const res = await nudgeNonRespondents(publicId, hostToken);
                      if (res.ok) {
                        setNudgeSent(res.data.sent);
                        setTimeout(() => setNudgeSent(0), 3000);
                      }
                      setNudging(false);
                    }}
                    disabled={nudging}
                    className="text-gold hover:text-gold-bright transition-colors disabled:opacity-50"
                  >
                    {nudging ? 'Sending...' : nudgeSent > 0 ? `✓ ${nudgeSent} reminded` : '🔔 Nudge non-respondents'}
                  </button>
                </>
              )}
            </div>
          </div>
          <div className="flex flex-col gap-2 sm:items-end">
            <Button
              variant="goldGhost"
              size="sm"
              onClick={handleCopyLink}
              success={copied}
            >
              {copied ? 'Copied!' : '📋 Copy Event Link'}
            </Button>
            <Button variant="ghost" size="sm" onClick={handleCopyResults}>
              📊 Copy Results Link
            </Button>
            {finalSelection && !googleUserId && (
              <Button
                variant="ghost"
                size="sm"
                onClick={async () => {
                  const res = await getGoogleAuthUrl();
                  if (res.ok && res.data?.url) {
                    window.open(res.data.url, '_blank');
                  } else {
                    showError('Google Calendar integration is not configured');
                  }
                }}
              >
                🗓️ Connect Google Calendar
              </Button>
            )}
          </div>
        </div>
      </Card>

      {/* Finalized banner */}
      {finalSelection && (
        <Card padding="md" border="emerald" className="mb-6 bg-emerald/5">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <div className="text-sm font-semibold text-jade mb-1">Final time selected</div>
              <div className="text-cream font-display text-lg">
                {(() => {
                  const { dateStr, timeStr } = formatSlotLocal(finalSelection.slotStartUtc, timezone);
                  return `${dateStr} at ${timeStr}`;
                })()}
              </div>
            </div>
            <div className="flex flex-wrap gap-2">
              <a
                href={icsUrl}
                download
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium bg-gold/10 text-gold border border-gold/20 hover:bg-gold/20 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold"
              >
                📥 Download .ICS
              </a>
              {googleUserId && !calendarLink && (
                <Button
                  variant="ghost"
                  size="sm"
                  loading={addingToCalendar}
                  onClick={async () => {
                    setAddingToCalendar(true);
                    const res = await addEventToCalendar(publicId, hostToken, googleUserId);
                    if (res.ok && res.data?.calendarLink) {
                      setCalendarLink(res.data.calendarLink);
                    } else {
                      showError(res.error?.message || 'Failed to add to calendar');
                    }
                    setAddingToCalendar(false);
                  }}
                >
                  🗓️ Add to Google Calendar
                </Button>
              )}
              {calendarLink && (
                <a
                  href={calendarLink}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium bg-sapphire/10 text-sapphire-bright border border-sapphire/20 hover:bg-sapphire/20 transition-colors"
                >
                  🗓️ Open in Google Calendar
                </a>
              )}
            </div>
          </div>
        </Card>
      )}

      {/* Edit form */}
      {editing && event && (
        <Card padding="lg" border="gold" className="mb-6">
          <h3 className="font-display text-xl text-cream mb-4">Edit Event</h3>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium text-cream-muted tracking-wide">Title</label>
              <input
                type="text"
                value={editData?.title ?? event.title}
                onChange={(e) => setEditData((prev) => ({ ...prev, title: e.target.value }))}
                className="w-full px-4 py-3 rounded-xl bg-charcoal/60 border border-white/10 text-cream transition-all hover:border-white/20 focus:border-gold/50 focus:outline-none"
              />
            </div>
            <div>
              <label className="text-sm font-medium text-cream-muted tracking-wide">Description</label>
              <textarea
                value={editData?.description ?? event.description ?? ''}
                onChange={(e) => setEditData((prev) => ({ ...prev, description: e.target.value }))}
                rows={3}
                className="w-full px-4 py-3 rounded-xl bg-charcoal/60 border border-white/10 text-cream transition-all hover:border-white/20 focus:border-gold/50 focus:outline-none resize-none"
              />
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-cream-muted tracking-wide">Location</label>
                <input
                  type="text"
                  value={editData?.location ?? event.location ?? ''}
                  onChange={(e) => setEditData((prev) => ({ ...prev, location: e.target.value }))}
                  className="w-full px-4 py-3 rounded-xl bg-charcoal/60 border border-white/10 text-cream transition-all hover:border-white/20 focus:border-gold/50 focus:outline-none"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-cream-muted tracking-wide">Meeting URL</label>
                <input
                  type="url"
                  value={editData?.meetingUrl ?? event.meetingUrl ?? ''}
                  onChange={(e) => setEditData((prev) => ({ ...prev, meetingUrl: e.target.value }))}
                  className="w-full px-4 py-3 rounded-xl bg-charcoal/60 border border-white/10 text-cream transition-all hover:border-white/20 focus:border-gold/50 focus:outline-none"
                />
              </div>
            </div>
            {saveError && <ErrorMessage message={saveError} />}
            <div className="flex justify-end gap-3 pt-2">
              <Button variant="ghost" onClick={() => { setEditing(false); setEditData(null); setSaveError(null); }}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={async () => {
                  setSaving(true);
                  setSaveError(null);
                  const payload = {
                    title: editData?.title ?? event.title,
                    description: (editData?.description ?? event.description) || undefined,
                    timezone: event.timezone,
                    slotMinutes: event.slotMinutes,
                    durationMinutes: event.durationMinutes,
                    startDate: event.startDate,
                    endDate: event.endDate,
                    dailyStartTime: event.dailyStartTime,
                    dailyEndTime: event.dailyEndTime,
                    location: (editData?.location ?? event.location) || undefined,
                    meetingUrl: (editData?.meetingUrl ?? event.meetingUrl) || undefined,
                    resultsVisibility: event.resultsVisibility,
                  };
                  const res = await updateEvent(hostToken, payload);
                  setSaving(false);
                  if (!res.ok) {
                    setSaveError(res.error?.message || 'Failed to save changes');
                    return;
                  }
                  setEditing(false);
                  setEditData(null);
                  loadData();
                }}
                disabled={saving}
              >
                {saving ? 'Saving...' : 'Save Changes'}
              </Button>
            </div>
          </div>
        </Card>
      )}

      {/* Agenda / Notes */}
      <Card padding="md" border="subtle" className="mb-6">
        <div className="flex items-center justify-between mb-3">
          <h3 className="font-display text-lg text-cream">Agenda & Notes</h3>
          <Button variant="ghost" size="sm" onClick={() => setNotesOpen((o) => !o)}>
            {notesOpen ? 'Close' : notes ? 'Edit' : 'Add'}
          </Button>
        </div>
        {!notesOpen && notes && (
          <div className="text-sm text-cream-muted whitespace-pre-wrap leading-relaxed">
            {notes.content}
          </div>
        )}
        {!notesOpen && !notes && (
          <p className="text-sm text-silver-dim">No agenda or notes yet. Click Add to create one.</p>
        )}
        {notesOpen && (
          <div className="space-y-3">
            <textarea
              value={notesDraft}
              onChange={(e) => setNotesDraft(e.target.value)}
              placeholder="Add agenda items, notes, or discussion points..."
              rows={6}
              className="w-full px-4 py-3 rounded-xl bg-charcoal/60 border border-white/10 text-cream placeholder-silver-dim/60 transition-all hover:border-white/20 focus:border-gold/50 focus:bg-charcoal-light/60 resize-none"
            />
            <div className="flex justify-end gap-2">
              <Button variant="ghost" size="sm" onClick={() => { setNotesOpen(false); setNotesDraft(notes?.content || ''); }}>
                Cancel
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={async () => {
                  setSavingNotes(true);
                  const res = await saveEventNotes(hostToken, notesDraft);
                  setSavingNotes(false);
                  if (res.ok) {
                    setNotes(res.data);
                    setNotesOpen(false);
                  } else {
                    showError(res.error?.message || 'Failed to save notes');
                  }
                }}
                disabled={savingNotes}
              >
                {savingNotes ? 'Saving...' : 'Save Notes'}
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Results grid */}
      {!finalSelection && topSlots?.length > 0 && (
        <div className="mb-4">
          <Button
            variant="goldGhost"
            size="sm"
            onClick={() => setShowSuggestions(true)}
          >
            ✨ Smart Suggest
          </Button>
        </div>
      )}

      {/* Results grid */}
      <div className="space-y-3">
        {topSlots
          ?.sort((a, b) => (b.score || 0) - (a.score || 0))
          .map((slot) => {
            const { dateStr, timeStr } = formatSlotLocal(slot.slotStartUtc, timezone);
            const isFinalized = finalSelection?.slotStartUtc === slot.slotStartUtc;
            const maxPercent = topSlots[0]?.percentOfMax || 100;
            const isBest = slot.percentOfMax === maxPercent && slot.percentOfMax > 0;

            return (
              <Card
                key={slot.slotStartUtc}
                padding="md"
                border={isFinalized ? 'gold' : 'subtle'}
                className={isFinalized ? 'bg-gold/5 border-gold/30' : ''}
              >
                <div className="flex flex-col sm:flex-row items-start justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-sm font-medium text-cream">{dateStr}</span>
                      {isBest && !finalSelection && (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-gold/15 text-gold uppercase">
                          Top Pick
                        </span>
                      )}
                      {isFinalized && (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald/15 text-jade uppercase">
                          Selected
                        </span>
                      )}
                    </div>
                    <div className="text-xs text-silver mb-3">{timeStr}</div>

                    {/* Score bar */}
                    <div className="w-full h-2 bg-charcoal-light rounded-full overflow-hidden mb-2">
                      <div
                        className={`h-full rounded-full transition-all ${
                          isBest ? 'bg-gold' : 'bg-emerald-muted'
                        }`}
                        style={{ width: `${slot.percentOfMax || 0}%` }}
                      />
                    </div>
                    <div className="text-xs text-silver-dim">
                      Score {slot.score?.toFixed(1) || 0} • {slot.yesCount || 0} yes / {slot.maybeCount || 0} maybe / {slot.bribeCount || 0} possible / {slot.noCount || 0} no
                    </div>

                    {/* Names */}
                    {slot.canAttend?.length > 0 && (
                      <div className="mt-2 flex flex-wrap gap-1">
                        {slot.canAttend.map((name) => (
                          <span
                            key={name}
                            className="px-2 py-0.5 rounded text-xs bg-emerald/10 text-jade border border-emerald/15"
                          >
                            ✓ {name}
                          </span>
                        ))}
                      </div>
                    )}
                    {slot.cannotAttend?.length > 0 && (
                      <div className="mt-1 flex flex-wrap gap-1">
                        {slot.cannotAttend.map((name) => (
                          <span
                            key={name}
                            className="px-2 py-0.5 rounded text-xs bg-ruby/10 text-crimson border border-ruby/15"
                          >
                            ✕ {name}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  {!finalSelection && (
                    <div className="flex-shrink-0">
                      <Button
                        variant={isBest ? 'primary' : 'ghost'}
                        size="sm"
                        onClick={() => handleFinalize(slot.slotStartUtc)}
                        loading={finalizing}
                      >
                        {isBest ? '🏆 Pick This' : 'Pick'}
                      </Button>
                    </div>
                  )}
                </div>
              </Card>
            );
          })}
      </div>

      {finalizeError && (
        <div className="mt-6">
          <ErrorMessage variant="inline" message={finalizeError} onRetry={() => setFinalizeError(null)} />
        </div>
      )}

      {/* Discussion */}
      <div className="mt-8">
        <EventCommentsPanel publicId={publicId} hostToken={hostToken} title={event?.title} />
      </div>

      {/* Smart Suggest Modal */}
      {showSuggestions && (
        <SmartSuggestModal
          hostToken={hostToken}
          timezone={timezone}
          onClose={() => setShowSuggestions(false)}
          onPick={(slotUtc) => {
            setShowSuggestions(false);
            handleFinalize(slotUtc);
          }}
        />
      )}

      {/* Footer actions */}
      <div className="mt-8 flex flex-wrap items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate(`/e/${publicId}`)}>
          View Public Page
        </Button>
        <Button variant="ghost" size="sm" onClick={() => navigate(`/e/${publicId}/results`)}>
          View Results Page
        </Button>
        <Button variant="ghost" size="sm" onClick={loadData}>
          🔄 Refresh
        </Button>
      </div>
    </div>
  );
}
