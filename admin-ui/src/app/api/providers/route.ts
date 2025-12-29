import { SERVER_CONFIG } from '@/constants/site-config';
import { NextResponse } from 'next/server';
import { Constants } from '@/constants/constant';
import { PROVIDER_PATH } from '@/constants/url';
import { withAuthenticatedAPI } from '@/data/client/with-authenticated-api';

const baseUrl = SERVER_CONFIG.ORCHESTRATOR_SERVER;

export const GET = withAuthenticatedAPI(async (_req, accessToken) => {
  const providerUrl = `${baseUrl}${PROVIDER_PATH.PROVIDERS}`;
  try {
    const res = await fetch(providerUrl, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });

    const jsonResult = await res.json();
    if (!res.ok) {
      return NextResponse.json({ error: jsonResult.message }, { status: res.status });
    }

    return NextResponse.json(jsonResult);
  } catch (err) {
    return NextResponse.json(
      { error: `${Constants.FAILED_TO_FETCH_PROVIDER_NAME_MSG} ${String(err)}` },
      { status: 500 }
    );
  }
});
