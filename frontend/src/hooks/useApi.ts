import { useCallback, useEffect, useRef, useState } from 'react'
import { toApiError } from '../api/client'
import type { ApiError } from '../api/types'

/**
 * Loads data with loading / error state. `reload()` fetches again; `pollMs` refreshes in the background
 * (used for live boards) without showing the skeleton again.
 */
export function useApi<T>(loader: () => Promise<T>, deps: unknown[] = [], pollMs?: number) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<ApiError | null>(null)
  const [loading, setLoading] = useState(true)
  const loaderRef = useRef(loader)
  loaderRef.current = loader

  const reload = useCallback(async (silent = false) => {
    if (!silent) setLoading(true)
    try {
      const result = await loaderRef.current()
      setData(result)
      setError(null)
    } catch (e) {
      setError(toApiError(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  useEffect(() => {
    if (!pollMs) return
    const id = window.setInterval(() => {
      if (document.visibilityState === 'visible') reload(true)
    }, pollMs)
    return () => window.clearInterval(id)
  }, [pollMs, reload])

  return { data, error, loading, reload, setData }
}
