import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'
import { CheckCircle2, AlertTriangle, Info, XCircle, X } from 'lucide-react'

type Kind = 'success' | 'error' | 'info' | 'warning'
interface Toast { id: number; kind: Kind; title: string; message?: string }
interface ToastValue { push: (kind: Kind, title: string, message?: string) => void }

const ToastContext = createContext<ToastValue>({ push: () => {} })

const ICON = { success: CheckCircle2, error: XCircle, info: Info, warning: AlertTriangle }
const TONE = {
  success: 'border-l-green-500',
  error: 'border-l-red-500',
  info: 'border-l-teal-500',
  warning: 'border-l-amber-500',
}
const ICON_TONE = { success: 'text-green-600', error: 'text-red-600', info: 'text-teal-600', warning: 'text-amber-600' }

/** Toast notifications in the bottom-right corner; they disappear after 5 seconds. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const dismiss = useCallback((id: number) => setToasts((list) => list.filter((t) => t.id !== id)), [])

  const push = useCallback(
    (kind: Kind, title: string, message?: string) => {
      const id = Date.now() + Math.random()
      setToasts((list) => [...list.slice(-3), { id, kind, title, message }])
      window.setTimeout(() => dismiss(id), 5000)
    },
    [dismiss],
  )

  return (
    <ToastContext.Provider value={{ push }}>
      {children}
      <div className="no-print fixed bottom-4 right-4 z-[60] flex w-[min(380px,calc(100vw-2rem))] flex-col gap-2" aria-live="polite">
        {toasts.map((t) => {
          const Icon = ICON[t.kind]
          return (
            <div key={t.id} role="status"
              className={`card animate-slide-in flex items-start gap-3 border-l-4 p-4 ${TONE[t.kind]}`}>
              <Icon className={`mt-0.5 h-5 w-5 shrink-0 ${ICON_TONE[t.kind]}`} aria-hidden />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-navy-900 dark:text-white">{t.title}</p>
                {t.message && <p className="mt-0.5 break-words text-sm muted">{t.message}</p>}
              </div>
              <button className="rounded p-1 text-slate-400 hover:text-slate-600" onClick={() => dismiss(t.id)} aria-label="Dismiss">
                <X className="h-4 w-4" />
              </button>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
