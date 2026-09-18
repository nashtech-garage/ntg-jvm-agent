import { NextResponse } from 'next/server';
import { SERVER_CONFIG } from '@/constants/site-config';
import { withAuthenticatedAPI } from '@/utils/withAuthen';

export const POST = withAuthenticatedAPI(async (req, accessToken) => {
  const conversationId = new URL(req.url).pathname.split('/').at(-2);

  try {
    const body = await req.text();
    const res = await fetch(
      `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/conversations/${conversationId}/answer`,
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body,
      }
    );

    return new Response(res.body, {
      status: res.status,
      headers: {
        'Content-Type': res.headers.get('Content-Type') || 'text/plain',
        'Cache-Control': 'no-cache, no-transform',
        Connection: 'keep-alive',
        'Content-Encoding': 'identity',
      },
    });
  } catch (error) {
    return NextResponse.json(
      { error: `Failed to answer question: ${String(error)}` },
      { status: 500 }
    );
  }
});
