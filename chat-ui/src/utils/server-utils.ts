import { SERVER_CONFIG } from '@/constants/site-config';
import logger from './logger';
import { getToken, GetTokenParams, JWT } from 'next-auth/jwt';
import { LoginErrors } from '@/constants/constant';
import { getRefreshToken } from '@/services/auth';

// Decodes the payload of a JWT token from base64 and parses it as JSON.
export function decodeToken(token: string) {
  try {
    return JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
  } catch (error) {
    logger.error('Failed to decode token:', error);
    return null;
  }
}

export async function refreshAccessToken(token: JWT): Promise<JWT> {
  if (!token.refreshToken) {
    logger.error('No refresh token available');
    return { ...token, error: LoginErrors.REFRESH_TOKEN_MISSING };
  }

  const refreshed = await getRefreshToken(token.refreshToken!);

  if (!refreshed?.access_token) {
    logger.error('Failed to refresh access token');
    return { ...token, error: LoginErrors.REFRESH_TOKEN_ERROR };
  }

  return {
    ...token,
    accessToken: refreshed.access_token,
    refreshToken: refreshed.refresh_token ?? token.refreshToken,
    expiresAt: Date.now() + (refreshed.expires_in ?? 3600) * 1000,
  };
}

/**
 * Retrieve the NextAuth session token for a request
 * Works in middleware/route handlers
 */
export async function getSessionToken(
  req: NonNullable<GetTokenParams['req']>
): Promise<JWT | null> {
  const session = await getToken({ req, secret: SERVER_CONFIG.NEXTAUTH_SECRET });
  if (session?.error) {
    return null;
  }
  return session;
}
