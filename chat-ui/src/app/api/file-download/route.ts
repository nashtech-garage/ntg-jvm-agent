import { NextResponse } from 'next/server';
import { getAccessToken, ORCHESTRATOR_URL } from '@/app/utils/server-utils';
import { Constants } from '@/app/utils/constant';

const baseUrl = `${ORCHESTRATOR_URL}/api/files`;

export async function GET(req: Request) {
  const accessToken = await getAccessToken(req);
  if (!accessToken) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const { searchParams } = new URL(req.url);
  const filePath = searchParams.get('path');

  if (!filePath) {
    return NextResponse.json({ error: 'File path is required' }, { status: 400 });
  }

  try {
    const encodedPath = encodeURIComponent(filePath);
    const targetUrl = `${baseUrl}/download?path=${encodedPath}`;

    const res = await fetch(targetUrl, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (!res.ok) {
      const errorText = await res.text();
      return NextResponse.json(
        { error: errorText || 'Failed to download file' },
        { status: res.status }
      );
    }

    const contentType = res.headers.get('content-type') || 'application/octet-stream';
    const contentDisposition = res.headers.get('content-disposition');

    let fileName = 'download';
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename="?([^"]+)"?/);
      if (fileNameMatch) {
        fileName = fileNameMatch[1];
      }
    }

    const blob = await res.blob();

    return new NextResponse(blob, {
      headers: {
        'Content-Type': contentType,
        'Content-Disposition': `attachment; filename="${fileName}"`,
        'Cache-Control': 'no-cache',
      },
    });
  } catch (err) {
    console.error('Error downloading file:', err);
    return NextResponse.json(
      { error: `${Constants.FAILED_TO_FETCH_CONVERSATIONS_MSG} ${String(err)}` },
      { status: 500 }
    );
  }
}
