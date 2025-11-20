import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const baseUrl = `${process.env.NEXT_PUBLIC_ORCHESTRATOR_SERVER}/api/settings`;

export async function GET() {
  const cookieStore = cookies();
  const accessToken = (await cookieStore).get('access_token')?.value;

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
    return NextResponse.json({ error: `Failed to fetch system setting: ${String(err)}` }, { status: 500 });
  }
}
