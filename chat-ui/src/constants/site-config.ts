export const IS_SERVER = typeof window === 'undefined';

export const IS_PRODUCTION = process.env.NODE_ENV === 'production';

export const SITE_CONFIG = {
  AUTH_SERVER: process.env.NEXT_PUBLIC_AUTH_SERVER,
  AUTH_SERVER_URL: IS_SERVER
    ? process.env.AUTH_SERVER_INTERNAL_URL
    : process.env.NEXT_PUBLIC_AUTH_SERVER,
  ORCHESTRATOR_URL: IS_SERVER
    ? process.env.ORCHESTRATOR_INTERNAL_URL
    : process.env.NEXT_PUBLIC_ORCHESTRATOR,
  CLIENT_ID: process.env.CLIENT_ID,
  CLIENT_ID_PUBLIC: process.env.NEXT_PUBLIC_CLIENT_ID,
  CLIENT_SECRET: process.env.CLIENT_SECRET,
  SCOPE: process.env.NEXT_PUBLIC_SCOPE,
};
