/**
 * Hero illustration drawn in SVG (no image files): a station canopy over three platforms with
 * trains on them, a signal and the junction name board. Purely decorative, so aria-hidden.
 */
export function RailwayIllustration({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 560 380" className={className} aria-hidden>
      <defs>
        <linearGradient id="sky" x1="0" x2="0" y1="0" y2="1">
          <stop offset="0" stopColor="#1f376b" />
          <stop offset="1" stopColor="#0f1d3a" />
        </linearGradient>
        <linearGradient id="trainA" x1="0" x2="1">
          <stop offset="0" stopColor="#13a392" />
          <stop offset="1" stopColor="#0d8378" />
        </linearGradient>
        <linearGradient id="trainB" x1="0" x2="1">
          <stop offset="0" stopColor="#5577bf" />
          <stop offset="1" stopColor="#365aa6" />
        </linearGradient>
      </defs>
      <rect width="560" height="380" rx="24" fill="url(#sky)" />
      {/* stars and hills */}
      {[[60, 40], [140, 70], [420, 36], [500, 80], [300, 28], [230, 54]].map(([x, y], i) => (
        <circle key={i} cx={x} cy={y} r={1.6} fill="#c9f6ec" opacity={0.7} />
      ))}
      <path d="M0 150 Q120 95 240 140 T560 120 V200 H0Z" fill="#172a52" />
      {/* canopy */}
      <path d="M40 132 L280 98 L520 132 Z" fill="#274585" />
      <rect x="40" y="132" width="480" height="8" fill="#5577bf" />
      {[70, 170, 280, 390, 490].map((x) => <rect key={x} x={x - 3} y={140} width={6} height={64} fill="#365aa6" />)}
      {/* name board */}
      <rect x="215" y="150" width="130" height="30" rx="5" fill="#fbbf24" />
      <text x="280" y="170" textAnchor="middle" fontSize="13" fontWeight="700" fill="#0f1d3a" fontFamily="Inter, sans-serif">
        PAYANAM JN.
      </text>
      {/* platforms and tracks */}
      {[0, 1, 2].map((i) => {
        const y = 210 + i * 54
        return (
          <g key={i}>
            <rect x="20" y={y} width="520" height="12" fill="#84a1d6" opacity={0.35} />
            <text x="30" y={y + 9.5} fontSize="8" fill="#d9e3f4" fontFamily="Inter, sans-serif" fontWeight="600">P{i + 1}</text>
            <line x1="0" x2="560" y1={y + 34} y2={y + 34} stroke="#5577bf" strokeWidth="2" />
            <line x1="0" x2="560" y1={y + 42} y2={y + 42} stroke="#5577bf" strokeWidth="2" />
            {Array.from({ length: 28 }, (_, k) => (
              <rect key={k} x={k * 20 + 4} y={y + 32} width={4} height={12} fill="#274585" />
            ))}
          </g>
        )
      })}
      {/* train on P1 */}
      <g>
        <rect x="70" y="226" width="300" height="20" rx="6" fill="url(#trainA)" />
        <path d="M370 226 h22 q16 0 18 20 h-40z" fill="#0f6962" />
        {[86, 126, 166, 206, 246, 286, 326].map((x) => <rect key={x} x={x} y={231} width={26} height={7} rx={2} fill="#c9f6ec" />)}
        <circle cx="400" cy="240" r="2.5" fill="#fde68a" />
      </g>
      {/* train on P2 */}
      <g>
        <rect x="190" y="280" width="250" height="20" rx="6" fill="url(#trainB)" />
        <path d="M190 280 h-18 q-14 0 -16 20 h34z" fill="#274585" />
        {[204, 244, 284, 324, 364, 404].map((x) => <rect key={x} x={x} y={285} width={26} height={7} rx={2} fill="#d9e3f4" />)}
      </g>
      {/* signal */}
      <g>
        <rect x="508" y="282" width="4" height="70" fill="#84a1d6" />
        <rect x="498" y="250" width="24" height="40" rx="5" fill="#0a1428" />
        <circle cx="510" cy="261" r="5" fill="#ef4444" opacity={0.35} />
        <circle cx="510" cy="279" r="5" fill="#22c55e" />
      </g>
      {/* clock */}
      <g>
        <circle cx="120" cy="170" r="16" fill="#eef3fb" stroke="#365aa6" strokeWidth="3" />
        <line x1="120" y1="170" x2="120" y2="160" stroke="#0f1d3a" strokeWidth="2" strokeLinecap="round" />
        <line x1="120" y1="170" x2="128" y2="173" stroke="#0f1d3a" strokeWidth="2" strokeLinecap="round" />
      </g>
    </svg>
  )
}
