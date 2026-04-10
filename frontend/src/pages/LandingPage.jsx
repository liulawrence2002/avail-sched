import { Link } from "react-router-dom";
import { LANDING_SECTIONS } from "../landingSections";

const LANDING_DETAILS = {
  serious: {
    heroLabel: "Scheduling that feels deliberate",
    heroTitle: "Make the planning page feel like part of the event, not admin debt.",
    heroBody:
      "Goblin Scheduler helps social hosts, operators, and lightweight teams coordinate in one polished flow. Share a clean link, collect nuanced availability, surface the best windows, and finalize without sending people into a dashboard maze.",
    heroMetrics: [
      { label: "Public response page", value: "Clean and account-free", note: "Guests answer in seconds." },
      { label: "Private host workspace", value: "Decide with confidence", note: "Finalize once and export." },
      { label: "Design posture", value: "Ready to send", note: "The link feels intentional." },
    ],
    productTitle: "A calmer layer between the invite and the calendar.",
    productBody:
      "The strongest scheduling tools earn trust through clarity. The page should feel premium, the workflow should feel recoverable, and the final decision should feel irreversible once the host commits.",
    productCards: [
      {
        label: "For social hosts",
        title: "Replace group-chat entropy with one thoughtful page.",
        body: "Dinner clubs, offsites, workshops, salons, and small member events all benefit from a shared surface that looks like it belongs to the plan.",
      },
      {
        label: "For lightweight teams",
        title: "Run coordination without buying an entire suite.",
        body: "Retros, office hours, interviews, and planning sessions can stay polished without introducing heavy project management machinery.",
      },
      {
        label: "For paid experiences",
        title: "Share something that can hold your brand.",
        body: "If you want to charge for an event or a membership, the scheduling page should reinforce quality instead of feeling disposable.",
      },
    ],
    flowTitle: "The product flow is short on purpose.",
    flowBody:
      "Set the time window once, invite the group, and move from interest to commitment without changing tools. The experience is meant to compress coordination, not become its own project.",
    flowSteps: [
      {
        step: "01",
        title: "Shape the scheduling window",
        body: "Choose the date range, meeting duration, and daily hours. The setup is compact, but the resulting share page still feels premium.",
      },
      {
        step: "02",
        title: "Collect weighted responses",
        body: "Guests answer from one public page using stronger and weaker signals instead of a flat yes-or-no grid, and they can come back later from the same device.",
      },
      {
        step: "03",
        title: "Finalize once and hand it off",
        body: "Hosts review ranked windows, lock the best option, and export a calendar file from a separate private workspace.",
      },
    ],
    demoLabel: "Product view",
    demoTitle: "One link for the group, one workspace for the host.",
    demoBody:
      "The public side stays welcoming and simple. The private side stays data-forward and decisive. That contrast is what makes the product feel more premium than a generic poll.",
    demoEvent: "Neighborhood Dinner Club",
    demoSummary: "Thu 6:30 PM is leading with broad support and the fewest hard conflicts.",
    demoSignals: [
      { label: "Unique respondents", value: "18" },
      { label: "Strong windows", value: "6" },
      { label: "Final decision", value: "One-way lock" },
    ],
    trustTitle: "The trust surface matters more than feature bulk.",
    trustBody:
      "A sellable frontend needs to feel credible in the edges: state comes back when guests revisit, counts mean what people think they mean, and finalization changes the posture of the entire page.",
    trustCards: [
      {
        label: "Revisitable responses",
        title: "Saved state comes back when the guest returns.",
        body: "The product now behaves like the page promises, which reduces anxiety and keeps the interaction lightweight.",
      },
      {
        label: "Honest metrics",
        title: "Respondent counts reflect people, not button presses.",
        body: "Hosts and guests see numbers they can actually trust while the ranking engine keeps its deeper score underneath.",
      },
      {
        label: "Locked finalization",
        title: "The chosen time becomes the final handoff.",
        body: "Once the host commits, the page stops feeling provisional and starts feeling ready to share beyond the planning group.",
      },
    ],
    startTitle: "Launch a scheduling page that looks ready before you add more features.",
    startBody:
      "The product does not need enterprise sprawl to be useful. It needs a premium default shell, a frictionless share flow, and enough confidence in the finish that people feel comfortable sending the link.",
    startPoints: [
      "Serious mode leads with a premium tone.",
      "Goblin Mode stays available as optional delight.",
      "The same core workflow serves both social plans and lightweight teams.",
    ],
  },
  goblin: {
    heroLabel: "Premium scheduling, lightly mossed",
    heroTitle: "Give the cave a polished planning page instead of ten chaotic side threads.",
    heroBody:
      "Goblin Scheduler still leads with a serious workflow underneath the whimsy. Share one link, gather weighted cave availability, and lock a winner without making anyone create an account or wander through a bloated dashboard.",
    heroMetrics: [
      { label: "Public cave page", value: "Fast to answer", note: "No account ritual required." },
      { label: "Private throne room", value: "Crown the winner", note: "One-way finalize and export." },
      { label: "Presentation", value: "Still polished", note: "Moss on top, trust underneath." },
    ],
    productTitle: "The reliable engine stays serious. The flavor is optional.",
    productBody:
      "Goblin Mode works best as a personality layer around a workflow people already trust. The page can be playful, but the host still needs clear state, recoverable edits, and a final decision that actually feels final.",
    productCards: [
      {
        label: "For cave hosts",
        title: "Keep the hangout energy without the coordination sludge.",
        body: "Snack summits, game nights, and spontaneous plots all go smoother when the planning page looks intentional.",
      },
      {
        label: "For serious work",
        title: "Under the moss, it is still a premium scheduler.",
        body: "Workshops, club logistics, and client coordination still benefit from the same clear structure and private host workspace.",
      },
      {
        label: "For memorable products",
        title: "Distinct personality is strongest after trust is established.",
        body: "That is why Goblin Mode decorates the shell instead of replacing the serious product posture underneath it.",
      },
    ],
    flowTitle: "Short setup. Honest cave math. One crowned answer.",
    flowBody:
      "The rhythm stays exactly the same: shape the window, let the horde respond, and crown the winner once the signal is clear. The design adds personality without diluting the product.",
    flowSteps: [
      {
        step: "01",
        title: "Sketch the cave calendar",
        body: "Set the date range, duration, and waking hours so the scheduling page appears fully formed as soon as you share it.",
      },
      {
        step: "02",
        title: "Let the horde mark availability",
        body: "Guests can answer with yes, maybe, snack-powered effort, or no, then return later from the same device if plans shift.",
      },
      {
        step: "03",
        title: "Lock the decree and send the scroll",
        body: "The throne room keeps the final decision private until the host locks it and exports the result.",
      },
    ],
    demoLabel: "Cave view",
    demoTitle: "One public cave page. One private throne room.",
    demoBody:
      "The public surface stays easy for goblins to answer, while the private host view stays sharp and decisive. That split is what lets the product keep both personality and trust.",
    demoEvent: "Moonlight Snack Summit",
    demoSummary: "Thu 6:30 PM is still the best cave compromise with strong support and very little grumbling.",
    demoSignals: [
      { label: "Unique goblins", value: "18" },
      { label: "Snack-ready windows", value: "6" },
      { label: "Final decree", value: "One-way lock" },
    ],
    trustTitle: "Even the goblin flavor depends on trust.",
    trustBody:
      "If the saved response disappears, the counts feel inflated, or the chosen time can still move around, the magic drops immediately. The frontend has to feel dependable first and charming second.",
    trustCards: [
      {
        label: "Returnable markings",
        title: "Your old cave response comes back when you return.",
        body: "That makes the product feel fair and lowers the cost of answering now instead of waiting for perfect certainty.",
      },
      {
        label: "Honest horde counts",
        title: "The visible stats reflect real respondents.",
        body: "That keeps the ranking trustworthy for both hosts and participants, even if someone edits their answer later.",
      },
      {
        label: "Locked decrees",
        title: "Once crowned, the time becomes the official handoff.",
        body: "The page changes posture after finalization, which makes the finish feel more premium and shareable.",
      },
    ],
    startTitle: "Ship the serious shell first. Let the moss become the memorable detail.",
    startBody:
      "The product becomes sellable when the default experience feels polished and dependable. Goblin Mode then adds distinct identity without turning the app into a joke people hesitate to share.",
    startPoints: [
      "The premium shell remains the default posture.",
      "Goblin Mode changes tone, not product trust.",
      "Social hosts and lightweight teams can still use the same flow.",
    ],
  },
};

const DEMO_RESPONSES = [
  { time: "Tue 6:30 PM", state: "yes", attendees: "12 strong" },
  { time: "Wed 7:00 PM", state: "maybe", attendees: "7 flexible" },
  { time: "Thu 6:30 PM", state: "yes", attendees: "15 strong" },
  { time: "Sat 11:00 AM", state: "bribe", attendees: "4 effort" },
];

export default function LandingPage({ copy, mode }) {
  const details = LANDING_DETAILS[mode];

  return (
    <div className="landing-shell">
      <section id={LANDING_SECTIONS[0].id} className="landing-band landing-band--hero landing-band--bleed">
        <div className="landing-band__inner landing-hero-grid">
          <div className="landing-hero-copy">
            <span className="eyebrow">{details.heroLabel}</span>
            <div className="space-y-5">
              <h1 className="display-title display-title-xl">{details.heroTitle}</h1>
              <p className="section-kicker">{details.heroBody}</p>
            </div>

            <div className="landing-hero-actions">
              <Link to="/create" className="btn btn-primary rounded-full px-6 py-3 text-sm font-semibold">
                {copy.landing.cta}
              </Link>
              <a href={`#${LANDING_SECTIONS[3].id}`} className="btn btn-secondary rounded-full px-6 py-3 text-sm font-semibold">
                See the product
              </a>
            </div>

            <div className="landing-metric-grid">
              {details.heroMetrics.map((item) => (
                <div key={item.label} className="landing-metric">
                  <span className="metric-label">{item.label}</span>
                  <span className="metric-value">{item.value}</span>
                  <span className="metric-note">{item.note}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="landing-hero-stage">
            <div className="landing-orbit-card">
              <div className="landing-orbit-card__header">
                <span className="detail-label">Live product preview</span>
                <span className="landing-chip">No accounts</span>
              </div>
              <div className="landing-orbit-card__body">
                <div className="landing-orbit-card__primary">
                  <p className="detail-label">Shared page</p>
                  <h2 className="display-title text-[2.3rem] leading-none">{details.demoEvent}</h2>
                  <p className="text-sm leading-7 text-[var(--muted)]">
                    Collect responses from one premium page, then move the final decision into a separate private host workflow.
                  </p>
                </div>

                <div className="landing-orbit-card__secondary">
                  <div className="landing-orbit-grid">
                    <div className="landing-orbit-grid__ring landing-orbit-grid__ring--outer" />
                    <div className="landing-orbit-grid__ring landing-orbit-grid__ring--inner" />
                    <div className="landing-orbit-grid__core">G</div>
                    <div className="landing-orbit-grid__note landing-orbit-grid__note--one">
                      Weighted responses
                    </div>
                    <div className="landing-orbit-grid__note landing-orbit-grid__note--two">
                      Locked finalization
                    </div>
                    <div className="landing-orbit-grid__note landing-orbit-grid__note--three">
                      Calendar export
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id={LANDING_SECTIONS[1].id} className="landing-band">
        <div className="landing-band__inner landing-split-grid">
          <div className="landing-sticky-copy">
            <span className="eyebrow">Product posture</span>
            <h2 className="display-title display-title-lg">{details.productTitle}</h2>
            <p className="section-kicker">{details.productBody}</p>
          </div>

          <div className="landing-story-grid">
            {details.productCards.map((card, index) => (
              <article key={card.title} className={`landing-story-card landing-story-card--offset-${(index % 3) + 1}`}>
                <span className="eyebrow">{card.label}</span>
                <h3 className="display-title text-[2.15rem] leading-none">{card.title}</h3>
                <p className="text-sm leading-7 text-[var(--muted)]">{card.body}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section id={LANDING_SECTIONS[2].id} className="landing-band landing-band--flow">
        <div className="landing-band__inner landing-split-grid">
          <div className="landing-sticky-copy">
            <span className="eyebrow">Operational flow</span>
            <h2 className="display-title display-title-lg">{details.flowTitle}</h2>
            <p className="section-kicker">{details.flowBody}</p>
          </div>

          <div className="landing-flow-stack">
            {details.flowSteps.map((step) => (
              <article key={step.step} className="landing-flow-step">
                <div className="landing-flow-step__meta">
                  <span className="landing-flow-step__number">{step.step}</span>
                  <div className="landing-flow-step__line" aria-hidden="true" />
                </div>
                <div className="space-y-3">
                  <h3 className="display-title text-[2rem] leading-none">{step.title}</h3>
                  <p className="text-sm leading-7 text-[var(--muted)]">{step.body}</p>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section id={LANDING_SECTIONS[3].id} className="landing-band landing-band--bleed landing-band--demo">
        <div className="landing-band__inner landing-demo-grid">
          <div className="landing-demo-copy">
            <span className="eyebrow">{details.demoLabel}</span>
            <h2 className="display-title display-title-lg">{details.demoTitle}</h2>
            <p className="section-kicker">{details.demoBody}</p>

            <div className="landing-signal-row">
              {details.demoSignals.map((signal) => (
                <div key={signal.label} className="landing-signal-card">
                  <span className="metric-label">{signal.label}</span>
                  <span className="metric-value">{signal.value}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="landing-command-surface">
            <div className="landing-command-surface__header">
              <div>
                <p className="detail-label">Event page</p>
                <h3 className="landing-command-surface__title">{details.demoEvent}</h3>
              </div>
              <span className="landing-chip">18 respondents</span>
            </div>

            <div className="landing-command-surface__board">
              <div className="landing-command-surface__rail">
                <div className="landing-rail-stat">
                  <span className="metric-label">Guest state</span>
                  <span className="metric-value">Saved</span>
                  <span className="metric-note">Return later and revise.</span>
                </div>
                <div className="landing-rail-stat">
                  <span className="metric-label">Best slot</span>
                  <span className="metric-value">Thu 6:30 PM</span>
                  <span className="metric-note">92% of max score</span>
                </div>
              </div>

              <div className="landing-command-surface__responses">
                {DEMO_RESPONSES.map((slot) => (
                  <div key={slot.time} className="landing-response-row" data-state={slot.state}>
                    <div>
                      <span className="landing-response-row__time">{slot.time}</span>
                      <p className="landing-response-row__meta">{slot.attendees}</p>
                    </div>
                    <strong>{copy.availability[slot.state]}</strong>
                  </div>
                ))}
              </div>
            </div>

            <div className="landing-command-surface__summary">
              <div>
                <p className="detail-label">Host recommendation</p>
                <h4 className="display-title text-[2rem] leading-none">Thu 6:30 PM</h4>
              </div>
              <p className="text-sm leading-7 text-[var(--muted)]">{details.demoSummary}</p>
            </div>
          </div>
        </div>
      </section>

      <section id={LANDING_SECTIONS[4].id} className="landing-band">
        <div className="landing-band__inner landing-split-grid">
          <div className="landing-sticky-copy">
            <span className="eyebrow">Trust surface</span>
            <h2 className="display-title display-title-lg">{details.trustTitle}</h2>
            <p className="section-kicker">{details.trustBody}</p>
          </div>

          <div className="landing-story-grid">
            {details.trustCards.map((card, index) => (
              <article key={card.title} className={`landing-story-card landing-story-card--trust landing-story-card--offset-${((index + 1) % 3) + 1}`}>
                <span className="eyebrow">{card.label}</span>
                <h3 className="display-title text-[2rem] leading-none">{card.title}</h3>
                <p className="text-sm leading-7 text-[var(--muted)]">{card.body}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section id={LANDING_SECTIONS[5].id} className="landing-band landing-band--start landing-band--bleed">
        <div className="landing-band__inner landing-start-grid">
          <div className="landing-start-copy">
            <span className="eyebrow">Start here</span>
            <h2 className="display-title display-title-lg">{details.startTitle}</h2>
            <p className="section-kicker">{details.startBody}</p>
          </div>

          <div className="landing-start-panel">
            <div className="landing-start-points">
              {details.startPoints.map((point) => (
                <div key={point} className="landing-start-point">
                  <span className="landing-start-point__mark" aria-hidden="true">
                    +
                  </span>
                  <span>{point}</span>
                </div>
              ))}
            </div>

            <div className="landing-start-actions">
              <Link to="/create" className="btn btn-primary rounded-full px-6 py-3 text-sm font-semibold">
                {copy.landing.cta}
              </Link>
              <Link to="/terms" className="btn btn-secondary rounded-full px-6 py-3 text-sm font-semibold">
                Review trust pages
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
