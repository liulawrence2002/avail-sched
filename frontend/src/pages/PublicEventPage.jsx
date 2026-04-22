import { useParams, useNavigate } from 'react-router-dom';
import { useEffect, useState, useCallback } from 'react';
import { getEvent, joinParticipant, getAvailability, saveAvailability } from '../api.js';
import { useAppState } from '../hooks/useAppState';
import { useLocalStorage } from '../hooks';
import Button from '../components/Button';
import Input from '../components/Input';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';
import EmptyState from '../components/EmptyState';

const WEIGHTS = [0.0, 1.0, 0.6, 0.3];
const WEIGHT_LABELS = {
  1.0: { label: 'Definitely', colorClass: 'bg-emerald/10 border-emerald/30 hover:bg-emerald/20 text-jade' },
  0.6: { label: 'Probably', colorClass: 'bg-sapphire/10 border-sapphire/30 hover:bg-sapphire/20 text-sapphire-bright' },
  0.3: { label: 'If I must', colorClass: 'bg-gold/5 border-gold/30 hover:bg-gold/10 text-gold' },
  0.0: { label: 'Nope', colorClass: 'bg-charcoal/40 border-white/5 hover:bg-ruby/10 hover:border-ruby/20 text-crimson' },
};

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
 * Public event response flow.
 * /e/:publicId
 */
export default function PublicEventPage() {
  const { publicId } = useParams();
  const navigate = useNavigate();
  const { showError } = useAppState();

  const [event, setEvent] = useState(null);
  const [eventLoading, setEventLoading] = useState(true);
  const [eventError, setEventError] = useState(null);

  const tokenKey = `goblin_participant_${publicId}`;
  const [storedToken, setStoredToken] = useLocalStorage(tokenKey, null);

  const [step, setStep] = useState(storedToken ? 'availability' : 'join');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [joining, setJoining] = useState(false);
  const [joinError, setJoinError] = useState(null);
  const [nameError, setNameError] = useState('');

  const [selections, setSelections] = useState({});
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [availLoading, setAvailLoading] = useState(false);

  // Load event
  useEffect(() => {
    let cancelled = false;
    async function load() {
      setEventLoading(true);
      setEventError(null);
      const res = await getEvent(publicId);
      if (cancelled) return;
      if (!res.ok) {
        setEventError(res.error?.message || 'Failed to load event');
      } else {
        setEvent(res.data);
      }
      setEventLoading(false);
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [publicId]);

  // Load existing availability when we have a token
  useEffect(() => {
    if (!storedToken || !publicId || !event) return;
    let cancelled = false;
    async function loadAvail() {
      setAvailLoading(true);
      const res = await getAvailability(publicId, storedToken);
      if (cancelled) return;
      if (res.ok && res.data?.items) {
        const next = {};
        res.data.items.forEach((item) => {
          next[item.slotStartUtc] = item.weight;
        });
        setSelections(next);
      }
      setAvailLoading(false);
    }
    loadAvail();
    return () => {
      cancelled = true;
    };
  }, [storedToken, publicId, event]);

  const handleJoin = async () => {
    if (!displayName.trim()) {
      setNameError('Please enter your name');
      return;
    }
    setNameError('');
    setJoining(true);
    setJoinError(null);

    const res = await joinParticipant(publicId, { displayName: displayName.trim(), email: email.trim() || undefined });
    if (!res.ok) {
      setJoinError(res.error?.message || 'Failed to join');
      showError(res.error?.message || 'Failed to join');
      setJoining(false);
      return;
    }

    setStoredToken(res.data.participantToken);
    setJoining(false);
    setStep('availability');
  };

  const cycleSlot = useCallback((slotStartUtc) => {
    setSelections((prev) => {
      const current = prev[slotStartUtc] ?? 0.0;
      const idx = WEIGHTS.indexOf(current);
      const nextIdx = (idx + 1) % WEIGHTS.length;
      const nextWeight = WEIGHTS[nextIdx];
      return { ...prev, [slotStartUtc]: nextWeight };
    });
    setSaveSuccess(false);
  }, []);

  const handleKeyDown = useCallback((e, slotStartUtc) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      cycleSlot(slotStartUtc);
    }
  }, [cycleSlot]);

  const handleSave = async () => {
    const token = storedToken;
    if (!token) {
      setStep('join');
      return;
    }

    setSaving(true);
    setSaveError(null);
    setSaveSuccess(false);

    const items = Object.entries(selections).map(([slotStartUtc, weight]) => ({
      slotStartUtc,
      weight,
    }));

    const res = await saveAvailability(publicId, token, { items });
    setSaving(false);

    if (!res.ok) {
      setSaveError(res.error?.message || 'Failed to save');
      showError(res.error?.message || 'Failed to save');
      return;
    }

    setSaveSuccess(true);
    setTimeout(() => setSaveSuccess(false), 3000);
  };

  // Pre-populate selections from candidateSlotsUtc
  useEffect(() => {
    if (event?.candidateSlotsUtc) {
      setSelections((prev) => {
        const next = { ...prev };
        event.candidateSlotsUtc.forEach((slotUtc) => {
          if (next[slotUtc] === undefined) {
            next[slotUtc] = 0.0;
          }
        });
        return next;
      });
    }
  }, [event]);

  if (eventLoading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12">
        <LoadingState message="The goblins are fetching your event..." />
      </div>
    );
  }

  if (eventError) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12">
        <ErrorMessage
          title="Event not found"
          message={eventError}
          onRetry={() => window.location.reload()}
        />
      </div>
    );
  }

  if (!event) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12">
        <EmptyState
          icon="🦎"
          title="Event vanished into the void"
          description="We couldn't find this event. It may have expired or the link is incorrect."
          action={
            <Button variant="primary" onClick={() => navigate('/create')}>
              Create a New Event
            </Button>
          }
        />
      </div>
    );
  }

  const sortedSlots = [...(event.candidateSlotsUtc || [])].sort(
    (a, b) => new Date(a) - new Date(b)
  );

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 sm:py-12">
      <Card padding="xl" border="gold" shadow="stage">
        {/* Event header */}
        <div className="mb-6">
          <h1 className="font-display text-2xl sm:text-3xl text-cream mb-2">{event.title}</h1>
          {event.description && (
            <p className="text-sm text-silver leading-relaxed">{event.description}</p>
          )}
          <div className="flex items-center gap-2 mt-3 text-xs text-silver-dim">
            <span>📅</span>
            <span>
              {sortedSlots.length} candidate time
              {sortedSlots.length !== 1 ? 's' : ''}
            </span>
            <span>•</span>
            <span>{event.durationMinutes} min slots</span>
          </div>
        </div>

        {step === 'join' && (
          <div className="space-y-5">
            <h2 className="font-display text-xl text-cream">Join this event</h2>
            <p className="text-sm text-silver">Enter your name so the host knows who you are.</p>

            <Input
              label="Your Name"
              value={displayName}
              onChange={(e) => {
                setDisplayName(e.target.value);
                if (nameError) setNameError('');
              }}
              error={nameError}
              placeholder="e.g., Gandalf the Grey"
              required
            />

            <Input
              label="Email (optional)"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              hint="Only used to send you a reminder if the host shares it."
              placeholder="you@example.com"
            />

            {joinError && <ErrorMessage variant="inline" message={joinError} />}

            <div className="flex items-center justify-between pt-2">
              <Button variant="ghost" size="sm" onClick={() => navigate(`/e/${publicId}/results`)}>
                Skip to results →
              </Button>
              <Button variant="primary" onClick={handleJoin} loading={joining}>
                Join & Mark Availability
              </Button>
            </div>
          </div>
        )}

        {step === 'availability' && (
          <div className="space-y-5">
            <div className="flex items-center justify-between">
              <h2 className="font-display text-xl text-cream">Mark your availability</h2>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setStoredToken(null);
                  setStep('join');
                }}
              >
                Not you? Switch
              </Button>
            </div>

            <p className="text-sm text-silver">
              Click each time slot to cycle through your availability. Tap through all four states to find the one that fits.
            </p>

            {/* Legend */}
            <div className="flex flex-wrap gap-2">
              <span className="px-2 py-1 rounded-full text-xs font-medium bg-emerald/10 text-jade border border-emerald/20">
                Definitely
              </span>
              <span className="px-2 py-1 rounded-full text-xs font-medium bg-sapphire/10 text-sapphire-bright border border-sapphire/20">
                Probably
              </span>
              <span className="px-2 py-1 rounded-full text-xs font-medium bg-gold/5 text-gold border border-gold/20">
                If I must
              </span>
              <span className="px-2 py-1 rounded-full text-xs font-medium bg-ruby/10 text-crimson border border-ruby/20">
                Nope
              </span>
            </div>

            {availLoading && (
              <div className="text-xs text-silver-dim">Loading your previous answers…</div>
            )}

            <div className="space-y-2">
              {sortedSlots.map((slotUtc) => {
                const weight = selections[slotUtc] ?? 0.0;
                const meta = WEIGHT_LABELS[weight];
                const { dateStr, timeStr } = formatSlotLocal(slotUtc, event.timezone);

                return (
                  <button
                    key={slotUtc}
                    onClick={() => cycleSlot(slotUtc)}
                    onKeyDown={(e) => handleKeyDown(e, slotUtc)}
                    className={`w-full flex items-center justify-between p-4 rounded-xl border transition-all duration-200 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold ${meta.colorClass}`}
                    aria-label={`${dateStr} ${timeStr}: ${meta.label}`}
                    tabIndex={0}
                  >
                    <div>
                      <div className="text-sm font-medium text-cream">{dateStr}</div>
                      <div className="text-xs text-silver">{timeStr}</div>
                    </div>
                    <div className={`px-3 py-1 rounded-full text-xs font-semibold ${meta.colorClass}`}>
                      {meta.label}
                    </div>
                  </button>
                );
              })}
            </div>

            {saveError && <ErrorMessage variant="inline" message={saveError} />}
            {saveSuccess && (
              <div className="flex items-center gap-2 text-sm text-jade bg-emerald/10 rounded-lg px-3 py-2">
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                </svg>
                Availability saved!
              </div>
            )}

            <div className="flex items-center justify-between pt-4 border-t border-white/5">
              <Button variant="ghost" size="sm" onClick={() => navigate(`/e/${publicId}/results`)}>
                See results →
              </Button>
              <Button
                variant="primary"
                onClick={handleSave}
                loading={saving}
                success={saveSuccess}
              >
                Save Availability
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
