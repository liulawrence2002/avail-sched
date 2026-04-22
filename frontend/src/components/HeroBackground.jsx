/**
 * Cinematic animated background with layered SVG effects.
 * Pure CSS/SVG — no canvas for accessibility and performance.
 */
export default function HeroBackground() {
  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none" aria-hidden="true">
      {/* Deep base layer */}
      <div className="absolute inset-0 bg-velvet" />

      {/* Animated orbs / floating light blobs */}
      <div className="absolute top-[-10%] left-[-10%] w-[50vw] h-[50vw] rounded-full bg-gold/[0.03] blur-3xl animate-pulse-slow" />
      <div
        className="absolute top-[20%] right-[-15%] w-[40vw] h-[40vw] rounded-full bg-emerald/[0.04] blur-3xl animate-pulse-slow"
        style={{ animationDelay: '1.5s' }}
      />
      <div
        className="absolute bottom-[-10%] left-[20%] w-[45vw] h-[45vw] rounded-full bg-sapphire/[0.03] blur-3xl animate-pulse-slow"
        style={{ animationDelay: '3s' }}
      />

      {/* SVG grain / noise texture overlay */}
      <svg className="absolute inset-0 w-full h-full opacity-[0.03]">
        <filter id="noise">
          <feTurbulence type="fractalNoise" baseFrequency="0.8" numOctaves="4" stitchTiles="stitch" />
        </filter>
        <rect width="100%" height="100%" filter="url(#noise)" />
      </svg>

      {/* Subtle star-like dots */}
      <Stars />

      {/* Bottom vignette */}
      <div
        className="absolute inset-0"
        style={{
          background:
            'radial-gradient(ellipse at 50% 100%, transparent 40%, rgba(10,10,15,0.6) 100%)',
        }}
      />
    </div>
  );
}

function Stars() {
  // Deterministic "random" stars for SSR consistency
  const stars = Array.from({ length: 60 }, (_, i) => {
    const seed = i * 137.508;
    const x = ((Math.sin(seed) * 0.5 + 0.5) * 100).toFixed(1);
    const y = ((Math.cos(seed * 1.3) * 0.5 + 0.5) * 100).toFixed(1);
    const r = (Math.sin(seed * 2.1) * 0.8 + 1.2).toFixed(1);
    const opacity = (Math.sin(seed * 3.7) * 0.03 + 0.06).toFixed(3);
    const delay = ((i % 5) * 0.8).toFixed(1);
    return { x, y, r, opacity, delay };
  });

  return (
    <svg className="absolute inset-0 w-full h-full" xmlns="http://www.w3.org/2000/svg">
      {stars.map((s, i) => (
        <circle
          key={i}
          cx={`${s.x}%`}
          cy={`${s.y}%`}
          r={s.r}
          fill="#f5f0e1"
          opacity={s.opacity}
          className="animate-pulse-slow"
          style={{ animationDelay: `${s.delay}s` }}
        />
      ))}
    </svg>
  );
}
