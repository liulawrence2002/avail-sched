/**
 * API wrapper for Goblin Scheduler backend.
 * All functions return structured data or error objects.
 * Base URL comes from VITE_API_BASE_URL env var, defaulting to "/api".
 */

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

/**
 * Standard fetch wrapper that returns { ok, data, error }.
 */
async function apiFetch(path, options = {}) {
  const url = `${API_BASE}${path}`;
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  try {
    const response = await fetch(url, { ...options, headers });

    if (response.status === 204) {
      return { ok: true, data: null, error: null };
    }

    const contentType = response.headers.get('content-type') || '';
    const isJson = contentType.includes('application/json');
    const data = isJson ? await response.json() : await response.text();

    if (!response.ok) {
      const errorMessage =
        data?.message || data?.error || `HTTP ${response.status}: ${response.statusText}`;
      return {
        ok: false,
        data: null,
        error: {
          status: response.status,
          message: errorMessage,
          body: data,
        },
      };
    }

    return { ok: true, data, error: null };
  } catch (networkError) {
    return {
      ok: false,
      data: null,
      error: {
        status: 0,
        message: networkError?.message || 'Network error. Is the backend running?',
        body: null,
      },
    };
  }
}

// ─── Events ──────────────────────────────────────────────────────────────────

/**
 * POST /api/events
 * Creates a new event.
 * @param {Object} eventData — { title, description?, timezone, slotMinutes, durationMinutes, startDate, endDate, dailyStartTime, dailyEndTime, resultsVisibility }
 */
export async function createEvent(eventData) {
  return apiFetch('/events', {
    method: 'POST',
    body: JSON.stringify(eventData),
  });
}

/**
 * GET /api/events/{publicId}
 * Fetches public event details.
 */
export async function getEvent(publicId) {
  return apiFetch(`/events/${encodeURIComponent(publicId)}`);
}

// ─── Participants ────────────────────────────────────────────────────────────

/**
 * POST /api/events/{publicId}/participants
 * Joins a participant to an event.
 * @param {Object} participantData — { displayName, email? }
 */
export async function joinParticipant(publicId, { displayName, email }) {
  return apiFetch(`/events/${encodeURIComponent(publicId)}/participants`, {
    method: 'POST',
    body: JSON.stringify({ displayName, email }),
  });
}

/**
 * GET /api/events/{publicId}/participants/{token}/availability
 * Gets a participant's saved availability.
 */
export async function getAvailability(publicId, token) {
  return apiFetch(
    `/events/${encodeURIComponent(publicId)}/participants/${encodeURIComponent(token)}/availability`
  );
}

/**
 * PUT /api/events/{publicId}/participants/{token}/availability
 * Saves a participant's availability selections.
 * @param {Object} payload — { items: [{slotStartUtc, weight}] }
 */
export async function saveAvailability(publicId, token, { items }) {
  return apiFetch(
    `/events/${encodeURIComponent(publicId)}/participants/${encodeURIComponent(token)}/availability`,
    {
      method: 'PUT',
      body: JSON.stringify({ items }),
    }
  );
}

// ─── Results ─────────────────────────────────────────────────────────────────

/**
 * GET /api/events/{publicId}/results
 * Public aggregate results (no auth required).
 */
export async function getPublicResults(publicId) {
  return apiFetch(`/events/${encodeURIComponent(publicId)}/results`);
}

/**
 * GET /api/host/{hostToken}/results
 * Host-only detailed results.
 */
export async function getHostResults(hostToken) {
  return apiFetch(`/host/${encodeURIComponent(hostToken)}/results`);
}

// ─── Finalize ──────────────────────────────────────────────────────────────

/**
 * POST /api/events/{publicId}/finalize
 * Host picks the final time slot.
 * @param {string} slotStartUtc — ISO string of the chosen slot
 * @param {string} hostToken — from the host URL
 */
export async function finalizeEvent(publicId, slotStartUtc, hostToken) {
  return apiFetch(`/events/${encodeURIComponent(publicId)}/finalize`, {
    method: 'POST',
    headers: {
      'X-Host-Token': hostToken,
    },
    body: JSON.stringify({ slotStartUtc }),
  });
}

/**
 * GET /api/events/{publicId}/final
 * Gets the finalized selection for an event.
 */
export async function getFinalSelection(publicId) {
  return apiFetch(`/events/${encodeURIComponent(publicId)}/final`);
}

/**
 * GET /api/events/{publicId}/final.ics
 * Returns the ICS calendar download URL as a plain string.
 */
export function getIcsUrl(publicId) {
  return `${API_BASE}/events/${encodeURIComponent(publicId)}/final.ics`;
}
