/**
 * Product-grade button component.
 *
 * Design language: Linear-style precision.
 * - Sharper radius (8px), not marketing-rounded
 * - Solid fills, no gradients
 * - Fast, subtle interactions (no bounce, no glow)
 * - Clear states: default, hover, active, disabled, loading, success, error
 */
export default function Button({
  children,
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  success = false,
  error = false,
  type = 'button',
  onClick,
  className = '',
  ariaLabel,
  ...props
}) {
  const baseStyles =
    'inline-flex items-center justify-center gap-1.5 font-medium transition-all duration-150 rounded-lg focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-gold focus-visible:ring-offset-1 focus-visible:ring-offset-void';

  const sizeStyles = {
    sm: 'px-3 py-1.5 text-sm h-8',
    md: 'px-4 py-2 text-sm h-10',
    lg: 'px-6 py-2.5 text-base h-12',
  };

  const variantStyles = {
    primary:
      'bg-gold text-void hover:brightness-110 active:brightness-95',
    secondary:
      'bg-emerald text-cream hover:brightness-110 active:brightness-95',
    danger:
      'bg-ruby text-cream hover:brightness-110 active:brightness-95',
    ghost:
      'bg-transparent border border-white/[0.10] text-cream hover:bg-white/[0.04] hover:border-white/[0.16] active:bg-white/[0.06]',
    goldGhost:
      'bg-transparent border border-gold/25 text-gold hover:bg-gold/[0.06] hover:border-gold/40 active:bg-gold/[0.10]',
  };

  const stateStyles = disabled || loading ? 'opacity-50 cursor-not-allowed active:scale-100' : '';
  const activeStyles = !(disabled || loading) ? 'active:scale-[0.98]' : '';

  const computedVariant = success ? 'primary' : error ? 'danger' : variant;

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      aria-label={ariaLabel}
      aria-busy={loading}
      className={`${baseStyles} ${sizeStyles[size]} ${variantStyles[computedVariant]} ${stateStyles} ${activeStyles} ${className}`}
      {...props}
    >
      {loading && (
        <svg
          className="animate-spin h-3.5 w-3.5"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <circle
            className="opacity-30"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-90"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      )}
      {success && !loading && (
        <svg
          className="h-3.5 w-3.5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={3}
          aria-hidden="true"
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
        </svg>
      )}
      {error && !loading && (
        <svg
          className="h-3.5 w-3.5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2}
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"
          />
        </svg>
      )}
      <span>{children}</span>
    </button>
  );
}
