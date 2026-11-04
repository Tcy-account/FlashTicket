/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#635BFF',
          hover: '#5148E5',
          light: '#F0EFFF',
          dark: '#19182F',
        },
      },
    },
  },
  plugins: [],
};
