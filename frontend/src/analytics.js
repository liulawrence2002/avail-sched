/**
 * Analytics stub — swap in a real provider (Plausible, PostHog, gtag) by
 * replacing the body of this function. Every call site stays the same.
 */
export function track(eventName, properties = {}) {
  if (import.meta.env.DEV) {
    console.log("[analytics]", eventName, properties);
  }

  // Plausible: window.plausible?.(eventName, { props: properties });
  // PostHog:   window.posthog?.capture(eventName, properties);
  // gtag:      window.gtag?.("event", eventName, properties);
}
