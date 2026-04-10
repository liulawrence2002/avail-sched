import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { api } from "./api";

function mockFetchJson(payload, init = {}) {
  return vi.fn().mockResolvedValue({
    ok: init.ok !== false,
    status: init.status ?? 200,
    headers: {
      get: (name) =>
        name.toLowerCase() === "content-type" ? "application/json" : null,
    },
    json: async () => payload,
    text: async () => JSON.stringify(payload),
  });
}

function mockFetchText(text, init = {}) {
  return vi.fn().mockResolvedValue({
    ok: init.ok !== false,
    status: init.status ?? 200,
    headers: {
      get: () => "text/plain",
    },
    json: async () => {
      throw new Error("not json");
    },
    text: async () => text,
  });
}

describe("api helpers", () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    globalThis.fetch = mockFetchJson({ ok: true });
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("createEvent POSTs JSON to /events", async () => {
    const payload = { title: "Demo", slotMinutes: 30 };
    await api.createEvent(payload);
    const [url, options] = globalThis.fetch.mock.calls[0];
    expect(url).toBe("/api/events");
    expect(options.method).toBe("POST");
    expect(options.body).toBe(JSON.stringify(payload));
    expect(options.headers["Content-Type"]).toBe("application/json");
  });

  it("getEvent GETs /events/:publicId", async () => {
    await api.getEvent("abc");
    const [url, options] = globalThis.fetch.mock.calls[0];
    expect(url).toBe("/api/events/abc");
    expect(options.method).toBeUndefined();
  });

  it("joinEvent POSTs to /events/:publicId/participants", async () => {
    await api.joinEvent("abc", { displayName: "Alice", email: "alice@example.com" });
    const [url, options] = globalThis.fetch.mock.calls[0];
    expect(url).toBe("/api/events/abc/participants");
    expect(options.method).toBe("POST");
    expect(options.body).toContain("Alice");
  });

  it("getParticipantAvailability GETs the participant-scoped URL", async () => {
    await api.getParticipantAvailability("abc", "tok");
    const [url] = globalThis.fetch.mock.calls[0];
    expect(url).toBe("/api/events/abc/participants/tok/availability");
  });

  it("saveAvailability PUTs the availability payload", async () => {
    const payload = { items: [{ slotStartUtc: "2026-04-01T09:00:00Z", weight: 1 }] };
    await api.saveAvailability("abc", "tok", payload);
    const [url, options] = globalThis.fetch.mock.calls[0];
    expect(url).toBe("/api/events/abc/participants/tok/availability");
    expect(options.method).toBe("PUT");
    expect(JSON.parse(options.body)).toEqual(payload);
  });

  it("getResults GETs /events/:publicId/results", async () => {
    await api.getResults("abc");
    expect(globalThis.fetch.mock.calls[0][0]).toBe("/api/events/abc/results");
  });

  it("getHostEvent GETs /host/:hostToken", async () => {
    await api.getHostEvent("host-tok");
    expect(globalThis.fetch.mock.calls[0][0]).toBe("/api/host/host-tok");
  });

  it("getHostResults GETs /host/:hostToken/results", async () => {
    await api.getHostResults("host-tok");
    expect(globalThis.fetch.mock.calls[0][0]).toBe("/api/host/host-tok/results");
  });

  it("finalizeEvent POSTs with host token in the X-Host-Token header", async () => {
    // Phase 1.2 moved the host token from the query string to the X-Host-Token header.
    await api.finalizeEvent("abc", "host tok/+", { slotStartUtc: "2026-04-01T09:00:00Z" });
    const [url, options] = globalThis.fetch.mock.calls[0];
    expect(url).toBe("/api/events/abc/finalize");
    expect(options.method).toBe("POST");
    expect(options.headers["X-Host-Token"]).toBe("host tok/+");
    expect(options.body).toBe(JSON.stringify({ slotStartUtc: "2026-04-01T09:00:00Z" }));
  });

  it("getFinal GETs /events/:publicId/final", async () => {
    await api.getFinal("abc");
    expect(globalThis.fetch.mock.calls[0][0]).toBe("/api/events/abc/final");
  });

  it("icsUrl returns the .ics download URL", () => {
    expect(api.icsUrl("abc")).toBe("/api/events/abc/final.ics");
  });

  it("throws an Error enriched with status and data on non-ok responses", async () => {
    globalThis.fetch = mockFetchJson({ message: "nope" }, { ok: false, status: 400 });
    await expect(api.getEvent("abc")).rejects.toMatchObject({
      message: "nope",
      status: 400,
      data: { message: "nope" },
    });
  });

  it("parses text responses when content-type is not JSON", async () => {
    globalThis.fetch = mockFetchText("plain body");
    const result = await api.getEvent("abc");
    expect(result).toBe("plain body");
  });
});
