import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../api";
import Card from "../components/Card";
import StatusBanner from "../components/StatusBanner";
import { formatInstant } from "../utils";

const HOST_DETAILS = {
  serious: {
    label: "Host studio",
    recommendation: "Leading recommendation",
    noSlots: "There are no scored slots yet. Invite a few more people to respond before finalizing.",
  },
  goblin: {
    label: "Throne room",
    recommendation: "Best cave guess",
    noSlots: "No scored slots yet. Summon more goblins before declaring the winner.",
  },
};

export default function HostPage({ copy, mode }) {
  const { hostToken } = useParams();
  const [event, setEvent] = useState(null);
  const [results, setResults] = useState(null);
  const [finalData, setFinalData] = useState(null);
  const [error, setError] = useState("");
  const details = HOST_DETAILS[mode];

  useEffect(() => {
    api.getHostEvent(hostToken)
      .then((hostEvent) => {
        setEvent(hostEvent);
        return Promise.all([
          api.getResults(hostEvent.publicId),
          api.getFinal(hostEvent.publicId).catch((err) => {
            if (err.status === 404) {
              return null;
            }
            throw err;
          }),
        ]);
      })
      .then(([resultsData, finalSelection]) => {
        setResults(resultsData);
        setFinalData(finalSelection);
      })
      .catch((err) => setError(err.message));
  }, [hostToken]);

  async function finalize(slotStartUtc) {
    try {
      const finalSelection = await api.finalizeEvent(event.publicId, hostToken, { slotStartUtc });
      setFinalData(finalSelection);
    } catch (err) {
      setError(err.message);
    }
  }

  if (!event || !results) {
    return (
      <div className="loading-shell">
        <Card variant="strong" className="max-w-2xl text-center">
          <span className="eyebrow">{details.label}</span>
          <p className="section-kicker mx-auto">{error || copy.host.loading}</p>
        </Card>
      </div>
    );
  }

  const [topSlot, ...otherSlots] = results.topSlots;

  return (
    <div className="space-y-6">
      <Card variant="strong" className="space-y-5">
        <span className="eyebrow">{details.label}</span>
        <div className="space-y-4">
          <h1 className="display-title display-title-lg">{copy.host.title}</h1>
          <p className="section-kicker">{copy.host.eventLabel}: {event.title}</p>
        </div>
        <div className="pill-row">
          <span className="meta-pill">Timezone {event.timezone}</span>
          <span className="meta-pill">Responses {event.stats.responseCount}</span>
          <span className="meta-pill">Top slots {results.topSlots.length}</span>
        </div>
      </Card>

      {!topSlot ? <StatusBanner tone="info">{details.noSlots}</StatusBanner> : null}

      {topSlot ? (
        <Card className="space-y-5">
          <span className="eyebrow">{details.recommendation}</span>
          <div className="grid gap-5 lg:grid-cols-[1fr,auto] lg:items-end">
            <div className="space-y-4">
              <h2 className="display-title text-[2.6rem] leading-none">{formatInstant(topSlot.slotStartUtc, event.timezone)}</h2>
              <div className="w-full max-w-md space-y-3">
                <div className="progress-track">
                  <div className="progress-bar" style={{ width: `${topSlot.percentOfMax}%` }} />
                </div>
                <div className="flex items-center justify-between gap-3 text-sm text-[var(--muted)]">
                  <span>{topSlot.percentOfMax}% {copy.host.percentLabel}</span>
                  <span>Score {topSlot.score}</span>
                </div>
              </div>
            </div>

            <button className="btn btn-primary rounded-full px-5 py-3 text-sm font-semibold" onClick={() => finalize(topSlot.slotStartUtc)}>
              {copy.host.finalizeButton}
            </button>
          </div>
        </Card>
      ) : null}

      {otherSlots.length ? (
        <div className="grid gap-4">
          {otherSlots.map((slot) => (
            <Card key={slot.slotStartUtc} variant="ghost" className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="detail-label">Alternative</p>
                <h2 className="mt-2 text-xl font-semibold tracking-[-0.03em]">{formatInstant(slot.slotStartUtc, event.timezone)}</h2>
                <p className="mt-2 text-sm text-[var(--muted)]">{slot.percentOfMax}% {copy.host.percentLabel}</p>
              </div>
              <button className="btn btn-secondary rounded-full px-5 py-3 text-sm font-semibold" onClick={() => finalize(slot.slotStartUtc)}>
                {copy.host.finalizeButton}
              </button>
            </Card>
          ))}
        </div>
      ) : null}

      {finalData ? (
        <Card className="space-y-4">
          <span className="eyebrow">{copy.host.finalizedTitle}</span>
          <h2 className="display-title text-[2.2rem] leading-none">{formatInstant(finalData.slotStartUtc, event.timezone)}</h2>
          <a className="btn btn-secondary inline-flex rounded-full px-4 py-2 text-sm font-semibold" href={api.icsUrl(event.publicId)}>
            {copy.host.downloadIcs}
          </a>
        </Card>
      ) : null}

      {error ? <StatusBanner tone="error">{error}</StatusBanner> : null}
    </div>
  );
}
