import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' }
})

// ── Request interceptor — attach JWT to every request ────────
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('stockx_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      // Every request automatically carries the JWT.
      // Backend JwtAuthenticationFilter reads this header.
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── Response interceptor — handle 401 globally ───────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid — clear storage and redirect to login.
      localStorage.removeItem('stockx_token')
      localStorage.removeItem('stockx_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api