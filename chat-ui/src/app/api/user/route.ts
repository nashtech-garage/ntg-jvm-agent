import { NextResponse } from 'next/server';
import { Constants } from '@/constants/constant';
import { SERVER_CONFIG } from '@/constants/site-config';
import { getAccessToken } from '@/actions/session';

export async function GET() {
  const accessToken = await getAccessToken();
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }

  try {
    const res = await fetch(`${SERVER_CONFIG.AUTH_SERVER}/userinfo`, {
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
