import { NextResponse } from 'next/server';
import { SERVER_CONFIG } from '@/constants/site-config';
import { withAuthenticatedAPI } from '@/utils/withAuthen';

export const GET = withAuthenticatedAPI(async (req, accessToken) => {
  const conversationId = new URL(req.url).pathname.split('/').at(-2);

  try {
    const res = await fetch(
      `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/conversations/${conversationId}/pending-question`,
      { headers: { Authorization: `Bearer ${accessToken}` } }
    );
    if (res.status === 204) return new Response(null, { status: 204 });

    const body = await res.json();
    return NextResponse.json(body, { status: res.status });
  } catch (error) {
    return NextResponse.json(
      { error: `Failed to fetch pending question: ${String(error)}` },
      { status: 500 }
    );
  }
});
