export default function Footer({ mode }) {
  const footerNote =
    mode === "goblin"
      ? "Quietly premium scheduling, with just enough goblin energy."
      : "A calmer way to coordinate the people you care about.";

  return (
    <footer className="pb-8">
      <div className="mx-auto max-w-6xl px-4 lg:px-6">
        <div className="surface-card surface-ghost flex flex-col gap-4 rounded-[2rem] px-6 py-5 md:flex-row md:items-center md:justify-between">
          <div>
            <p className="eyebrow">Group scheduling, softened</p>
            <p className="mt-3 text-sm leading-7 text-[var(--muted)]">{footerNote}</p>
          </div>

          <div className="flex flex-wrap items-center gap-4 text-sm font-semibold text-[var(--muted)]">
            <span>&copy; {new Date().getFullYear()} Goblin Scheduler</span>
            <a href="/terms" className="hover:text-[var(--text)]">
              Terms
            </a>
            <a href="/privacy" className="hover:text-[var(--text)]">
              Privacy
            </a>
            <a href="https://github.com/liuliu/avail-sched" target="_blank" rel="noopener noreferrer" className="hover:text-[var(--text)]">
              GitHub
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
