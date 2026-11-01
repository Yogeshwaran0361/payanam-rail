import { NavLink } from 'react-router-dom'
import { ChevronsLeft, ChevronsRight, X } from 'lucide-react'
import { Logo } from '../brand/Logo'
import type { NavSection } from './nav'

/**
 * Left navigation. On desktop it collapses to an icon rail; on mobile it slides in as a drawer
 * over the page (opened from the top bar's menu button).
 */
export function Sidebar({ sections, collapsed, onToggle, mobileOpen, onCloseMobile, portalLabel }: {
  sections: NavSection[]; collapsed: boolean; onToggle: () => void; mobileOpen: boolean
  onCloseMobile: () => void; portalLabel: string
}) {
  return (
    <>
      {mobileOpen && <div className="fixed inset-0 z-40 bg-navy-950/60 lg:hidden" onClick={onCloseMobile} aria-hidden />}
      <aside
        className={`no-print fixed inset-y-0 left-0 z-50 flex flex-col bg-navy-900 text-white transition-all duration-200
          dark:bg-navy-950 dark:border-r dark:border-navy-800
          ${collapsed ? 'lg:w-[72px]' : 'lg:w-64'} w-64
          ${mobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}
        aria-label={`${portalLabel} navigation`}>
        <div className="flex h-16 items-center justify-between px-4">
          <Logo light compact={collapsed} />
          <button className="rounded-md p-1.5 text-navy-200 hover:bg-white/10 lg:hidden" onClick={onCloseMobile} aria-label="Close menu">
            <X className="h-5 w-5" />
          </button>
        </div>
        {!collapsed && (
          <p className="mx-4 mb-2 rounded-md bg-white/5 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wider text-teal-300">
            {portalLabel}
          </p>
        )}
        <nav className="flex-1 space-y-5 overflow-y-auto px-3 py-3">
          {sections.map((s) => (
            <div key={s.title}>
              {!collapsed && <p className="mb-1.5 px-3 text-[11px] font-semibold uppercase tracking-wider text-navy-300">{s.title}</p>}
              <ul className="space-y-0.5">
                {s.items.map((item) => (
                  <li key={item.to}>
                    <NavLink to={item.to} end={item.end} onClick={onCloseMobile} title={collapsed ? item.label : undefined}
                      className={({ isActive }) => `nav-link ${isActive ? 'nav-link-active' : ''} ${collapsed ? 'lg:justify-center' : ''}`}>
                      <item.icon className="h-[18px] w-[18px] shrink-0" aria-hidden />
                      <span className={collapsed ? 'lg:sr-only' : ''}>{item.label}</span>
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>
        <button onClick={onToggle}
          className="hidden items-center gap-2 border-t border-white/10 px-5 py-3 text-xs text-navy-200 hover:text-white lg:flex"
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}>
          {collapsed ? <ChevronsRight className="h-4 w-4" /> : <><ChevronsLeft className="h-4 w-4" /> Collapse</>}
        </button>
      </aside>
    </>
  )
}
