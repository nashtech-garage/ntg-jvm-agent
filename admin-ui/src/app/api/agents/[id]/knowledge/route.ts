import { NextRequest, NextResponse } from 'next/server';
import { SERVER_CONFIG } from '@/constants/site-config';
import { getAccessToken } from '@/actions/session';

export async function GET(_req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const token = await getAccessToken();
  if (!token) return new NextResponse('Unauthorized', { status: 401 });

  const { id } = await params;

  const backendRes = await fetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/agents/${id}/knowledge`,
    {
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
      cache: 'no-store',
    }
  );

  if (!backendRes.ok) {
    return new NextResponse('Failed to load knowledge', {
      status: backendRes.status,
    });
  }

  const data = await backendRes.json();
  return NextResponse.json(data);
}

export async function POST(req: NextRequest, ctx: { params: Promise<{ id: string }> }) {
  const contentType = req.headers.get('content-type') ?? '';

  if (contentType.startsWith('multipart/form-data')) {
    return handleMultipart(req, ctx);
  } else {
    return handleJson(req, ctx);
  }
}

async function handleMultipart(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const token = await getAccessToken();
  if (!token) return new NextResponse('Unauthorized', { status: 401 });

  const { id } = await params;

  const incoming = await req.formData();
  const outForm = new FormData();
  incoming.forEach((value, key) => {
    if (key === 'files' && value instanceof File) {
      outForm.append('files', value);
    } else {
      outForm.append(key, value);
    }
  });

  const backendRes = await fetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/agents/${id}/knowledge/import`,
    {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: outForm,
    }
  );

  const data = await backendRes.json();
  return new NextResponse(JSON.stringify(data), { status: backendRes.status });
}

async function handleJson(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const token = await getAccessToken();
  if (!token) return new NextResponse('Unauthorized', { status: 401 });

  const { id } = await params;

  const body = await req.json();

  const backendRes = await fetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/agents/${id}/knowledge`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    }
  );

  const data = await backendRes.json();
  return new NextResponse(JSON.stringify(data), { status: backendRes.status });
}
