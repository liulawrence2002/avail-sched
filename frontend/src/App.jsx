import { Link, Route, Routes } from "react-router-dom";
import LandingPage from "./pages/LandingPage";
import CreatePage from "./pages/CreatePage";
import EventPage from "./pages/EventPage";
import ResultsPage from "./pages/ResultsPage";
import HostPage from "./pages/HostPage";
import NotFoundPage from "./pages/NotFoundPage";
import ErrorBoundary from "./components/ErrorBoundary";
import Footer from "./components/Footer";
import { useMode } from "./useMode";

export default function App() {
  const { mode, toggleMode, copy } = useMode();
  const headerLabel = mode === "goblin" ? "Premium planning for gentle chaos" : "Thoughtful group availability";

  return (
    <div className={`app-shell flex min-h-screen flex-col ${mode === "goblin" ? "theme-goblin" : "theme-serious"}`}>
      <div className="ambient-ring ambient-ring--one" aria-hidden="true" />
      <div className="ambient-ring ambient-ring--two" aria-hidden="true" />
      <div className="ambient-ring ambient-ring--three" aria-hidden="true" />

      <header className="site-header">
        <div className="mx-auto max-w-6xl px-4 lg:px-6">
          <div className="site-header-inner flex items-center justify-between gap-3 px-4 py-3 md:px-6">
            <Link to="/" className="wordmark">
              <span className="wordmark-mark" aria-hidden="true">
                G
              </span>
              <span>
                <span className="wordmark-label">{headerLabel}</span>
                <span className="wordmark-title">Goblin Scheduler</span>
              </span>
            </Link>

            <div className="flex items-center gap-2">
              <Link className="btn btn-secondary hidden rounded-full px-4 py-2 text-sm font-semibold sm:inline-flex" to="/create">
                Create
              </Link>
              <button className="btn btn-tonal rounded-full px-4 py-2 text-sm font-semibold" onClick={toggleMode}>
                {copy.toggle}
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pb-12 pt-2 lg:px-6">
        <ErrorBoundary>
          <div className="page-stack">
            <Routes>
              <Route path="/" element={<LandingPage copy={copy} mode={mode} />} />
              <Route path="/create" element={<CreatePage copy={copy} mode={mode} />} />
              <Route path="/e/:publicId" element={<EventPage copy={copy} mode={mode} />} />
              <Route path="/e/:publicId/results" element={<ResultsPage copy={copy} mode={mode} />} />
              <Route path="/host/:hostToken" element={<HostPage copy={copy} mode={mode} />} />
              <Route path="*" element={<NotFoundPage mode={mode} />} />
            </Routes>
          </div>
        </ErrorBoundary>
      </main>

      <Footer mode={mode} />
    </div>
  );
}
