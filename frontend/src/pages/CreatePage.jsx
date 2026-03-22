import { useState } from "react";
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
};

const CREATE_DETAILS = {
  serious: {
    headerLabel: "Host setup",
    overview: [
      { label: "Host ready", value: "Private finalize link" },
      { label: "Guest ready", value: "Public response page" },
      { label: "Portable", value: "Calendar export after finalize" },
    ],
    sections: {
      basics: "Event identity",
      cadence: "Time shape",
      window: "Date window",
      hours: "Daily hours",
    },
    asideTitle: "What hosts get",
    asideBody: "A private host page for finalization, a public event page for responses, and a cleaner path from link to decision.",
    asidePoints: [
      "Every event gets a shareable public URL.",
      "Hosts keep a private control link for finalizing.",
      "The same design language carries through every route.",
    ],
  },
  goblin: {
    headerLabel: "Quest setup",
    overview: [
      { label: "Host ready", value: "Private throne link" },
      { label: "Guest ready", value: "Public cave page" },
      { label: "Portable", value: "Calendar scroll after finalize" },
    ],
    sections: {
      basics: "Quest identity",
      cadence: "Chaos settings",
      window: "Date window",
      hours: "Daily hours",
    },
    asideTitle: "What hosts get",
    asideBody: "A private throne room for finalizing, a public quest page for the cave, and a much cleaner way to stop scheduling drama.",
    asidePoints: [
      "Every quest gets a public link for the horde.",
      "Hosts keep a private link for decrees.",
      "The whole flow stays polished, even in Goblin Mode.",
    ],
  },
};

export default function CreatePage({ copy, mode }) {
  const [form, setForm] = useState(initialForm);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const details = CREATE_DETAILS[mode];

  async function handleSubmit(event) {
    event.preventDefault();
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
    <div className="space-y-6">
      <section className="grid gap-6 xl:grid-cols-[1.05fr,0.95fr]">
        <Card variant="strong" className="space-y-6">
          <span className="eyebrow">{details.headerLabel}</span>
          <div className="space-y-4">
            <h1 className="display-title display-title-lg">{copy.create.title}</h1>
            <p className="section-kicker">{copy.create.subtitle}</p>
          </div>

          <div className="grid gap-3 sm:grid-cols-3">
            {details.overview.map((item) => (
              <div key={item.label} className="metric-pill">
                <span className="metric-label">{item.label}</span>
                <span className="metric-value">{item.value}</span>
              </div>
            ))}
          </div>
        </Card>

        <Card className="space-y-5">
          <span className="eyebrow">{details.asideTitle}</span>
          <h2 className="display-title text-[2.4rem] leading-none">{copy.create.shareTitle}</h2>
          <p className="text-sm leading-7 text-[var(--muted)]">{details.asideBody}</p>
          <div className="space-y-3">
            {details.asidePoints.map((item, index) => (
              <div key={item} className="flex items-start gap-3">
                <span className="list-number">{index + 1}</span>
                <p className="pt-1 text-sm leading-7 text-[var(--text)]">{item}</p>
              </div>
            ))}
          </div>
        </Card>
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.15fr,0.85fr]">
        <Card as="form" onSubmit={handleSubmit} className="space-y-8">
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
            <Field label={copy.create.fieldTimezone}>
              <input className="input" value={form.timezone} onChange={(e) => setForm({ ...form, timezone: e.target.value })} required />
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
              <Field label={copy.create.fieldSlotSize}>
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
            <span className="text-sm text-[var(--muted)]">Share links appear instantly once the event is created.</span>
          </div>
        </Card>

        <div className="space-y-4">
          <Card className="space-y-5">
            <span className="eyebrow">{copy.create.shareTitle}</span>
            {result ? (
              <>
                <div className="rounded-[1.6rem] border border-[var(--line)] bg-white/60 p-4">
                  <p className="detail-label">{copy.create.sharePublicLabel}</p>
                  <div className="mt-3 flex flex-wrap items-center gap-2">
                    <Link className="text-sm font-semibold leading-6 underline decoration-[rgba(28,25,23,0.2)] underline-offset-4" to={`/e/${result.publicId}`}>
                      {publicLink}
                    </Link>
                    <CopyButton text={publicLink} />
                  </div>
                </div>

                <div className="rounded-[1.6rem] border border-[var(--line)] bg-white/60 p-4">
                  <p className="detail-label">{copy.create.shareHostLabel}</p>
                  <div className="mt-3 flex flex-wrap items-center gap-2">
                    <a className="text-sm font-semibold leading-6 underline decoration-[rgba(28,25,23,0.2)] underline-offset-4" href={result.hostLink}>
                      {result.hostLink}
                    </a>
                    <CopyButton text={result.hostLink} />
                  </div>
                </div>

                <StatusBanner tone="success">Your event is live. Send the public link to guests and keep the host link private.</StatusBanner>
              </>
            ) : (
              <p className="text-sm leading-7 text-[var(--muted)]">{copy.create.shareEmpty}</p>
            )}
          </Card>

          <Card variant="ghost" className="space-y-3">
            <span className="eyebrow">Preview</span>
            <h3 className="display-title text-[2rem] leading-none">A cleaner share moment.</h3>
            <p className="text-sm leading-7 text-[var(--muted)]">
              The create flow now hands off directly into the event, results, and host surfaces so the whole experience feels related from start to finish.
            </p>
          </Card>
        </div>
      </section>
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
