import { useMemo, useState } from "react";
import { AVAILABILITY_OPTIONS, buildGrid, formatInstant } from "../utils";

export default function TimeGrid({ event, selections, onChange }) {
  const columns = useMemo(() => buildGrid(event.candidateSlotsUtc, event.timezone), [event]);
  const [activeWeight, setActiveWeight] = useState(1.0);
  const [dragging, setDragging] = useState(false);

  function applyWeight(slot) {
    onChange({ ...selections, [slot]: activeWeight });
  }

  function clearAll() {
    onChange({});
  }

  function markEvenings() {
    const next = { ...selections };
    event.candidateSlotsUtc.forEach((slot) => {
      const hour = Number(new Intl.DateTimeFormat("en-US", { hour: "numeric", hour12: false, timeZone: event.timezone }).format(new Date(slot)));
      if (hour >= 17) {
        next[slot] = 1.0;
      }
    });
    onChange(next);
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        {AVAILABILITY_OPTIONS.map((option) => (
          <button
            key={option.label}
            type="button"
            className={`rounded-full border px-3 py-2 text-sm font-semibold ${activeWeight === option.weight ? "border-black bg-black text-white" : "bg-white"}`}
            onClick={() => setActiveWeight(option.weight)}
          >
            {option.label}
          </button>
        ))}
        <button type="button" className="rounded-full border px-3 py-2 text-sm" onClick={markEvenings}>
          Mark all evenings
        </button>
        <button type="button" className="rounded-full border px-3 py-2 text-sm" onClick={clearAll}>
          Clear
        </button>
      </div>
      <div
        className="overflow-x-auto rounded-[28px] border border-black/10 bg-white/70 p-3"
        onMouseUp={() => setDragging(false)}
        onMouseLeave={() => setDragging(false)}
      >
        <div className="grid min-w-[720px] gap-3" style={{ gridTemplateColumns: `repeat(${columns.length}, minmax(140px, 1fr))` }}>
          {columns.map((column) => (
            <div key={column.date} className="space-y-2">
              <div className="sticky top-0 rounded-2xl bg-slate-950 px-3 py-2 text-sm font-semibold text-white">{column.date}</div>
              {column.slots.map((slot) => {
                const option = AVAILABILITY_OPTIONS.find((item) => item.weight === (selections[slot] ?? 0));
                return (
                  <button
                    key={slot}
                    type="button"
                    className={`block w-full rounded-2xl border px-3 py-3 text-left text-sm ${option?.color || "bg-white"}`}
                    onMouseDown={() => {
                      setDragging(true);
                      applyWeight(slot);
                    }}
                    onMouseEnter={() => dragging && applyWeight(slot)}
                    onClick={() => applyWeight(slot)}
                  >
                    <div className="font-semibold">{formatInstant(slot, event.timezone)}</div>
                    <div className="text-xs opacity-70">{option?.label || "No"}</div>
                  </button>
                );
              })}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
