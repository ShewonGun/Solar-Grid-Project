import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss()
  ],
  server: {
    // 5173 is the origin allowed by the Web API's CORS policy. strictPort makes
    // a second `npm run dev` fail loudly instead of silently starting on 5174,
    // which leaves a stale server serving the old build.
    port: 5173,
    strictPort: true,
  },
})
