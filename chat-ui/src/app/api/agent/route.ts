import { NextResponse } from 'next/server';
import { getAccessToken } from '@/utils/server-utils';
import { Constants } from '@/constants/constant';
import { SERVER_CONFIG } from '@/constants/site-config';

const agentUrl = `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/agents`;

export async function GET(req: Request) {
  const accessToken = await getAccessToken(req);
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }
  try {
    const res = await fetch(`${agentUrl}/active`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });

    const jsonResult = await res.json();
    if (!res.ok) {
      return NextResponse.json({ error: jsonResult.message }, { status: res.status });
    }

    return NextResponse.json(jsonResult);
  } catch (err) {
    return NextResponse.json(
      { error: `${Constants.FAILED_TO_FETCH_AGENTS_MSG} ${String(err)}` },
      { status: 500 }
    );
  }
}
