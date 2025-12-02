import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import { TokenInfo } from '@/models/token';
import { Constants } from '@/constants/constant';
import { IS_PRODUCTION, SITE_CONFIG } from '@/constants/site-config';
import logger from './logger';

export async function getRefreshToken(refreshToken: string): Promise<TokenInfo | null> {
  try {
    const tokenUrl = `${SITE_CONFIG.AUTH_SERVER_URL}/oauth2/token`;
    const clientId = SITE_CONFIG.CLIENT_ID;
    const clientSecret = SITE_CONFIG.CLIENT_SECRET;
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
      logger.error(`Token refresh failed: ${res.status}`, await res.text());
      return null;
    }

    return await res.json();
  } catch (error) {
    logger.error('Token refresh failed:', error);
    return null;
  }
}

export function setTokenIntoCookie(tokenInfo: TokenInfo, res: NextResponse) {
  if (tokenInfo.access_token) {
    res.cookies.set('access_token', tokenInfo.access_token, {
      httpOnly: true,
      sameSite: 'lax',
      secure: IS_PRODUCTION,
      path: '/',
      maxAge: tokenInfo.expires_in ?? 3600,
    });
  }

  // Keep refresh token on server-side using setting httpOnly cookie
  if (tokenInfo.refresh_token) {
    res.cookies.set('refresh_token', tokenInfo.refresh_token, {
      httpOnly: true,
      sameSite: 'lax',
      secure: IS_PRODUCTION,
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

export function deleteCookies(response: NextResponse): NextResponse {
  response.cookies.delete('access_token');
  response.cookies.delete('refresh_token');
  response.cookies.delete('JSESSIONID');
  return response;
}
