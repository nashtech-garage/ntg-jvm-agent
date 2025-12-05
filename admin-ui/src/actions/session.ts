'use server';

import { authOptions } from '@/libs/auth-options';
import logger from '@/utils/logger';
import { getServerSession } from 'next-auth';
import { cookies } from 'next/headers';

/**
 * Convenience helper to grab the access token from the server session.
 */
export async function getAccessToken(): Promise<string | null> {
  const session = await getServerSession(authOptions);
  return session?.accessToken ?? null;
}

/* Names cover both dev and secure-cookie variants
  so we clear sessions in all environments. */
const NEXT_AUTH_COOKIES = [
  'JSESSIONID',
  'access_token',
  'refresh_token',
  'next-auth.session-token',
  '__Secure-next-auth.session-token',
  'next-auth.csrf-token',
  '__Host-next-auth.csrf-token',
  'next-auth.callback-url',
  '__Secure-next-auth.callback-url',
  'next-auth.pkce.code_verifier',
  '__Secure-next-auth.pkce.code_verifier',
  'next-auth.state',
  '__Secure-next-auth.state',
];

export async function clearSession() {
  const cookieStore = await cookies();

  try {
    NEXT_AUTH_COOKIES.forEach((name) => {
      cookieStore.delete(name);
    });
    logger.info('Cleared NextAuth session cookies');
    return { success: true };
  } catch (error: Error | unknown) {
    logger.error('Error when clear NextAuth session cookies', error);
    return { success: false, error: (error as Error).message };
  }
}
