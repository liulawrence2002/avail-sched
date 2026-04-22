import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createEvent } from '../api.js';
import { useAppState } from '../hooks/useAppState';
import Button from '../components/Button';
import Input from '../components/Input';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';

/**
 * Create Event flow.
 * Collects scheduling parameters and sends them to the backend.
 */
export default function CreateEventPage() {
  const navigate = useNavigate();
  const { showError } = useAppState();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState(null);

  // Form state
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [titleError, setTitleError] = useState('');

  // Scheduling parameters
  const [timezone, setTimezone] = useState('America/New_York');
  const [slotMinutes, setSlotMinutes] = useState(30);
  const [durationMinutes, setDurationMinutes] = useState(60);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [dailyStartTime, setDailyStartTime] = useState('09:00');
  const [dailyEndTime, setDailyEndTime] = useState('18:00');
  const [resultsVisibility, setResultsVisibility] = useState('aggregate_public');

  const [scheduleErrors, setScheduleErrors] = useState({});

  const validateStep1 = () => {
    let valid = true;
    if (!title.trim()) {
      setTitleError('Event title is required');
      valid = false;
    } else if (title.trim().length < 2) {
      setTitleError('Title must be at least 2 characters');
      valid = false;
    } else {
      setTitleError('');
    }
    return valid;
  };

  const validateSchedule = () => {
    const errors = {};
    if (!startDate) errors.startDate = 'Start date is required';
    if (!endDate) errors.endDate = 'End date is required';
    if (startDate && endDate && endDate < startDate) {
      errors.endDate = 'End date must be on or after start date';
    }
    if (!dailyStartTime) errors.dailyStartTime = 'Daily start time is required';
    if (!dailyEndTime) errors.dailyEndTime = 'Daily end time is required';
    if (dailyStartTime && dailyEndTime && dailyEndTime <= dailyStartTime) {
      errors.dailyEndTime = 'Daily end time must be after start time';
    }
    if (durationMinutes % slotMinutes !== 0) {
      errors.durationMinutes = `Duration must be divisible by slot size (${slotMinutes} min)`;
    }
    setScheduleErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validateStep1() || !validateSchedule()) return;

    setLoading(true);
    setApiError(null);

    const result = await createEvent({
      title: title.trim(),
      description: description.trim() || undefined,
      timezone,
      slotMinutes: Number(slotMinutes),
      durationMinutes: Number(durationMinutes),
      startDate,
      endDate,
      dailyStartTime,
      dailyEndTime,
      resultsVisibility,
    });

    setLoading(false);

    if (!result.ok) {
      setApiError(result.error?.message || 'Failed to create event');
      showError(result.error?.message || 'Failed to create event');
      return;
    }

    const { hostToken } = result.data;
    // Navigate to host workspace
    navigate(`/host/${hostToken}`);
  };

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12">
        <LoadingState message="The goblins are forging your event..." />
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 sm:py-12">
      <Card padding="xl" border="gold" shadow="stage">
        {/* Progress */}
        <div className="flex items-center gap-2 mb-8">
          <div className={`h-1.5 flex-1 rounded-full ${step >= 1 ? 'bg-gold' : 'bg-charcoal-light'}`} />
          <div className={`h-1.5 flex-1 rounded-full ${step >= 2 ? 'bg-gold' : 'bg-charcoal-light'}`} />
          <span className="text-xs text-silver-dim ml-2">Step {step} of 2</span>
        </div>

        {step === 1 && (
          <div className="space-y-6">
            <div>
              <h2 className="font-display text-2xl sm:text-3xl text-cream mb-2">What's the occasion?</h2>
              <p className="text-silver text-sm">Give your event a name. You can add details too.</p>
            </div>

            <Input
              label="Event Title"
              value={title}
              onChange={(e) => {
                setTitle(e.target.value);
                if (titleError) setTitleError('');
              }}
              error={titleError}
              placeholder="e.g., Dragon's Lair Raid Night"
              required
            />

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-cream-muted tracking-wide">Description</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Optional details about your event..."
                rows={3}
                className="w-full px-4 py-3 rounded-xl bg-charcoal/60 border border-white/10 text-cream placeholder-silver-dim/60 transition-all hover:border-white/20 focus:border-gold/50 focus:bg-charcoal-light/60 resize-none"
              />
            </div>

            <div className="flex justify-end">
              <Button
                variant="primary"
                onClick={() => {
                  if (validateStep1()) setStep(2);
                }}
              >
                Next: Set Schedule →
              </Button>
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-6">
            <div>
              <h2 className="font-display text-2xl sm:text-3xl text-cream mb-2">Set the schedule</h2>
              <p className="text-silver text-sm">
                Define the date range and daily window. The backend will generate all candidate slots automatically.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Start Date"
                type="date"
                value={startDate}
                onChange={(e) => {
                  setStartDate(e.target.value);
                  setScheduleErrors((prev) => ({ ...prev, startDate: '' }));
                }}
                error={scheduleErrors.startDate}
                required
              />
              <Input
                label="End Date"
                type="date"
                value={endDate}
                onChange={(e) => {
                  setEndDate(e.target.value);
                  setScheduleErrors((prev) => ({ ...prev, endDate: '' }));
                }}
                error={scheduleErrors.endDate}
                required
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Daily Start Time"
                type="time"
                value={dailyStartTime}
                onChange={(e) => {
                  setDailyStartTime(e.target.value);
                  setScheduleErrors((prev) => ({ ...prev, dailyStartTime: '' }));
                }}
                error={scheduleErrors.dailyStartTime}
                required
              />
              <Input
                label="Daily End Time"
                type="time"
                value={dailyEndTime}
                onChange={(e) => {
                  setDailyEndTime(e.target.value);
                  setScheduleErrors((prev) => ({ ...prev, dailyEndTime: '' }));
                }}
                error={scheduleErrors.dailyEndTime}
                required
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-cream-muted tracking-wide">Slot Size</label>
                <select
                  value={slotMinutes}
                  onChange={(e) => {
                    setSlotMinutes(Number(e.target.value));
                    setScheduleErrors((prev) => ({ ...prev, durationMinutes: '' }));
                  }}
                  className="w-full px-4 py-3 rounded-xl bg-charcoal/60 border border-white/10 text-cream transition-all hover:border-white/20 focus:border-gold/50 focus:outline-none"
                >
                  <option value={15}>15 minutes</option>
                  <option value={30}>30 minutes</option>
                  <option value={60}>60 minutes</option>
                </select>
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-cream-muted tracking-wide">Duration</label>
                <select
                  value={durationMinutes}
                  onChange={(e) => {
                    setDurationMinutes(Number(e.target.value));
                    setScheduleErrors((prev) => ({ ...prev, durationMinutes: '' }));
                  }}
                  className={`w-full px-4 py-3 rounded-xl bg-charcoal/60 border transition-all hover:border-white/20 focus:border-gold/50 focus:outline-none text-cream ${scheduleErrors.durationMinutes ? 'border-crimson/50' : 'border-white/10'}`}
                >
                  <option value={30}>30 minutes</option>
                  <option value={60}>60 minutes</option>
                  <option value={90}>90 minutes</option>
                </select>
                {scheduleErrors.durationMinutes && (
                  <p className="text-xs text-crimson flex items-center gap-1">
                    <svg className="h-3.5 w-3.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    {scheduleErrors.durationMinutes}
                  </p>
                )}
              </div>
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-cream-muted tracking-wide">Timezone</label>
              <input
                type="text"
                value={timezone}
                onChange={(e) => setTimezone(e.target.value)}
                placeholder="America/New_York"
                className="w-full px-4 py-3 rounded-xl bg-charcoal/60 border border-white/10 text-cream placeholder-silver-dim/60 transition-all hover:border-white/20 focus:border-gold/50 focus:outline-none"
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-cream-muted tracking-wide">Results Visibility</label>
              <div className="flex gap-3">
                <label className="flex items-center gap-2 px-4 py-3 rounded-xl bg-charcoal/40 border border-white/5 cursor-pointer hover:border-white/10 transition-colors">
                  <input
                    type="radio"
                    name="resultsVisibility"
                    value="aggregate_public"
                    checked={resultsVisibility === 'aggregate_public'}
                    onChange={(e) => setResultsVisibility(e.target.value)}
                    className="accent-gold"
                  />
                  <span className="text-sm text-cream">Public aggregate</span>
                </label>
                <label className="flex items-center gap-2 px-4 py-3 rounded-xl bg-charcoal/40 border border-white/5 cursor-pointer hover:border-white/10 transition-colors">
                  <input
                    type="radio"
                    name="resultsVisibility"
                    value="host_only"
                    checked={resultsVisibility === 'host_only'}
                    onChange={(e) => setResultsVisibility(e.target.value)}
                    className="accent-gold"
                  />
                  <span className="text-sm text-cream">Host only</span>
                </label>
              </div>
            </div>

            {apiError && <ErrorMessage message={apiError} onRetry={handleSubmit} />}

            <div className="flex justify-between pt-4 border-t border-white/5">
              <Button variant="ghost" onClick={() => setStep(1)}>
                ← Back
              </Button>
              <Button variant="primary" onClick={handleSubmit}>
                Create Event
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
