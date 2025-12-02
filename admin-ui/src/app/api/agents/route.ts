import { cookies } from 'next/headers';
import { SITE_CONFIG } from '@/constants/site-config';

export async function GET() {
  const cookieStore = await cookies();
  const token = cookieStore.get('access_token')?.value;

  if (!token) {
    return new Response('Unauthorized', { status: 401 });
  }

  const backendRes = await fetch(`${SITE_CONFIG.ORCHESTRATOR_SERVER}/api/agents`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
    },
    cache: 'no-store',
  });

  if (!backendRes.ok) {
    return new Response('Failed to fetch agents', { status: backendRes.status });
  }

  const data = await backendRes.json();
  return Response.json(data);
}

export async function POST(req: Request) {
  const cookieStore = await cookies();
  const token = cookieStore.get('access_token')?.value;

  if (!token) {
    return new Response('Unauthorized', { status: 401 });
  }

  // Parse request body from frontend
  const body = await req.json();

  const backendRes = await fetch(`${SITE_CONFIG.ORCHESTRATOR_SERVER}/api/agents`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
  });

  if (!backendRes.ok) {
    const errorText = await backendRes.text();
    return new Response(errorText || 'Failed to create agent', {
      status: backendRes.status,
    });
  }

  const data = await backendRes.json();
  return Response.json(data);
}
