import { useCallback, useMemo, useRef, useState } from "react";
import { AVAILABILITY_OPTIONS, buildGrid, formatInstant } from "../utils";

export default function TimeGrid({ event, selections, onChange, copy, disabled = false }) {
  const columns = useMemo(() => buildGrid(event.candidateSlotsUtc, event.timezone), [event.candidateSlotsUtc, event.timezone]);
  const [activeWeight, setActiveWeight] = useState(1.0);
  const [dragging, setDragging] = useState(false);
  const selectionsRef = useRef(selections);
  selectionsRef.current = selections;

  const applyWeight = useCallback(
    (slot) => {
      if (disabled) {
        return;
      }
      onChange({ ...selectionsRef.current, [slot]: activeWeight });
    },
    [activeWeight, disabled, onChange],
  );

  function clearAll() {
    if (disabled) {
      return;
    }
    onChange({});
  }

  function markEvenings() {
    if (disabled) {
      return;
    }
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
    const slotButton = el?.closest("[data-slot]");
    if (slotButton) {
      applyWeight(slotButton.dataset.slot);
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
            className={`btn option-toggle ${activeWeight === option.weight ? "is-active" : ""}`}
            aria-pressed={activeWeight === option.weight}
            disabled={disabled}
            onClick={() => setActiveWeight(option.weight)}
          >
            {copy.availability[option.key]}
          </button>
        ))}

        <button type="button" className="btn btn-secondary rounded-full px-3 py-2 text-sm font-semibold" disabled={disabled} onClick={markEvenings}>
          {copy.grid.markEvenings}
        </button>

        <button type="button" className="btn btn-secondary rounded-full px-3 py-2 text-sm font-semibold" disabled={disabled} onClick={clearAll}>
          {copy.grid.clear}
        </button>
      </div>

      <div
        className="grid-shell"
        onMouseUp={() => setDragging(false)}
        onMouseLeave={() => setDragging(false)}
        onTouchMove={handleTouchMove}
        onTouchEnd={() => setDragging(false)}
      >
        <div className="grid gap-2 sm:gap-3" style={{ ...gridStyle, touchAction: disabled ? "auto" : "none" }}>
          {columns.map((column) => (
            <div key={column.date} className="day-column space-y-2">
              <div className="day-label">
                <div className="day-title">{column.title}</div>
                <div className="day-subtitle">{column.subtitle}</div>
              </div>

              {column.slots.map((slot) => {
                const option = AVAILABILITY_OPTIONS.find((item) => item.weight === (selections[slot] ?? 0));
                return (
                  <button
                    key={slot}
                    type="button"
                    data-slot={slot}
                    data-state={option?.key || "no"}
                    className="btn slot-button text-sm"
                    aria-pressed={Object.prototype.hasOwnProperty.call(selections, slot)}
                    disabled={disabled}
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
                    <div className="slot-label">{formatInstant(slot, event.timezone)}</div>
                    <div className="slot-meta">{option ? copy.availability[option.key] : copy.availability.no}</div>
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
