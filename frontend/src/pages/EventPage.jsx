import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";
import Card from "../components/Card";
import CopyButton from "../components/CopyButton";
import TimeGrid from "../components/TimeGrid";
import { t } from "../utils";
import { track } from "../analytics";

export default function EventPage({ copy }) {
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

  const shareLink = `${window.location.origin}/e/${publicId}`;

  async function handleJoin() {
    try {
      const data = await api.joinEvent(publicId, { displayName });
      localStorage.setItem(`participant:${publicId}`, data.participantToken);
      setToken(data.participantToken);
      setStatus(copy.event.joinedStatus);
      track("event_joined");
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleSave() {
    try {
      await api.saveAvailability(publicId, token, {
        items: Object.entries(selections).map(([slotStartUtc, weight]) => ({ slotStartUtc, weight })),
      });
      setStatus(copy.event.savedStatus);
      track("availability_saved");
    } catch (err) {
      setError(err.message);
    }
  }

  if (!event) {
    return <p>{error || copy.event.loading}</p>;
  }

  return (
    <div className="space-y-6">
      <Card className="space-y-3">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-3xl font-black">{event.title}</h1>
            <p className="mt-2 text-slate-700">{event.description || copy.event.noDescription}</p>
          </div>
          <div className="flex items-center gap-2">
            <CopyButton text={shareLink} />
            <Link className="btn rounded-full border px-4 py-2 text-sm font-semibold" to={`/e/${publicId}/results`}>
              {copy.event.viewResults}
            </Link>
          </div>
        </div>
        <p className="text-sm text-slate-600">
          Timezone: {event.timezone} • Duration: {event.durationMinutes} minutes • Views: {event.stats.viewCount} • Responses: {event.stats.responseCount}
        </p>
      </Card>

      {event.finalSelection ? (
        <Card className="space-y-2">
          <h2 className="text-xl font-black">{copy.event.finalizedTitle}</h2>
          <p className="text-slate-700">{new Intl.DateTimeFormat(undefined, {
            weekday: "long",
            month: "short",
            day: "numeric",
            hour: "numeric",
            minute: "2-digit",
            timeZone: event.timezone,
          }).format(new Date(event.finalSelection.slotStartUtc))}</p>
          <a className="btn inline-flex rounded-full border px-4 py-2 text-sm font-semibold" href={api.icsUrl(publicId)}>
            {copy.event.downloadCal}
          </a>
        </Card>
      ) : null}

      {!token ? (
        <Card className="space-y-4">
          <h2 className="text-xl font-black">{copy.event.joinTitle}</h2>
          <input className="input max-w-md" placeholder={copy.event.joinPlaceholder} value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          <button className="btn rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white" onClick={handleJoin}>
            {copy.event.joinButton}
          </button>
        </Card>
      ) : null}

      {token ? (
        <Card className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-xl font-black">{copy.event.availTitle}</h2>
              <p className="text-sm text-slate-600">{t(copy.event.availCount, { count: filledCount })}</p>
            </div>
            <button className="btn rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white" onClick={handleSave}>
              {copy.event.saveButton}
            </button>
          </div>
          <TimeGrid event={event} selections={selections} onChange={setSelections} copy={copy} />
        </Card>
      ) : null}

      {status ? <p className="rounded-2xl bg-emerald-100 px-4 py-3 text-sm text-emerald-800">{status}</p> : null}
      {error ? <p className="rounded-2xl bg-rose-100 px-4 py-3 text-sm text-rose-800">{error}</p> : null}
    </div>
  );
}
