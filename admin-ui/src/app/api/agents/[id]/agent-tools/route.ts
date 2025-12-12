import { SERVER_CONFIG } from '@/constants/site-config';
import { getAccessToken } from '@/actions/session';

export async function GET(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  const token = await getAccessToken();

  if (!token) {
    return new Response('Unauthorized', { status: 401 });
  }

  // Extract search query parameter from request URL
  const { searchParams } = new URL(req.url);
  const name = searchParams.get('name');

  // Build backend URL with search parameter if provided
  const backendUrl = new URL(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/agents/${id}/tools/assignment`
  );
  if (name) {
    backendUrl.searchParams.set('name', name);
  }

  const backendRes = await fetch(backendUrl.toString(), {
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
  const { id: agentId } = await params;

  const token = await getAccessToken();
  if (!token) {
    return new Response('Unauthorized', { status: 401 });
  }

  const body = await req.json();

  const backendRes = await fetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/agents/${agentId}/tools/${body.toolId}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      cache: 'no-store',
    }
  );

  if (!backendRes.ok) {
    return new Response('Failed to assign tool', {
      status: backendRes.status,
    });
  }

  return backendRes;
}

export async function DELETE(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id: agentId } = await params;

  const token = await getAccessToken();

  if (!token) {
    return new Response('Unauthorized', { status: 401 });
  }

  const body = await req.json();

  const backendRes = await fetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/agents/${agentId}/tools/${body.toolId}`,
    {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      cache: 'no-store',
    }
  );

  if (!backendRes.ok) {
    return new Response('Failed to un assign tool', {
      status: backendRes.status,
    });
  }

  return backendRes;
}
