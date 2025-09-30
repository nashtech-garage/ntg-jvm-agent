import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';

export async function fetchWithAuth(
  input: RequestInfo,
  init?: RequestInit
): Promise<any> {
  let res = null;
  const accessToken = (await cookies()).get('access_token')?.value;
  const refreshToken = (await cookies()).get('refresh_token')?.value || '';

  // Case accessToken doesn't exist in cookie due to removing by expired time
  if (!accessToken) {
    const refreshRes = await getRefreshToken(refreshToken);

    // Refresh fail → logout
    if (refreshRes === null) {
      return NextResponse.redirect(new URL('/login'));
    }

    res = await callTargetAPI(input, refreshRes.access_token, init);
    return {
      ...res,
      token: refreshRes
    };
  }
  
  res = await callTargetAPI(input, accessToken, init);
  return res;
}

async function callTargetAPI(input: RequestInfo, accessToken: string, init?: RequestInit): Promise<Response> {
  const response = await fetch(input, {
      ...init,
      credentials: 'include',
      headers: {
        ...init?.headers,
        Authorization: `Bearer ${accessToken}`,
      },
    });

    return (await response.json());
}

export async function getRefreshToken(refreshToken: string): Promise<any|null> {
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

    return (await res.json());
  } catch {
    return null;
  }
}