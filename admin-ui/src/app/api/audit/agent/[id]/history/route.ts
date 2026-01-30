import { NextRequest, NextResponse } from 'next/server';
import { SERVER_CONFIG } from '@/constants/site-config';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/libs/auth-options';

export async function GET(
  req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
): Promise<NextResponse> {
  const { id } = await params;
  const session = await getServerSession(authOptions);

  if (!session?.accessToken) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const backendUrl = `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/v1/audit/agent/${id}/history`;
    const response = await fetch(backendUrl, {
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: 'Failed to fetch audit history' },
        { status: response.status }
      );
    }
    // Handle orchestrator responses that may return an empty body on 200
    const raw = await response.text();
    const data = raw ? JSON.parse(raw) : [];
    return NextResponse.json(data);
  } catch (error) {
    console.error('Audit history fetch error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

