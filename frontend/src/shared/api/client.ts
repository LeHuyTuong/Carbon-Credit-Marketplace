import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { toast } from 'sonner'
import { createTraceId } from '@/shared/lib/format'
import { useAuthStore } from '@/shared/stores/auth.store'
import type { ApiEnvelope } from './types'

export const API_BASE = import.meta.env.VITE_API_BASE || '/api/v1'

/** Backend có 2 envelope: responseData (auth) và response (các endpoint còn lại). */
export function unwrapEnvelope<T>(payload: unknown): T {
  if (payload && typeof payload === 'object') {
    const envelope = payload as ApiEnvelope<T>
    for (const candidate of [envelope.response, envelope.responseData, envelope.data]) {
      if (candidate !== undefined && candidate !== null) return candidate
    }
  }
  return payload as T
}

export const apiClient = axios.create({
  baseURL: API_BASE,
  timeout: 20_000,
  headers: { Accept: 'application/json' },
})

let lastUnauthorizedAt = 0
let lastRateLimitedAt = 0

apiClient.interceptors.request.use((config) => {
  const { token } = useAuthStore.getState()
  if (token) config.headers.Authorization = `Bearer ${token}`

  const traceId = createTraceId()
  config.headers['X-Trace-Id'] = traceId
  // Backend đọc X-Request-Trace; giữ cả hai để tương thích gateway cũ.
  config.headers['X-Request-Trace'] = traceId
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    const status = error.response?.status
    const now = Date.now()

    if (status === 401) {
      useAuthStore.getState().logout()
      if (now - lastUnauthorizedAt > 4_000) {
        lastUnauthorizedAt = now
        toast.error('Phiên đăng nhập đã hết hạn', {
          description: 'Vui lòng đăng nhập lại để tiếp tục.',
        })
      }
      if (!window.location.pathname.startsWith('/login')) {
        const from = `${window.location.pathname}${window.location.search}`
        window.location.assign(`/login?from=${encodeURIComponent(from)}`)
      }
      return Promise.reject(error)
    }

    if (status === 429 && now - lastRateLimitedAt > 4_000) {
      lastRateLimitedAt = now
      toast.warning('Quá nhiều yêu cầu', {
        description: 'Hệ thống đang giới hạn tốc độ, vui lòng thử lại sau giây lát.',
      })
    }

    return Promise.reject(error)
  },
)

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await apiClient.request<unknown>(config)
  return unwrapEnvelope<T>(response.data)
}

export const api = {
  get: <T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig) =>
    request<T>({ ...config, method: 'GET', url, params }),
  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request<T>({ ...config, method: 'POST', url, data }),
  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request<T>({ ...config, method: 'PUT', url, data }),
  patch: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request<T>({ ...config, method: 'PATCH', url, data }),
  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    request<T>({ ...config, method: 'DELETE', url }),
}
