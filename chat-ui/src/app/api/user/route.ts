import { NextResponse } from 'next/server';
import { Constants } from '@/constants/constant';
import { SERVER_CONFIG } from '@/constants/site-config';
import { withAuthenticatedAPI } from '@/utils/withAuthen';

export const GET = withAuthenticatedAPI(async (_req, accessToken) => {
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
});
