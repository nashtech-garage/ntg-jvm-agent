import { NextResponse } from 'next/server';
import { getAccessToken, ORCHESTRATOR_URL } from '@/utils/server-utils';

const baseUrl = `${ORCHESTRATOR_URL}/api/share/shared-conversations`;

export async function POST(req: Request) {
  const accessToken = await getAccessToken(req);
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }

  const { searchParams } = new URL(req.url);
  const conversationId = searchParams.get('conversationId');

  if (!conversationId) {
    return NextResponse.json({ error: 'Conversation ID is required' }, { status: 400 });
  }

  try {
    const body = await req.json();
    const { expiryDays = 7 } = body;

    const res = await fetch(`${baseUrl}/${conversationId}/share`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ expiryDays }),
    });

    const jsonResult = await res.json();
    if (!res.ok) {
      return NextResponse.json({ error: jsonResult.message }, { status: res.status });
    }

    return NextResponse.json(jsonResult);
  } catch (err) {
    return NextResponse.json(
      { error: `Failed to create share link: ${String(err)}` },
      { status: 500 }
    );
  }
}
