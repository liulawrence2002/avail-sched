import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../api";
import Card from "../components/Card";
import { formatInstant, t } from "../utils";

export default function ResultsPage({ copy }) {
  const { publicId } = useParams();
  const [results, setResults] = useState(null);
  const [event, setEvent] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api.getResults(publicId), api.getEvent(publicId)])
      .then(([resultsData, eventData]) => {
        setResults(resultsData);
        setEvent(eventData);
      })
      .catch((err) => setError(err.message));
  }, [publicId]);

  if (!results || !event) {
    return <p>{error || copy.results.loading}</p>;
  }

  return (
    <div className="space-y-4">
      <Card>
        <h1 className="text-3xl font-black">{copy.results.title}</h1>
        <p className="mt-2 text-slate-700">{t(copy.results.participantCount, { count: results.participantCount })}</p>
      </Card>
      {results.topSlots.map((slot, index) => (
        <Card key={slot.slotStartUtc} className="space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.25em] text-slate-500">{copy.results.rankLabel} #{index + 1}</p>
              <h2 className="text-2xl font-black">{formatInstant(slot.slotStartUtc, event.timezone)}</h2>
            </div>
            <div className="rounded-3xl bg-slate-950 px-4 py-3 text-white">
              <div className="text-sm">{slot.percentOfMax}% {copy.results.percentLabel}</div>
              <div className="text-xs opacity-70">{copy.results.scoreLabel} {slot.score}</div>
            </div>
          </div>
          <p className="text-sm text-slate-700">
            Yes {slot.yesCount} • Maybe {slot.maybeCount} • Snacks {slot.bribeCount} • No {slot.noCount}
          </p>
          <p className="text-sm text-slate-700">{copy.results.canAttendLabel}: {slot.canAttend.join(", ") || copy.results.nobodyYet}</p>
          <p className="text-sm text-slate-700">{copy.results.cannotAttendLabel}: {slot.cannotAttend.join(", ") || copy.results.nobody}</p>
        </Card>
      ))}
    </div>
  );
}
