import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import Card from "../components/Card";
import CopyButton from "../components/CopyButton";
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

export default function CreatePage({ copy }) {
  const [form, setForm] = useState(initialForm);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

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
    <div className="grid gap-6 lg:grid-cols-[1fr,0.8fr]">
      <Card>
        <h1 className="text-3xl font-black">{copy.create.title}</h1>
        <p className="mt-2 text-slate-700">{copy.create.subtitle}</p>
        <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
          <Field label={copy.create.fieldTitle}><input className="input" placeholder={copy.create.fieldTitlePlaceholder} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></Field>
          <Field label={copy.create.fieldDescription}><textarea className="input min-h-24" placeholder={copy.create.fieldDescriptionPlaceholder} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
          <Field label={copy.create.fieldTimezone}><input className="input" value={form.timezone} onChange={(e) => setForm({ ...form, timezone: e.target.value })} required /></Field>
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
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label={copy.create.fieldStartDate}><input className="input" type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} required /></Field>
            <Field label={copy.create.fieldEndDate}><input className="input" type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} required /></Field>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label={copy.create.fieldDailyStart}><input className="input" type="time" value={form.dailyStartTime} onChange={(e) => setForm({ ...form, dailyStartTime: e.target.value })} required /></Field>
            <Field label={copy.create.fieldDailyEnd}><input className="input" type="time" value={form.dailyEndTime} onChange={(e) => setForm({ ...form, dailyEndTime: e.target.value })} required /></Field>
          </div>
          {error ? <p className="rounded-2xl bg-rose-100 px-4 py-3 text-sm text-rose-800">{error}</p> : null}
          <button className="btn rounded-full bg-slate-950 px-6 py-3 text-sm font-semibold text-white" disabled={loading}>
            {loading ? copy.create.submitLoading : copy.create.submitButton}
          </button>
        </form>
      </Card>
      <Card className="space-y-4">
        <h2 className="text-xl font-black">{copy.create.shareTitle}</h2>
        {result ? (
          <>
            <div className="flex flex-wrap items-center gap-2">
              <p>{copy.create.sharePublicLabel}: <Link className="underline" to={`/e/${result.publicId}`}>{publicLink}</Link></p>
              <CopyButton text={publicLink} />
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <p>{copy.create.shareHostLabel}: <a className="underline" href={result.hostLink}>{result.hostLink}</a></p>
              <CopyButton text={result.hostLink} />
            </div>
          </>
        ) : (
          <p>{copy.create.shareEmpty}</p>
        )}
      </Card>
    </div>
  );
}

function Field({ label, children }) {
  return (
    <label className="block space-y-2">
      <span className="text-sm font-semibold">{label}</span>
      {children}
    </label>
  );
}
