import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const remoteHost = process.env.MACHINEVISION_ALLOWED_HOST;

export default defineConfig({
  plugins: [react()],
  server: {
    host: "0.0.0.0",
    allowedHosts: remoteHost ? [remoteHost] : [],
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
