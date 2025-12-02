import path from 'path';
import { fileURLToPath } from 'url';
import { defineConfig } from 'eslint/config';
import nextVitals from 'eslint-config-next/core-web-vitals';
import nextTs from 'eslint-config-next/typescript';
import prettier from 'eslint-plugin-prettier';
import prettierConfig from 'eslint-config-prettier';
import tailwindcss from 'eslint-plugin-tailwindcss';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const tailwindConfigPath = path.join(__dirname, 'tailwind.config.js');

const eslintConfig = defineConfig([
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
]);

export default eslintConfig;
