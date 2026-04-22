/**
 * Product-grade container component.
 *
 * Design language: Linear/Palantir-style tooling interface.
 * - Tight border radius (10–12px), not pillowy
 * - Clean solid border, no glow, no gradient noise
 * - Depth from layering, not blur/shadow
 * - Quiet default state, subtle elevation on hover
 */
export default function Card({
  children,
  className = '',
  padding = 'md',
  border = 'default',
  hover = false,
  onClick,
  role,
  tabIndex,
  ariaLabel,
}) {
  const paddingStyles = {
    none: '',
    sm: 'p-3',
    md: 'p-4',
    lg: 'p-5',
    xl: 'p-6',
  };

  const borderStyles = {
    none: 'border-transparent',
    default: 'border-white/[0.08]',
    strong: 'border-white/[0.12]',
    gold: 'border-gold/20',
  };

  const hoverStyles = hover
    ? 'hover:border-white/[0.14] hover:bg-[rgba(24,24,28,0.85)] transition-colors duration-150 cursor-pointer'
    : '';

  return (
    <div
      className={`
        rounded-xl bg-[rgba(20,20,24,0.75)]
        border ${borderStyles[border]}
        ${paddingStyles[padding]}
        ${hoverStyles}
        ${className}
      `}
      onClick={onClick}
      role={role}
      tabIndex={tabIndex}
      aria-label={ariaLabel}
    >
      {children}
    </div>
  );
}
