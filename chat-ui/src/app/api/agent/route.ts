import { NextResponse } from 'next/server';
import { Constants } from '@/constants/constant';
import { SERVER_CONFIG } from '@/constants/site-config';
import { withAuthenticatedAPI } from '@/utils/withAuthen';

const agentUrl = `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/agents`;

export const GET = withAuthenticatedAPI(async (_req, accessToken) => {
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
});
