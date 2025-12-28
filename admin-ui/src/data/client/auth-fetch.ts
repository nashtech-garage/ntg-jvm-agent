import 'server-only';
import { getAccessToken } from '@/actions/session';

/**
 * Server-only authenticated fetch helper.
 *
 * Automatically attaches the current user's access token to the request.
 * Throws on missing authentication or non-2xx backend responses.
 *
 * Intended for use in:
 * - Next.js route handlers (app/api)
 * - Server components
 * - Server actions
 *
 * Must NOT be used in client components or hooks.
 */
export async function authFetch(input: string, init: RequestInit = {}) {
  const token = await getAccessToken();
  if (!token) {
    throw new Error('Unauthorized');
  }

  const res = await fetch(input, {
    ...init,
    headers: {
      ...init.headers,
      Authorization: `Bearer ${token}`,
    },
    cache: 'no-store',
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Backend request failed (${res.status}): ${text || res.statusText}`);
  }

  return res;
}
