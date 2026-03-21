export default function Footer() {
  return (
    <footer className="border-t border-black/10 bg-white/50 backdrop-blur">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-4 text-sm text-slate-600">
        <span>&copy; {new Date().getFullYear()} Goblin Scheduler</span>
        <div className="flex gap-4">
          <a href="/terms" className="hover:text-slate-900">Terms</a>
          <a href="/privacy" className="hover:text-slate-900">Privacy</a>
          <a href="https://github.com/liuliu/avail-sched" target="_blank" rel="noopener noreferrer" className="hover:text-slate-900">GitHub</a>
        </div>
      </div>
    </footer>
  );
}
