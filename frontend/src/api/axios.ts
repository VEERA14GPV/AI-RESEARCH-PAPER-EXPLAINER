import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL as string | undefined

if (!baseURL) {
  // Fails loudly at startup rather than silently hitting a relative path.
  console.error(
    'VITE_API_BASE_URL is not set. Copy .env.example to .env and set it (e.g. http://localhost:8080).',
  )
}

export const apiClient = axios.create({
  baseURL: baseURL ?? 'http://localhost:8080',
  timeout: 60_000, // paper ingestion (extract -> chunk -> embed) runs synchronously on upload
})
