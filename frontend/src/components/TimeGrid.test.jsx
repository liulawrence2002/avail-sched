import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import TimeGrid from "./TimeGrid";

const copy = {
  availability: {
    yes: "Yes",
    maybe: "Maybe",
    bribe: "Bribe",
    no: "No",
  },
  grid: {
    markEvenings: "Mark evenings",
    clear: "Clear",
  },
};

const baseEvent = {
  timezone: "UTC",
  candidateSlotsUtc: [
    "2026-04-01T09:00:00Z",
    "2026-04-01T09:30:00Z",
    "2026-04-01T17:00:00Z",
  ],
};

describe("TimeGrid", () => {
  it("renders one cell per candidate slot", () => {
    const onChange = vi.fn();
    render(
      <TimeGrid event={baseEvent} selections={{}} onChange={onChange} copy={copy} />
    );
    expect(screen.getAllByRole("button", { name: /Apr|09:00|17:00|No/i }).length).toBeGreaterThanOrEqual(
      baseEvent.candidateSlotsUtc.length
    );
  });

  it("click on a slot calls onChange with that slot weighted as 1 (yes by default)", () => {
    const onChange = vi.fn();
    render(
      <TimeGrid event={baseEvent} selections={{}} onChange={onChange} copy={copy} />
    );
    const firstSlotCell = document.querySelector(
      `[data-slot="2026-04-01T09:00:00Z"]`
    );
    expect(firstSlotCell).not.toBeNull();
    fireEvent.mouseDown(firstSlotCell);
    expect(onChange).toHaveBeenCalledWith({
      "2026-04-01T09:00:00Z": 1.0,
    });
  });

  it("switching active weight to 'maybe' applies 0.6 on subsequent click", () => {
    const onChange = vi.fn();
    render(
      <TimeGrid event={baseEvent} selections={{}} onChange={onChange} copy={copy} />
    );
    fireEvent.click(screen.getByRole("button", { name: "Maybe" }));
    const slotCell = document.querySelector(`[data-slot="2026-04-01T09:30:00Z"]`);
    fireEvent.mouseDown(slotCell);
    expect(onChange).toHaveBeenCalledWith({ "2026-04-01T09:30:00Z": 0.6 });
  });

  it("drag selection (mouseDown then mouseEnter) applies the active weight to both cells", () => {
    const onChange = vi.fn();
    render(
      <TimeGrid event={baseEvent} selections={{}} onChange={onChange} copy={copy} />
    );
    const firstCell = document.querySelector(`[data-slot="2026-04-01T09:00:00Z"]`);
    const secondCell = document.querySelector(`[data-slot="2026-04-01T09:30:00Z"]`);
    fireEvent.mouseDown(firstCell);
    fireEvent.mouseEnter(secondCell);
    expect(onChange).toHaveBeenCalledTimes(2);
    expect(onChange.mock.calls[0][0]).toEqual({ "2026-04-01T09:00:00Z": 1.0 });
    expect(onChange.mock.calls[1][0]).toEqual({ "2026-04-01T09:30:00Z": 1.0 });
  });

  it("clear button empties the selections", () => {
    const onChange = vi.fn();
    render(
      <TimeGrid
        event={baseEvent}
        selections={{ "2026-04-01T09:00:00Z": 1.0 }}
        onChange={onChange}
        copy={copy}
      />
    );
    fireEvent.click(screen.getByRole("button", { name: "Clear" }));
    expect(onChange).toHaveBeenCalledWith({});
  });

  it("mark evenings button sets weight 1 on all slots whose hour is >= 17 in the event timezone", () => {
    const onChange = vi.fn();
    render(
      <TimeGrid event={baseEvent} selections={{}} onChange={onChange} copy={copy} />
    );
    fireEvent.click(screen.getByRole("button", { name: "Mark evenings" }));
    expect(onChange).toHaveBeenCalledWith({
      "2026-04-01T17:00:00Z": 1.0,
    });
  });

  it("disabled grid ignores clicks", () => {
    const onChange = vi.fn();
    render(
      <TimeGrid event={baseEvent} selections={{}} onChange={onChange} copy={copy} disabled />
    );
    const firstCell = document.querySelector(`[data-slot="2026-04-01T09:00:00Z"]`);
    fireEvent.mouseDown(firstCell);
    expect(onChange).not.toHaveBeenCalled();
  });
});
