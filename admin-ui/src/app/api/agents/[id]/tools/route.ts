import { cookies } from 'next/headers';
import { SITE_CONFIG } from '@/constants/site-config';

export async function GET(_req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  const cookieStore = await cookies();
  const token = cookieStore.get('access_token')?.value;

  if (!token) {
    return new Response('Unauthorized', { status: 401 });
  }

  const backendRes = await fetch(`${SITE_CONFIG.ORCHESTRATOR_SERVER}/api/agents/${id}/tools`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
    },
    cache: 'no-store',
  });

  if (!backendRes.ok) {
    return new Response('Failed to load tools', {
      status: backendRes.status,
    });
  }

  const data = await backendRes.json();
  return Response.json(data);
}

export async function POST(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  const cookieStore = await cookies();
  const token = cookieStore.get('access_token')?.value;

  if (!token) {
    return new Response('Unauthorized', { status: 401 });
  }

  const body = await req.json();

  const backendRes = await fetch(`${SITE_CONFIG.ORCHESTRATOR_SERVER}/api/agents/${id}/tools`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
    cache: 'no-store',
  });

  if (!backendRes.ok) {
    return new Response('Failed to create tool', {
      status: backendRes.status,
    });
  }

  const data = await backendRes.json();
  return Response.json(data);
}
