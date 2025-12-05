/* eslint-disable no-console */
import { NextResponse } from 'next/server';
import { SERVER_CONFIG } from '@/constants/site-config';

const baseUrl = `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/share/shared-conversations`;

export async function GET(req: Request, { params }: { params: Promise<{ shareToken: string }> }) {
  const { shareToken } = await params;
  if (!shareToken) {
    return NextResponse.json({ error: 'Share token is required' }, { status: 400 });
  }

  try {
    const res = await fetch(`${baseUrl}/${shareToken}`);

    if (!res.ok) {
      const contentType = res.headers.get('content-type');
      let errorData = { message: `Error: ${res.status}` };

      if (contentType && contentType.includes('application/json')) {
        try {
          errorData = await res.json();
        } catch (e) {
          console.error('Failed to parse error response:', e);
        }
      }

      return NextResponse.json(
        { error: errorData.message || 'Failed to fetch shared conversation' },
        { status: res.status }
      );
    }

    const jsonResult = await res.json();
    return NextResponse.json(jsonResult);
  } catch (err) {
    console.error('Error fetching shared conversation:', err);
    return NextResponse.json(
      { error: `Failed to fetch shared conversation: ${String(err)}` },
      { status: 500 }
    );
  }
}
