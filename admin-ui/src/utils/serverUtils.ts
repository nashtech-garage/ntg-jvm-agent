/**
 * Utility functions to be used in Server Component only
 */

import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import { TokenInfo } from '@/models/token';
import { Constants } from '@/constants/constant';

// Decodes the payload of a JWT token from base64 and parses it as JSON.
export function decodeToken(token: string) {
  try {
    return JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
  } catch (error) {
    console.error('Failed to decode token:', error);
    return null;
  }
}

export async function getRefreshToken(refreshToken: string): Promise<TokenInfo | null> {
  try {
    const tokenUrl = `${process.env.NEXT_PUBLIC_AUTH_SERVER}/oauth2/token`;
    const clientId = process.env.CLIENT_ID;
    const clientSecret = process.env.CLIENT_SECRET;

    if (!clientId || !clientSecret) {
      console.error('Missing required environment variables: CLIENT_ID and/or CLIENT_SECRET');
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
      console.error(`Token refresh failed: HTTP ${res.status} - ${errorBody}`);
      return null;
    }

    return await res.json();
  } catch (error) {
    console.error('Token refresh failed:', error);
    return null;
  }
}

export function setTokenIntoCookie(tokenInfo: TokenInfo, res: NextResponse) {
  if (tokenInfo.access_token) {
    res.cookies.set('access_token', tokenInfo.access_token, {
      httpOnly: true,
      sameSite: 'lax',
      secure: process.env.NODE_ENV === 'production',
      path: '/',
      maxAge: tokenInfo.expires_in ?? 3600,
    });
  }

  // Keep refresh token on server-side by setting an httpOnly cookie
  if (tokenInfo.refresh_token) {
    res.cookies.set('refresh_token', tokenInfo.refresh_token, {
      httpOnly: true,
      sameSite: 'lax',
      secure: process.env.NODE_ENV === 'production',
      path: '/',
      maxAge: Constants.THIRTY_DAYS_IN_SECONDS,
    });
  }

  return res;
}

/**
 * Get access token from custom header or cookie.
 *
 * @param req The inbound request
 *
 * @returns A Promise that resolve to an access token string
 * or undefined if no access token is set.
 */
export async function getAccessToken(req: Request): Promise<string | undefined> {
  const headerToken = req.headers.get('x-access-token');
  if (headerToken) {
    return headerToken;
  }
  return (await cookies()).get('access_token')?.value;
}
