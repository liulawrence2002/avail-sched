import { useParams, useNavigate } from 'react-router-dom';
import { useEffect, useState, useCallback } from 'react';
import {
  getHostResults,
  getEvent,
  finalizeEvent,
  getIcsUrl,
} from '../api.js';
import { useAppState } from '../hooks/useAppState';
import Card from '../components/Card';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';
import EmptyState from '../components/EmptyState';

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
  const [finalizeError, setFinalizeError] = useState(null);
  const [copied, setCopied] = useState(false);

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
      const eventRes = await getEvent(resultsRes.data.publicId);
      if (eventRes.ok) {
        setEvent(eventRes.data);
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
            <div className="flex items-center gap-3 mt-2 text-xs text-silver-dim">
              <span>{participantCount} invited</span>
              <span>•</span>
              <span>{respondentCount} responded</span>
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
            <a
              href={icsUrl}
              download
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium bg-gold/10 text-gold border border-gold/20 hover:bg-gold/20 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold"
            >
              📥 Download .ICS
            </a>
          </div>
        </Card>
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
