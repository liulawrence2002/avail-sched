import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import Card from "../components/Card";
import CopyButton from "../components/CopyButton";
import StatusBanner from "../components/StatusBanner";
import { track } from "../analytics";

const initialForm = {
  title: "",
  description: "",
  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
  slotMinutes: 30,
  durationMinutes: 60,
  startDate: "",
  endDate: "",
  dailyStartTime: "09:00",
  dailyEndTime: "18:00",
  resultsVisibility: "aggregate_public",
};

const CREATE_DETAILS = {
  serious: {
    headerLabel: "Host setup",
    overview: [
      { label: "Public page", value: "Ready for guests", note: "Clean link, no accounts required." },
      { label: "Host workspace", value: "Private finalize flow", note: "Keep the final decision separate." },
      { label: "Final handoff", value: "Calendar export", note: "Lock it once and ship it." },
    ],
    sections: {
      basics: "Event identity",
      cadence: "Time shape",
      window: "Date window",
      hours: "Daily hours",
    },
    asideTitle: "What this creates",
    asideBody: "A premium public response page for guests, plus a private host workspace that locks the final decision once you are ready.",
  },
  goblin: {
    headerLabel: "Quest setup",
    overview: [
      { label: "Public cave", value: "Ready for goblins", note: "One link for the whole horde." },
      { label: "Throne room", value: "Private finalize flow", note: "Only the host gets the decree button." },
      { label: "Final handoff", value: "Royal scroll export", note: "Lock it once and send the calendar." },
    ],
    sections: {
      basics: "Quest identity",
      cadence: "Chaos settings",
      window: "Date window",
      hours: "Daily hours",
    },
    asideTitle: "What this creates",
    asideBody: "A polished public cave page for the horde, plus a private throne room that locks the decree once you pick the winner.",
  },
};

export default function CreatePage({ copy, mode }) {
  const [form, setForm] = useState(initialForm);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const details = CREATE_DETAILS[mode];

  const schedulePreview = useMemo(() => {
    const dateRange = form.startDate && form.endDate ? `${form.startDate} to ${form.endDate}` : "Choose your date range";
    return {
      timezone: form.timezone || "Timezone pending",
      dateRange,
      hours: `${form.dailyStartTime} - ${form.dailyEndTime}`,
      duration: `${form.durationMinutes} minute meeting`,
      results: form.resultsVisibility === "host_only" ? "Host-only ranking" : "Guest-safe ranking",
    };
  }, [form.dailyEndTime, form.dailyStartTime, form.durationMinutes, form.endDate, form.resultsVisibility, form.startDate, form.timezone]);

  async function handleSubmit(event) {
    event.preventDefault();
    const validationError = validateForm(form);
    if (validationError) {
      setError(validationError);
      return;
    }

    setLoading(true);
    setError("");
    try {
      const data = await api.createEvent(form);
      setResult(data);
      track("event_created");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  const publicLink = result ? `${window.location.origin}/e/${result.publicId}` : "";

  return (
    <div className="route-shell route-shell--create space-y-6">
      <section className="route-hero route-hero--create">
        <div className="route-hero__copy">
          <span className="eyebrow">{details.headerLabel}</span>
          <div className="space-y-4">
            <h1 className="display-title display-title-lg">{copy.create.title}</h1>
            <p className="section-kicker">{copy.create.subtitle}</p>
          </div>

          <div className="route-metric-grid">
            {details.overview.map((item) => (
              <div key={item.label} className="metric-pill">
                <span className="metric-label">{item.label}</span>
                <span className="metric-value">{item.value}</span>
                <span className="metric-note">{item.note}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="route-hero__panel">
          <span className="eyebrow">{details.asideTitle}</span>
          <h2 className="display-title text-[2.4rem] leading-none">{copy.create.shareTitle}</h2>
          <p className="text-sm leading-7 text-[var(--muted)]">{details.asideBody}</p>
          <div className="route-detail-grid">
            <MetricTile label="Timezone" value={schedulePreview.timezone} />
            <MetricTile label="Date range" value={schedulePreview.dateRange} />
            <MetricTile label="Daily window" value={schedulePreview.hours} />
            <MetricTile label="Duration" value={schedulePreview.duration} />
            <MetricTile label="Results" value={schedulePreview.results} />
          </div>
        </div>
      </section>

      <section className="route-grid route-grid--create">
        <Card as="form" onSubmit={handleSubmit} className="route-form-panel space-y-8">
          <FormSection title={details.sections.basics}>
            <Field label={copy.create.fieldTitle}>
              <input
                className="input"
                placeholder={copy.create.fieldTitlePlaceholder}
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                required
              />
            </Field>
            <Field label={copy.create.fieldDescription}>
              <textarea
                className="input min-h-28"
                placeholder={copy.create.fieldDescriptionPlaceholder}
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </Field>
            <Field label={copy.create.fieldTimezone} help="Use an IANA timezone like America/New_York.">
              <input className="input" value={form.timezone} onChange={(e) => setForm({ ...form, timezone: e.target.value })} required />
            </Field>
            <Field label="Results visibility" help="Guests always see aggregate counts. Host-only hides the ranking screen from the public link.">
              <select className="input" value={form.resultsVisibility} onChange={(e) => setForm({ ...form, resultsVisibility: e.target.value })}>
                <option value="aggregate_public">Guest-safe ranking</option>
                <option value="host_only">Host only</option>
              </select>
            </Field>
          </FormSection>

          <div className="section-rule" />

          <FormSection title={details.sections.cadence}>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label={copy.create.fieldDuration}>
                <select className="input" value={form.durationMinutes} onChange={(e) => setForm({ ...form, durationMinutes: Number(e.target.value) })}>
                  <option value={30}>30 minutes</option>
                  <option value={60}>60 minutes</option>
                  <option value={90}>90 minutes</option>
                </select>
              </Field>
              <Field label={copy.create.fieldSlotSize} help="Fixed at 30 minutes for this version.">
                <input className="input" value="30 minutes" disabled />
              </Field>
            </div>
          </FormSection>

          <div className="section-rule" />

          <FormSection title={details.sections.window}>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label={copy.create.fieldStartDate}>
                <input className="input" type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} required />
              </Field>
              <Field label={copy.create.fieldEndDate}>
                <input className="input" type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} required />
              </Field>
            </div>
          </FormSection>

          <div className="section-rule" />

          <FormSection title={details.sections.hours}>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label={copy.create.fieldDailyStart}>
                <input className="input" type="time" value={form.dailyStartTime} onChange={(e) => setForm({ ...form, dailyStartTime: e.target.value })} required />
              </Field>
              <Field label={copy.create.fieldDailyEnd}>
                <input className="input" type="time" value={form.dailyEndTime} onChange={(e) => setForm({ ...form, dailyEndTime: e.target.value })} required />
              </Field>
            </div>
          </FormSection>

          {error ? <StatusBanner tone="error">{error}</StatusBanner> : null}

          <div className="flex flex-wrap items-center gap-3">
            <button className="btn btn-primary rounded-full px-6 py-3 text-sm font-semibold" disabled={loading}>
              {loading ? copy.create.submitLoading : copy.create.submitButton}
            </button>
            <span className="text-sm text-[var(--muted)]">Guests get a public page. Hosts get a separate private workspace.</span>
          </div>
        </Card>

        <aside className="route-sidebar route-sidebar--sticky space-y-4">
          <Card className="route-share-panel space-y-5">
            <span className="eyebrow">{copy.create.shareTitle}</span>
            {result ? (
              <>
                <div className="route-link-card">
                  <p className="detail-label">{copy.create.sharePublicLabel}</p>
                  <div className="mt-3 flex flex-wrap items-center gap-2">
                    <Link className="text-sm font-semibold leading-6 underline decoration-[rgba(28,25,23,0.2)] underline-offset-4" to={`/e/${result.publicId}`}>
                      {publicLink}
                    </Link>
                    <CopyButton text={publicLink} />
                  </div>
                </div>

                <div className="route-link-card">
                  <p className="detail-label">{copy.create.shareHostLabel}</p>
                  <div className="mt-3 flex flex-wrap items-center gap-2">
                    <a className="text-sm font-semibold leading-6 underline decoration-[rgba(28,25,23,0.2)] underline-offset-4" href={result.hostLink}>
                      {result.hostLink}
                    </a>
                    <CopyButton text={result.hostLink} />
                  </div>
                </div>

                <div className="flex flex-wrap gap-3">
                  <Link className="btn btn-primary rounded-full px-5 py-3 text-sm font-semibold" to={`/e/${result.publicId}`}>
                    Open public page
                  </Link>
                  <a className="btn btn-secondary rounded-full px-5 py-3 text-sm font-semibold" href={result.hostLink}>
                    Open host workspace
                  </a>
                </div>

                <StatusBanner tone="success">Your event is live. Share the public page widely and keep the host workspace private.</StatusBanner>
              </>
            ) : (
              <p className="text-sm leading-7 text-[var(--muted)]">{copy.create.shareEmpty}</p>
            )}
          </Card>

          <Card variant="ghost" className="space-y-3">
            <span className="eyebrow">Preview</span>
            <h3 className="display-title text-[2rem] leading-none">A cleaner handoff from setup to share.</h3>
            <p className="text-sm leading-7 text-[var(--muted)]">
              The event page, ranking screen, and host workspace now share the same visual system so the product feels related end to end.
            </p>
          </Card>
        </aside>
      </section>
    </div>
  );
}

function validateForm(form) {
  if (!form.title.trim()) {
    return "Add a title before creating the event.";
  }
  if (!form.startDate || !form.endDate) {
    return "Choose both a start date and an end date.";
  }
  if (form.endDate < form.startDate) {
    return "End date must be on or after the start date.";
  }
  if (form.dailyEndTime <= form.dailyStartTime) {
    return "Daily end time must be after the daily start time.";
  }

  try {
    new Intl.DateTimeFormat(undefined, { timeZone: form.timezone }).format(new Date());
  } catch {
    return "Use a valid IANA timezone, like America/New_York.";
  }

  return "";
}

function MetricTile({ label, value }) {
  return (
    <div className="route-detail-tile">
      <p className="detail-label">{label}</p>
      <p className="mt-2 text-sm font-semibold leading-6 text-[var(--text)]">{value}</p>
    </div>
  );
}

function FormSection({ title, children }) {
  return (
    <section className="space-y-4">
      <div>
        <p className="eyebrow">{title}</p>
      </div>
      {children}
    </section>
  );
}

function Field({ label, children, help }) {
  return (
    <label className="block">
      <span className="field-label">{label}</span>
      {children}
      {help ? <span className="field-help">{help}</span> : null}
    </label>
  );
}
