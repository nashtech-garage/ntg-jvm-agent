import { Constants } from '@/constants/constant';
import { SERVER_CONFIG } from '@/constants/site-config';
import { PROVIDER_PATH } from '@/constants/url';
import { withAuthenticatedAPI } from '@/data/client/with-authenticated-api';
import { NextResponse } from 'next/server';

const baseUrl = SERVER_CONFIG.ORCHESTRATOR_SERVER;

type ProviderContext = { params: Promise<{ id: string }> };

const getHandler = withAuthenticatedAPI<ProviderContext>(async (req, accessToken, context) => {
  const params = await context?.params;
  const id = params?.id ?? '';
  const endpointURL = `${baseUrl}${PROVIDER_PATH.PROVIDER_DETAIL(id)}`;

  try {
    const res = await fetch(endpointURL, {
      method: 'GET',
      headers: { Authorization: `Bearer ${accessToken}` },
    });

    const jsonResult = await res.json();
    if (!res.ok) {
      return NextResponse.json({ error: jsonResult.message }, { status: res.status });
    }

    return NextResponse.json(jsonResult);
  } catch (err) {
    return NextResponse.json(
      { error: `${Constants.FAILED_TO_FETCH_PROVIDER_MSG} ${String(err)}` },
      { status: 500 }
    );
  }
});

export const GET = (req: Request, context: ProviderContext) => getHandler(req, context);
