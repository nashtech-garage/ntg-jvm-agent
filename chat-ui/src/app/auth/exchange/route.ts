import { AUTH_SERVER_URL, setTokenIntoCookie } from '@/app/utils/utils';
import { NextResponse } from 'next/server';

export async function POST(req: Request) {
  const { code, redirect_uri } = await req.json();

  const tokenUrl = `${AUTH_SERVER_URL}/oauth2/token`;
  const clientId = process.env.CLIENT_ID;
  const clientSecret = process.env.CLIENT_SECRET;

  const basic = Buffer.from(`${clientId}:${clientSecret}`).toString('base64');

  const body = new URLSearchParams();
  body.set('grant_type', 'authorization_code');
  body.set('code', code);
  body.set('redirect_uri', redirect_uri);

  const tokenRes = await fetch(tokenUrl, {
    method: 'POST',
    headers: {
      Authorization: `Basic ${basic}`,
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: body.toString(),
  });

  if (!tokenRes.ok) {
    const errText = await tokenRes.text();
    return NextResponse.json({ error: errText }, { status: 400 });
  }

  const tokenJson = await tokenRes.json();
  const res = NextResponse.json({
    success: true,
  });
  return setTokenIntoCookie(tokenJson, res);
}
