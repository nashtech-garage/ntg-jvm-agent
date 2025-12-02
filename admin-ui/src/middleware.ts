import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { getRefreshToken, setTokenIntoCookie } from '@/utils/server-utils';

export async function middleware(request: NextRequest) {
  // Check if user is accessing admin routes
  if (request.nextUrl.pathname.startsWith('/admin')) {
    // Check for authentication token in cookies
    const accessToken = request.cookies.get('access_token');
    const refreshToken = request.cookies.get('refresh_token');

    if (!accessToken) {
      if (refreshToken) {
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
        }
      }
      // Redirect to login if no token
      const loginUrl = new URL('/login', request.url);
      loginUrl.searchParams.set('redirect', request.nextUrl.pathname);
      return NextResponse.redirect(loginUrl);
    }

    // Token validation and role checking is handled in the AuthContext
    // and admin layout components for better UX
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/admin/:path*'],
};
