import { NextResponse } from 'next/server';
import { getAccessToken } from '@/utils/server-utils';
import { SERVER_CONFIG } from '@/constants/site-config';
import { Constants } from '@/constants/constant';
const baseUrl = `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/conversations/messages`;

export async function PUT(req: Request, { params }: { params: Promise<{ messageId: string }> }) {
  const { messageId } = await params;
  const accessToken = await getAccessToken(req);
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }

  try {
    const body = await req.json();
    const res = await fetch(`${baseUrl}/${messageId}/reaction`, {
      method: 'PUT',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    const jsonResult = await res.json();

    if (!res.ok) {
      return NextResponse.json({ error: jsonResult.message }, { status: res.status });
    }

    return NextResponse.json(jsonResult);
  } catch (err) {
    return NextResponse.json(
      { error: `${Constants.FAILED_TO_REACT_MSG ?? 'Failed to react'} ${String(err)}` },
      { status: 500 }
    );
  }
}
