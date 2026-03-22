import { Link } from "react-router-dom";
import Card from "../components/Card";

const LANDING_DETAILS = {
  serious: {
    metrics: [
      { label: "Share once", value: "No accounts", note: "A single link handles the whole group." },
      { label: "Signals", value: "Weighted responses", note: "See enthusiasm, flexibility, and hard no's." },
      { label: "Finish", value: "Calendar ready", note: "Pick the winner and export in one step." },
    ],
    storyTitle: "A calmer way to get everyone to yes.",
    storyBody:
      "Borrow the warmth of a premium product page, then give it a job to do. Goblin Scheduler keeps the flow simple, tactile, and easy to share.",
    featureCards: [
      { eyebrow: "Shareable", title: "One page for the whole group", body: "Hosts get a polished event page, guests get an obvious place to respond, and nobody has to log in first." },
      { eyebrow: "Readable", title: "Results that feel human", body: "Recommended times show both the score and the people behind it, so hosts can finalize with confidence." },
      { eyebrow: "Portable", title: "A simple finish", body: "Finalize the winning slot, then send it straight into a calendar instead of transcribing it by hand." },
    ],
    closingTitle: "Make the planning page feel as intentional as the event.",
    closingBody: "From team dinners to family weekends, the flow stays calm, warm, and obvious on every screen size.",
    orbitNote: "Designed to feel softer than a dashboard and more useful than a landing page.",
  },
  goblin: {
    metrics: [
      { label: "Share once", value: "No accounts", note: "One sacred link for the whole cave." },
      { label: "Signals", value: "Weighted vibes", note: "Capture yes, maybe, snack-bribe, and absolutely not." },
      { label: "Finish", value: "Calendar ready", note: "When the cave agrees, export the decree." },
    ],
    storyTitle: "A gentler ritual for chaotic creatures.",
    storyBody:
      "It still has goblin energy, just with better posture. The new surface stays warm, premium, and a little mischievous without becoming loud.",
    featureCards: [
      { eyebrow: "Shareable", title: "One link to rule the logistics", body: "Send one page into the chat and let the horde sort itself out without ten branching threads." },
      { eyebrow: "Readable", title: "Results with context", body: "See the best windows, the strongest slots, and who can actually make each one." },
      { eyebrow: "Portable", title: "A clean ending", body: "Pick the winner, download the calendar file, and stop talking about scheduling forever." },
    ],
    closingTitle: "Keep the whimsy. Lose the planning sludge.",
    closingBody: "The flow stays playful in Goblin Mode, but the visual system is calmer, clearer, and much easier to trust.",
    orbitNote: "Premium enough for serious plans. Goblin enough for snack diplomacy.",
  },
};

export default function LandingPage({ copy, mode }) {
  const details = LANDING_DETAILS[mode];

  return (
    <div className="space-y-6 md:space-y-8">
      <section className="surface-card surface-strong overflow-hidden px-6 py-8 md:px-8 md:py-10 lg:px-12 lg:py-12">
        <div className="grid gap-8 lg:grid-cols-[1.1fr,0.9fr] lg:items-end">
          <div className="space-y-6">
            <span className="eyebrow">{copy.landing.badge}</span>
            <div className="space-y-4">
              <h1 className="display-title display-title-xl max-w-4xl">{copy.landing.hero}</h1>
              <p className="section-kicker">{copy.landing.subhero}</p>
            </div>

            <div className="flex flex-wrap gap-3">
              <Link to="/create" className="btn btn-primary rounded-full px-6 py-3 text-sm font-semibold">
                {copy.landing.cta}
              </Link>
              <a href="#how-it-works" className="btn btn-secondary rounded-full px-6 py-3 text-sm font-semibold">
                See the flow
              </a>
            </div>

            <div className="pill-row">
              {details.metrics.map((item) => (
                <div key={item.label} className="metric-pill">
                  <span className="metric-label">{item.label}</span>
                  <span className="metric-value">{item.value}</span>
                  <span className="metric-note">{item.note}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="grid gap-4">
            <div className="hero-orbit min-h-[18rem]">
              <div className="hero-orbit-center" aria-hidden="true">
                G
              </div>
              <div className="hero-orbit-note">{details.orbitNote}</div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="insight-card">
                <div className="insight-meta">Response pulse</div>
                <div className="insight-value">82%</div>
                <p className="insight-copy">A soft recommendation card helps the best time stand out immediately.</p>
              </div>

              <div className="insight-card">
                <div className="insight-meta">Host clarity</div>
                <div className="insight-value">Top 5</div>
                <p className="insight-copy">Scored results stay readable even when the event gets crowded.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="how-it-works" className="grid gap-6 lg:grid-cols-[0.95fr,1.05fr]">
        <Card variant="ghost" className="space-y-5">
          <span className="eyebrow">{copy.landing.howTitle}</span>
          <h2 className="display-title display-title-lg text-[2.8rem]">{details.storyTitle}</h2>
          <p className="section-kicker">{details.storyBody}</p>
          <p className="text-sm leading-7 text-[var(--muted)]">{copy.landing.howDescription}</p>

          <div className="space-y-3 pt-2">
            {copy.landing.howSteps.map((step, index) => (
              <div key={step} className="flex items-start gap-3">
                <span className="list-number">{index + 1}</span>
                <p className="pt-1 text-sm leading-7 text-[var(--text)]">{step}</p>
              </div>
            ))}
          </div>
        </Card>

        <div className="grid gap-4">
          {details.featureCards.map((item) => (
            <Card key={item.title} className="space-y-3">
              <span className="eyebrow">{item.eyebrow}</span>
              <h3 className="display-title text-[2rem] leading-none">{item.title}</h3>
              <p className="text-sm leading-7 text-[var(--muted)]">{item.body}</p>
            </Card>
          ))}
        </div>
      </section>

      <section className="surface-card px-6 py-8 md:px-8 md:py-10">
        <div className="grid gap-6 lg:grid-cols-[1fr,0.8fr] lg:items-end">
          <div className="space-y-4">
            <span className="eyebrow">Finish with confidence</span>
            <h2 className="display-title display-title-lg text-[2.9rem]">{details.closingTitle}</h2>
            <p className="section-kicker">{details.closingBody}</p>
          </div>

          <div className="flex flex-wrap gap-3 lg:justify-end">
            <Link to="/create" className="btn btn-primary rounded-full px-6 py-3 text-sm font-semibold">
              {copy.landing.cta}
            </Link>
            <Link to="/create" className="btn btn-secondary rounded-full px-6 py-3 text-sm font-semibold">
              Build a share page
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
