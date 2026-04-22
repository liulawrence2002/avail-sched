/**
 * Polished non-blocking error display.
 */
export default function ErrorMessage({
  title,
  message,
  onRetry,
  className = '',
  variant = 'card',
}) {
  const content = (
    <div className={`flex flex-col sm:flex-row items-start gap-4 ${className}`}>
      <div className="flex-shrink-0 p-2 rounded-xl bg-ruby/10 border border-ruby/20">
        <svg
          className="h-6 w-6 text-crimson"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={1.5}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"
          />
        </svg>
      </div>
      <div className="flex-1 min-w-0">
        {title && (
          <h3 className="text-base font-semibold text-crimson mb-1">{title}</h3>
        )}
        <p className="text-sm text-cream-muted leading-relaxed">
          {message || 'Something went wrong. Please try again.'}
        </p>
        {onRetry && (
          <button
            onClick={onRetry}
            className="mt-3 text-sm text-gold hover:text-gold-bright font-medium inline-flex items-center gap-1.5 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold rounded-lg px-1"
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182"
              />
            </svg>
            Try again
          </button>
        )}
      </div>
    </div>
  );

  if (variant === 'inline') return content;

  return (
    <div
      className="glass-card rounded-2xl p-5 sm:p-6 border border-ruby/15 shadow-glass"
      role="alert"
      aria-live="assertive"
    >
      {content}
    </div>
  );
}
