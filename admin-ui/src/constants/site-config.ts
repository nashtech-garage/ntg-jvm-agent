export const IS_SERVER = typeof window === 'undefined';

export const IS_PRODUCTION = process.env.NODE_ENV === 'production';

export const SERVER_CONFIG = {
  AUTH_SERVER: process.env.INTERNAL_AUTH_SERVER ?? process.env.NEXT_PUBLIC_AUTH_SERVER,
  ORCHESTRATOR_SERVER:
    process.env.INTERNAL_ORCHESTRATOR_SERVER ?? process.env.NEXT_PUBLIC_ORCHESTRATOR_SERVER,
  CLIENT_ID: process.env.CLIENT_ID,
  CLIENT_SECRET: process.env.CLIENT_SECRET,
  // DON'T CHANGE NAME: NEXTAUTH_URL as-is—NextAuth looks specifically for that exact env var to build its callback/redirect URLs
  NEXTAUTH_URL: process.env.NEXTAUTH_URL,
  /* Reference: https://next-auth.js.org/configuration/options#nextauth_secret
    Required secret for NextAuth encryption/signing in all environments
   */
  NEXTAUTH_SECRET: process.env.NEXTAUTH_SECRET,
} as const;

export const PUBLIC_CONFIG = {
  AUTH_SERVER: process.env.NEXT_PUBLIC_AUTH_SERVER,
  // Uncomment the line below if AUTH_SERVER is needed in the client-side code
  // ORCHESTRATOR_SERVER: process.env.NEXT_PUBLIC_ORCHESTRATOR_SERVER,
  CLIENT_ID: process.env.NEXT_PUBLIC_CLIENT_ID,
  SCOPE: process.env.NEXT_PUBLIC_SCOPE,
} as const;
