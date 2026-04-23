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

/**
 * PATCH /api/host/{hostToken}
 * Updates an event.
 */
export async function updateEvent(hostToken, eventData) {
  return apiFetch(`/host/${encodeURIComponent(hostToken)}`, {
    method: 'PATCH',
    body: JSON.stringify(eventData),
  });
}

/**
 * DELETE /api/host/{hostToken}
 * Soft-deletes an event.
 */
export async function deleteEvent(hostToken) {
  return apiFetch(`/host/${encodeURIComponent(hostToken)}`, {
    method: 'DELETE',
  });
}

/**
 * POST /api/events/lookup
 * Looks up events by host tokens.
 */
export async function lookupEvents(hostTokens) {
  return apiFetch('/events/lookup', {
    method: 'POST',
    body: JSON.stringify({ hostTokens }),
  });
}

/**
 * GET /api/events/{publicId}/notes
 * Gets the agenda/notes for an event.
 */
export async function getEventNotes(publicId) {
  return apiFetch(`/events/${encodeURIComponent(publicId)}/notes`);
}

/**
 * PUT /api/host/{hostToken}/notes
 * Saves or updates the agenda/notes for an event.
 */
export async function saveEventNotes(hostToken, content) {
  return apiFetch(`/host/${encodeURIComponent(hostToken)}/notes`, {
    method: 'PUT',
    body: JSON.stringify({ content }),
  });
}

/**
 * GET /api/events/{publicId}/comments
 * Gets all comments for an event.
 */
export async function getEventComments(publicId) {
  return apiFetch(`/events/${encodeURIComponent(publicId)}/comments`);
}

/**
 * POST /api/events/{publicId}/comments
 * Posts a comment to an event.
 */
export async function postEventComment(publicId, token, content, isHost = false) {
  const headers = {};
  if (isHost) headers['X-Host-Token'] = token;
  else headers['X-Participant-Token'] = token;
  return apiFetch(`/events/${encodeURIComponent(publicId)}/comments`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ content }),
  });
}

/**
 * GET /api/templates
 * Lists all event templates.
 */
export async function listTemplates() {
  return apiFetch('/templates');
}

/**
 * POST /api/templates
 * Creates a new event template.
 */
export async function createTemplate(data) {
  return apiFetch('/templates', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

/**
 * DELETE /api/templates/{id}
 * Deletes a template.
 */
export async function deleteTemplate(id) {
  return apiFetch(`/templates/${id}`, { method: 'DELETE' });
}

/**
 * GET /api/oauth/google/url
 * Gets the Google OAuth authorization URL.
 */
export async function getGoogleAuthUrl() {
  return apiFetch('/oauth/google/url');
}

/**
 * POST /api/events/{publicId}/calendar
 * Adds a finalized event to the host's Google Calendar.
 */
export async function addEventToCalendar(publicId, hostToken, providerUserId) {
  return apiFetch(`/events/${encodeURIComponent(publicId)}/calendar`, {
    method: 'POST',
    headers: { 'X-Host-Token': hostToken },
    body: JSON.stringify({ providerUserId }),
  });
}

/**
 * GET /api/host/{hostToken}/suggestions
 * Gets AI-style slot suggestions for an event.
 */
export async function getSuggestions(hostToken) {
  return apiFetch(`/host/${encodeURIComponent(hostToken)}/suggestions`);
}

/**
 * POST /api/events/{publicId}/nudge
 * Sends reminder emails to all non-respondents.
 */
export async function nudgeNonRespondents(publicId, hostToken) {
  return apiFetch(`/events/${encodeURIComponent(publicId)}/nudge`, {
    method: 'POST',
    headers: { 'X-Host-Token': hostToken },
  });
}

// ─── AI Features ────────────────────────────────────────────────────────────

/**
 * GET /api/ai/status
 * Check if AI features are available.
 */
export async function getAIStatus() {
  return apiFetch('/ai/status');
}

/**
 * POST /api/ai/parse-event
 * Parse natural language event description into structured fields.
 */
export async function parseEventText(text) {
  return apiFetch('/ai/parse-event', {
    method: 'POST',
    body: JSON.stringify({ text }),
  });
}

/**
 * GET /api/host/{hostToken}/ai-suggestions
 * Get AI-enhanced slot suggestions.
 */
export async function getAISuggestions(hostToken) {
  return apiFetch(`/host/${encodeURIComponent(hostToken)}/ai-suggestions`);
}

/**
 * POST /api/insights
 * Get dashboard insights for events.
 */
export async function getInsights(hostTokens) {
  return apiFetch('/insights', {
    method: 'POST',
    body: JSON.stringify({ hostTokens }),
  });
}

/**
 * POST /api/host/{hostToken}/chat
 * Send a chat message to the AI assistant.
 */
export async function sendChatMessage(hostToken, message) {
  return apiFetch(`/host/${encodeURIComponent(hostToken)}/chat`, {
    method: 'POST',
    body: JSON.stringify({ message }),
  });
}

/**
 * GET /api/host/{hostToken}/chat
 * Get chat history for an event.
 */
export async function getChatHistory(hostToken) {
  return apiFetch(`/host/${encodeURIComponent(hostToken)}/chat`);
}

/**
 * POST /api/host/{hostToken}/generate-prep
 * Generate meeting prep notes.
 */
export async function generatePrepNotes(hostToken) {
  return apiFetch(`/host/${encodeURIComponent(hostToken)}/generate-prep`, {
    method: 'POST',
  });
}

/**
 * POST /api/host/{hostToken}/generate-followup
 * Generate a follow-up message draft.
 */
export async function generateFollowup(hostToken, variant) {
  return apiFetch(`/host/${encodeURIComponent(hostToken)}/generate-followup`, {
    method: 'POST',
    body: JSON.stringify({ variant }),
  });
}

/**
 * GET /api/host/{hostToken}/agent-actions
 * Get AI agent action log.
 */
export async function getAgentActions(hostToken) {
  return apiFetch(`/host/${encodeURIComponent(hostToken)}/agent-actions`);
}

/**
 * POST /api/ai/parse-recurrence
 * Parse a natural language recurrence pattern into concrete dates.
 */
export async function parseRecurrence(text, timezone) {
  return apiFetch('/ai/parse-recurrence', {
    method: 'POST',
    body: JSON.stringify({ text, timezone }),
  });
}

/**
 * POST /api/ai/create-series
 * Create a series of linked events from parsed recurrence dates.
 */
export async function createEventSeries(eventData, dates) {
  return apiFetch('/ai/create-series', {
    method: 'POST',
    body: JSON.stringify({ eventData, dates }),
  });
}
