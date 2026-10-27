import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { readFileSync } from "node:fs";
import { createHash } from "node:crypto";
import { workerSource } from "./service-worker.mjs";
const brand = Object.fromEntries(
  readFileSync(new URL("../shared/brand.properties", import.meta.url), "utf8")
    .split("\n")
    .filter((line) => line.includes("=") && !line.trim().startsWith("#"))
    .map((line) => {
      const i = line.indexOf("=");
      return [line.slice(0, i).trim(), line.slice(i + 1).trim()];
    }),
);
export default defineConfig({
  plugins: [
    react(),
    {
      name: "offline-shell",
      generateBundle(_, bundle) {
        const assets = Object.keys(bundle)
          .filter((name) => /\.(js|css|svg|woff2)$/.test(name))
          .map((name) => "/" + name);
        const version = createHash("sha256")
          .update(assets.join("|"))
          .digest("hex")
          .slice(0, 12);
        this.emitFile({
          type: "asset",
          fileName: "sw.js",
          source: workerSource(assets, version),
        });
      },
    },
    {
      name: "app-brand",
      transformIndexHtml: (html) =>
        html.replace(
          /<title>.*?<\/title>/,
          `<title>${brand.displayName.replaceAll("&", "&amp;").replaceAll("<", "&lt;")} · Seu próximo nível</title>`,
        ),
    },
  ],
  define: { __APP_BRAND__: JSON.stringify(brand) },
  server: { proxy: { "/api": "http://127.0.0.1:8097" } },
});
