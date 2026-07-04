import axios from 'axios'
import { useAuthStore } from '../store/authStore'

// Dev: localhost:8080 | Produção: /api/v1 (proxy nginx) ou VITE_API_URL personalizado
// Em localhost (qualquer porta) sem VITE_API_URL, usa backend em 8080 para evitar 403 ao logar
const getApiBaseUrl = () => {
  if (import.meta.env.VITE_API_URL) return import.meta.env.VITE_API_URL
  if (import.meta.env.DEV) return 'http://localhost:8080/api/v1'
  const isLocalhost = typeof window !== 'undefined' && /^https?:\/\/localhost(:\d+)?$/i.test(window.location.origin)
  if (isLocalhost) return 'http://localhost:8080/api/v1'
  return '/api/v1'
}

const api = axios.create({
  baseURL: getApiBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    const status = error.response?.status
    const apiErrorCode = String(error.response?.data?.error?.code || '').toUpperCase()
    const apiErrorMessage = String(error.response?.data?.error?.message || '').toLowerCase()

    const requestUrl = String(originalRequest?.url || '')
    const isAuthEndpoint = /\/auth\/(login|refresh|logout)/.test(requestUrl)
    const hasAuthHeader = Boolean(
      originalRequest?.headers?.Authorization || originalRequest?.headers?.authorization
    )
    const hasAccessToken = Boolean(useAuthStore.getState().accessToken)
    const hasSessionContext = hasAuthHeader || hasAccessToken

    // Não tentar refresh em 403 claramente de autorização/permissão.
    const isExplicitAuthorizationError =
      apiErrorCode.includes('FORBIDDEN') ||
      apiErrorCode.includes('ACCESS_DENIED') ||
      apiErrorMessage.includes('permission') ||
      apiErrorMessage.includes('permiss')

    const hasTokenHint =
      apiErrorCode.includes('TOKEN') ||
      apiErrorCode.includes('JWT') ||
      apiErrorCode.includes('UNAUTHORIZED') ||
      apiErrorCode.includes('AUTH') ||
      apiErrorMessage.includes('token') ||
      apiErrorMessage.includes('jwt') ||
      apiErrorMessage.includes('expired') ||
      apiErrorMessage.includes('expirad') ||
      apiErrorMessage.includes('sessão') ||
      apiErrorMessage.includes('sessao')

    const shouldTryRefresh =
      !originalRequest?._retry &&
      !isAuthEndpoint &&
      (status === 401 ||
        (status === 403 &&
          hasSessionContext &&
          !isExplicitAuthorizationError &&
          (hasTokenHint || !apiErrorCode)))

    if (shouldTryRefresh) {
      originalRequest._retry = true
      
      try {
        const refreshToken = useAuthStore.getState().refreshToken
        if (!refreshToken) {
          throw new Error('No refresh token')
        }
        
        const response = await axios.post(
          `${getApiBaseUrl()}/auth/refresh`,
          { refreshToken }
        )
        
        const { accessToken, refreshToken: newRefreshToken } = response.data.data
        
        useAuthStore.getState().setTokens(accessToken, newRefreshToken)
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        
        return api(originalRequest)
      } catch (refreshError) {
        useAuthStore.getState().clearTokens()
        window.location.href = '/login'
        return Promise.reject(refreshError)
      }
    }
    
    return Promise.reject(error)
  }
)

export default api

// Função auxiliar para logout
export const logout = async () => {
  const refreshToken = useAuthStore.getState().refreshToken
  if (refreshToken) {
    try {
      await api.post('/auth/logout', { refreshToken })
    } catch (error) {
      console.error('Erro ao fazer logout:', error)
    }
  }
  useAuthStore.getState().clearTokens()
  window.location.href = '/login'
}
