import { useEffect, useRef, useState } from 'react';

/**
 * CinematicHeroBackground — layered video/poster background.
 *
 * Reduced intensity by ~20% for a quieter, more product-grade hero.
 * No animated orbs. No excessive glow. Depth through layering, not blur.
 */

const VIDEO_SRC = '/hero-video.mp4';
const POSTER_SRC = '/hero-poster.jpg';

export default function CinematicHeroBackground() {
  const videoRef = useRef(null);
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false);
  const [videoState, setVideoState] = useState('idle');

  useEffect(() => {
    const mql = window.matchMedia('(prefers-reduced-motion: reduce)');
    setPrefersReducedMotion(mql.matches);
    const handler = (e) => setPrefersReducedMotion(e.matches);
    mql.addEventListener('change', handler);
    return () => mql.removeEventListener('change', handler);
  }, []);

  useEffect(() => {
    if (prefersReducedMotion) return;
    if (!videoRef.current) return;

    const video = videoRef.current;

    const onCanPlay = () => {
      setVideoState('playing');
      video.play().catch(() => setVideoState('error'));
    };
    const onError = () => setVideoState('error');

    video.addEventListener('canplaythrough', onCanPlay);
    video.addEventListener('error', onError);
    setVideoState('loading');
    video.load();

    return () => {
      video.removeEventListener('canplaythrough', onCanPlay);
      video.removeEventListener('error', onError);
    };
  }, [prefersReducedMotion]);

  const showVideo = !prefersReducedMotion && videoState === 'playing';

  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none" aria-hidden="true">
      {/* Layer 0: Solid base */}
      <div className="absolute inset-0 bg-velvet" />

      {/* Layer 1: Poster image — reduced brightness */}
      <img
        src={POSTER_SRC}
        alt=""
        className={`absolute inset-0 w-full h-full object-cover transition-opacity duration-1000 ${
          showVideo ? 'opacity-0' : 'opacity-100'
        }`}
        style={{ filter: 'brightness(0.55) saturate(1.05)' }}
        loading="eager"
        decoding="async"
      />

      {/* Layer 2: Video element */}
      {!prefersReducedMotion && (
        <video
          ref={videoRef}
          src={VIDEO_SRC}
          poster={POSTER_SRC}
          preload="metadata"
          muted
          loop
          playsInline
          autoPlay={false}
          aria-hidden="true"
          className={`absolute inset-0 w-full h-full object-cover transition-opacity duration-1000 ${
            showVideo ? 'opacity-100' : 'opacity-0'
          }`}
          style={{ filter: 'brightness(0.55) saturate(1.05)' }}
        />
      )}

      {/* Layer 3: Dark overlay — stronger for text clarity */}
      <div
        className="absolute inset-0"
        style={{
          background: `
            radial-gradient(ellipse at 50% 50%, rgba(10, 10, 15, 0.55) 0%, rgba(10, 10, 15, 0.82) 100%)
          `,
        }}
      />

      {/* Layer 4: Subtle warm tint — reduced */}
      <div
        className="absolute inset-0"
        style={{
          background:
            'radial-gradient(ellipse at 60% 35%, rgba(212, 175, 55, 0.035) 0%, transparent 55%)',
        }}
      />

      {/* Layer 5: Bottom vignette — seamless content transition */}
      <div
        className="absolute inset-0"
        style={{
          background:
            'linear-gradient(to bottom, transparent 45%, rgba(10, 10, 15, 0.9) 100%)',
        }}
      />

      {/* Layer 6: Top edge gradient — subtle depth under header */}
      <div
        className="absolute top-0 left-0 right-0 h-20"
        style={{
          background:
            'linear-gradient(to bottom, rgba(10, 10, 15, 0.35) 0%, transparent 100%)',
        }}
      />
    </div>
  );
}
