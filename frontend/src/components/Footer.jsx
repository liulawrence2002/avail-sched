import { Link } from "react-router-dom";

export default function Footer({ isLanding = false, mode }) {
  const footerNote =
    mode === "goblin"
      ? "Serious scheduling underneath, optional goblin energy on top."
      : "A calmer way to coordinate people without making the planning page feel disposable.";

  return (
    <footer className={`site-footer ${isLanding ? "site-footer--landing" : ""}`}>
      <div className="mx-auto max-w-6xl px-4 lg:px-6">
        <div className="site-footer__frame">
          <div className="site-footer__copy">
            <p className="eyebrow">Scheduling, with some posture</p>
            <p className="mt-3 text-sm leading-7 text-[var(--muted)]">{footerNote}</p>
          </div>

          <div className="site-footer__links">
            <span>&copy; {new Date().getFullYear()} Goblin Scheduler</span>
            <Link to="/" className="hover:text-[var(--text)]">
              Home
            </Link>
            <Link to="/create" className="hover:text-[var(--text)]">
              Create
            </Link>
            <Link to="/terms" className="hover:text-[var(--text)]">
              Terms
            </Link>
            <Link to="/privacy" className="hover:text-[var(--text)]">
              Privacy
            </Link>
            <a href="https://github.com/liulawrence2002/avail-sched" target="_blank" rel="noopener noreferrer" className="hover:text-[var(--text)]">
              GitHub
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
