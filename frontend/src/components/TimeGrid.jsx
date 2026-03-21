import { useCallback, useMemo, useRef, useState } from "react";
import { AVAILABILITY_OPTIONS, buildGrid, formatInstant } from "../utils";

export default function TimeGrid({ event, selections, onChange, copy }) {
  const columns = useMemo(() => buildGrid(event.candidateSlotsUtc, event.timezone), [event]);
  const [activeWeight, setActiveWeight] = useState(1.0);
  const [dragging, setDragging] = useState(false);
  const selectionsRef = useRef(selections);
  selectionsRef.current = selections;

  const applyWeight = useCallback(
    (slot) => {
      onChange({ ...selectionsRef.current, [slot]: activeWeight });
    },
    [activeWeight, onChange],
  );

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

  function handleTouchMove(e) {
    if (!dragging) return;
    const touch = e.touches[0];
    const el = document.elementFromPoint(touch.clientX, touch.clientY);
    const slotBtn = el?.closest("[data-slot]");
    if (slotBtn) {
      applyWeight(slotBtn.dataset.slot);
    }
  }

  const gridStyle = useMemo(() => {
    if (columns.length <= 2) {
      return { gridTemplateColumns: `repeat(${columns.length}, 1fr)` };
    }
    return {
      gridTemplateColumns: `repeat(${columns.length}, minmax(120px, 1fr))`,
      minWidth: `${columns.length * 120}px`,
    };
  }, [columns.length]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        {AVAILABILITY_OPTIONS.map((option) => (
          <button
            key={option.key}
            type="button"
            className={`btn rounded-full border px-3 py-2 text-sm font-semibold ${activeWeight === option.weight ? "border-black bg-black text-white" : "bg-white"}`}
            onClick={() => setActiveWeight(option.weight)}
          >
            {copy.availability[option.key]}
          </button>
        ))}
        <button type="button" className="btn rounded-full border px-3 py-2 text-sm" onClick={markEvenings}>
          {copy.grid.markEvenings}
        </button>
        <button type="button" className="btn rounded-full border px-3 py-2 text-sm" onClick={clearAll}>
          {copy.grid.clear}
        </button>
      </div>
      <div
        className="overflow-x-auto rounded-[28px] border border-black/10 bg-white/70 p-2 sm:p-3"
        onMouseUp={() => setDragging(false)}
        onMouseLeave={() => setDragging(false)}
        onTouchMove={handleTouchMove}
        onTouchEnd={() => setDragging(false)}
      >
        <div className="grid gap-2 sm:gap-3" style={{ ...gridStyle, touchAction: "none" }}>
          {columns.map((column) => (
            <div key={column.date} className="space-y-2">
              <div className="sticky top-0 rounded-2xl bg-slate-950 px-3 py-2 text-sm font-semibold text-white">{column.date}</div>
              {column.slots.map((slot) => {
                const option = AVAILABILITY_OPTIONS.find((item) => item.weight === (selections[slot] ?? 0));
                return (
                  <button
                    key={slot}
                    type="button"
                    data-slot={slot}
                    className={`block w-full rounded-2xl border px-3 py-3 text-left text-sm ${option?.color || "bg-white"}`}
                    onMouseDown={() => {
                      setDragging(true);
                      applyWeight(slot);
                    }}
                    onMouseEnter={() => dragging && applyWeight(slot)}
                    onClick={() => applyWeight(slot)}
                    onTouchStart={(e) => {
                      e.preventDefault();
                      setDragging(true);
                      applyWeight(slot);
                    }}
                  >
                    <div className="font-semibold">{formatInstant(slot, event.timezone)}</div>
                    <div className="text-xs opacity-70">{option ? copy.availability[option.key] : copy.availability.no}</div>
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
