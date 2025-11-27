import { NextResponse } from 'next/server';
import { getAccessToken, ORCHESTRATOR_URL } from '@/app/utils/server-utils';
import { Constants } from '@/app/utils/constant';

const baseUrl = `${ORCHESTRATOR_URL}/api/conversations`;

export async function GET(req: Request) {
  const accessToken = await getAccessToken(req);
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }

  const { searchParams } = new URL(req.url);
  const conversationId = searchParams.get('conversationId');
  let targetUrl = `${baseUrl}/user`;
  try {
    if (conversationId) {
      targetUrl = `${baseUrl}/${conversationId}/messages`;
    }
    const res = await fetch(targetUrl, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });

    const jsonResult = await res.json();
    if (!res.ok) {
      return NextResponse.json({ error: jsonResult.message }, { status: res.status });
    }

    return NextResponse.json(jsonResult);
  } catch (err) {
    return NextResponse.json(
      { error: `${Constants.FAILED_TO_FETCH_CONVERSATIONS_MSG} ${String(err)}` },
      { status: 500 }
    );
  }
}

export async function POST(req: Request) {
  const accessToken = await getAccessToken(req);
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }

  try {
    const formData = await req.formData();
    const res = await fetch(baseUrl, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      body: formData,
    });

    return new Response(res.body, {
      status: res.status,
      headers: {
        'Content-Type': res.headers.get('Content-Type') || 'text/plain',
        'Cache-Control': 'no-cache, no-transform',
        Connection: 'keep-alive',
        'Content-Encoding': 'identity',
      },
    });
  } catch (err) {
    return NextResponse.json(
      { error: `${Constants.FAILED_TO_ASK_QUESTION} ${String(err)}` },
      { status: 500 }
    );
  }
}

export async function PUT(req: Request) {
  const accessToken = await getAccessToken(req);
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }

  const { searchParams } = new URL(req.url);
  const conversationId = searchParams.get('conversationId');

  try {
    const body = await req.json();
    const res = await fetch(`${baseUrl}/${conversationId}`, {
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
      { error: `Failed to update conversation: ${String(err)}` },
      { status: 500 }
    );
  }
}

export async function DELETE(req: Request) {
  const accessToken = await getAccessToken(req);
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }

  const { searchParams } = new URL(req.url);
  const conversationId = searchParams.get('conversationId');

  try {
    const res = await fetch(`${baseUrl}/${conversationId}`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (!res.ok) {
      const errorJson = await res.json();
      return NextResponse.json({ error: errorJson.message }, { status: res.status });
    }

    return NextResponse.json({ status: res.status });
  } catch (err) {
    return NextResponse.json(
      { error: `${Constants.FAILED_TO_DELETE_CONVERSATION_MSG} ${String(err)}` },
      { status: 500 }
    );
  }
}
