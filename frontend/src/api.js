const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

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
    throw new Error(data.message || "Request failed");
  }
  return data;
}

export const api = {
  createEvent: (payload) => request("/events", { method: "POST", body: JSON.stringify(payload) }),
  getEvent: (publicId) => request(`/events/${publicId}`),
  joinEvent: (publicId, payload) => request(`/events/${publicId}/participants`, { method: "POST", body: JSON.stringify(payload) }),
  saveAvailability: (publicId, token, payload) =>
    request(`/events/${publicId}/participants/${token}/availability`, { method: "PUT", body: JSON.stringify(payload) }),
  getResults: (publicId) => request(`/events/${publicId}/results`),
  getHostEvent: (hostToken) => request(`/host/${hostToken}`),
  finalizeEvent: (publicId, hostToken, payload) =>
    request(`/events/${publicId}/finalize?hostToken=${encodeURIComponent(hostToken)}`, { method: "POST", body: JSON.stringify(payload) }),
  getFinal: (publicId) => request(`/events/${publicId}/final`),
  icsUrl: (publicId) => `${API_BASE}/events/${publicId}/final.ics`,
};

