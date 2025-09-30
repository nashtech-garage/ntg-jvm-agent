import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import { TokenInfo } from '@/app/models/token';

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
  } catch {
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
      maxAge: 60 * 60 * 24 * 30,
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
