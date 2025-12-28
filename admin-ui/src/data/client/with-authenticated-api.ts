import { NextResponse } from 'next/server';
import { getAccessToken } from '@/actions/session';

type AuthenticatedHandler<TContext = unknown> = (
  req: Request,
  accessToken: string,
  context: TContext
) => Promise<Response> | Response;

/**
 * Server-side API route wrapper that enforces authentication.
 *
 * Resolves the current user's access token and passes it to the inner handler.
 * If no access token is present, responds with HTTP 401 and does not invoke
 * the wrapped handler.
 *
 * Intended for use in Next.js App Router route handlers (app/api/**).
 */
export function withAuthenticatedAPI<TContext = unknown>(handler: AuthenticatedHandler<TContext>) {
  return async (req: Request, context: TContext) => {
    const accessToken = await getAccessToken();
    if (!accessToken) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    return handler(req, accessToken, context);
  };
}
