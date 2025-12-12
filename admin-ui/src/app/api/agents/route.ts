import { SERVER_CONFIG } from '@/constants/site-config';
import { BACKEND_PATH } from '@/constants/url';
import { getAccessToken } from '@/actions/session';

export async function GET(req: Request) {
  const token = await getAccessToken();

  if (!token) {
    return new Response('Unauthorized', { status: 401 });
  }

  const { searchParams } = new URL(req.url);
  const searchQuery = searchParams.get('name') || '';
  const backendPath = BACKEND_PATH.AGENTS_SEARCH(searchQuery);
  const backendUrl = `${SERVER_CONFIG.ORCHESTRATOR_SERVER}${backendPath}`;

  const backendRes = await fetch(backendUrl, {
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
  const token = await getAccessToken();

  if (!token) {
    return new Response('Unauthorized', { status: 401 });
  }

  const body = await req.json();

  const backendRes = await fetch(`${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/agents`, {
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
