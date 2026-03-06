export default function Card({ children, className = "" }) {
  return <div className={`rounded-[28px] border border-black/10 bg-white/80 p-5 shadow-[0_12px_40px_rgba(0,0,0,0.08)] ${className}`}>{children}</div>;
}

