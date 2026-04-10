import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { api } from "../api";
import Card from "../components/Card";
import StatusBanner from "../components/StatusBanner";
import { formatInstant } from "../utils";

const HOST_DETAILS = {
  serious: {
    label: "Host workspace",
    recommendation: "Recommended winner",
    noSlots: "There are no scored slots yet. Invite a few more people to respond before locking the schedule.",
    lockedNote: "Finalization is intentionally one-way here. Once the time is locked, this page becomes the final handoff and export surface.",
    confirmMessage: "Finalize this time? This locks the schedule and turns the page into a final handoff.",
  },
  goblin: {
    label: "Throne room",
    recommendation: "Best cave guess",
    noSlots: "No scored slots yet. Summon a few more goblins before locking the decree.",
    lockedNote: "This decree is one-way on purpose. Once crowned, the page becomes the official cave handoff.",
    confirmMessage: "Crown this time? Once you do, the decree is locked.",
  },
};

export default function HostPage({ copy, mode }) {
  const { hostToken } = useParams();
  const [event, setEvent] = useState(null);
  const [results, setResults] = useState(null);
  const [finalData, setFinalData] = useState(null);
  const [error, setError] = useState("");
  const [finalizingSlot, setFinalizingSlot] = useState("");
  const details = HOST_DETAILS[mode];

  useEffect(() => {
    let isActive = true;

    Promise.all([api.getHostEvent(hostToken), api.getHostResults(hostToken)])
      .then(([hostEvent, resultsData]) => {
        if (!isActive) {
          return;
        }
        setEvent(hostEvent);
        setFinalData(hostEvent.finalSelection);
        setResults(resultsData);
      })
      .catch((err) => {
        if (isActive) {
          setError(err.message);
        }
      });

    return () => {
      isActive = false;
    };
  }, [hostToken]);

  async function finalize(slotStartUtc) {
    if (!event || finalData) {
      return;
    }

    const confirmed = window.confirm(details.confirmMessage);
    if (!confirmed) {
      return;
    }

    setError("");
    setFinalizingSlot(slotStartUtc);
    try {
      const selection = await api.finalizeEvent(event.publicId, hostToken, { slotStartUtc });
      setFinalData(selection);
    } catch (err) {
      if (err.status === 409) {
        try {
          const existingSelection = await api.getFinal(event.publicId);
          setFinalData(existingSelection);
        } catch {
          // Keep the conflict message even if the follow-up fetch fails.
        }
        setError(err.message);
      } else {
        setError(err.message);
      }
    } finally {
      setFinalizingSlot("");
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
    <div className="route-shell route-shell--host space-y-6">
      <section className="route-hero route-hero--host">
        <div className="route-hero__copy">
          <span className="eyebrow">{details.label}</span>
          <div className="space-y-4">
            <h1 className="display-title display-title-lg">{copy.host.title}</h1>
            <p className="section-kicker">{copy.host.eventLabel}: {event.title}</p>
          </div>
          <div className="pill-row">
            <span className="meta-pill">Timezone {event.timezone}</span>
            <span className="meta-pill">Respondents {event.stats.respondentCount}</span>
            <span className="meta-pill">Views {event.stats.viewCount}</span>
            <span className="meta-pill">Top slots {results.topSlots.length}</span>
          </div>
        </div>

        <div className="route-hero__panel">
          <div className="route-note-panel">
            <p className="detail-label">Decision posture</p>
            <p className="text-sm leading-7 text-[var(--muted)]">
              Review the ranked windows here, then lock one winner. After finalization, this page becomes the handoff and export surface.
            </p>
          </div>
        </div>
      </section>

      {finalData ? (
        <Card className="route-final-panel space-y-5">
          <span className="eyebrow">{copy.host.finalizedTitle}</span>
          <div className="grid gap-5 lg:grid-cols-[1fr,auto] lg:items-end">
            <div className="space-y-3">
              <h2 className="display-title text-[2.4rem] leading-none">{formatInstant(finalData.slotStartUtc, event.timezone)}</h2>
              <p className="text-sm leading-7 text-[var(--muted)]">{details.lockedNote}</p>
            </div>
            <div className="flex flex-wrap gap-3">
              <a className="btn btn-primary inline-flex rounded-full px-5 py-3 text-sm font-semibold" href={api.icsUrl(event.publicId)}>
                {copy.host.downloadIcs}
              </a>
              {event.resultsVisibility !== "host_only" ? (
                <Link className="btn btn-secondary rounded-full px-5 py-3 text-sm font-semibold" to={`/e/${event.publicId}/results`}>
                  View public ranking
                </Link>
              ) : null}
            </div>
          </div>
        </Card>
      ) : null}

      {!topSlot ? <StatusBanner tone="info">{details.noSlots}</StatusBanner> : null}

      {topSlot && !finalData ? (
        <Card className="host-lead-card space-y-5">
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

            <button
              className="btn btn-primary rounded-full px-5 py-3 text-sm font-semibold"
              disabled={Boolean(finalizingSlot)}
              onClick={() => finalize(topSlot.slotStartUtc)}
            >
              {finalizingSlot === topSlot.slotStartUtc ? "Finalizing..." : copy.host.finalizeButton}
            </button>
          </div>

          {results.participantDetailsVisible ? (
            <div className="grid gap-4 lg:grid-cols-2">
              <NameGroup label="Can attend" names={topSlot.canAttend} fallback="Nobody yet" tone="positive" />
              <NameGroup label="Would need compromise" names={topSlot.cannotAttend} fallback="Nobody" tone="muted" />
            </div>
          ) : null}
        </Card>
      ) : null}

      {otherSlots.length && !finalData ? (
        <div className="grid gap-4">
          {otherSlots.map((slot) => (
            <Card key={slot.slotStartUtc} variant="ghost" className="host-option-card space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="detail-label">Alternative</p>
                  <h2 className="mt-2 text-xl font-semibold tracking-[-0.03em]">{formatInstant(slot.slotStartUtc, event.timezone)}</h2>
                  <p className="mt-2 text-sm text-[var(--muted)]">{slot.percentOfMax}% {copy.host.percentLabel}</p>
                </div>
                <button
                  className="btn btn-secondary rounded-full px-5 py-3 text-sm font-semibold"
                  disabled={Boolean(finalizingSlot)}
                  onClick={() => finalize(slot.slotStartUtc)}
                >
                  {finalizingSlot === slot.slotStartUtc ? "Finalizing..." : copy.host.finalizeButton}
                </button>
              </div>

              {results.participantDetailsVisible ? (
                <div className="grid gap-4 lg:grid-cols-2">
                  <NameGroup label="Can attend" names={slot.canAttend} fallback="Nobody yet" tone="positive" />
                  <NameGroup label="Would need compromise" names={slot.cannotAttend} fallback="Nobody" tone="muted" />
                </div>
              ) : null}
            </Card>
          ))}
        </div>
      ) : null}

      {error ? <StatusBanner tone={finalData ? "info" : "error"}>{error}</StatusBanner> : null}
      {event.resultsVisibility === "host_only" ? <StatusBanner tone="info">Public ranking is hidden for this event. Only the host workspace shows attendee details.</StatusBanner> : null}
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
