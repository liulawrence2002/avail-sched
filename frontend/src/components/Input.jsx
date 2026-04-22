import { forwardRef } from 'react';

/**
 * Styled input with label, validation hints, and on-brand focus states.
 */
const Input = forwardRef(function Input(
  {
    label,
    id,
    type = 'text',
    placeholder,
    value,
    onChange,
    onBlur,
    error,
    hint,
    disabled = false,
    required = false,
    className = '',
    ...props
  },
  ref
) {
  const inputId = id || `input-${Math.random().toString(36).slice(2, 8)}`;
  const errorId = error ? `${inputId}-error` : undefined;
  const hintId = hint ? `${inputId}-hint` : undefined;

  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      {label && (
        <label
          htmlFor={inputId}
          className="text-sm font-medium text-cream-muted tracking-wide"
        >
          {label}
          {required && (
            <span className="text-crimson ml-1" aria-hidden="true">
              *
            </span>
          )}
        </label>
      )}
      <input
        ref={ref}
        id={inputId}
        type={type}
        value={value}
        onChange={onChange}
        onBlur={onBlur}
        disabled={disabled}
        required={required}
        placeholder={placeholder}
        aria-invalid={!!error}
        aria-describedby={errorId || hintId || undefined}
        className={`
          w-full px-4 py-3 rounded-xl
          bg-charcoal/60 border border-white/10
          text-cream placeholder-silver-dim/60
          transition-all duration-200
          hover:border-white/20
          focus:border-gold/50 focus:bg-charcoal-light/60
          disabled:opacity-50 disabled:cursor-not-allowed
          ${error ? 'border-crimson/50 focus:border-crimson' : ''}
        `}
        {...props}
      />
      {hint && !error && (
        <p id={hintId} className="text-xs text-silver-dim">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="text-xs text-crimson flex items-center gap-1">
          <svg className="h-3.5 w-3.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          {error}
        </p>
      )}
    </div>
  );
});

export default Input;
