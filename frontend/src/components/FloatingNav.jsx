import { useEffect, useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { LANDING_SECTIONS } from "../landingSections";

const ROUTE_LABELS = [
  { match: (pathname) => pathname === "/create", label: "Host setup" },
  { match: (pathname) => pathname.endsWith("/results"), label: "Ranking view" },
  { match: (pathname) => pathname.startsWith("/e/"), label: "Shared event page" },
  { match: (pathname) => pathname.startsWith("/host/"), label: "Host workspace" },
  { match: (pathname) => pathname === "/privacy", label: "Privacy" },
  { match: (pathname) => pathname === "/terms", label: "Terms" },
];

export default function FloatingNav({ copy, mode, onToggleMode }) {
  const location = useLocation();
  const isLanding = location.pathname === "/";
  const [isCondensed, setIsCondensed] = useState(false);
  const [activeSection, setActiveSection] = useState(LANDING_SECTIONS[0].id);
  const [mobileOpen, setMobileOpen] = useState(false);

  const routeLabel = useMemo(
    () => ROUTE_LABELS.find((item) => item.match(location.pathname))?.label || "Scheduling app",
    [location.pathname],
  );

  useEffect(() => {
    function updateCondensed() {
      setIsCondensed(window.scrollY > (isLanding ? 52 : 8));
    }

    updateCondensed();
    window.addEventListener("scroll", updateCondensed, { passive: true });
    return () => window.removeEventListener("scroll", updateCondensed);
  }, [isLanding]);

  useEffect(() => {
    setMobileOpen(false);
  }, [location.hash, location.pathname]);

  useEffect(() => {
    if (!mobileOpen) {
      document.body.style.overflow = "";
      return () => {
        document.body.style.overflow = "";
      };
    }

    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = "";
    };
  }, [mobileOpen]);

  useEffect(() => {
    if (!isLanding) {
      setActiveSection("");
      return () => {};
    }

    const sectionElements = LANDING_SECTIONS.map((section) => document.getElementById(section.id)).filter(Boolean);
    if (!sectionElements.length) {
      return () => {};
    }

    const observer = new IntersectionObserver(
      (entries) => {
        const visibleEntries = entries.filter((entry) => entry.isIntersecting);
        if (!visibleEntries.length) {
          return;
        }

        visibleEntries.sort((left, right) => right.intersectionRatio - left.intersectionRatio);
        setActiveSection(visibleEntries[0].target.id);
      },
      {
        rootMargin: "-28% 0px -48% 0px",
        threshold: [0.2, 0.35, 0.5, 0.75],
      },
    );

    sectionElements.forEach((sectionElement) => observer.observe(sectionElement));
    return () => observer.disconnect();
  }, [isLanding, location.pathname]);

  useEffect(() => {
    if (location.hash) {
      setActiveSection(location.hash.slice(1));
    }
  }, [location.hash]);

  return (
    <header className={`floating-nav ${isCondensed ? "is-condensed" : ""} ${mobileOpen ? "is-open" : ""}`}>
      <div className="floating-nav__frame">
        <div className="floating-nav__brand-row">
          <Link to="/" className="floating-nav__brand" aria-label="Goblin Scheduler home">
            <span className="floating-nav__mark" aria-hidden="true">
              G
            </span>
            <span>
              <span className="floating-nav__kicker">
                {mode === "goblin" ? "Reliable scheduling with optional moss" : "Premium scheduling for real plans"}
              </span>
              <span className="floating-nav__title">Goblin Scheduler</span>
            </span>
          </Link>

          <button
            type="button"
            className="floating-nav__menu-button"
            aria-expanded={mobileOpen}
            aria-controls="floating-nav-mobile"
            onClick={() => setMobileOpen((current) => !current)}
          >
            <span aria-hidden="true">{mobileOpen ? "Close" : "Menu"}</span>
          </button>
        </div>

        <div className="floating-nav__desktop">
          {isLanding ? (
            <nav className="floating-nav__section-links" aria-label="Landing sections">
              {LANDING_SECTIONS.map((section) => (
                <SectionLink
                  key={section.id}
                  section={section}
                  active={activeSection === section.id}
                  isLanding={isLanding}
                  onSelect={() => setMobileOpen(false)}
                />
              ))}
            </nav>
          ) : (
            <div className="floating-nav__route-context" aria-label="Current route">
              <span className="floating-nav__route-label">Current route</span>
              <span className="floating-nav__route-value">{routeLabel}</span>
            </div>
          )}

          <div className="floating-nav__actions">
            {!isLanding ? (
              <Link className="btn btn-secondary rounded-full px-4 py-2 text-sm font-semibold" to="/#hero">
                Overview
              </Link>
            ) : null}
            <Link className="btn btn-secondary rounded-full px-4 py-2 text-sm font-semibold" to="/create">
              {copy.landing.cta}
            </Link>
            <button type="button" className="btn btn-tonal rounded-full px-4 py-2 text-sm font-semibold" onClick={onToggleMode}>
              {copy.toggle}
            </button>
          </div>
        </div>
      </div>

      <div id="floating-nav-mobile" className="floating-nav__mobile-panel">
        <div className="floating-nav__mobile-actions">
          <Link className="btn btn-primary rounded-full px-4 py-3 text-sm font-semibold" to="/create">
            {copy.landing.cta}
          </Link>
          <button type="button" className="btn btn-tonal rounded-full px-4 py-3 text-sm font-semibold" onClick={onToggleMode}>
            {copy.toggle}
          </button>
        </div>

        <nav className="floating-nav__mobile-links" aria-label="Primary navigation">
          {!isLanding ? (
            <Link className="floating-nav__mobile-link" to="/#hero">
              Overview
            </Link>
          ) : null}
          {LANDING_SECTIONS.map((section) => (
            <SectionLink
              key={`mobile-${section.id}`}
              section={section}
              active={isLanding && activeSection === section.id}
              isLanding={isLanding}
              mobile
              onSelect={() => setMobileOpen(false)}
            />
          ))}
        </nav>
      </div>
    </header>
  );
}

function SectionLink({ active, isLanding, mobile = false, onSelect, section }) {
  const className = mobile
    ? `floating-nav__mobile-link ${active ? "is-active" : ""}`
    : `floating-nav__section-link ${active ? "is-active" : ""}`;

  if (isLanding) {
    return (
      <a className={className} href={`#${section.id}`} onClick={onSelect}>
        {section.label}
      </a>
    );
  }

  return (
    <Link className={className} to={`/#${section.id}`} onClick={onSelect}>
      {section.label}
    </Link>
  );
}
