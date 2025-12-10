export const PAGE_PATH = {
  LOGIN: '/login',
  HOME: '/',
} as const;

export const API_PATH = {
  AUTH: '/api/auth',
  AUTH_CALLBACK: (providerId: string) => `/api/auth/callback/${providerId}`,
} as const;
