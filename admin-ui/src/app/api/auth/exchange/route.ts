<<<<<<< HEAD
import { AUTH_SERVER_URL, decodeToken } from '@/app/utils/utils';
=======
import { decodeToken, setTokenIntoCookie } from '@/app/utils/serverUtils';
>>>>>>> dd362ba936c61a6bc141cd4636721c88933e1346
import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  try {
    const { code, redirect_uri } = await request.json();

    if (!code) {
      return NextResponse.json({ error: 'Authorization code is required' }, { status: 400 });
    }
    const tokenUrl = `${AUTH_SERVER_URL}/oauth2/token`;
    const clientId = process.env.CLIENT_ID;
    const clientSecret = process.env.CLIENT_SECRET;

    const basic = Buffer.from(`${clientId}:${clientSecret}`).toString('base64');

    const body = new URLSearchParams();
    body.set('grant_type', 'authorization_code');
    body.set('code', code);
    body.set('redirect_uri', redirect_uri);

    // Exchange authorization code for tokens with the authorization server
    const tokenResponse = await fetch(tokenUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        Authorization: 'Basic ' + basic,
      },
      body: body.toString(),
    });

    if (!tokenResponse.ok) {
      const errorText = await tokenResponse.text();
      console.error('Token exchange failed:', errorText);
      return NextResponse.json({ error: 'Token exchange failed' }, { status: 401 });
    }

    const tokenData = await tokenResponse.json();

    // Get user info from decoded access token
    const decodedToken = decodeToken(tokenData.access_token);
    const userInfo = {
      id: decodedToken?.sub,
      username: decodedToken?.sub,
      roles: decodedToken?.roles || [],
    };
    console.log('Decoded token:', decodedToken);

    // Check if user has admin role
    if (!userInfo.roles?.some((role: string) => role === 'ADMIN')) {
      return NextResponse.json({ error: 'Access denied. Admin role required.' }, { status: 403 });
    }

    // Create response with cookies
    const response = NextResponse.json({
      user: userInfo,
      token: {
        access_token: tokenData.access_token,
        refresh_token: tokenData.refresh_token,
        expires_in: tokenData.expires_in,
        token_type: tokenData.token_type,
        scope: tokenData.scope,
      },
    });

    // Set secure HTTP-only cookies
    setTokenIntoCookie(tokenData, response);

    return response;
  } catch (error) {
    console.error('Authentication error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
