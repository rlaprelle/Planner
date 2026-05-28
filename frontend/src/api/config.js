// Prefix prepended to every /api/v1/... path. Empty in dev (Vite proxies /api
// to the backend on localhost:8080). Set at build time in production via
// VITE_API_URL=https://api.echelplanner.com to target a cross-origin backend.
export const API_BASE = import.meta.env.VITE_API_URL || ''
