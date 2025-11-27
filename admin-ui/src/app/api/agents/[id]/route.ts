import { cookies } from 'next/headers';

export async function GET(_req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const cookieStore = await cookies();
  const token = cookieStore.get('access_token')?.value;

  if (!token) {
    return new Response('Unauthorized', { status: 401 });
  }

  const backendRes = await fetch(
    `${process.env.NEXT_PUBLIC_ORCHESTRATOR_SERVER}/api/agents/${id}`,
    {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
      },
      cache: 'no-store',
    }
  );

  if (!backendRes.ok) {
    return new Response('Agent not found', { status: backendRes.status });
  }

  const data = await backendRes.json();
  return Response.json(data);
}

export async function PUT(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const cookieStore = await cookies();
  const token = cookieStore.get('access_token')?.value;

  if (!token) {
    return new Response('Unauthorized', { status: 401 });
  }

  // Parse request body from frontend
  const body = await req.json();

  const backendRes = await fetch(
    `${process.env.NEXT_PUBLIC_ORCHESTRATOR_SERVER}/api/agents/${id}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(body),
    }
  );

  if (!backendRes.ok) {
    const errorText = await backendRes.text();
    return new Response(errorText || 'Failed to update agent', {
      status: backendRes.status,
    });
  }

  const data = await backendRes.json();
  return Response.json(data);
}
