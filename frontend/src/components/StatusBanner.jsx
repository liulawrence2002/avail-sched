export default function StatusBanner({ children, tone = "info", className = "" }) {
  return (
    <div className={`status-banner ${className}`.trim()} data-tone={tone}>
      {children}
    </div>
  );
}
