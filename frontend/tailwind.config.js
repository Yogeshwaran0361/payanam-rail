/** Payanam Rail design tokens: navy + teal, Inter, rounded-xl cards, soft shadows. */
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Inter Variable"', 'Inter', 'system-ui', 'sans-serif'],
      },
      colors: {
        navy: {
          50: '#eef3fb', 100: '#d9e3f4', 200: '#b3c6e8', 300: '#84a1d6', 400: '#5577bf',
          500: '#365aa6', 600: '#274585', 700: '#1f376b', 800: '#172a52', 900: '#0f1d3a', 950: '#0a1428',
        },
        teal: {
          50: '#effcf9', 100: '#c9f6ec', 200: '#95ecdb', 300: '#5ad9c5', 400: '#2bbfab',
          500: '#13a392', 600: '#0d8378', 700: '#0f6962', 800: '#11534f', 900: '#124542', 950: '#042a29',
        },
      },
      boxShadow: {
        card: '0 1px 2px rgba(15, 29, 58, 0.04), 0 4px 16px rgba(15, 29, 58, 0.06)',
        lift: '0 10px 30px rgba(15, 29, 58, 0.12)',
      },
      keyframes: {
        shimmer: { '100%': { transform: 'translateX(100%)' } },
        'slide-in': { from: { transform: 'translateY(8px)', opacity: '0' }, to: { transform: 'none', opacity: '1' } },
      },
      animation: {
        shimmer: 'shimmer 1.4s infinite',
        'slide-in': 'slide-in .18s ease-out',
      },
    },
  },
  plugins: [],
}
