// @ts-check
/**
 * JS mirror of the design tokens that live in `tokens.css`. The CSS file is the runtime source
 * of truth (it drives `var(--foo)` references across every stylesheet); this file exists so
 * Tailwind's theme extension can consume the same values without duplicating them in string
 * form inside tailwind.config.js.
 *
 * When you add or rename a token, update both `tokens.css` and this file in the same commit.
 * The two are kept in lock-step by convention, not by tooling, so a mismatch is a bug.
 *
 * @typedef {Object} GoblinTokens
 * @property {Object<string, string>} colors
 * @property {Object<string, string>} radius
 * @property {Object<string, string>} shadow
 * @property {Object<string, string[]>} fonts
 */

/** @type {GoblinTokens} */
export const tokens = {
  colors: {
    bg: "#e9e7e1",
    surface: "rgba(245, 242, 236, 0.84)",
    surfaceStrong: "rgba(239, 233, 224, 0.94)",
    surfaceGhost: "rgba(255, 255, 255, 0.52)",
    surfaceDeep: "#1f2c39",
    text: "#14212d",
    muted: "#61707d",
    line: "rgba(20, 33, 45, 0.1)",
    lineStrong: "rgba(20, 33, 45, 0.18)",
    accentSerious: "#6f879f",
    accentGoblin: "#8ea45f",
    accentWarm: "#caa48a",
  },
  radius: {
    xl: "2rem",
    lg: "1.5rem",
    md: "1.15rem",
  },
  shadow: {
    soft: "0 18px 48px rgba(19, 29, 40, 0.08)",
    lg: "0 28px 90px rgba(19, 29, 40, 0.1)",
    xl: "0 44px 120px rgba(19, 29, 40, 0.14)",
  },
  fonts: {
    ui: ["Manrope", "system-ui", "sans-serif"],
    display: ["Instrument Serif", "Georgia", "serif"],
  },
};
