import { Link } from "react-router-dom";
import Card from "../components/Card";

export default function LandingPage({ copy }) {
  return (
    <div className="grid gap-6 lg:grid-cols-[1.3fr,0.7fr]">
      <section className="rounded-[36px] border border-black/10 bg-[radial-gradient(circle_at_top_left,_rgba(255,255,255,0.95),_rgba(255,255,255,0.55)),linear-gradient(135deg,_#f6d365,_#fda085)] p-8 shadow-[0_20px_70px_rgba(0,0,0,0.12)]">
        <div className="mb-4 inline-flex rounded-full border border-black/10 bg-white/70 px-4 py-2 text-xs font-black uppercase tracking-[0.25em]">
          Group availability scheduler
        </div>
        <h1 className="max-w-3xl text-5xl font-black leading-tight">{copy.hero}</h1>
        <p className="mt-4 max-w-2xl text-lg text-slate-700">{copy.subhero}</p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link to="/create" className="rounded-full bg-slate-950 px-6 py-3 text-sm font-semibold text-white">
            {copy.cta}
          </Link>
        </div>
      </section>
      <Card className="space-y-4">
        <h2 className="text-xl font-black">How it works</h2>
        <p>Create an event, share the public link, collect availability votes, and finalize one winner.</p>
        <ul className="space-y-3 text-sm text-slate-700">
          <li>No accounts required.</li>
          <li>Weighted votes for yes, maybe, snacks, and no.</li>
          <li>ICS download after finalizing.</li>
        </ul>
      </Card>
    </div>
  );
}

