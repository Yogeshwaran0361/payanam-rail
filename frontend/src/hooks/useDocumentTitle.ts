import { useEffect } from 'react'

/** Sets the browser tab title, e.g. "My Bookings · Payanam Rail". */
export function useDocumentTitle(title: string) {
  useEffect(() => {
    document.title = title ? `${title} · Payanam Rail` : 'Payanam Rail'
  }, [title])
}
