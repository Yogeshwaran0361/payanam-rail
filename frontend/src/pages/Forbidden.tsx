import { Link } from 'react-router-dom'
import { ShieldAlert } from 'lucide-react'
import { homeOf, useAuth } from '../context/AuthContext'
import { useDocumentTitle } from '../hooks/useDocumentTitle'

/** 403: signed in, but this role or designation may not open the page. */
export function Forbidden({ embedded = false }: { embedded?: boolean }) {
  const { user } = useAuth()
  useDocumentTitle('Access denied')
  return (
    <div className={`flex flex-col items-center justify-center px-4 text-center ${embedded ? 'py-20' : 'min-h-screen'}`}>
      <div className="rounded-2xl bg-red-50 p-4 text-red-600 dark:bg-red-500/10"><ShieldAlert className="h-10 w-10" /></div>
      <p className="mt-6 text-sm font-semibold text-red-600">Error 403</p>
      <h1 className="mt-1 text-3xl font-bold">Access denied</h1>
      <p className="mt-2 max-w-md muted">
        {user
          ? `Your account (${user.displayTitle}) does not have permission to open this page.`
          : 'You need to sign in with an account that has access to this page.'}
      </p>
      <Link to={homeOf(user)} className="btn-primary mt-6">Back to my home page</Link>
    </div>
  )
}
