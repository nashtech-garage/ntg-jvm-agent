import { NextResponse } from 'next/server';
import { AUTH_SERVER_URL, getAccessToken } from '@/utils/server-utils';
import { Constants } from '@/constants/constant';

export async function GET(req: Request) {
  const accessToken = await getAccessToken(req);
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }

  try {
    const res = await fetch(`${AUTH_SERVER_URL}/userinfo`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });

    if (!res.ok) {
      return NextResponse.json(
        { error: Constants.FAILED_TO_FETCH_USER_INFO_MSG },
        { status: res.status }
      );
    }

    return NextResponse.json(await res.json());
  } catch (err) {
    return NextResponse.json(
      { error: `${Constants.FAILED_TO_FETCH_USER_INFO_MSG} ${String(err)}` },
      { status: 500 }
    );
  }
}
