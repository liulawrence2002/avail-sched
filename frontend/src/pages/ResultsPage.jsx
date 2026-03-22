import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";
import Card from "../components/Card";
import StatusBanner from "../components/StatusBanner";
import { formatInstant, t } from "../utils";

const RESULTS_DETAILS = {
  serious: {
    label: "Results view",
    guidance: "Scores combine everyone's response weights across the full event duration, so higher-ranked times are easier for the group to keep.",
  },
  goblin: {
    label: "Goblin ranking board",
    guidance: "Scores combine the cave's response weights across the whole hangout, so higher-ranked times are easier for the horde to keep.",
  },
};

export default function ResultsPage({ copy, mode }) {
  const { publicId } = useParams();
  const [results, setResults] = useState(null);
  const [event, setEvent] = useState(null);
  const [error, setError] = useState("");
  const details = RESULTS_DETAILS[mode];

  useEffect(() => {
    let isActive = true;

    Promise.all([api.getResults(publicId), api.getEvent(publicId)])
      .then(([resultsData, eventData]) => {
        if (!isActive) {
          return;
        }
        setResults(resultsData);
        setEvent(eventData);
      })
      .catch((err) => {
        if (isActive) {
          setError(err.message);
        }
      });

    return () => {
      isActive = false;
    };
  }, [publicId]);

  if (!results || !event) {
    return (
      <div className="loading-shell">
        <Card variant="strong" className="max-w-2xl text-center">
          <span className="eyebrow">{details.label}</span>
          <p className="section-kicker mx-auto">{error || copy.results.loading}</p>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Card variant="strong" className="space-y-5">
        <span className="eyebrow">{details.label}</span>
        <div className="space-y-4">
          <h1 className="display-title display-title-lg">{copy.results.title}</h1>
          <p className="section-kicker">{t(copy.results.participantCount, { count: results.participantCount })}</p>
        </div>
        <div className="pill-row">
          <span className="meta-pill">Timezone {event.timezone}</span>
          <span className="meta-pill">Respondents {event.stats.respondentCount}</span>
          <span className="meta-pill">Candidates {results.topSlots.length}</span>
          <span className="meta-pill">Views {event.stats.viewCount}</span>
        </div>
      </Card>

      <Card variant="ghost" className="space-y-3">
        <span className="eyebrow">How to read this</span>
        <p className="text-sm leading-7 text-[var(--muted)]">{details.guidance}</p>
        {event.finalSelection ? (
          <Link className="btn btn-secondary inline-flex rounded-full px-4 py-2 text-sm font-semibold" to={`/e/${publicId}`}>
            See finalized event page
          </Link>
        ) : null}
      </Card>

      {event.finalSelection ? (
        <StatusBanner tone="success">
          Finalized for {formatInstant(event.finalSelection.slotStartUtc, event.timezone)}.
        </StatusBanner>
      ) : null}

      {results.topSlots.length === 0 ? <StatusBanner tone="info">No scored slots yet. Ask a few more people to respond and check back.</StatusBanner> : null}

      {results.topSlots.map((slot, index) => {
        const isFinal = event.finalSelection?.slotStartUtc === slot.slotStartUtc;
        return (
          <Card key={slot.slotStartUtc} className="space-y-5">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
              <div className="flex items-start gap-4">
                <span className="rank-badge">{String(index + 1).padStart(2, "0")}</span>
                <div className="space-y-2">
                  <p className="detail-label">
                    {copy.results.rankLabel} #{index + 1} {isFinal ? "- Finalized" : index === 0 ? "- Best fit" : ""}
                  </p>
                  <h2 className="display-title text-[2.3rem] leading-none">{formatInstant(slot.slotStartUtc, event.timezone)}</h2>
                </div>
              </div>

              <div className="w-full max-w-sm space-y-3">
                <div className="progress-track">
                  <div className="progress-bar" style={{ width: `${slot.percentOfMax}%` }} />
                </div>
                <div className="flex items-center justify-between gap-3 text-sm text-[var(--muted)]">
                  <span>{slot.percentOfMax}% {copy.results.percentLabel}</span>
                  <span>{copy.results.scoreLabel} {slot.score}</span>
                </div>
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              <MetricCard label="Works well" value={slot.yesCount} note="Strong availability" />
              <MetricCard label="Can flex" value={slot.maybeCount} note="Possible with compromise" />
              <MetricCard label="Needs effort" value={slot.bribeCount} note="Only if needed" />
              <MetricCard label="Unavailable" value={slot.noCount} note="Would conflict" />
            </div>

            <div className="grid gap-4 lg:grid-cols-2">
              <NameGroup label={copy.results.canAttendLabel} names={slot.canAttend} fallback={copy.results.nobodyYet} tone="positive" />
              <NameGroup label={copy.results.cannotAttendLabel} names={slot.cannotAttend} fallback={copy.results.nobody} tone="muted" />
            </div>
          </Card>
        );
      })}
    </div>
  );
}

function MetricCard({ label, value, note }) {
  return (
    <div className="metric-pill">
      <span className="metric-label">{label}</span>
      <span className="metric-value">{value}</span>
      <span className="metric-note">{note}</span>
    </div>
  );
}

function NameGroup({ label, names, fallback, tone }) {
  return (
    <div className="rounded-[1.5rem] border border-[var(--line)] bg-white/50 p-4">
      <p className="detail-label">{label}</p>
      <div className="mt-3 flex flex-wrap gap-2">
        {names.length
          ? names.map((name) => (
              <span key={`${label}-${name}`} className="name-chip" data-tone={tone}>
                {name}
              </span>
            ))
          : <span className="name-chip" data-tone={tone}>{fallback}</span>}
      </div>
    </div>
  );
}
