import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './app/**/*.{ts,tsx,js,jsx}',
    './components/**/*.{ts,tsx,js,jsx}',
    './pages/**/*.{ts,tsx,js,jsx}',
    './node_modules/@shadcn/ui/dist/**/*.js',
  ],
  darkMode: ['class', '.dark'],
};

export default config;
