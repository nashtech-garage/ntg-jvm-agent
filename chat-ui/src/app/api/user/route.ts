import { NextResponse } from 'next/server';
import { getAccessToken } from '@/app/libs/util';

export async function GET(req: Request) {
  const accessToken = await getAccessToken(req);

  if (!accessToken) {
    return NextResponse.json(null);
  }

  const res = await fetch(`${process.env.NEXT_PUBLIC_AUTH_SERVER}/userinfo`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  const data = await res.json();
  return NextResponse.json(data);
}