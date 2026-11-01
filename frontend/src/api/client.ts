import axios, { AxiosError } from 'axios'
import type { ApiError } from './types'

/**
 * One Axios instance for the whole app. The request interceptor adds "Authorization: Bearer <jwt>";
 * the response interceptor signs the user out when the server answers 401 (expired / revoked token).
 */
export const api = axios.create({ baseURL: '/api/v1', timeout: 20000 })

const TOKEN_KEY = 'payanam-token'

export const tokenStore = {
  get(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  /** "Remember me" keeps the token in localStorage; otherwise it lives only for this browser tab. */
  set(token: string, remember: boolean) {
    try {
      this.clear()
      ;(remember ? localStorage : sessionStorage).setItem(TOKEN_KEY, token)
    } catch {
      /* storage blocked: the session still works until reload */
    }
  },
  clear() {
    try {
      localStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem(TOKEN_KEY)
    } catch {
      /* ignore */
    }
  },
}

let onUnauthorized: (() => void) | null = null
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

api.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const url = error.config?.url ?? ''
    if (error.response?.status === 401 && !url.startsWith('/auth/signin') && tokenStore.get()) {
      onUnauthorized?.()
    }
    return Promise.reject(error)
  },
)

/** Turns any Axios error into the backend's ApiError shape. */
export function toApiError(error: unknown): ApiError {
  const e = error as AxiosError<ApiError>
  if (e?.response?.data && typeof e.response.data === 'object' && 'message' in e.response.data) {
    return { ...e.response.data, fieldErrors: e.response.data.fieldErrors ?? [] }
  }
  return {
    status: e?.response?.status ?? 0,
    code: 'NETWORK',
    message: e?.response ? 'Unexpected server response' : 'Cannot reach the server. Is it running?',
    path: '',
    fieldErrors: [],
  }
}

/** Reads the "exp" claim of a JWT (seconds since epoch) without verifying it (the server verifies). */
export function tokenExpiry(token: string): number | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return typeof payload.exp === 'number' ? payload.exp * 1000 : null
  } catch {
    return null
  }
}
