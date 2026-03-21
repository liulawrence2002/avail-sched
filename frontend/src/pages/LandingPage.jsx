import { Link } from "react-router-dom";
import Card from "../components/Card";

export default function LandingPage({ copy }) {
  return (
    <div className="grid gap-6 lg:grid-cols-[1.3fr,0.7fr]">
      <section className="rounded-[36px] border border-black/10 bg-[radial-gradient(circle_at_top_left,_rgba(255,255,255,0.95),_rgba(255,255,255,0.55)),linear-gradient(135deg,_#f6d365,_#fda085)] p-8 shadow-[0_20px_70px_rgba(0,0,0,0.12)]">
        <div className="mb-4 inline-flex rounded-full border border-black/10 bg-white/70 px-4 py-2 text-xs font-black uppercase tracking-[0.25em]">
          {copy.landing.badge}
        </div>
        <h1 className="max-w-3xl text-5xl font-black leading-tight">{copy.landing.hero}</h1>
        <p className="mt-4 max-w-2xl text-lg text-slate-700">{copy.landing.subhero}</p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link to="/create" className="btn rounded-full bg-slate-950 px-6 py-3 text-sm font-semibold text-white">
            {copy.landing.cta}
          </Link>
        </div>
      </section>
      <Card className="space-y-4">
        <h2 className="text-xl font-black">{copy.landing.howTitle}</h2>
        <p>{copy.landing.howDescription}</p>
        <ul className="space-y-3 text-sm text-slate-700">
          {copy.landing.howSteps.map((step, i) => (
            <li key={i}>{step}</li>
          ))}
        </ul>
      </Card>
    </div>
  );
}
