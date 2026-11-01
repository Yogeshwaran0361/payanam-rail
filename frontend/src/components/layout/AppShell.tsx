import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { visibleNav, type NavSection } from './nav'

const COLLAPSED_KEY = 'payanam-sidebar-collapsed'

/** Sidebar + top bar + page area. Used by both portals with their own navigation. */
export function AppShell({ sections, portalLabel }: { sections: NavSection[]; portalLabel: string }) {
  const { can } = useAuth()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [collapsed, setCollapsed] = useState(() => {
    try {
      return localStorage.getItem(COLLAPSED_KEY) === '1'
    } catch {
      return false
    }
  })
  const toggle = () => {
    setCollapsed((c) => {
      try {
        localStorage.setItem(COLLAPSED_KEY, c ? '0' : '1')
      } catch {
        /* ignore */
      }
      return !c
    })
  }

  return (
    <div className="min-h-screen">
      <a href="#main" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[70] focus:rounded focus:bg-white focus:px-3 focus:py-2">
        Skip to content
      </a>
      <Sidebar sections={visibleNav(sections, can)} collapsed={collapsed} onToggle={toggle} mobileOpen={mobileOpen}
        onCloseMobile={() => setMobileOpen(false)} portalLabel={portalLabel} />
      <div className={`transition-all duration-200 print:!pl-0 ${collapsed ? 'lg:pl-[72px]' : 'lg:pl-64'}`}>
        <Topbar onMenu={() => setMobileOpen(true)} />
        <main id="main" className="mx-auto max-w-[1400px] px-4 py-6 sm:px-6 lg:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
