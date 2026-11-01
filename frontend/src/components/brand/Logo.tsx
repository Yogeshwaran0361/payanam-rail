/** The Payanam Rail mark: a teal rail-track roundel with the word mark beside it. */
export function Logo({ light = false, compact = false }: { light?: boolean; compact?: boolean }) {
  return (
    <span className="inline-flex items-center gap-2.5">
      <svg viewBox="0 0 40 40" className="h-9 w-9 shrink-0" aria-hidden>
        <rect width="40" height="40" rx="10" fill="#0d8378" />
        <path d="M13 31 L18 9 M27 31 L22 9" stroke="white" strokeWidth="2.4" strokeLinecap="round" />
        <path d="M14.2 26h11.6M15.3 21h9.4M16.3 16h7.4M17.2 12h5.6" stroke="#95ecdb" strokeWidth="1.8" strokeLinecap="round" />
      </svg>
      {!compact && (
        <span className="leading-tight">
          <span className={`block text-[15px] font-bold tracking-tight ${light ? 'text-white' : 'text-navy-900 dark:text-white'}`}>
            Payanam Rail
          </span>
          <span className={`block text-[11px] ${light ? 'text-navy-200' : 'muted'}`}>Payanam Junction</span>
        </span>
      )}
    </span>
  )
}
