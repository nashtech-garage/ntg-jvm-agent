'use server';

import { authOptions } from '@/libs/auth-options';
import { getServerSession } from 'next-auth';

/**
 * Convenience helper to grab the access token from the server session.
 */
export async function getAccessToken(): Promise<string | null> {
  const session = await getServerSession(authOptions);
  return session?.accessToken ?? null;
}
