import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../api";
import Card from "../components/Card";
import { formatInstant } from "../utils";

export default function HostPage({ copy }) {
  const { hostToken } = useParams();
  const [event, setEvent] = useState(null);
  const [results, setResults] = useState(null);
  const [finalData, setFinalData] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.getHostEvent(hostToken)
      .then((hostEvent) => {
        setEvent(hostEvent);
        return Promise.all([api.getResults(hostEvent.publicId), api.getFinal(hostEvent.publicId).catch(() => null)]);
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
    return <p>{error || copy.host.loading}</p>;
  }

  return (
    <div className="space-y-4">
      <Card>
        <h1 className="text-3xl font-black">{copy.host.title}</h1>
        <p className="mt-2 text-slate-700">{copy.host.eventLabel}: {event.title}</p>
      </Card>
      {results.topSlots.map((slot) => (
        <Card key={slot.slotStartUtc} className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-black">{formatInstant(slot.slotStartUtc, event.timezone)}</h2>
            <p className="text-sm text-slate-700">{slot.percentOfMax}% {copy.host.percentLabel}</p>
          </div>
          <button className="btn rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white" onClick={() => finalize(slot.slotStartUtc)}>
            {copy.host.finalizeButton}
          </button>
        </Card>
      ))}
      {finalData ? (
        <Card className="space-y-3">
          <h2 className="text-2xl font-black">{copy.host.finalizedTitle}</h2>
          <p>{formatInstant(finalData.slotStartUtc, event.timezone)}</p>
          <a className="btn inline-flex rounded-full border px-4 py-2 text-sm font-semibold" href={api.icsUrl(event.publicId)}>
            {copy.host.downloadIcs}
          </a>
        </Card>
      ) : null}
      {error ? <p className="rounded-2xl bg-rose-100 px-4 py-3 text-sm text-rose-800">{error}</p> : null}
    </div>
  );
}
