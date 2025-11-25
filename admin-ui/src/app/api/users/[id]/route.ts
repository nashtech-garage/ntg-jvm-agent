import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const baseUrl = `${process.env.NEXT_PUBLIC_AUTH_SERVER}/api/users`;

export async function PATCH(req: Request, context: unknown) {
  const cookieStore = cookies();
  const accessToken = (await cookieStore).get('access_token')?.value;

  const params = (context as { params?: { id?: string } })?.params;
  const id = params?.id;

  if (!accessToken) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  if (!id) {
    return NextResponse.json({ error: 'Missing user id' }, { status: 400 });
  }

  try {
    const body = await req.json();
    const res = await fetch(`${baseUrl}/${id}`, {
      method: 'PATCH',
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
    return NextResponse.json({ error: `Failed to update user: ${String(err)}` }, { status: 500 });
  }
}
