import { useParams, useNavigate } from 'react-router-dom';
import { useEffect, useState, useCallback } from 'react';
import { getPublicResults, getEvent } from '../api.js';
import { useAppState } from '../hooks/useAppState';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

function formatSlotLocal(isoString, timezone) {
  const d = new Date(isoString);
  const dateStr = d.toLocaleDateString('en-US', {
    timeZone: timezone,
    weekday: 'short',
    month: 'short',
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
 * Public aggregate results.
 * /e/:publicId/results
 */
export default function PublicResultsPage() {
  const { publicId } = useParams();
  const navigate = useNavigate();
  const { showError } = useAppState();

  const [results, setResults] = useState(null);
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);

    const [resultsRes, eventRes] = await Promise.all([
      getPublicResults(publicId),
      getEvent(publicId),
    ]);

    if (!resultsRes.ok) {
      setError(resultsRes.error?.message || 'Failed to load results');
      showError(resultsRes.error?.message || 'Failed to load results');
      setLoading(false);
      return;
    }

    setResults(resultsRes.data);
    if (eventRes.ok) {
      setEvent(eventRes.data);
    }
    setLoading(false);
  }, [publicId, showError]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <LoadingState message="Tallying up the goblin votes..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <ErrorMessage title="Could not load results" message={error} onRetry={loadData} />
      </div>
    );
  }

  if (!results || !results.topSlots?.length) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <EmptyState
          icon="📊"
          title="No responses yet"
          description="No one has marked their availability. Share the link to get started!"
          action={
            <Button variant="primary" onClick={() => navigate(`/e/${publicId}`)}>
              Back to Event
            </Button>
          }
        />
      </div>
    );
  }

  const { topSlots, participantCount, respondentCount, finalSelection, timezone } = results;
  const maxPercent = topSlots[0]?.percentOfMax || 100;

  return (
    <div className="max-w-3xl mx-auto px-4 py-8 sm:py-12">
      <Card padding="xl" border="gold" shadow="stage">
        <h1 className="font-display text-2xl sm:text-3xl text-cream mb-2">
          {event?.title || 'Event Results'}
        </h1>
        {event?.description && (
          <p className="text-sm text-silver mb-2">{event.description}</p>
        )}
        {(event?.location || event?.meetingUrl) && (
          <div className="flex flex-wrap items-center gap-3 mb-2">
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
        <div className="flex items-center gap-3 mb-6 text-xs text-silver-dim">
          <span>{participantCount} invited</span>
          <span>•</span>
          <span>{respondentCount} responded</span>
        </div>

        {/* Finalized banner */}
        {finalSelection && (
          <div className="mb-6 p-4 rounded-xl bg-gold/10 border border-gold/20">
            <div className="flex items-center gap-2 mb-1">
              <span className="text-gold">🏆</span>
              <span className="text-sm font-semibold text-gold">Finalized!</span>
            </div>
            <p className="text-sm text-cream">
              The host selected:{' '}
              <strong>
                {(() => {
                  const { dateStr, timeStr } = formatSlotLocal(finalSelection.slotStartUtc, timezone);
                  return `${dateStr} at ${timeStr}`;
                })()}
              </strong>
            </p>
          </div>
        )}

        {/* Slot summary */}
        <div className="space-y-3">
          {topSlots
            .sort((a, b) => (b.score || 0) - (a.score || 0))
            .map((slot) => {
              const { dateStr, timeStr } = formatSlotLocal(slot.slotStartUtc, timezone);
              const isBest = slot.percentOfMax === maxPercent && slot.percentOfMax > 0;

              return (
                <div
                  key={slot.slotStartUtc}
                  className={`p-4 rounded-xl border transition-all ${
                    isBest
                      ? 'bg-gold/5 border-gold/30'
                      : 'bg-charcoal/40 border-white/5'
                  }`}
                >
                  <div className="flex items-start justify-between gap-4 mb-3">
                    <div>
                      <div className="text-sm font-medium text-cream flex items-center gap-2">
                        {dateStr}
                        {isBest && (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-gold/20 text-gold uppercase tracking-wider">
                            Best
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-silver">{timeStr}</div>
                    </div>
                    <div className="text-right">
                      <div className="text-lg font-bold text-cream">{slot.score?.toFixed(1) || 0}</div>
                      <div className="text-xs text-silver-dim">
                        {slot.percentOfMax || 0}% of best
                      </div>
                    </div>
                  </div>

                  {/* Progress bar */}
                  <div className="w-full h-2 bg-charcoal-light rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${
                        isBest ? 'bg-gold' : 'bg-emerald-muted'
                      }`}
                      style={{ width: `${slot.percentOfMax || 0}%` }}
                    />
                  </div>

                  <div className="mt-3 flex flex-wrap gap-2 text-xs text-silver-dim">
                    <span className="px-2 py-0.5 rounded bg-emerald/10 text-jade">{slot.yesCount || 0} yes</span>
                    <span className="px-2 py-0.5 rounded bg-sapphire/10 text-sapphire-bright">{slot.maybeCount || 0} maybe</span>
                    <span className="px-2 py-0.5 rounded bg-gold/5 text-gold">{slot.bribeCount || 0} possible</span>
                    <span className="px-2 py-0.5 rounded bg-ruby/10 text-crimson">{slot.noCount || 0} no</span>
                  </div>
                </div>
              );
            })}
        </div>

        <div className="flex items-center justify-between mt-8 pt-4 border-t border-white/5">
          <Button variant="ghost" size="sm" onClick={() => navigate(`/e/${publicId}`)}>
            ← Back to Event
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={() => {
              const url = window.location.href.replace('/results', '');
              navigator.clipboard.writeText(url);
            }}
          >
            📋 Copy Event Link
          </Button>
        </div>
      </Card>
    </div>
  );
}
