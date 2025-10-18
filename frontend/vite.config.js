import path from "node:path";
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Trỏ đúng vào backend thật
const TARGET = 'https://carbonx.io.vn';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: TARGET,
        changeOrigin: true,
        secure: false, // Giữ false để tránh lỗi chứng chỉ khi dùng HTTPS dev
        // Không rewrite vì backend đã có /api rồi
      },
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
   build: {
    sourcemap: true,
  },
});

