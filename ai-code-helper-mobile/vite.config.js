import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
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
