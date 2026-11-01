import { Link, NavLink, Outlet } from 'react-router-dom'
import { Moon, Sun } from 'lucide-react'
import { Logo } from '../components/brand/Logo'
import { useAuth, homeOf } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'

/** Header and footer around the public pages (landing, PNR lookup, sign in). */
export function PublicLayout() {
  const { user } = useAuth()
  const { theme, toggle } = useTheme()
  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur dark:border-navy-800 dark:bg-navy-900/90">
        <div className="mx-auto flex h-16 max-w-7xl items-center gap-6 px-4 sm:px-6">
          <Link to="/" aria-label="Payanam Rail home"><Logo /></Link>
          <nav className="hidden items-center gap-5 text-sm font-medium text-slate-600 md:flex dark:text-slate-300">
            <a href="/#features" className="hover:text-teal-600">Features</a>
            <a href="/#how" className="hover:text-teal-600">How it works</a>
            <NavLink to="/pnr" className={({ isActive }) => (isActive ? 'text-teal-600' : 'hover:text-teal-600')}>PNR status</NavLink>
          </nav>
          <div className="ml-auto flex items-center gap-2">
            <button className="btn-ghost h-9 w-9 !p-0" onClick={toggle} aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}>
              {theme === 'dark' ? <Sun className="h-[18px] w-[18px]" /> : <Moon className="h-[18px] w-[18px]" />}
            </button>
            {user ? (
              <Link to={homeOf(user)} className="btn-primary">Open my portal</Link>
            ) : (
              <>
                <Link to="/signin" className="btn-ghost hidden sm:inline-flex">Sign in</Link>
                <Link to="/signup" className="btn-primary">Create account</Link>
              </>
            )}
          </div>
        </div>
      </header>
      <main className="flex-1"><Outlet /></main>
      <footer className="border-t border-slate-200 bg-white dark:border-navy-800 dark:bg-navy-900">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 md:grid-cols-4">
          <div className="md:col-span-2">
            <Logo />
            <p className="mt-3 max-w-sm text-sm muted">
              Reservations and platform allocation for Payanam Junction: 6 trains, 3 platforms, one control room.
            </p>
          </div>
          <div>
            <p className="text-sm font-semibold">Passengers</p>
            <ul className="mt-2 space-y-1.5 text-sm muted">
              <li><Link to="/signin" className="hover:text-teal-600">Book a ticket</Link></li>
              <li><Link to="/pnr" className="hover:text-teal-600">Check PNR status</Link></li>
              <li><Link to="/signup" className="hover:text-teal-600">Create an account</Link></li>
            </ul>
          </div>
          <div>
            <p className="text-sm font-semibold">Station staff</p>
            <ul className="mt-2 space-y-1.5 text-sm muted">
              <li><Link to="/signin?as=staff" className="hover:text-teal-600">Staff sign in</Link></li>
              <li><a href="/swagger-ui.html" className="hover:text-teal-600">API documentation</a></li>
            </ul>
          </div>
        </div>
        <p className="border-t border-slate-200 py-4 text-center text-xs muted dark:border-navy-800">
          © {new Date().getFullYear()} Payanam Rail · Mini Project 16, Railway Reservation and Platform Allocation System
        </p>
      </footer>
    </div>
  )
}
