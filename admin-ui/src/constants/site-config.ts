export const IS_SERVER = typeof window === 'undefined';

export const IS_PRODUCTION = process.env.NODE_ENV === 'production';

export const SITE_CONFIG = {
  ORCHESTRATOR_SERVER: process.env.NEXT_PUBLIC_ORCHESTRATOR_SERVER,
  /* TODO: uncomment if still need to use
  ORCHESTRATOR_URL: IS_SERVER
    ? process.env.ORCHESTRATOR_INTERNAL_URL
    : process.env.NEXT_PUBLIC_ORCHESTRATOR, */
  AUTH_SERVER: process.env.NEXT_PUBLIC_AUTH_SERVER,
  AUTH_SERVER_URL: IS_SERVER
    ? process.env.AUTH_SERVER_INTERNAL_URL
    : process.env.NEXT_PUBLIC_AUTH_SERVER,
  CLIENT_ID: process.env.CLIENT_ID,
  CLIENT_ID_PUBLIC: process.env.NEXT_PUBLIC_CLIENT_ID,
  CLIENT_SECRET: process.env.CLIENT_SECRET,
  SCOPE: process.env.NEXT_PUBLIC_SCOPE,
};
