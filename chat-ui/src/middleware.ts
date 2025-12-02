import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { setTokenIntoCookie, getRefreshToken, deleteCookies } from './utils/server-utils';

export async function middleware(req: NextRequest) {
  const pathName = req.nextUrl.pathname;

  const accessToken = req.cookies.get('access_token');
  const refreshToken = req.cookies.get('refresh_token');

  const hasAuthToken = accessToken || refreshToken;

  // Treat UI auth pages (like /login, /auth) as auth pages, but do NOT treat
  // API endpoints (e.g. /api/auth/*) as auth pages. This prevents the
  // middleware from redirecting API calls such as `/api/auth/logout`.
  const isAuthPage = ['/login', '/auth'].some((name) => pathName.startsWith(name));
  const isAuthApi = pathName.startsWith('/api/auth');

  // If not logged in and not on an auth page or auth API → redirect to /login
  if (!hasAuthToken && !isAuthPage && !isAuthApi) {
    return NextResponse.redirect(new URL('/login', req.url));
  }

  // If already logged in and trying to access UI auth pages (login/register),
  // redirect to the app root. Do NOT redirect API calls.
  if (hasAuthToken && isAuthPage) {
    return NextResponse.redirect(new URL('/', req.url));
  }

  // Handle refreshing token
  if (!accessToken && refreshToken) {
    const tokenInfo = await getRefreshToken(refreshToken.value);

    if (tokenInfo) {
      const res = NextResponse.next();
      /*
        Because cookies set in NextResponse are only stored on the browser after the current response is completed,
        the new cookie is not available to the same request where it was issued. This means that on the first request,
        the middleware sets the cookie but the API route still sees the old (null) cookie.
        Only on the next request does the browser send back the updated cookie.
        To make the new token immediately usable within the same request,
        we need to also pass it through a custom header (e.g. x-access-token).
      */
      res.headers.set('x-access-token', tokenInfo.access_token);
      setTokenIntoCookie(tokenInfo, res);
      return res;
    } else {
      // refresh token expired, so we have to logout and login again
      const fromHandler = req.headers.get('From-Handler') === 'true';
      let res;
      const url = new URL('/login', req.url);
      if (fromHandler) {
        // route handler calls expect a json response
        res = NextResponse.json({ redirectTo: url.pathname });
      } else {
        // normal page can be directly redirected
        res = NextResponse.redirect(url);
      }
      deleteCookies(res);
      return res;
    }
  }

  return NextResponse.next();
}

// Apply middleware for entire routes (except static files)
export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
