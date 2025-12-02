/**
 * IMPORTANT: DO NOT MOVE OR RENAME THIS FILE
 * This file is referenced as 'utils' alias in components.json
 * Moving this file will break component imports across the application
 */

import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
