import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";
import Card from "../components/Card";
import CopyButton from "../components/CopyButton";
import StatusBanner from "../components/StatusBanner";
import TimeGrid from "../components/TimeGrid";
import { t } from "../utils";
import { track } from "../analytics";

const EVENT_DETAILS = {
  serious: {
    label: "Shared event page",
    joinNote: "Guests can join with a name only. No account wall, no extra setup.",
    gridNote: "Tap or drag across the schedule to paint availability. Your latest save becomes the current response.",
  },
  goblin: {
    label: "Shared quest page",
    joinNote: "Guests can join with a name only. No account wall, no ceremonial paperwork.",
    gridNote: "Tap or drag across the schedule to paint availability. Save when your goblin instincts feel right.",
  },
};

export default function EventPage({ copy, mode }) {
  const { publicId } = useParams();
  const [event, setEvent] = useState(null);
  const [displayName, setDisplayName] = useState("");
  const [token, setToken] = useState(() => localStorage.getItem(`participant:${publicId}`) || "");
  const [selections, setSelections] = useState({});
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const details = EVENT_DETAILS[mode];

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
    return (
      <div className="loading-shell">
        <Card variant="strong" className="max-w-2xl text-center">
          <span className="eyebrow">{details.label}</span>
          <p className="section-kicker mx-auto">{error || copy.event.loading}</p>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <section className="surface-card surface-strong overflow-hidden px-6 py-8 md:px-8 md:py-10">
        <div className="grid gap-6 lg:grid-cols-[1.1fr,0.9fr] lg:items-end">
          <div className="space-y-5">
            <span className="eyebrow">{details.label}</span>
            <div className="space-y-4">
              <h1 className="display-title display-title-lg">{event.title}</h1>
              <p className="section-kicker">{event.description || copy.event.noDescription}</p>
            </div>

            <div className="pill-row">
              <span className="meta-pill">Timezone {event.timezone}</span>
              <span className="meta-pill">Duration {event.durationMinutes} min</span>
              <span className="meta-pill">Views {event.stats.viewCount}</span>
              <span className="meta-pill">Responses {event.stats.responseCount}</span>
            </div>
          </div>

          <div className="grid gap-4">
            <Card variant="ghost" className="space-y-3 p-5 md:p-6">
              <p className="detail-label">Share this event</p>
              <p className="break-all text-sm font-semibold leading-7 text-[var(--text)]">{shareLink}</p>
              <div className="flex flex-wrap gap-2">
                <CopyButton text={shareLink} />
                <Link className="btn btn-secondary rounded-full px-4 py-2 text-sm font-semibold" to={`/e/${publicId}/results`}>
                  {copy.event.viewResults}
                </Link>
              </div>
            </Card>

            <Card variant="ghost" className="space-y-2 p-5 md:p-6">
              <p className="detail-label">Participant experience</p>
              <p className="text-sm leading-7 text-[var(--muted)]">{details.joinNote}</p>
            </Card>
          </div>
        </div>
      </section>

      {event.finalSelection ? (
        <Card className="space-y-4">
          <span className="eyebrow">{copy.event.finalizedTitle}</span>
          <div className="grid gap-4 md:grid-cols-[1fr,auto] md:items-end">
            <div>
              <div className="display-title text-[2.2rem] leading-none">
                {new Intl.DateTimeFormat(undefined, {
                  weekday: "long",
                  month: "short",
                  day: "numeric",
                  hour: "numeric",
                  minute: "2-digit",
                  timeZone: event.timezone,
                }).format(new Date(event.finalSelection.slotStartUtc))}
              </div>
              <p className="mt-3 text-sm leading-7 text-[var(--muted)]">The host has already finalized this time, so guests can move straight into calendar mode.</p>
            </div>
          </div>
          <a className="btn btn-secondary inline-flex rounded-full px-4 py-2 text-sm font-semibold" href={api.icsUrl(publicId)}>
            {copy.event.downloadCal}
          </a>
        </Card>
      ) : null}

      {!token ? (
        <section className="grid gap-6 lg:grid-cols-[0.95fr,1.05fr]">
          <Card className="space-y-4">
            <span className="eyebrow">{copy.event.joinTitle}</span>
            <h2 className="display-title text-[2.2rem] leading-none">Add your name and step inside.</h2>
            <p className="text-sm leading-7 text-[var(--muted)]">{details.joinNote}</p>
            <input className="input max-w-md" placeholder={copy.event.joinPlaceholder} value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
            <button className="btn btn-primary rounded-full px-5 py-3 text-sm font-semibold" onClick={handleJoin}>
              {copy.event.joinButton}
            </button>
          </Card>

          <Card variant="ghost" className="space-y-4">
            <span className="eyebrow">What happens next</span>
            <div className="space-y-3">
              <div className="flex items-start gap-3">
                <span className="list-number">1</span>
                <p className="pt-1 text-sm leading-7 text-[var(--text)]">Join once with a display name.</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="list-number">2</span>
                <p className="pt-1 text-sm leading-7 text-[var(--text)]">Tap or drag across the schedule to mark availability.</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="list-number">3</span>
                <p className="pt-1 text-sm leading-7 text-[var(--text)]">Save your response and come back any time to revise it.</p>
              </div>
            </div>
          </Card>
        </section>
      ) : null}

      {token ? (
        <Card className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <span className="eyebrow">{copy.event.availTitle}</span>
              <h2 className="display-title mt-3 text-[2.2rem] leading-none">Paint your best windows.</h2>
              <p className="mt-3 text-sm leading-7 text-[var(--muted)]">
                {t(copy.event.availCount, { count: filledCount })}. {details.gridNote}
              </p>
            </div>
            <button className="btn btn-primary rounded-full px-5 py-3 text-sm font-semibold" onClick={handleSave}>
              {copy.event.saveButton}
            </button>
          </div>
          <TimeGrid event={event} selections={selections} onChange={setSelections} copy={copy} />
        </Card>
      ) : null}

      {status ? <StatusBanner tone="success">{status}</StatusBanner> : null}
      {error ? <StatusBanner tone="error">{error}</StatusBanner> : null}
    </div>
  );
}
