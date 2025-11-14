import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const baseUrl = 'http://localhost:9000/api/users';

export async function GET(req: Request) {
  const cookieStore = cookies();
  const accessToken = (await cookieStore).get('access_token')?.value;

  if (!accessToken) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const { searchParams } = new URL(req.url);
  const page = searchParams.get('page') || '0';
  const size = searchParams.get('size') || '10';

  try {
    const res = await fetch(`${baseUrl}?page=${page}&size=${size}`, {
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
    return NextResponse.json({ error: `Failed to fetch users: ${String(err)}` }, { status: 500 });
  }
}

export async function POST(req: Request) {
  const cookieStore = cookies();
  const accessToken = (await cookieStore).get('access_token')?.value;

  if (!accessToken) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const body = await req.json();

    const res = await fetch(baseUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify(body),
    });

    const jsonResult = await res.json().catch(() => ({}));

    if (!res.ok) {
      return NextResponse.json(
        { error: jsonResult.message || 'Failed to create user' },
        { status: res.status }
      );
    }

    return NextResponse.json(jsonResult, { status: 201 });
  } catch (err) {
    return NextResponse.json(
      { error: `Failed to create user: ${String(err)}` },
      { status: 500 }
    );
  }
}
