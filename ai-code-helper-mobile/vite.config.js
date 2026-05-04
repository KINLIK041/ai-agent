import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

import { cloudflare } from "@cloudflare/vite-plugin";

export default defineConfig({
  plugins: [vue(), cloudflare()],
  server: {
    host: 'localhost',
    port: 5174,
  },
  build: {
    base: '/',
    sourcemap: false,
    chunkSizeWarningLimit: 1000,
  },
  envPrefix: 'VITE_',
});