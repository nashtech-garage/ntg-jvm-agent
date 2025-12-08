import { NextResponse } from 'next/server';
import { SERVER_CONFIG } from '@/constants/site-config';
import { getAccessToken } from '@/actions/session';

const baseUrl = `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/settings`;

export async function GET() {
  const accessToken = await getAccessToken();

  if (!accessToken) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const res = await fetch(`${baseUrl}`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    const jsonResult = await res.json();
    if (!res.ok) {
      return NextResponse.json({ error: jsonResult.message }, { status: res.status });
    }

    return NextResponse.json(jsonResult);
  } catch (err) {
    return NextResponse.json(
      { error: `Failed to fetch system setting: ${String(err)}` },
      { status: 500 }
    );
  }
}

export async function PUT(req: Request) {
  const accessToken = await getAccessToken();
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }
  try {
    const body = await req.json();
    const res = await fetch(baseUrl, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify(body),
    });

    const jsonResult = await res.json();
    if (!res.ok) {
      return NextResponse.json({ error: jsonResult.message }, { status: res.status });
    }

    return NextResponse.json(jsonResult);
  } catch (err) {
    return NextResponse.json({ error: `${String(err)}` }, { status: 500 });
  }
}
