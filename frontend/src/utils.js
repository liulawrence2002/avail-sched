export const AVAILABILITY_OPTIONS = [
  { label: "Yes", weight: 1.0, color: "bg-emerald-400" },
  { label: "Maybe", weight: 0.6, color: "bg-amber-300" },
  { label: "If bribed with snacks", weight: 0.3, color: "bg-fuchsia-300" },
  { label: "No", weight: 0.0, color: "bg-slate-200" },
];

export function formatInstant(value, timezone) {
  return new Intl.DateTimeFormat(undefined, {
    weekday: "short",
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
    timeZone: timezone || "UTC",
  }).format(new Date(value));
}

export function buildGrid(candidateSlotsUtc, timezone) {
  const grouped = {};
  candidateSlotsUtc.forEach((slot) => {
    const dateKey = new Intl.DateTimeFormat("en-CA", { timeZone: timezone, year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date(slot));
    grouped[dateKey] = grouped[dateKey] || [];
    grouped[dateKey].push(slot);
  });
  return Object.entries(grouped).map(([date, slots]) => ({
    date,
    slots,
  }));
}

