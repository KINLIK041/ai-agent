import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    host: 'localhost',
    port: 5173,
  },
  build: {
    // Netlify 部署时的基础路径
    // 如果部署在子路径，改为 '/ai-agent/' 或 '/'
    base: '/',
    // 生成 sourcemap 方便调试（生产环境可关闭）
    sourcemap: false,
    // 打包文件大小限制
    chunkSizeWarningLimit: 1000,
  },
  // 环境变量前缀（安全）
  envPrefix: 'VITE_',
});
