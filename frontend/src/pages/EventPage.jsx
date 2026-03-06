import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";
import Card from "../components/Card";
import TimeGrid from "../components/TimeGrid";

export default function EventPage() {
  const { publicId } = useParams();
  const [event, setEvent] = useState(null);
  const [displayName, setDisplayName] = useState("");
  const [token, setToken] = useState(() => localStorage.getItem(`participant:${publicId}`) || "");
  const [selections, setSelections] = useState({});
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    api.getEvent(publicId).then(setEvent).catch((err) => setError(err.message));
  }, [publicId]);

  const filledCount = useMemo(() => Object.keys(selections).length, [selections]);

  async function handleJoin() {
    try {
      const data = await api.joinEvent(publicId, { displayName });
      localStorage.setItem(`participant:${publicId}`, data.participantToken);
      setToken(data.participantToken);
      setStatus("Participant link minted.");
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleSave() {
    try {
      await api.saveAvailability(publicId, token, {
        items: Object.entries(selections).map(([slotStartUtc, weight]) => ({ slotStartUtc, weight })),
      });
      setStatus("Availability saved.");
    } catch (err) {
      setError(err.message);
    }
  }

  if (!event) {
    return <p>{error || "Loading event..."}</p>;
  }

  return (
    <div className="space-y-6">
      <Card className="space-y-3">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-3xl font-black">{event.title}</h1>
            <p className="mt-2 text-slate-700">{event.description || "No description provided."}</p>
          </div>
          <Link className="rounded-full border px-4 py-2 text-sm font-semibold" to={`/e/${publicId}/results`}>
            View results
          </Link>
        </div>
        <p className="text-sm text-slate-600">
          Timezone: {event.timezone} • Duration: {event.durationMinutes} minutes • Views: {event.stats.viewCount} • Responses: {event.stats.responseCount}
        </p>
      </Card>

      {event.finalSelection ? (
        <Card className="space-y-2">
          <h2 className="text-xl font-black">Finalized time</h2>
          <p className="text-slate-700">{new Intl.DateTimeFormat(undefined, {
            weekday: "long",
            month: "short",
            day: "numeric",
            hour: "numeric",
            minute: "2-digit",
            timeZone: event.timezone,
          }).format(new Date(event.finalSelection.slotStartUtc))}</p>
          <a className="inline-flex rounded-full border px-4 py-2 text-sm font-semibold" href={api.icsUrl(publicId)}>
            Download calendar file
          </a>
        </Card>
      ) : null}

      {!token ? (
        <Card className="space-y-4">
          <h2 className="text-xl font-black">Join this event</h2>
          <input className="input max-w-md" placeholder="Display name" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          <button className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white" onClick={handleJoin}>
            Join and get token
          </button>
        </Card>
      ) : null}

      {token ? (
        <Card className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-xl font-black">Mark your availability</h2>
              <p className="text-sm text-slate-600">{filledCount} slots marked</p>
            </div>
            <button className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white" onClick={handleSave}>
              Save availability
            </button>
          </div>
          <TimeGrid event={event} selections={selections} onChange={setSelections} />
        </Card>
      ) : null}

      {status ? <p className="rounded-2xl bg-emerald-100 px-4 py-3 text-sm text-emerald-800">{status}</p> : null}
      {error ? <p className="rounded-2xl bg-rose-100 px-4 py-3 text-sm text-rose-800">{error}</p> : null}
    </div>
  );
}
