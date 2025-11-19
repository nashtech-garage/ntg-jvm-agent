import { NextResponse } from 'next/server';
import { getAccessToken, ORCHESTRATOR_URL } from '@/app/utils/server-utils';
import { Constants } from '@/app/utils/constant';

const agentUrl = `${ORCHESTRATOR_URL}/api/agents`;

export async function GET(req: Request) {
  const accessToken = await getAccessToken(req);
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }
  try {
    const res = await fetch(agentUrl, {
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
