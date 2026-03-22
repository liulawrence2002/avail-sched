export const AVAILABILITY_OPTIONS = [
  { key: "yes", weight: 1.0 },
  { key: "maybe", weight: 0.6 },
  { key: "bribe", weight: 0.3 },
  { key: "no", weight: 0.0 },
];

export function t(template, vars) {
  return template.replace(/\{(\w+)\}/g, (_, key) => vars[key] ?? "");
}

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
    const date = new Date(slot);
    const dateKey = new Intl.DateTimeFormat("en-CA", {
      timeZone: timezone,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }).format(date);

    if (!grouped[dateKey]) {
      grouped[dateKey] = {
        date: dateKey,
        title: new Intl.DateTimeFormat(undefined, {
          weekday: "short",
          month: "short",
          day: "numeric",
          timeZone: timezone,
        }).format(date),
        subtitle: new Intl.DateTimeFormat(undefined, {
          year: "numeric",
          timeZone: timezone,
        }).format(date),
        slots: [],
      };
    }

    grouped[dateKey].slots.push(slot);
  });

  return Object.values(grouped);
}
