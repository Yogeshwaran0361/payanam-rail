import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { Permission, Role } from '../api/types'
import { useAuth } from '../context/AuthContext'
import { PageSkeleton } from '../components/ui/Skeleton'
import { Forbidden } from '../pages/Forbidden'

/**
 * Route guard. Not signed in → the sign-in page (remembering where the user was going).
 * Signed in with the wrong role → 403 page. The backend checks the same rules on every API call;
 * this guard only keeps the UI honest.
 */
export function RequireRole({ role }: { role: Role }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <div className="p-8"><PageSkeleton /></div>
  if (!user) return <Navigate to="/signin" replace state={{ from: location.pathname + location.search }} />
  if (user.role !== role) return <Forbidden />
  return <Outlet />
}

/** Station pages that need a designation permission (e.g. a Booking Clerk opening the Platform Board). */
export function RequirePermission({ anyOf, children }: { anyOf: Permission[]; children: ReactNode }) {
  const { can } = useAuth()
  return anyOf.some(can) ? <>{children}</> : <Forbidden embedded />
}

/** Sign-in pages redirect away when a session already exists. */
export function GuestOnly({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return null
  if (user) return <Navigate to={user.role === 'PASSENGER' ? '/passenger' : '/station'} replace />
  return <>{children}</>
}
