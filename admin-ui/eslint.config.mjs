import path from 'path';
import { fileURLToPath } from 'url';
// Next.js-related imports
import nextVitals from 'eslint-config-next/core-web-vitals';
import nextTs from 'eslint-config-next/typescript';
// ESLint plugin imports
import prettier from 'eslint-plugin-prettier';
import prettierConfig from 'eslint-config-prettier';
import tailwindcss from 'eslint-plugin-tailwindcss';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const tailwindConfigPath = path.join(__dirname, 'tailwind.config.js');

const config = [
  {
    ignores: ['node_modules/**', '.next/**', 'tailwind.config.js'],
  },
  ...nextVitals,
  ...nextTs,
  prettierConfig,
  {
    plugins: {
      prettier,
      tailwindcss,
    },
    rules: {
      'prettier/prettier': 'error',
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      'no-console': 'error',
      'tailwindcss/classnames-order': 'off',
      'tailwindcss/no-custom-classname': 'off',
    },
    settings: {
      tailwindcss: {
        config: tailwindConfigPath,
      },
    },
  },
];

export default config;
