/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // Deep backgrounds — neutral, engineered
        void: '#0a0a0f',
        ink: '#111118',
        charcoal: '#1a1a24',
        'charcoal-light': '#222230',

        // Primary accent — gold as metal, not primary color
        gold: {
          DEFAULT: '#d4af37',
          bright: '#e4c25a',
          muted: '#c9a84c',
          dark: '#a68a2e',
          dim: '#6b5b1e',
        },

        // Secondary accents — controlled use
        emerald: {
          DEFAULT: '#2d5a3d',
          bright: '#3d7a52',
          muted: '#4a9b6b',
          dark: '#1e3d2a',
        },
        ruby: {
          DEFAULT: '#8b1c3b',
          bright: '#a8254a',
          muted: '#c44b4b',
          dark: '#5c1227',
        },
        sapphire: {
          DEFAULT: '#1e3a5f',
          bright: '#2e5282',
          dark: '#142840',
        },

        // Text — tighter opacity scale
        cream: '#f5f0e1',
        'cream-muted': '#b8b3a8',
        silver: '#8a8780',
        'silver-dim': '#5c5954',

        // Utility
        jade: '#4a9b6b',
        crimson: '#c44b4b',
      },
      fontFamily: {
        display: ['"Playfair Display"', 'Georgia', 'serif'],
        body: ['"Inter"', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        /* Subtle, controlled — no glow bombs */
        'product': '0 1px 3px rgba(0,0,0,0.4), 0 0 0 1px rgba(255,255,255,0.04)',
        'product-hover': '0 4px 12px rgba(0,0,0,0.5), 0 0 0 1px rgba(255,255,255,0.06)',
      },
      borderRadius: {
        /* Tighter scale — tooling, not pillows */
        'sm': '6px',
        'DEFAULT': '8px',
        'lg': '10px',
        'xl': '12px',
        '2xl': '14px',
      },
      animation: {
        'pulse-slow': 'pulse 4s ease-in-out infinite',
      },
    },
  },
  plugins: [],
};
