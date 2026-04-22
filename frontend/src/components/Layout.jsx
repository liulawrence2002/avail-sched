import { Link, useLocation } from 'react-router-dom';
import { useAppState } from '../hooks/useAppState';

export default function Layout({ children }) {
  const location = useLocation();
  const { globalError, clearError, navTitle } = useAppState();
  const isHome = location.pathname === '/';

  return (
    <div className="min-h-screen bg-void text-cream font-body relative overflow-x-hidden">
      {/* Very subtle ambient depth — no glow */}
      <div className="fixed inset-0 pointer-events-none stage-light z-0" />

      {/* Header — tighter, more engineered */}
      <header
        className={`relative z-20 w-full ${
          isHome ? 'bg-transparent' : 'bg-void/90 backdrop-blur-sm border-b border-white/[0.06]'
        }`}
      >
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-14 sm:h-16">
            <Link
              to="/"
              className="flex items-center gap-2.5 group focus-visible:outline-none"
              aria-label="Goblin Scheduler home"
            >
              <span
                className="text-xl sm:text-2xl transition-transform group-hover:scale-105 duration-150"
                aria-hidden="true"
              >
                🦎
              </span>
              <div className="flex flex-col">
                <span className="font-display text-base sm:text-lg font-semibold text-gold tracking-tight leading-none">
                  Goblin Scheduler
                </span>
                <span className="text-[10px] text-silver-dim tracking-wider uppercase hidden sm:block leading-tight mt-0.5">
                  Availability without accounts
                </span>
              </div>
            </Link>

            <nav className="flex items-center gap-0.5" aria-label="Main navigation">
              <NavLink to="/" label="Home" />
              <NavLink to="/create" label="Create" />
              <NavLink to="/privacy" label="Privacy" />
            </nav>
          </div>
        </div>
      </header>

      {/* Page title bar (non-home pages) */}
      {!isHome && navTitle && (
        <div className="relative z-10 border-b border-white/[0.06] bg-ink/60">
          <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-3">
            <h1 className="font-display text-lg sm:text-xl text-cream">{navTitle}</h1>
          </div>
        </div>
      )}

      {/* Global error banner */}
      {globalError && (
        <div className="relative z-30 bg-ruby/10 border-b border-ruby/20">
          <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-2.5 flex items-center justify-between">
            <p className="text-sm text-crimson">{globalError}</p>
            <button
              onClick={clearError}
              className="text-xs text-silver hover:text-cream transition-colors"
              aria-label="Dismiss error"
            >
              Dismiss
            </button>
          </div>
        </div>
      )}

      {/* Main content */}
      <main className="relative z-10">{children}</main>

      {/* Footer — tighter */}
      <footer className="relative z-10 border-t border-white/[0.06] mt-auto">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
            <div className="flex items-center gap-2 text-silver-dim text-sm">
              <span aria-hidden="true">🦎</span>
              <span>Goblin Scheduler</span>
            </div>
            <nav className="flex items-center gap-5 text-sm" aria-label="Footer navigation">
              <Link to="/privacy" className="text-silver hover:text-cream transition-colors duration-150">
                Privacy
              </Link>
              <Link to="/terms" className="text-silver hover:text-cream transition-colors duration-150">
                Terms
              </Link>
              <a
                href="/api-docs"
                className="text-silver hover:text-cream transition-colors duration-150"
                target="_blank"
                rel="noopener noreferrer"
              >
                API Docs
              </a>
            </nav>
          </div>
          <p className="text-center text-xs text-silver-dim mt-4">
            No accounts. No tracking. Just scheduling.
          </p>
        </div>
      </footer>
    </div>
  );
}

function NavLink({ to, label }) {
  const location = useLocation();
  const isActive = location.pathname === to;

  return (
    <Link
      to={to}
      aria-current={isActive ? 'page' : undefined}
      className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors duration-150 ${
        isActive
          ? 'text-gold bg-gold/[0.08]'
          : 'text-silver hover:text-cream hover:bg-white/[0.04]'
      }`}
    >
      {label}
    </Link>
  );
}
