import { NextResponse } from 'next/server';
import { withAuthenticatedAPI } from '@/utils/withAuthen';

const baseUrl = `${process.env.NEXT_PUBLIC_AUTH_SERVER}/api/users`;

export const PATCH = withAuthenticatedAPI(
  async (req: Request, accessToken: string, { params }: { params: Promise<{ id: string }> }) => {
    const { id } = await params;

    if (!id) {
      return NextResponse.json({ error: 'Missing user id' }, { status: 400 });
    }

    try {
      const body = await req.json();
      const res = await fetch(`${baseUrl}/${id}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify(body),
      });

      const jsonResult = await res.json();

      if (!res.ok) {
        return NextResponse.json(
          { error: jsonResult.message || jsonResult.error || 'Failed to update user' },
          { status: res.status }
        );
      }

      return NextResponse.json(jsonResult);
    } catch (err) {
      return NextResponse.json({ error: `Failed to update user: ${String(err)}` }, { status: 500 });
    }
  }
);
