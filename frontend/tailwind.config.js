/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        surface: {
          DEFAULT: '#FAF8F6',
          raised: '#FFFFFF',
          soft: '#F3EEF4',
          accent: '#EDE7F0',
        },
        primary: {
          50: '#F3EEF8',
          100: '#E8E0F0',
          200: '#D4C8E2',
          300: '#B8A6CF',
          400: '#9B89B8',
          500: '#7C6B9E',
          600: '#6A5A8A',
          700: '#574876',
          800: '#443761',
          900: '#2D2545',
        },
        ink: {
          heading: '#2D2A33',
          body: '#4A4553',
          secondary: '#6B6573',
          muted: '#9B95A3',
          faint: '#C4BFC9',
        },
        edge: {
          DEFAULT: '#E5DFE8',
          subtle: '#F0ECF2',
          focus: '#9B89B8',
        },
        success: {
          DEFAULT: '#6B9E7C',
          bg: '#E6F0EA',
          dark: '#4A7A5C',
        },
        error: {
          DEFAULT: '#C07070',
          bg: '#F5E6E6',
        },
        deadline: {
          'today-bg': '#F5E6E6',
          'today-text': '#9E4B4B',
          'week-bg': '#F5EDE0',
          'week-text': '#8A6E3E',
        },
      },
      boxShadow: {
        card: '0 1px 3px rgba(124,107,158,0.08)',
        'card-hover': '0 4px 12px rgba(124,107,158,0.12)',
        modal: '0 8px 32px rgba(124,107,158,0.18)',
        soft: '0 1px 2px rgba(124,107,158,0.05)',
      },
    },
  },
  plugins: [],
}
