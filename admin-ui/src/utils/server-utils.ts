import { getToken, type GetTokenParams, type JWT } from 'next-auth/jwt';
import { TokenInfo } from '@/models/token';
import { SERVER_CONFIG } from '@/constants/site-config';
import logger from './logger';
import { LoginErrors } from '@/constants/constant';

// Decodes the payload of a JWT token from base64 and parses it as JSON.
export function decodeToken(token: string) {
  try {
    return JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
  } catch (error) {
    logger.error('Failed to decode token:', error);
    return null;
  }
}

async function getRefreshToken(refreshToken: string): Promise<TokenInfo | null> {
  try {
    const tokenUrl = `${SERVER_CONFIG.AUTH_SERVER}/oauth2/token`;
    const clientId = SERVER_CONFIG.CLIENT_ID;
    const clientSecret = SERVER_CONFIG.CLIENT_SECRET;

    if (!clientId || !clientSecret) {
      logger.error('Missing required environment variables: CLIENT_ID and/or CLIENT_SECRET');
      return null;
    }

    const basic = Buffer.from(`${clientId}:${clientSecret}`).toString('base64');

    const body = new URLSearchParams();
    body.set('grant_type', 'refresh_token');
    body.set('refresh_token', refreshToken);

    const res = await fetch(tokenUrl, {
      method: 'POST',
      headers: {
        Authorization: `Basic ${basic}`,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: body.toString(),
    });

    if (!res.ok) {
      const errorBody = await res.text();
      logger.error(`Token refresh failed: HTTP ${res.status} - ${errorBody}`);
      return null;
    }

    return await res.json();
  } catch (error) {
    logger.error('Token refresh failed:', error);
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
 * Retrieve the NextAuth session token for a request (works in middleware/route handlers).
 */
export async function getSessionToken(
  req: NonNullable<GetTokenParams['req']>
): Promise<JWT | null> {
  return getToken({ req, secret: SERVER_CONFIG.NEXTAUTH_SECRET });
}
