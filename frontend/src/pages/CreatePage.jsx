import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import Card from "../components/Card";

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

export default function CreatePage() {
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
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr,0.8fr]">
      <Card>
        <h1 className="text-3xl font-black">Create a Hangout</h1>
        <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
          <Field label="Title"><input className="input" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></Field>
          <Field label="Description"><textarea className="input min-h-24" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
          <Field label="Timezone"><input className="input" value={form.timezone} onChange={(e) => setForm({ ...form, timezone: e.target.value })} required /></Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Duration">
              <select className="input" value={form.durationMinutes} onChange={(e) => setForm({ ...form, durationMinutes: Number(e.target.value) })}>
                <option value={30}>30 minutes</option>
                <option value={60}>60 minutes</option>
                <option value={90}>90 minutes</option>
              </select>
            </Field>
            <Field label="Slot size">
              <input className="input" value="30 minutes" disabled />
            </Field>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Start date"><input className="input" type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} required /></Field>
            <Field label="End date"><input className="input" type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} required /></Field>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Daily start"><input className="input" type="time" value={form.dailyStartTime} onChange={(e) => setForm({ ...form, dailyStartTime: e.target.value })} required /></Field>
            <Field label="Daily end"><input className="input" type="time" value={form.dailyEndTime} onChange={(e) => setForm({ ...form, dailyEndTime: e.target.value })} required /></Field>
          </div>
          {error ? <p className="rounded-2xl bg-rose-100 px-4 py-3 text-sm text-rose-800">{error}</p> : null}
          <button className="rounded-full bg-slate-950 px-6 py-3 text-sm font-semibold text-white" disabled={loading}>
            {loading ? "Summoning..." : "Create event"}
          </button>
        </form>
      </Card>
      <Card className="space-y-4">
        <h2 className="text-xl font-black">Share links</h2>
        {result ? (
          <>
            <p>Public event: <Link className="underline" to={`/e/${result.publicId}`}>{`${window.location.origin}/e/${result.publicId}`}</Link></p>
            <p>Host link: <a className="underline" href={result.hostLink}>{result.hostLink}</a></p>
          </>
        ) : (
          <p>Create an event to get a public link and a private host finalize link.</p>
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
