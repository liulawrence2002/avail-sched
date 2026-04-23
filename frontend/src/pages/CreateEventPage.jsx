import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { createEvent, listTemplates, createTemplate, deleteTemplate, getAIStatus, parseRecurrence, createEventSeries } from '../api.js';
import { saveHostToken } from './DashboardPage.jsx';
import { useAppState } from '../hooks/useAppState';
import Button from '../components/Button';
import Input from '../components/Input';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';
import NLParseBanner from '../components/NLParseBanner';

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
  const detectedTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  const [timezone, setTimezone] = useState(detectedTimezone);
  const [slotMinutes, setSlotMinutes] = useState(30);
  const [durationMinutes, setDurationMinutes] = useState(60);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [dailyStartTime, setDailyStartTime] = useState('09:00');
  const [dailyEndTime, setDailyEndTime] = useState('18:00');
  const [resultsVisibility, setResultsVisibility] = useState('aggregate_public');
  const [location, setLocation] = useState('');
  const [meetingUrl, setMeetingUrl] = useState('');
  const [deadline, setDeadline] = useState('');
  const [autoFinalize, setAutoFinalize] = useState(false);

  const [agentEnabled, setAgentEnabled] = useState(false);

  // Recurrence
  const [recurrenceEnabled, setRecurrenceEnabled] = useState(false);
  const [recurrenceText, setRecurrenceText] = useState('');
  const [recurrenceDates, setRecurrenceDates] = useState([]);
  const [parsingRecurrence, setParsingRecurrence] = useState(false);

  const [scheduleErrors, setScheduleErrors] = useState({});

  // AI status
  const [aiAvailable, setAiAvailable] = useState(null);

  useEffect(() => {
    getAIStatus().then(r => { if (r.ok) setAiAvailable(r.data?.available); });
  }, []);

  // Templates
  const [templates, setTemplates] = useState([]);
  const [showTemplates, setShowTemplates] = useState(false);
  const [templateName, setTemplateName] = useState('');
  const [savingTemplate, setSavingTemplate] = useState(false);

  useEffect(() => {
    if (step !== 2) return;
    listTemplates().then(r => { if (r.ok) setTemplates(r.data); });
  }, [step]);

  const applyTemplate = (t) => {
    if (t.timezone) setTimezone(t.timezone);
    if (t.slotMinutes) setSlotMinutes(t.slotMinutes);
    if (t.durationMinutes) setDurationMinutes(t.durationMinutes);
    if (t.dailyStartTime) setDailyStartTime(t.dailyStartTime);
    if (t.dailyEndTime) setDailyEndTime(t.dailyEndTime);
    if (t.resultsVisibility) setResultsVisibility(t.resultsVisibility);
    if (t.location) setLocation(t.location);
    if (t.meetingUrl) setMeetingUrl(t.meetingUrl);
  };

  const handleSaveTemplate = async () => {
    const name = templateName.trim();
    if (!name) return;
    setSavingTemplate(true);
    const res = await createTemplate({
      name,
      description: description.trim() || undefined,
      timezone,
      slotMinutes: Number(slotMinutes),
      durationMinutes: Number(durationMinutes),
      dailyStartTime,
      dailyEndTime,
      resultsVisibility,
      location: location.trim() || undefined,
      meetingUrl: meetingUrl.trim() || undefined,
    });
    if (res.ok) {
      setTemplates(prev => [res.data, ...prev]);
      setTemplateName('');
    }
    setSavingTemplate(false);
  };

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

  const handleNLParsed = (parsed) => {
    if (parsed.title) setTitle(parsed.title);
    if (parsed.description) setDescription(parsed.description);
    if (parsed.startDate) setStartDate(parsed.startDate);
    if (parsed.endDate) setEndDate(parsed.endDate);
    if (parsed.dailyStartTime) setDailyStartTime(parsed.dailyStartTime);
    if (parsed.dailyEndTime) setDailyEndTime(parsed.dailyEndTime);
    if (parsed.slotMinutes) setSlotMinutes(Number(parsed.slotMinutes));
    if (parsed.durationMinutes) setDurationMinutes(Number(parsed.durationMinutes));
    if (parsed.location) setLocation(parsed.location);
    if (parsed.meetingUrl) setMeetingUrl(parsed.meetingUrl);
  };

  const handleParseRecurrence = async () => {
    if (!recurrenceText.trim()) return;
    setParsingRecurrence(true);
    const res = await parseRecurrence(recurrenceText.trim(), timezone);
    setParsingRecurrence(false);
    if (res.ok && res.data?.dates) {
      setRecurrenceDates(res.data.dates);
    }
  };

  const handleSubmit = async () => {
    if (!validateStep1() || !validateSchedule()) return;

    setLoading(true);
    setApiError(null);

    const eventData = {
      title: title.trim(),
      description: description.trim() || undefined,
      timezone,
      slotMinutes: Number(slotMinutes),
      durationMinutes: Number(durationMinutes),
      startDate,
      endDate,
      dailyStartTime,
      dailyEndTime,
      location: location.trim() || undefined,
      meetingUrl: meetingUrl.trim() || undefined,
      resultsVisibility,
      deadline: deadline ? new Date(deadline).toISOString() : undefined,
      autoFinalize: autoFinalize || undefined,
      agentEnabled: agentEnabled || undefined,
    };

    // Handle recurrence series
    if (recurrenceEnabled && recurrenceDates.length > 0) {
      const seriesResult = await createEventSeries(eventData, recurrenceDates);
      setLoading(false);
      if (!seriesResult.ok) {
        setApiError(seriesResult.error?.message || 'Failed to create event series');
        showError(seriesResult.error?.message || 'Failed to create event series');
        return;
      }
      const events = seriesResult.data;
      if (events.length > 0) {
        events.forEach(e => saveHostToken(e.hostToken, `${title.trim()} (series)`));
        navigate(`/host/${events[0].hostToken}`);
      }
      return;
    }

    const result = await createEvent(eventData);

    setLoading(false);

    if (!result.ok) {
      setApiError(result.error?.message || 'Failed to create event');
      showError(result.error?.message || 'Failed to create event');
      return;
    }

    const { hostToken } = result.data;
    saveHostToken(hostToken, title.trim());
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
            <NLParseBanner onParsed={handleNLParsed} aiAvailable={aiAvailable} />

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

            {/* Templates */}
            {templates.length > 0 && (
              <div className="bg-charcoal/30 border border-white/5 rounded-xl p-4">
                <button
                  onClick={() => setShowTemplates(s => !s)}
                  className="flex items-center gap-2 text-sm font-medium text-gold hover:text-gold-bright transition-colors"
                >
                  <svg className={`w-4 h-4 transition-transform ${showTemplates ? 'rotate-90' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                  </svg>
                  📋 Load from Template ({templates.length})
                </button>
                {showTemplates && (
                  <div className="mt-3 flex flex-wrap gap-2">
                    {templates.map(t => (
                      <div key={t.id} className="flex items-center gap-2 bg-charcoal/60 border border-white/10 rounded-lg px-3 py-2">
                        <button
                          onClick={() => applyTemplate(t)}
                          className="text-sm text-cream hover:text-gold transition-colors"
                          title={`${t.dailyStartTime || '09:00'}-${t.dailyEndTime || '18:00'}, ${t.slotMinutes || 30}min slots`}
                        >
                          {t.name}
                        </button>
                        <button
                          onClick={async () => {
                            await deleteTemplate(t.id);
                            setTemplates(prev => prev.filter(x => x.id !== t.id));
                          }}
                          className="text-xs text-crimson hover:text-crimson-bright"
                          title="Delete template"
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

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
              <label className="text-sm font-medium text-cream-muted tracking-wide">
                Timezone <span className="text-xs text-silver-dim font-normal">(auto-detected)</span>
              </label>
              <input
                type="text"
                value={timezone}
                onChange={(e) => setTimezone(e.target.value)}
                placeholder="America/New_York"
                className="w-full px-4 py-3 rounded-xl bg-charcoal/60 border border-white/10 text-cream placeholder-silver-dim/60 transition-all hover:border-white/20 focus:border-gold/50 focus:outline-none"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Location (optional)"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="e.g., Conference Room A"
              />
              <Input
                label="Meeting URL (optional)"
                type="url"
                value={meetingUrl}
                onChange={(e) => setMeetingUrl(e.target.value)}
                placeholder="https://zoom.us/j/..."
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

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-cream-muted tracking-wide">Response Deadline (optional)</label>
                <input
                  type="datetime-local"
                  value={deadline}
                  onChange={(e) => setDeadline(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-charcoal/60 border border-white/10 text-cream transition-all hover:border-white/20 focus:border-gold/50 focus:outline-none"
                />
              </div>
              <div className="flex items-center">
                <label className="flex items-center gap-2 px-4 py-3 rounded-xl bg-charcoal/40 border border-white/5 cursor-pointer hover:border-white/10 transition-colors">
                  <input
                    type="checkbox"
                    checked={autoFinalize}
                    onChange={(e) => setAutoFinalize(e.target.checked)}
                    className="accent-gold"
                  />
                  <span className="text-sm text-cream">Auto-finalize when deadline hits</span>
                </label>
              </div>
            </div>

            {/* AI Agent toggle */}
            {aiAvailable && (
              <div className="flex items-center">
                <label className="flex items-center gap-2 px-4 py-3 rounded-xl bg-charcoal/40 border border-white/5 cursor-pointer hover:border-white/10 transition-colors">
                  <input
                    type="checkbox"
                    checked={agentEnabled}
                    onChange={(e) => setAgentEnabled(e.target.checked)}
                    className="accent-gold"
                  />
                  <span className="text-sm text-cream">Enable AI Agent</span>
                  <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-gold/15 text-gold uppercase tracking-widest border border-gold/20">AI</span>
                </label>
              </div>
            )}

            {/* Recurrence */}
            {aiAvailable && (
              <div className="bg-charcoal/30 border border-white/5 rounded-xl p-4">
                <label className="flex items-center gap-2 mb-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={recurrenceEnabled}
                    onChange={(e) => setRecurrenceEnabled(e.target.checked)}
                    className="accent-gold"
                  />
                  <span className="text-sm font-medium text-cream-muted">Recurring event?</span>
                  <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-gold/15 text-gold uppercase tracking-widest border border-gold/20">AI</span>
                </label>
                {recurrenceEnabled && (
                  <div className="space-y-2">
                    <div className="flex gap-2">
                      <input
                        type="text"
                        value={recurrenceText}
                        onChange={(e) => setRecurrenceText(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter') handleParseRecurrence(); }}
                        placeholder='e.g., "Every other Friday at 3pm for 6 weeks"'
                        className="flex-1 px-4 py-2 rounded-lg bg-charcoal/60 border border-white/10 text-cream text-sm placeholder-silver-dim/60 focus:outline-none focus:border-gold/50"
                      />
                      <Button variant="goldGhost" size="sm" onClick={handleParseRecurrence} loading={parsingRecurrence} disabled={!recurrenceText.trim()}>
                        Parse
                      </Button>
                    </div>
                    {recurrenceDates.length > 0 && (
                      <div className="space-y-1">
                        <p className="text-xs text-silver-dim">{recurrenceDates.length} occurrences:</p>
                        <div className="flex flex-wrap gap-1">
                          {recurrenceDates.map((d, i) => (
                            <span key={i} className="px-2 py-0.5 rounded text-xs bg-gold/10 text-gold border border-gold/15">
                              {d.startDate}{d.endDate && d.endDate !== d.startDate ? ` → ${d.endDate}` : ''}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {apiError && <ErrorMessage message={apiError} onRetry={handleSubmit} />}

            {/* Save as Template */}
            <div className="bg-charcoal/30 border border-white/5 rounded-xl p-4">
              <p className="text-sm font-medium text-cream-muted mb-2">💾 Save this configuration as a template for future events</p>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={templateName}
                  onChange={e => setTemplateName(e.target.value)}
                  placeholder="Template name..."
                  className="flex-1 px-4 py-2 rounded-lg bg-charcoal/60 border border-white/10 text-cream text-sm placeholder-silver-dim/60 focus:outline-none focus:border-gold/50"
                />
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleSaveTemplate}
                  loading={savingTemplate}
                  disabled={!templateName.trim()}
                >
                  Save Template
                </Button>
              </div>
            </div>

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
