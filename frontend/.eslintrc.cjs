/**
 * Legacy (.eslintrc) config — ESLint 8.x.
 * Upgrading to flat config is deferred; see the project plan, Phase 0.2.
 */
module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true,
  },
  parserOptions: {
    ecmaVersion: 2022,
    sourceType: "module",
    ecmaFeatures: { jsx: true },
  },
  settings: {
    react: { version: "detect" },
    "import/resolver": {
      node: { extensions: [".js", ".jsx"] },
    },
  },
  extends: [
    "eslint:recommended",
    "plugin:react/recommended",
    "plugin:react/jsx-runtime",
    "plugin:react-hooks/recommended",
    "plugin:jsx-a11y/recommended",
    "plugin:import/recommended",
  ],
  plugins: ["react", "react-hooks", "jsx-a11y", "import"],
  rules: {
    "react/prop-types": "off",
    "no-unused-vars": ["warn", { argsIgnorePattern: "^_", varsIgnorePattern: "^_" }],
    "import/order": [
      "warn",
      {
        groups: ["builtin", "external", "internal", "parent", "sibling", "index"],
        "newlines-between": "always",
        alphabetize: { order: "asc", caseInsensitive: true },
      },
    ],
    "import/no-unresolved": "off",
  },
  overrides: [
    {
      files: ["**/*.test.{js,jsx}", "src/test/**/*.{js,jsx}"],
      env: { node: true },
      globals: {
        describe: "readonly",
        it: "readonly",
        test: "readonly",
        expect: "readonly",
        beforeAll: "readonly",
        afterAll: "readonly",
        beforeEach: "readonly",
        afterEach: "readonly",
        vi: "readonly",
      },
    },
    {
      files: [".eslintrc.cjs", "postcss.config.js", "tailwind.config.js", "vite.config.js", "vitest.config.js"],
      env: { node: true },
    },
  ],
  ignorePatterns: ["dist", "node_modules", "coverage", "build"],
};
