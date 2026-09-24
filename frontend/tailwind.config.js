/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        harbour: {
          DEFAULT: '#0E2233',
          700: '#163247',
          600: '#1E4058',
          300: '#7C93A6',
        },
        paper: '#F6F5F1',
        edge: '#E2E0D8',
        brand: {
          DEFAULT: '#0B6E5B',
          dark: '#085647',
          soft: '#E4F0EC',
        },
        signal: {
          DEFAULT: '#C77A06',
          soft: '#FBF0DC',
        },
        danger: {
          DEFAULT: '#A83A2B',
          soft: '#F8E7E4',
        },
        slate950: '#0A1721',
      },
      fontFamily: {
        display: ['Space Grotesk', 'system-ui', 'sans-serif'],
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        lift: '0 1px 2px rgba(14, 34, 51, 0.06), 0 8px 24px -16px rgba(14, 34, 51, 0.35)',
      },
    },
  },
  plugins: [],
};
