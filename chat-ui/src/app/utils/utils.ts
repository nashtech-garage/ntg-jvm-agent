import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import { TokenInfo, UserInfo, JWTPayload } from '@/app/models/token';
import { Constants } from '@/app/utils/constant';

export async function getRefreshToken(refreshToken: string): Promise<TokenInfo | null> {
  try {
    const tokenUrl = `${process.env.NEXT_PUBLIC_AUTH_SERVER}/oauth2/token`;
    const clientId = process.env.CLIENT_ID;
    const clientSecret = process.env.CLIENT_SECRET;
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

  // Keep refresh token on server-side using setting httpOnly cookie
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
 * Get access token from both cookie and custom header
 *
 * @param {Request} req
 *
 * @return {Promise<string|undefined>}
 */
export async function getAccessToken(req: Request): Promise<string | undefined> {
  const headerToken = req.headers.get('x-access-token');
  const cookieToken = (await cookies()).get('access_token')?.value;
  return headerToken || cookieToken;
}

/**
 * Decode JWT token without verification (for client-side use only)
 * In production, always verify the token on the server side
 */
export function decodeJWT(token: string): JWTPayload | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }

    const payload = parts[1];
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    return decoded as JWTPayload;
  } catch (error) {
    console.error('Failed to decode JWT:', error);
    return null;
  }
}

/**
 * Get user information from access token
 */
export async function getUserInfo(req?: Request): Promise<UserInfo | null> {
  try {
    const token = req ? await getAccessToken(req) : (await cookies()).get('access_token')?.value;

    if (!token) {
      return null;
    }

    const payload = decodeJWT(token);
    if (!payload) {
      return null;
    }

    return {
      sub: payload.sub,
      name: payload.name,
      email: payload.email,
      roles: payload.roles || [],
    };
  } catch (error) {
    console.error('Failed to get user info:', error);
    return null;
  }
}

/**
 * Check if user has admin role
 */
export async function isAdmin(req?: Request): Promise<boolean> {
  const userInfo = await getUserInfo(req);
  return userInfo?.roles.includes('admin') || userInfo?.roles.includes('ADMIN') || false;
}

/**
 * Check if user has specific role
 */
export async function hasRole(role: string, req?: Request): Promise<boolean> {
  const userInfo = await getUserInfo(req);
  return userInfo?.roles.includes(role) || userInfo?.roles.includes(role.toUpperCase()) || false;
}
