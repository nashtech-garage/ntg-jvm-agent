import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { decodeToken } from '@/app/utils/utils';

export async function GET(request: NextRequest) {
  try {
    const cookieStore = cookies();
    const accessToken = (await cookieStore).get('access_token')?.value;

    if (!accessToken) {
      return NextResponse.json({ error: 'No access token found' }, { status: 401 });
    }

    // Fetch user info from the OAuth2 provider
    const userInfoResponse = await fetch(`${process.env.NEXT_PUBLIC_AUTH_SERVER}/userinfo`, {
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
