import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  createEvent,
  getEvent,
  joinParticipant,
  saveAvailability,
  getPublicResults,
  getHostResults,
  finalizeEvent,
  getFinalSelection,
  getIcsUrl,
} from './api.js';

describe('API wrapper', () => {
  beforeEach(() => {
    global.fetch = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function mockResponse(response, options = {}) {
    const { status = 200, headers = {} } = options;
    return {
      ok: status >= 200 && status < 300,
      status,
      statusText: status === 204 ? 'No Content' : 'OK',
      headers: new Map(Object.entries(headers)),
      json: () => Promise.resolve(response),
      text: () => Promise.resolve(typeof response === 'string' ? response : JSON.stringify(response)),
    };
  }

  // ─── createEvent ───────────────────────────────────────────────────────────

  it('createEvent sends correct POST body and returns parsed response', async () => {
    const eventData = {
      title: 'Dragon Raid',
      timezone: 'America/New_York',
      slotMinutes: 30,
      durationMinutes: 60,
      startDate: '2024-08-01',
      endDate: '2024-08-03',
      dailyStartTime: '09:00',
      dailyEndTime: '18:00',
      resultsVisibility: 'aggregate_public',
    };
    const mockData = { publicId: 'abc123', hostToken: 'host_xyz' };
    global.fetch.mockResolvedValue(
      mockResponse(mockData, { headers: { 'content-type': 'application/json' } })
    );

    const result = await createEvent(eventData);

    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/events'),
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
        body: JSON.stringify(eventData),
      })
    );
    expect(result).toEqual({ ok: true, data: mockData, error: null });
  });

  // ─── getEvent ──────────────────────────────────────────────────────────────

  it('getEvent calls correct URL', async () => {
    const publicId = 'evt-123';
    const mockData = { title: 'Test Event', candidateSlotsUtc: [] };
    global.fetch.mockResolvedValue(
      mockResponse(mockData, { headers: { 'content-type': 'application/json' } })
    );

    const result = await getEvent(publicId);

    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining(`/events/${publicId}`),
      expect.any(Object)
    );
    expect(result).toEqual({ ok: true, data: mockData, error: null });
  });

  // ─── joinParticipant ─────────────────────────────────────────────────────────

  it('joinParticipant sends { displayName, email } and reads participantToken from response', async () => {
    const publicId = 'evt-456';
    const participantData = { displayName: 'Gandalf', email: 'gandalf@middleearth.com' };
    const mockData = { participantToken: 'tok_abc123' };
    global.fetch.mockResolvedValue(
      mockResponse(mockData, { headers: { 'content-type': 'application/json' } })
    );

    const result = await joinParticipant(publicId, participantData);

    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining(`/events/${publicId}/participants`),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ displayName: 'Gandalf', email: 'gandalf@middleearth.com' }),
      })
    );
    expect(result).toEqual({ ok: true, data: mockData, error: null });
    expect(result.data.participantToken).toBe('tok_abc123');
  });

  // ─── saveAvailability ────────────────────────────────────────────────────────

  it('saveAvailability sends { items: [{ slotStartUtc, weight }] }', async () => {
    const publicId = 'evt-789';
    const token = 'tok_xyz';
    const payload = {
      items: [
        { slotStartUtc: '2024-08-01T09:00:00Z', weight: 1.0 },
        { slotStartUtc: '2024-08-01T10:00:00Z', weight: 0.6 },
      ],
    };
    global.fetch.mockResolvedValue(
      mockResponse({ saved: true }, { headers: { 'content-type': 'application/json' } })
    );

    const result = await saveAvailability(publicId, token, payload);

    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining(`/events/${publicId}/participants/${token}/availability`),
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify(payload),
      })
    );
    expect(result.ok).toBe(true);
  });

  // ─── Error handling ──────────────────────────────────────────────────────────

  it('error handling returns { ok: false, error: { status, message } } for HTTP error', async () => {
    const errorBody = { message: 'Event not found', error: 'NotFound' };
    global.fetch.mockResolvedValue({
      ok: false,
      status: 404,
      statusText: 'Not Found',
      headers: new Map([['content-type', 'application/json']]),
      json: () => Promise.resolve(errorBody),
      text: () => Promise.resolve(JSON.stringify(errorBody)),
    });

    const result = await getEvent('missing-id');

    expect(result.ok).toBe(false);
    expect(result.data).toBeNull();
    expect(result.error).toMatchObject({
      status: 404,
      message: 'Event not found',
    });
  });

  it('handles network errors with status 0', async () => {
    global.fetch.mockRejectedValue(new Error('Failed to fetch'));

    const result = await getEvent('any-id');

    expect(result.ok).toBe(false);
    expect(result.error.status).toBe(0);
    expect(result.error.message).toContain('Failed to fetch');
  });

  // ─── 204 response handling ─────────────────────────────────────────────────

  it('handles 204 No Content responses', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      status: 204,
      statusText: 'No Content',
      headers: new Map(),
      json: () => Promise.resolve(null),
      text: () => Promise.resolve(''),
    });

    const result = await getPublicResults('evt-abc');

    expect(result.ok).toBe(true);
    expect(result.data).toBeNull();
    expect(result.error).toBeNull();
  });

  // ─── Additional functions smoke tests ──────────────────────────────────────

  it('getPublicResults calls correct URL', async () => {
    global.fetch.mockResolvedValue(
      mockResponse({ topSlots: [] }, { headers: { 'content-type': 'application/json' } })
    );

    await getPublicResults('evt-abc');

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/events/evt-abc/results'),
      expect.any(Object)
    );
  });

  it('getHostResults calls correct URL', async () => {
    global.fetch.mockResolvedValue(
      mockResponse({ topSlots: [] }, { headers: { 'content-type': 'application/json' } })
    );

    await getHostResults('host-tok');

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/host/host-tok/results'),
      expect.any(Object)
    );
  });

  it('finalizeEvent sends correct headers and body', async () => {
    global.fetch.mockResolvedValue(
      mockResponse({ finalized: true }, { headers: { 'content-type': 'application/json' } })
    );

    await finalizeEvent('evt-abc', '2024-08-01T09:00:00Z', 'host-tok');

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/events/evt-abc/finalize'),
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
          'X-Host-Token': 'host-tok',
        }),
        body: JSON.stringify({ slotStartUtc: '2024-08-01T09:00:00Z' }),
      })
    );
  });

  it('getFinalSelection calls correct URL', async () => {
    global.fetch.mockResolvedValue(
      mockResponse({ slotStartUtc: '2024-08-01T09:00:00Z' }, { headers: { 'content-type': 'application/json' } })
    );

    await getFinalSelection('evt-abc');

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/events/evt-abc/final'),
      expect.any(Object)
    );
  });

  it('getIcsUrl returns the ICS download URL string', () => {
    const url = getIcsUrl('evt-abc');
    expect(url).toContain('/events/evt-abc/final.ics');
  });
});
