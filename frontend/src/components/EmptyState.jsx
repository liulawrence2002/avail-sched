/**
 * Delightful empty states with goofy charm.
 */
export default function EmptyState({
  icon = '🦎',
  title = 'Nothing here yet',
  description = 'The goblins are still organizing.',
  action,
  className = '',
}) {
  return (
    <div
      className={`flex flex-col items-center justify-center text-center py-12 sm:py-16 px-4 ${className}`}
      role="status"
    >
      <div className="text-5xl sm:text-6xl mb-4 select-none opacity-80" aria-hidden="true">
        {icon}
      </div>
      <h3 className="font-display text-xl sm:text-2xl text-cream mb-2">{title}</h3>
      <p className="text-sm sm:text-base text-silver max-w-sm leading-relaxed mb-6">
        {description}
      </p>
      {action && <div>{action}</div>}
    </div>
  );
}
