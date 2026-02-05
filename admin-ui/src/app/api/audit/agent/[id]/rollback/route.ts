import { NextRequest, NextResponse } from 'next/server';
import { SERVER_CONFIG } from '@/constants/site-config';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/libs/auth-options';

export async function POST(
  req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
): Promise<NextResponse> {
  const { id } = await params;
  const session = await getServerSession(authOptions);

  if (!session?.accessToken) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const body = await req.json();
    const backendUrl = `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/v1/audit/agent/${id}/rollback`;

    const response = await fetch(backendUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${session.accessToken}`,
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ error: 'Rollback failed' }));
      return NextResponse.json(errorData, { status: response.status });
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Rollback error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

