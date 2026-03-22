import { Link } from "react-router-dom";
import Card from "../components/Card";

const LANDING_DETAILS = {
  serious: {
    audiences: [
      {
        eyebrow: "Social plans",
        title: "Dinners, clubs, and weekend hangs",
        body: "Make the invite link look intentional instead of tossing a spreadsheet into the group chat.",
      },
      {
        eyebrow: "Lean teams",
        title: "Workshops, retros, and office hours",
        body: "Use one polished page when a full scheduling suite would be overkill.",
      },
      {
        eyebrow: "Client work",
        title: "Small-group sessions that need polish",
        body: "Share a page that feels premium enough to represent your brand in front of clients or members.",
      },
    ],
    strengths: [
      "Public response page plus private host workspace",
      "Weighted availability instead of flat yes or no",
      "One-way finalization with calendar export",
    ],
    flowTitle: "Built for people who host things.",
    flowBody: "The core pitch is simple: make scheduling feel closer to a premium invitation and farther from admin work.",
    closingTitle: "Charge for polish, clarity, and trust.",
    closingBody: "The product does not need enterprise complexity to be valuable. It needs a smooth setup, a credible share surface, and a finish that feels final.",
    demoNote: "A real product preview beats made-up proof cards every time.",
  },
  goblin: {
    audiences: [
      {
        eyebrow: "Snack diplomacy",
        title: "Caves, hangouts, and mossy plans",
        body: "Still the same premium scheduling flow, just with a little more cave atmosphere.",
      },
      {
        eyebrow: "Low-stakes chaos",
        title: "Board games and food quests",
        body: "Let the horde answer from one page instead of fermenting ten side threads.",
      },
      {
        eyebrow: "Optional flavor",
        title: "Whimsy without losing trust",
        body: "Goblin Mode stays playful while the underlying flow keeps its grown-up posture.",
      },
    ],
    strengths: [
      "One public cave page and one private throne room",
      "Weighted cave math instead of flat yes or no",
      "A locked decree plus royal scroll export",
    ],
    flowTitle: "Same reliable engine. More moss.",
    flowBody: "Goblin Mode is the personality layer, not the product strategy. The premium behavior stays exactly the same underneath it.",
    closingTitle: "Keep the goblin energy optional.",
    closingBody: "The serious shell sells the product. Goblin Mode becomes the memorable delight that makes it feel distinct.",
    demoNote: "Still premium. Just a little gremlin-coded.",
  },
};

const DEMO_SLOTS = [
  { time: "Tue 6:30 PM", state: "yes" },
  { time: "Wed 7:00 PM", state: "maybe" },
  { time: "Thu 6:30 PM", state: "yes" },
  { time: "Sat 11:00 AM", state: "bribe" },
];

export default function LandingPage({ copy, mode }) {
  const details = LANDING_DETAILS[mode];

  return (
    <div className="space-y-6 md:space-y-8">
      <section className="surface-card surface-strong overflow-hidden px-6 py-8 md:px-8 md:py-10 lg:px-12 lg:py-12">
        <div className="grid gap-8 lg:grid-cols-[1.02fr,0.98fr] lg:items-center">
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
              {details.strengths.map((item) => (
                <span key={item} className="meta-pill">
                  {item}
                </span>
              ))}
            </div>
          </div>

          <div className="demo-window">
            <div className="demo-window-bar">
              <span className="eyebrow">Product Demo</span>
              <span className="demo-note">{details.demoNote}</span>
            </div>

            <div className="demo-window-shell">
              <div className="demo-panel">
                <div className="demo-panel-header">
                  <div>
                    <p className="detail-label">Event page</p>
                    <h2 className="demo-title">Neighborhood Dinner Club</h2>
                  </div>
                  <span className="demo-chip">No accounts</span>
                </div>
                <p className="demo-copy">Guests answer from one clean page, then the host locks the final time from a separate private workspace.</p>
              </div>

              <div className="demo-board">
                {DEMO_SLOTS.map((slot) => (
                  <div key={slot.time} className="demo-slot" data-state={slot.state}>
                    <span>{slot.time}</span>
                    <strong>{slot.state === "yes" ? copy.availability.yes : slot.state === "maybe" ? copy.availability.maybe : copy.availability.bribe}</strong>
                  </div>
                ))}
              </div>

              <div className="demo-panel demo-panel--highlight">
                <div className="demo-panel-header">
                  <div>
                    <p className="detail-label">Top recommendation</p>
                    <h3 className="demo-title">Thu 6:30 PM</h3>
                  </div>
                  <span className="demo-score">92%</span>
                </div>
                <p className="demo-copy">Finalize once, export to calendar, and stop coordinating.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-3">
        {details.audiences.map((item) => (
          <Card key={item.title} className="space-y-3">
            <span className="eyebrow">{item.eyebrow}</span>
            <h2 className="display-title text-[2rem] leading-none">{item.title}</h2>
            <p className="text-sm leading-7 text-[var(--muted)]">{item.body}</p>
          </Card>
        ))}
      </section>

      <section id="how-it-works" className="grid gap-6 lg:grid-cols-[0.95fr,1.05fr]">
        <Card variant="ghost" className="space-y-5">
          <span className="eyebrow">{copy.landing.howTitle}</span>
          <h2 className="display-title display-title-lg text-[2.8rem]">{details.flowTitle}</h2>
          <p className="section-kicker">{details.flowBody}</p>
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
          <Card className="space-y-3">
            <span className="eyebrow">What makes it sellable</span>
            <h3 className="display-title text-[2rem] leading-none">Premium behavior, not just premium paint.</h3>
            <p className="text-sm leading-7 text-[var(--muted)]">
              Trust grows when people can revisit their saved response, hosts see honest respondent counts, and finalization actually feels final.
            </p>
          </Card>

          <Card className="space-y-3">
            <span className="eyebrow">Share surface</span>
            <h3 className="display-title text-[2rem] leading-none">A link you would not mind sending to clients.</h3>
            <p className="text-sm leading-7 text-[var(--muted)]">
              The design language stays warm and memorable, but the serious-default shell is doing the selling work now.
            </p>
          </Card>

          <Card className="space-y-3">
            <span className="eyebrow">Finish</span>
            <h3 className="display-title text-[2rem] leading-none">Once the time is chosen, it stays chosen.</h3>
            <p className="text-sm leading-7 text-[var(--muted)]">
              That finality makes the product feel dependable, which matters a lot more than adding feature bulk too early.
            </p>
          </Card>
        </div>
      </section>

      <section className="surface-card px-6 py-8 md:px-8 md:py-10">
        <div className="grid gap-6 lg:grid-cols-[1fr,0.8fr] lg:items-end">
          <div className="space-y-4">
            <span className="eyebrow">Launch-ready direction</span>
            <h2 className="display-title display-title-lg text-[2.9rem]">{details.closingTitle}</h2>
            <p className="section-kicker">{details.closingBody}</p>
          </div>

          <div className="flex flex-wrap gap-3 lg:justify-end">
            <Link to="/create" className="btn btn-primary rounded-full px-6 py-3 text-sm font-semibold">
              {copy.landing.cta}
            </Link>
            <Link to="/terms" className="btn btn-secondary rounded-full px-6 py-3 text-sm font-semibold">
              Review legal pages
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
