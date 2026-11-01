/** Tab bar (role="tablist") used for My Bookings, auth forms and similar. */
export function Tabs<T extends string>({ tabs, value, onChange, label }: {
  tabs: { id: T; label: string; count?: number }[]; value: T; onChange: (id: T) => void; label: string
}) {
  return (
    <div role="tablist" aria-label={label} className="inline-flex max-w-full overflow-x-auto rounded-xl bg-slate-100 p-1 dark:bg-navy-800">
      {tabs.map((t) => (
        <button key={t.id} role="tab" aria-selected={value === t.id} onClick={() => onChange(t.id)}
          className={`whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium transition sm:px-3.5 ${value === t.id
            ? 'bg-white text-navy-900 shadow-sm dark:bg-navy-950 dark:text-white'
            : 'text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200'}`}>
          {t.label}
          {t.count !== undefined && <span className="ml-1.5 rounded-full bg-slate-200 px-1.5 text-xs dark:bg-navy-700">{t.count}</span>}
        </button>
      ))}
    </div>
  )
}
