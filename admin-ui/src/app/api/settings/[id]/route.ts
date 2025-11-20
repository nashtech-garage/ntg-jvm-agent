import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const baseUrl = `${process.env.NEXT_PUBLIC_ORCHESTRATOR_SERVER}/api/settings`;

export async function PUT(req: Request, context: { params: Record<string, string> }) {
  const cookieStore = cookies();
  const accessToken = (await cookieStore).get('access_token')?.value;
  if (!accessToken) {
    return NextResponse.json(null, { status: 401 });
  }
  try {
    const { id } = context.params;
    if (!id) return NextResponse.json({ message: 'Missing id' }, { status: 400 });
    const body = await req.json();
    const targetUrl = `${baseUrl}/${id}`;
    const res = await fetch(targetUrl, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${accessToken}`
      },
      body:  JSON.stringify(body),
    });

    const jsonResult = await res.json();
    if (!res.ok) {
      return NextResponse.json({ error: jsonResult.message }, { status: res.status });
    }

    return NextResponse.json(jsonResult);
  } catch (err) {
    return NextResponse.json(
      { error: `${String(err)}` },
      { status: 500 }
    );
  }
}
