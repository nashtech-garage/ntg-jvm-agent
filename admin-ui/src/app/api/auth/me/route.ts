import { NextResponse } from 'next/server';
import { decodeToken, getAccessToken } from '@/utils/server-utils';
import { SITE_CONFIG } from '@/constants/site-config';
import logger from '@/utils/logger';

export async function GET(req: Request) {
  try {
    const accessToken = await getAccessToken(req);

    if (!accessToken) {
      return NextResponse.json({ error: 'No access token found' }, { status: 401 });
    }

    // Fetch user info from the OAuth2 provider
    const userInfoResponse = await fetch(`${SITE_CONFIG.AUTH_SERVER_URL}/userinfo`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (!userInfoResponse.ok) {
      return NextResponse.json({ error: 'Failed to fetch user info' }, { status: 401 });
    }
    const userInfo = await userInfoResponse.json();
    const decodedToken = decodeToken(accessToken);
    userInfo.roles = decodedToken?.roles || [];

    // Check admin role
    if (!userInfo.roles?.some((role: string) => role === 'ADMIN')) {
      return NextResponse.json({ error: 'Access denied. Admin role required.' }, { status: 403 });
    }

    return NextResponse.json({
      user: userInfo,
      token: {
        access_token: accessToken,
      },
    });
  } catch (error) {
    logger.error('Auth check error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
