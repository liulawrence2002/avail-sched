import { tokens } from "./src/styles/tokens.js";

/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        bg: tokens.colors.bg,
        surface: tokens.colors.surface,
        "surface-strong": tokens.colors.surfaceStrong,
        "surface-ghost": tokens.colors.surfaceGhost,
        "surface-deep": tokens.colors.surfaceDeep,
        text: tokens.colors.text,
        muted: tokens.colors.muted,
        line: tokens.colors.line,
        "line-strong": tokens.colors.lineStrong,
        "accent-serious": tokens.colors.accentSerious,
        "accent-goblin": tokens.colors.accentGoblin,
        "accent-warm": tokens.colors.accentWarm,
      },
      borderRadius: {
        "token-md": tokens.radius.md,
        "token-lg": tokens.radius.lg,
        "token-xl": tokens.radius.xl,
      },
      boxShadow: {
        "token-soft": tokens.shadow.soft,
        "token-lg": tokens.shadow.lg,
        "token-xl": tokens.shadow.xl,
      },
      fontFamily: {
        ui: tokens.fonts.ui,
        display: tokens.fonts.display,
      },
    },
  },
  plugins: [],
};
