import { NextResponse } from 'next/server';
import { getAccessToken } from '@/actions/session';
import { decodeToken } from '@/utils/server-utils';
import { SERVER_CONFIG } from '@/constants/site-config';
import logger from '@/utils/logger';

export async function GET() {
  try {
    const accessToken = await getAccessToken();

    if (!accessToken) {
      return NextResponse.json({ error: 'No access token found' }, { status: 401 });
    }

    const userInfoResponse = await fetch(`${SERVER_CONFIG.AUTH_SERVER}/userinfo`, {
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
