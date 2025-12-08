import { NextResponse } from 'next/server';
import { getAccessToken } from '@/actions/session';

type AuthenticatedHandler<TContext = unknown> = (
  req: Request,
  accessToken: string,
  context?: TContext
) => Promise<Response> | Response;

/**
 * Wraps an API route handler to ensure the caller is authenticated.
 * Passes the resolved access token to the inner handler.
 */
export function withAuthenticatedAPI<TContext = unknown>(handler: AuthenticatedHandler<TContext>) {
  return async (req: Request, context?: TContext) => {
    const accessToken = await getAccessToken();
    if (!accessToken) {
      return NextResponse.json(null, { status: 401 });
    }

    return handler(req, accessToken, context);
  };
}
