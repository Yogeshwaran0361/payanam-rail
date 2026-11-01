import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { setUnauthorizedHandler, tokenExpiry, tokenStore } from '../api/client'
import { authApi } from '../api/endpoints'
import type { AuthResponse, Permission, UserProfile } from '../api/types'

interface AuthValue {
  user: UserProfile | null
  loading: boolean
  /** Stores the token and profile after sign-in / sign-up. */
  accept: (auth: AuthResponse, remember: boolean) => void
  logout: (reason?: string) => Promise<void>
  refresh: () => Promise<void>
  can: (permission: Permission) => boolean
  sessionMessage: string | null
  clearSessionMessage: () => void
}

const AuthContext = createContext<AuthValue | null>(null)

/**
 * Holds the signed-in user. On start it validates a stored token with /auth/me. A timer signs the user
 * out automatically when the JWT expires, and any 401 from the API does the same.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [sessionMessage, setSessionMessage] = useState<string | null>(null)
  const timer = useRef<number | undefined>(undefined)

  const clearLocal = useCallback((reason?: string) => {
    tokenStore.clear()
    setUser(null)
    window.clearTimeout(timer.current)
    if (reason) setSessionMessage(reason)
  }, [])

  const schedule = useCallback(
    (token: string) => {
      window.clearTimeout(timer.current)
      const exp = tokenExpiry(token)
      if (exp) {
        const ms = Math.min(exp - Date.now(), 2_147_000_000)
        timer.current = window.setTimeout(() => clearLocal('Your session expired. Please sign in again.'), Math.max(ms, 0))
      }
    },
    [clearLocal],
  )

  const refresh = useCallback(async () => {
    const token = tokenStore.get()
    if (!token) {
      setLoading(false)
      return
    }
    try {
      setUser(await authApi.me())
      schedule(token)
    } catch {
      clearLocal()
    } finally {
      setLoading(false)
    }
  }, [clearLocal, schedule])

  useEffect(() => {
    setUnauthorizedHandler(() => clearLocal('Your session has ended. Please sign in again.'))
    refresh()
    return () => window.clearTimeout(timer.current)
  }, [refresh, clearLocal])

  const accept = useCallback(
    (auth: AuthResponse, remember: boolean) => {
      tokenStore.set(auth.token, remember)
      setUser(auth.user)
      setSessionMessage(null)
      schedule(auth.token)
    },
    [schedule],
  )

  const logout = useCallback(
    async (reason?: string) => {
      try {
        if (tokenStore.get()) await authApi.logout()
      } catch {
        /* already invalid */
      }
      clearLocal(reason)
    },
    [clearLocal],
  )

  const value = useMemo<AuthValue>(
    () => ({
      user,
      loading,
      accept,
      logout,
      refresh,
      can: (p) => !!user?.permissions.includes(p),
      sessionMessage,
      clearSessionMessage: () => setSessionMessage(null),
    }),
    [user, loading, accept, logout, refresh, sessionMessage],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

/** Home page of a role. */
export function homeOf(user: UserProfile | null): string {
  if (!user) return '/signin'
  return user.role === 'PASSENGER' ? '/passenger' : '/station'
}
