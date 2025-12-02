export const IS_SERVER = typeof window === 'undefined';

export const IS_PRODUCTION = process.env.NODE_ENV === 'production';

export const SERVER_CONFIG = {
  AUTH_SERVER: process.env.AUTH_SERVER_INTERNAL_URL,
  ORCHESTRATOR_SERVER: process.env.ORCHESTRATOR_INTERNAL_URL,
  CLIENT_ID: process.env.CLIENT_ID,
  CLIENT_SECRET: process.env.CLIENT_SECRET,
} as const;

export const PUBLIC_CONFIG = {
  AUTH_SERVER: process.env.NEXT_PUBLIC_AUTH_SERVER,
  // Uncomment the line below if AUTH_SERVER is needed in the client-side code
  // ORCHESTRATOR_SERVER: process.env.NEXT_PUBLIC_ORCHESTRATOR_SERVER,
  CLIENT_ID: process.env.NEXT_PUBLIC_CLIENT_ID,
  SCOPE: process.env.NEXT_PUBLIC_SCOPE,
} as const;
