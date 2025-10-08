import { NextResponse } from 'next/server';
import { getAccessToken } from '@/app/utils/utils';
import { Constants } from '@/app/utils/constant';

export async function GET(req: Request) {
  const accessToken = await getAccessToken(req);
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }

  try {
    const res = await fetch(`${process.env.NEXT_PUBLIC_AUTH_SERVER}/userinfo`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });

    if (!res.ok) {
      return NextResponse.json({ error: Constants.FAILED_TO_FETCH_USER_INFO_MSG }, { status: res.status });
    }

    return NextResponse.json(await res.json());
  } catch (err) {
    return NextResponse.json({ error: `${Constants.FAILED_TO_FETCH_USER_INFO_MSG} ${String(err)}` }, { status: 500 });
  }
}
