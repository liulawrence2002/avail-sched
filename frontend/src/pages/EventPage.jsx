import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";

import { track } from "../analytics";
import { api } from "../api";
import Card from "../components/Card";
import CopyButton from "../components/CopyButton";
import StatusBanner from "../components/StatusBanner";
import TimeGrid from "../components/TimeGrid";
import { buildParticipantLink, t } from "../utils";

const EVENT_DETAILS = {
  serious: {
    label: "Shared event page",
    joinNote: "Guests join with a name only, then paint the times that work. No account wall, no dashboard detour.",
    gridNote: "Choose a response level, then tap or drag across the schedule. Save anytime and come back later to revise.",
    finalizedNote: "The host locked the schedule, so this page now acts as the final handoff for guests.",
    nextSteps: [
      "Join once with your name.",
      "Fill in the blocks that work best for you.",
      "Save whenever you want. Your response stays attached to this link on this device.",
    ],
  },
  goblin: {
    label: "Shared cave page",
    joinNote: "Guests join with a name only, then smear their availability across the cave wall. No forms of great ceremony.",
    gridNote: "Choose a mood, then tap or drag across the schedule. Save anytime and come back later to edit the etchings.",
    finalizedNote: "The host locked the decree, so this page is now the official cave handoff.",
    nextSteps: [
      "Join once with your goblin name.",
      "Paint the blocks that work for your schedule.",
      "Save and return later if your cave calendar changes.",
    ],
  },
};

export default function EventPage({ copy, mode }) {
  const { publicId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [event, setEvent] = useState(null);
  const [displayName, setDisplayName] = useState("");
  const [participantEmail, setParticipantEmail] = useState("");
  const [token, setToken] = useState(() => localStorage.getItem(storageKey(publicId)) || "");
  const [participantLink, setParticipantLink] = useState("");
  const [selections, setSelections] = useState({});
  const [savedSelections, setSavedSelections] = useState({});
  const [status, setStatus] = useState(null);
  const [error, setError] = useState("");
  const [loadingEvent, setLoadingEvent] = useState(true);
  const [hydratingAvailability, setHydratingAvailability] = useState(false);
  const [joining, setJoining] = useState(false);
  const [saving, setSaving] = useState(false);
  const details = EVENT_DETAILS[mode];

  useEffect(() => {
    setDisplayName("");
    setParticipantEmail("");
    setToken(localStorage.getItem(storageKey(publicId)) || "");
    setParticipantLink("");
  }, [publicId]);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const tokenFromUrl = params.get("token");
    if (!tokenFromUrl) {
      return;
    }

    localStorage.setItem(storageKey(publicId), tokenFromUrl);
    setToken(tokenFromUrl);
    setParticipantLink(buildParticipantLink(publicId, tokenFromUrl));
    setStatus({ tone: "success", message: "Recovered your edit link on this device." });
    navigate(`/e/${publicId}`, { replace: true });
  }, [location.search, navigate, publicId]);

  useEffect(() => {
    let isActive = true;
    setLoadingEvent(true);
    setError("");
    api.getEvent(publicId)
      .then((data) => {
        if (isActive) {
          setEvent(data);
        }
      })
      .catch((err) => {
        if (isActive) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (isActive) {
          setLoadingEvent(false);
        }
      });
    return () => {
      isActive = false;
    };
  }, [publicId]);

  useEffect(() => {
    let isActive = true;
    setSelections({});
    setSavedSelections({});
    if (!token) {
      setHydratingAvailability(false);
      return () => {
        isActive = false;
      };
    }

    setHydratingAvailability(true);
    api.getParticipantAvailability(publicId, token)
      .then((data) => {
        if (!isActive) {
          return;
        }
        const nextSelections = itemsToSelections(data.items);
        setDisplayName(data.displayName);
        setParticipantEmail(data.email || "");
        setSelections(nextSelections);
        setSavedSelections(nextSelections);
        setParticipantLink(buildParticipantLink(publicId, token));
      })
      .catch(() => {
        if (!isActive) {
          return;
        }
        localStorage.removeItem(storageKey(publicId));
        setToken("");
        setDisplayName("");
        setParticipantEmail("");
        setParticipantLink("");
        setStatus({ tone: "info", message: copy.event.sessionExpired });
      })
      .finally(() => {
        if (isActive) {
          setHydratingAvailability(false);
        }
      });

    return () => {
      isActive = false;
    };
  }, [copy.event.sessionExpired, publicId, token]);

  const filledCount = useMemo(() => Object.keys(selections).length, [selections]);
  const hasUnsavedChanges = useMemo(() => !selectionMapsEqual(selections, savedSelections), [savedSelections, selections]);
  const shareLink = `${window.location.origin}/e/${publicId}`;
  const editLink = participantLink || buildParticipantLink(publicId, token);
  const isFinalized = Boolean(event?.finalSelection);
  const canViewPublicResults = event?.resultsVisibility !== "host_only";

  const selectionSummary = useMemo(() => {
    const counts = {
      yes: 0,
      maybe: 0,
      bribe: 0,
    };

    Object.values(selections).forEach((weight) => {
      if (weight >= 0.99) {
        counts.yes += 1;
      } else if (weight >= 0.59) {
        counts.maybe += 1;
      } else if (weight >= 0.29) {
        counts.bribe += 1;
      }
    });

    return counts;
  }, [selections]);

  async function handleJoin() {
    const trimmedName = displayName.trim();
    if (!trimmedName) {
      setError("Please add your name before joining.");
      return;
    }

    setJoining(true);
    setError("");
    try {
      const normalizedEmail = participantEmail.trim();
      const data = await api.joinEvent(publicId, { displayName: trimmedName, email: normalizedEmail || null });
      localStorage.setItem(storageKey(publicId), data.participantToken);
      setDisplayName(trimmedName);
      setSavedSelections({});
      setSelections({});
      setToken(data.participantToken);
      setParticipantEmail(normalizedEmail);
      setParticipantLink(data.participantLink);
      setStatus({
        tone: data.existingParticipant ? "info" : "success",
        message: data.existingParticipant ? "We found your existing response and reopened it here." : copy.event.joinedStatus,
      });
      track("event_joined");
    } catch (err) {
      setError(err.message);
    } finally {
      setJoining(false);
    }
  }

  async function handleSave() {
    if (!token) {
      return;
    }

    setSaving(true);
    setError("");
    try {
      await api.saveAvailability(publicId, token, {
        items: Object.entries(selections).map(([slotStartUtc, weight]) => ({ slotStartUtc, weight })),
      });
      setSavedSelections(selections);
      setStatus({ tone: "success", message: Object.keys(selections).length ? copy.event.savedStatus : copy.event.clearedStatus });
      track("availability_saved", { filledCount: Object.keys(selections).length });
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  function restoreSavedSelections() {
    setSelections(savedSelections);
    setStatus({ tone: "info", message: "Restored your last saved response." });
  }

  if (loadingEvent || !event) {
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
    <div className="route-shell route-shell--event space-y-6">
      <section className="route-hero route-hero--event">
        <div className="route-hero__copy">
          <span className="eyebrow">{details.label}</span>
          <div className="space-y-4">
            <h1 className="display-title display-title-lg">{event.title}</h1>
            <p className="section-kicker">{event.description || copy.event.noDescription}</p>
          </div>

          <div className="pill-row">
            <span className="meta-pill">Timezone {event.timezone}</span>
            <span className="meta-pill">Duration {event.durationMinutes} min</span>
            <span className="meta-pill">Respondents {event.stats.respondentCount}</span>
            <span className="meta-pill">Views {event.stats.viewCount}</span>
          </div>
        </div>

        <div className="route-hero__panel">
          <div className="space-y-3">
            <p className="detail-label">Share this page</p>
            <p className="break-all text-sm font-semibold leading-7 text-[var(--text)]">{shareLink}</p>
            <div className="flex flex-wrap gap-2">
              <CopyButton text={shareLink} />
              {canViewPublicResults ? (
                <Link className="btn btn-secondary rounded-full px-4 py-2 text-sm font-semibold" to={`/e/${publicId}/results`}>
                  {copy.event.viewResults}
                </Link>
              ) : null}
            </div>
          </div>

          <div className="route-note-panel">
            <p className="detail-label">Guest experience</p>
            <p className="text-sm leading-7 text-[var(--muted)]">
              {details.joinNote} {canViewPublicResults ? "Guests can still review the aggregate ranking." : "The host kept the ranking private to the host workspace."}
            </p>
          </div>
        </div>
      </section>

      {isFinalized ? (
        <Card className="route-final-panel space-y-5">
          <span className="eyebrow">{copy.event.finalizedTitle}</span>
          <div className="grid gap-5 lg:grid-cols-[1fr,auto] lg:items-end">
            <div className="space-y-3">
              <h2 className="display-title text-[2.4rem] leading-none">
                {new Intl.DateTimeFormat(undefined, {
                  weekday: "long",
                  month: "short",
                  day: "numeric",
                  hour: "numeric",
                  minute: "2-digit",
                  timeZone: event.timezone,
                }).format(new Date(event.finalSelection.slotStartUtc))}
              </h2>
              <p className="text-sm leading-7 text-[var(--muted)]">{details.finalizedNote}</p>
            </div>
            <div className="flex flex-wrap gap-3">
              <a className="btn btn-primary inline-flex rounded-full px-5 py-3 text-sm font-semibold" href={api.icsUrl(publicId)}>
                {copy.event.downloadCal}
              </a>
              {canViewPublicResults ? (
                <Link className="btn btn-secondary rounded-full px-5 py-3 text-sm font-semibold" to={`/e/${publicId}/results`}>
                  {copy.event.viewResults}
                </Link>
              ) : null}
            </div>
          </div>
        </Card>
      ) : null}

      {!token && !isFinalized ? (
        <section className="route-grid route-grid--event-intro">
          <Card className="space-y-4">
            <span className="eyebrow">{copy.event.joinTitle}</span>
            <h2 className="display-title text-[2.2rem] leading-none">Join the schedule in one step.</h2>
            <p className="text-sm leading-7 text-[var(--muted)]">{details.joinNote}</p>
            <input
              className="input max-w-md"
              placeholder={copy.event.joinPlaceholder}
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
            <input
              className="input max-w-md"
              type="email"
              placeholder="Optional email for easier recovery later"
              value={participantEmail}
              onChange={(e) => setParticipantEmail(e.target.value)}
            />
            <div className="flex flex-wrap items-center gap-3">
              <button className="btn btn-primary rounded-full px-5 py-3 text-sm font-semibold" disabled={joining} onClick={handleJoin}>
                {joining ? "Joining..." : copy.event.joinButton}
              </button>
              <span className="text-sm text-[var(--muted)]">We keep your editing token on this device, and the optional email helps you recover it more easily later.</span>
            </div>
          </Card>

          <Card variant="ghost" className="space-y-4">
            <span className="eyebrow">What happens next</span>
            <div className="space-y-3">
              {details.nextSteps.map((step, index) => (
                <div key={step} className="flex items-start gap-3">
                  <span className="list-number">{index + 1}</span>
                  <p className="pt-1 text-sm leading-7 text-[var(--text)]">{step}</p>
                </div>
              ))}
            </div>
          </Card>
        </section>
      ) : null}

      {token && !isFinalized ? (
        <section className="route-grid route-grid--event-response">
          <Card className="space-y-5">
            <div className="space-y-3">
              <span className="eyebrow">{copy.event.availTitle}</span>
              <h2 className="display-title text-[2.2rem] leading-none">Responding as {displayName || "participant"}.</h2>
              <p className="text-sm leading-7 text-[var(--muted)]">
                {t(copy.event.availCount, { count: filledCount })}. {details.gridNote}
              </p>
            </div>

            {hydratingAvailability ? <StatusBanner tone="info">Loading your saved response...</StatusBanner> : null}

            <TimeGrid event={event} selections={selections} onChange={setSelections} copy={copy} disabled={hydratingAvailability || saving} />
          </Card>

          <aside className="route-sidebar route-sidebar--sticky">
            <Card className="event-action-rail space-y-5">
              <span className="eyebrow">Response status</span>
              <div className="event-action-rail__headline">
                <h3 className="display-title text-[2rem] leading-none">{hasUnsavedChanges ? "Unsaved changes" : "Saved and synced"}</h3>
                <p className="text-sm leading-7 text-[var(--muted)]">
                  {hasUnsavedChanges ? "Your edits are local until you save them." : "Your latest response is stored on this event."}
                </p>
              </div>

              <div className="event-stat-grid">
                <div className="metric-pill">
                  <span className="metric-label">Marked blocks</span>
                  <span className="metric-value">{filledCount}</span>
                  <span className="metric-note">Across the full scheduling window.</span>
                </div>
                <div className="metric-pill">
                  <span className="metric-label">Works well</span>
                  <span className="metric-value">{selectionSummary.yes}</span>
                  <span className="metric-note">Your strongest blocks.</span>
                </div>
                <div className="metric-pill">
                  <span className="metric-label">Can flex</span>
                  <span className="metric-value">{selectionSummary.maybe}</span>
                  <span className="metric-note">Possible with compromise.</span>
                </div>
                <div className="metric-pill">
                  <span className="metric-label">Needs effort</span>
                  <span className="metric-value">{selectionSummary.bribe}</span>
                  <span className="metric-note">Only if needed.</span>
                </div>
              </div>

              <div className="event-action-rail__buttons">
                <button
                  className="btn btn-secondary rounded-full px-4 py-3 text-sm font-semibold"
                  disabled={!hasUnsavedChanges || hydratingAvailability || saving}
                  onClick={restoreSavedSelections}
                  type="button"
                >
                  {copy.grid.restore}
                </button>
                <button
                  className="btn btn-primary rounded-full px-5 py-3 text-sm font-semibold"
                  disabled={hydratingAvailability || saving || !hasUnsavedChanges}
                  onClick={handleSave}
                  type="button"
                >
                  {saving ? "Saving..." : copy.event.saveButton}
                </button>
              </div>

              <div className="route-note-panel">
                <p className="detail-label">Edit-anywhere link</p>
                <p className="break-all text-sm leading-7 text-[var(--muted)]">
                  Keep this link if you want to recover your response on another device.
                </p>
                <div className="mt-3 flex flex-wrap gap-2">
                  <CopyButton text={editLink} />
                  {participantEmail ? <span className="text-sm text-[var(--muted)]">Saved with {participantEmail}.</span> : null}
                </div>
              </div>

              <div className="route-note-panel">
                <p className="detail-label">Working rhythm</p>
                <p className="text-sm leading-7 text-[var(--muted)]">
                  Save whenever you want. Your response stays attached to this link on this device, and clearing the grid now saves as a real empty response.
                </p>
              </div>
            </Card>
          </aside>
        </section>
      ) : null}

      {status ? <StatusBanner tone={status.tone}>{status.message}</StatusBanner> : null}
      {error ? <StatusBanner tone="error">{error}</StatusBanner> : null}
    </div>
  );
}

function storageKey(publicId) {
  return `participant:${publicId}`;
}

function itemsToSelections(items) {
  return Object.fromEntries(items.map(({ slotStartUtc, weight }) => [slotStartUtc, weight]));
}

function selectionMapsEqual(left, right) {
  const leftKeys = Object.keys(left).sort();
  const rightKeys = Object.keys(right).sort();
  if (leftKeys.length !== rightKeys.length) {
    return false;
  }

  return leftKeys.every((key, index) => key === rightKeys[index] && left[key] === right[key]);
}
