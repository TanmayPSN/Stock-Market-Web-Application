import axios from 'axios'  // ← this is the only change

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('stockx_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('stockx_token')
      localStorage.removeItem('stockx_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api