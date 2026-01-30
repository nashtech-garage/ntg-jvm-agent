import { NextRequest, NextResponse } from 'next/server';
import { SERVER_CONFIG } from '@/constants/site-config';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/libs/auth-options';

export async function GET(
  req: NextRequest,
  { params }: { params: Promise<{ id: string; revision: string }> }
): Promise<NextResponse> {
  const { id, revision } = await params;
  const session = await getServerSession(authOptions);

  if (!session?.accessToken) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const backendUrl = `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/v1/audit/agent/${id}/revision/${revision}`;
    const response = await fetch(backendUrl, {
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: 'Failed to fetch revision' },
        { status: response.status }
      );
    }
    // Orchestrator might respond with 200 and an empty body; handle gracefully
    const raw = await response.text();
    const data = raw ? JSON.parse(raw) : null;
    return NextResponse.json(data);
  } catch (error) {
    console.error('Revision fetch error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

