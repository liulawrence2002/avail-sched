import { useEffect } from "react";
import { Route, Routes, useLocation } from "react-router-dom";

import ErrorBoundary from "./components/ErrorBoundary";
import FloatingNav from "./components/FloatingNav";
import Footer from "./components/Footer";
import CreatePage from "./pages/CreatePage";
import EventPage from "./pages/EventPage";
import HostPage from "./pages/HostPage";
import LandingPage from "./pages/LandingPage";
import NotFoundPage from "./pages/NotFoundPage";
import PrivacyPage from "./pages/PrivacyPage";
import ResultsPage from "./pages/ResultsPage";
import TermsPage from "./pages/TermsPage";
import { useMode } from "./useMode";

export default function App() {
  const location = useLocation();
  const { mode, toggleMode, copy } = useMode();
  const isLanding = location.pathname === "/";

  useEffect(() => {
    const targetId = location.hash.replace("#", "");

    if (!targetId) {
      window.scrollTo({ top: 0, left: 0, behavior: "auto" });
      return;
    }

    requestAnimationFrame(() => {
      const target = document.getElementById(targetId);
      if (target) {
        target.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    });
  }, [location.hash, location.pathname]);

  return (
    <div
      className={`app-shell flex min-h-screen flex-col ${isLanding ? "app-shell--landing" : "app-shell--product"}`}
    >
      <div className="ambient-ring ambient-ring--one" aria-hidden="true" />
      <div className="ambient-ring ambient-ring--two" aria-hidden="true" />
      <div className="ambient-ring ambient-ring--three" aria-hidden="true" />

      <FloatingNav copy={copy} mode={mode} onToggleMode={toggleMode} />

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pb-14 pt-28 lg:px-6 lg:pt-32">
        <ErrorBoundary>
          <div className="page-stack">
            <Routes>
              <Route path="/" element={<LandingPage copy={copy} mode={mode} />} />
              <Route path="/create" element={<CreatePage copy={copy} mode={mode} />} />
              <Route path="/e/:publicId" element={<EventPage copy={copy} mode={mode} />} />
              <Route path="/e/:publicId/results" element={<ResultsPage copy={copy} mode={mode} />} />
              <Route path="/host/:hostToken" element={<HostPage copy={copy} mode={mode} />} />
              <Route path="/privacy" element={<PrivacyPage />} />
              <Route path="/terms" element={<TermsPage />} />
              <Route path="*" element={<NotFoundPage mode={mode} />} />
            </Routes>
          </div>
        </ErrorBoundary>
      </main>

      <Footer isLanding={isLanding} mode={mode} />
    </div>
  );
}
