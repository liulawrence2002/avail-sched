import { Link, Route, Routes } from "react-router-dom";
import LandingPage from "./pages/LandingPage";
import CreatePage from "./pages/CreatePage";
import EventPage from "./pages/EventPage";
import ResultsPage from "./pages/ResultsPage";
import HostPage from "./pages/HostPage";
import ErrorBoundary from "./components/ErrorBoundary";
import { useMode } from "./useMode";

export default function App() {
  const { mode, toggleMode, copy } = useMode();

  return (
    <div className={`min-h-screen ${mode === "goblin" ? "theme-goblin" : "theme-serious"}`}>
      <header className="border-b border-black/10 bg-white/70 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4">
          <Link to="/" className="flex items-center gap-3 text-xl font-black uppercase tracking-[0.2em]">
            <span className="mascot">{mode === "goblin" ? "G" : "S"}</span>
            <span>Goblin Scheduler</span>
          </Link>
          <button className="rounded-full border px-4 py-2 text-sm font-semibold" onClick={toggleMode}>
            {copy.toggle}
          </button>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8">
        <ErrorBoundary>
          <Routes>
            <Route path="/" element={<LandingPage copy={copy} />} />
            <Route path="/create" element={<CreatePage copy={copy} />} />
            <Route path="/e/:publicId" element={<EventPage copy={copy} />} />
            <Route path="/e/:publicId/results" element={<ResultsPage copy={copy} />} />
            <Route path="/host/:hostToken" element={<HostPage copy={copy} />} />
          </Routes>
        </ErrorBoundary>
      </main>
    </div>
  );
}

