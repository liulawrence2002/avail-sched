import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const apiTarget = process.env.VITE_DEV_PROXY_TARGET || "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": apiTarget,
      "/api-docs": apiTarget,
      "/swagger-ui.html": apiTarget,
      "/swagger-ui": apiTarget,
    },
  },
  preview: {
    proxy: {
      "/api": apiTarget,
      "/api-docs": apiTarget,
      "/swagger-ui.html": apiTarget,
      "/swagger-ui": apiTarget,
    },
  },
});
