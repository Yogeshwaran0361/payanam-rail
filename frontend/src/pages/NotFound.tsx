import { Link } from 'react-router-dom'
import { homeOf, useAuth } from '../context/AuthContext'
import { useDocumentTitle } from '../hooks/useDocumentTitle'

/** 404 page, with a small derailed-track drawing. */
export function NotFound() {
  const { user } = useAuth()
  useDocumentTitle('Page not found')
  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 text-center">
      <svg viewBox="0 0 240 80" className="w-64" aria-hidden>
        <line x1="0" y1="50" x2="150" y2="50" stroke="#5577bf" strokeWidth="3" />
        <line x1="0" y1="62" x2="150" y2="62" stroke="#5577bf" strokeWidth="3" />
        <line x1="160" y1="44" x2="240" y2="30" stroke="#5577bf" strokeWidth="3" />
        <line x1="160" y1="58" x2="240" y2="44" stroke="#5577bf" strokeWidth="3" />
        {Array.from({ length: 8 }, (_, i) => <rect key={i} x={i * 18 + 4} y={46} width={5} height={20} fill="#274585" />)}
        <rect x="140" y="38" width="12" height="30" fill="#fbbf24" />
      </svg>
      <p className="mt-6 text-sm font-semibold text-teal-600">Error 404</p>
      <h1 className="mt-1 text-3xl font-bold">This track ends here</h1>
      <p className="mt-2 max-w-md muted">The page you were looking for does not exist or has moved.</p>
      <div className="mt-6 flex gap-3">
        <Link to="/" className="btn-secondary">Home</Link>
        {user && <Link to={homeOf(user)} className="btn-primary">My portal</Link>}
      </div>
    </div>
  )
}
