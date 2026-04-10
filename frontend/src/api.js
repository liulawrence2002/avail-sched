const API_BASE = import.meta.env.VITE_API_BASE_URL || "/api";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
    ...options,
  });
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json") ? await response.json() : await response.text();
  if (!response.ok) {
    const message =
      typeof data === "object" && data !== null
        ? data.message || "Request failed"
        : typeof data === "string" && data
          ? data
          : "Request failed";
    const error = new Error(message);
    error.status = response.status;
    error.data = data;
    throw error;
  }
  return data;
}

export const api = {
  createEvent: (payload) => request("/events", { method: "POST", body: JSON.stringify(payload) }),
  getEvent: (publicId) => request(`/events/${publicId}`),
  joinEvent: (publicId, payload) => request(`/events/${publicId}/participants`, { method: "POST", body: JSON.stringify(payload) }),
  getParticipantAvailability: (publicId, token) => request(`/events/${publicId}/participants/${token}/availability`),
  saveAvailability: (publicId, token, payload) =>
    request(`/events/${publicId}/participants/${token}/availability`, { method: "PUT", body: JSON.stringify(payload) }),
  getResults: (publicId) => request(`/events/${publicId}/results`),
  getHostEvent: (hostToken) => request(`/host/${hostToken}`),
  getHostResults: (hostToken) => request(`/host/${hostToken}/results`),
  finalizeEvent: (publicId, hostToken, payload) =>
    request(`/events/${publicId}/finalize?hostToken=${encodeURIComponent(hostToken)}`, { method: "POST", body: JSON.stringify(payload) }),
  getFinal: (publicId) => request(`/events/${publicId}/final`),
  icsUrl: (publicId) => `${API_BASE}/events/${publicId}/final.ics`,
};
