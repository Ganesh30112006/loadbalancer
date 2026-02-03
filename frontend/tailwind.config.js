/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Indigo primary palette
        primary: {
          50: '#eef2ff',
          100: '#e0e7ff',
          200: '#c7d2fe',
          300: '#a5b4fc',
          400: '#818cf8',
          500: '#6366f1',
          600: '#4f46e5',
          700: '#4338ca',
          800: '#3730a3',
          900: '#312e81',
          950: '#1e1b4b',
        },
        // Slate palette for backgrounds
        slate: {
          850: '#1a1f2e',
          950: '#0f1219',
        },
        // Status colors
        success: {
          DEFAULT: '#22c55e',
          light: 'rgba(34, 197, 94, 0.1)',
          border: 'rgba(34, 197, 94, 0.2)',
        },
        warning: {
          DEFAULT: '#f59e0b',
          light: 'rgba(245, 158, 11, 0.1)',
          border: 'rgba(245, 158, 11, 0.2)',
        },
        error: {
          DEFAULT: '#ef4444',
          light: 'rgba(239, 68, 68, 0.1)',
          border: 'rgba(239, 68, 68, 0.2)',
        },
      },
      fontFamily: {
        sans: ['Inter var', 'Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'fade-in': 'fadeIn 0.2s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'slide-down': 'slideDown 0.2s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideDown: {
          '0%': { opacity: '0', transform: 'translateY(-10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
      boxShadow: {
        'glow-green': '0 0 20px -5px rgba(34, 197, 94, 0.3)',
        'glow-amber': '0 0 20px -5px rgba(245, 158, 11, 0.3)',
        'glow-red': '0 0 20px -5px rgba(239, 68, 68, 0.3)',
        'glow-blue': '0 0 20px -5px rgba(99, 102, 241, 0.3)',
      },
      borderRadius: {
        '4xl': '2rem',
      },
    },
  },
  plugins: [],
}
