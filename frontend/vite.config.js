// import { defineConfig } from 'vite'
// import react from '@vitejs/plugin-react'

// // https://vitejs.dev/config/
// export default defineConfig({
//   plugins: [react()]
// })

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Đổi target sang URL backend thật của bạn
const TARGET = 'http://163.61.111.120:8082'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Mọi request bắt đầu bằng /api sẽ được proxy sang backend
      '/api': {
        target: TARGET,
        changeOrigin: true,
        secure: false,          // nếu backend là https self-signed thì giữ false
        // Nếu muốn bỏ tiền tố '/api' khi chuyển tiếp (tùy backend mong đợi)
        // Ví dụ FE gọi /api/v1/auth/register -> BE nhận /v1/auth/register
        // rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
