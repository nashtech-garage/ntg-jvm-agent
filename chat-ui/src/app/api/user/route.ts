import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

export async function GET() {
  const token = (await cookies()).get('access_token')?.value;

  if (!token) {
    return NextResponse.json(null);
  }

  const res = await fetch(`${process.env.NEXT_PUBLIC_AUTH_SERVER}/userinfo`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  const data = await res.json();
  return NextResponse.json(data);
}
