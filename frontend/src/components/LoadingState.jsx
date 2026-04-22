/**
 * Cinematic loading state with goofy charm.
 */
export default function LoadingState({ message = 'Summoning the goblins...', className = '' }) {
  return (
    <div
      className={`flex flex-col items-center justify-center gap-6 py-16 ${className}`}
      role="status"
      aria-live="polite"
    >
      {/* Animated goblin mascot */}
      <div className="relative">
        <div className="text-6xl sm:text-7xl animate-float select-none" aria-hidden="true">
          🦎
        </div>
        {/* Sparkle effects */}
        <div className="absolute -top-2 -right-2 text-lg animate-pulse" aria-hidden="true">
          ✨
        </div>
        <div
          className="absolute -bottom-1 -left-3 text-base animate-pulse"
          style={{ animationDelay: '0.5s' }}
          aria-hidden="true"
        >
          ⭐
        </div>
      </div>

      {/* Loading message */}
      <p className="text-lg text-cream-muted font-display italic text-center max-w-sm">
        {message}
      </p>

      {/* Animated loading bar */}
      <div className="w-48 h-1 bg-charcoal rounded-full overflow-hidden">
        <div
          className="h-full bg-gradient-to-r from-gold via-gold-bright to-gold rounded-full animate-shimmer"
          style={{
            backgroundSize: '200% 100%',
            width: '60%',
          }}
        />
      </div>

      {/* Screen reader text */}
      <span className="sr-only">Loading, please wait.</span>
    </div>
  );
}
