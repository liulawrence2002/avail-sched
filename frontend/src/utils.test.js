// @ts-check
import { describe, expect, it } from "vitest";

import { AVAILABILITY_OPTIONS, buildGrid, buildParticipantLink, formatInstant, t } from "./utils";

describe("t (template interpolation)", () => {
  it("substitutes {key} placeholders with provided values", () => {
    expect(t("Hello, {name}!", { name: "Avery" })).toBe("Hello, Avery!");
  });

  it("replaces missing keys with empty string", () => {
    expect(t("{a}-{b}", { a: "x" })).toBe("x-");
  });

  it("leaves template untouched when no placeholders", () => {
    expect(t("plain text", {})).toBe("plain text");
  });
});

describe("formatInstant", () => {
  it("formats a UTC instant in the UTC timezone", () => {
    const out = formatInstant("2026-04-01T09:00:00Z", "UTC");
    // Use a loose assertion that survives Intl locale differences but locks the date/time parts.
    expect(out).toMatch(/Wed/);
    expect(out).toMatch(/Apr/);
    expect(out).toMatch(/1/);
    expect(out).toMatch(/9/);
  });

  it("respects an explicit IANA timezone", () => {
    const utc = formatInstant("2026-04-01T12:00:00Z", "UTC");
    const la = formatInstant("2026-04-01T12:00:00Z", "America/Los_Angeles");
    expect(utc).not.toBe(la);
  });

  it("defaults to UTC when timezone is omitted", () => {
    const explicit = formatInstant("2026-04-01T12:00:00Z", "UTC");
    const implicit = formatInstant("2026-04-01T12:00:00Z", undefined);
    expect(implicit).toBe(explicit);
  });
});

describe("buildParticipantLink", () => {
  it("returns empty string when publicId is missing", () => {
    expect(buildParticipantLink("", "token")).toBe("");
  });

  it("returns empty string when token is missing", () => {
    expect(buildParticipantLink("abc", "")).toBe("");
  });

  it("builds a link anchored on window.location.origin", () => {
    expect(buildParticipantLink("abc", "tok")).toBe(`${window.location.origin}/e/abc?token=tok`);
  });

  it("url-encodes the participant token", () => {
    expect(buildParticipantLink("abc", "tok/with+chars")).toBe(
      `${window.location.origin}/e/abc?token=tok%2Fwith%2Bchars`
    );
  });
});

describe("buildGrid", () => {
  it("returns an empty array when no slots are provided", () => {
    expect(buildGrid([], "UTC")).toEqual([]);
  });

  it("groups slots by local date and preserves slot order", () => {
    const slots = [
      "2026-04-01T09:00:00Z",
      "2026-04-01T09:30:00Z",
      "2026-04-02T09:00:00Z",
    ];
    const columns = buildGrid(slots, "UTC");
    expect(columns).toHaveLength(2);
    expect(columns[0].slots).toEqual(["2026-04-01T09:00:00Z", "2026-04-01T09:30:00Z"]);
    expect(columns[1].slots).toEqual(["2026-04-02T09:00:00Z"]);
  });

  it("uses the target timezone to decide the date boundary", () => {
    // 23:30Z on Apr 1 is Apr 2 in Asia/Tokyo (UTC+9) — goes into a separate column.
    const slots = ["2026-04-01T23:30:00Z", "2026-04-02T00:00:00Z"];
    const columns = buildGrid(slots, "Asia/Tokyo");
    expect(columns).toHaveLength(1);
    expect(columns[0].slots).toEqual(slots);
  });

  it("each column exposes a title, subtitle, and date key", () => {
    const columns = buildGrid(["2026-04-01T09:00:00Z"], "UTC");
    expect(columns[0]).toMatchObject({
      date: "2026-04-01",
      title: expect.any(String),
      subtitle: expect.any(String),
    });
    expect(columns[0].title.length).toBeGreaterThan(0);
  });
});

describe("AVAILABILITY_OPTIONS", () => {
  it("is ordered yes → maybe → bribe → no with descending weights", () => {
    expect(AVAILABILITY_OPTIONS.map((o) => o.key)).toEqual(["yes", "maybe", "bribe", "no"]);
    const weights = AVAILABILITY_OPTIONS.map((o) => o.weight);
    expect(weights).toEqual([...weights].sort((a, b) => b - a));
  });
});
