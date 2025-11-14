import { NextResponse } from 'next/server';
<<<<<<< HEAD
import { cookies } from 'next/headers';
import { AUTH_SERVER_URL, decodeToken } from '@/app/utils/utils';
=======
import { decodeToken, getAccessToken } from '@/app/utils/serverUtils';
>>>>>>> dd362ba936c61a6bc141cd4636721c88933e1346

export async function GET(req: Request) {
  try {
    const accessToken = await getAccessToken(req);

    if (!accessToken) {
      return NextResponse.json({ error: 'No access token found' }, { status: 401 });
    }

    // Fetch user info from the OAuth2 provider
    const userInfoResponse = await fetch(`${AUTH_SERVER_URL}/userinfo`, {
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
    console.error('Auth check error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
