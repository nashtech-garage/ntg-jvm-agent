import { NextResponse } from 'next/server';
import { SERVER_CONFIG } from '@/constants/site-config';
import { withAuthenticatedAPI } from '@/utils/withAuthen';
import { REACTION_PATH } from '@/constants/url';
const baseUrl = SERVER_CONFIG.ORCHESTRATOR_SERVER;

type ReactionContext = { params: Promise<{ messageId: string }> };

const putHandler = withAuthenticatedAPI<ReactionContext>(async (req, accessToken, context) => {
  const params = await context?.params;
  const messageId = params?.messageId ?? '';
  const body = await req.json();
  const url = `${baseUrl}${REACTION_PATH.CHAT_MESSAGE_ENDPOINT(messageId)}`;

  const res = await fetch(url, {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  });

  const json = await res.json();
  if (!res.ok) return NextResponse.json({ error: json.message }, { status: res.status });

  return NextResponse.json(json);
});

export const PUT = (req: Request, context: ReactionContext) => putHandler(req, context);
