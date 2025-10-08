import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { setTokenIntoCookie, getRefreshToken } from './app/utils/utils';

export async function middleware(req: NextRequest) {
  const hasAuthToken = req.cookies.get('access_token') || req.cookies.get('refresh_token');
  const isAuthPage =
    req.nextUrl.pathname.startsWith('/login') || req.nextUrl.pathname.startsWith('/auth');
  const isAdminPage = req.nextUrl.pathname.startsWith('/admin');
  const isUnauthorizedPage = req.nextUrl.pathname.startsWith('/unauthorized');

  // If not login yet and not in login/auth/unauthorized page → redirect to /login
  if (!hasAuthToken && !isAuthPage && !isUnauthorizedPage) {
    return NextResponse.redirect(new URL('/login', req.url));
  }

  // For admin pages, check if user has admin role
  if (isAdminPage && hasAuthToken) {
    try {
      const { getUserInfo } = await import('./app/utils/utils');
      const userInfo = await getUserInfo(req);
      console.log('User Info in Middleware:', userInfo);

      if (!userInfo || !userInfo.roles.some(role =>
        role.toLowerCase() === 'admin' || role === 'ADMIN'
      )) {
        return NextResponse.redirect(new URL('/unauthorized', req.url));
      }
    } catch (error) {
      console.error('Error checking admin access:', error);
      return NextResponse.redirect(new URL('/login', req.url));
    }
  }

  // Handle refreshing token
  if (!req.cookies.get('access_token') && req.cookies.get('refresh_token')) {
    const tokenInfo = await getRefreshToken(req.cookies.get('refresh_token')?.value || '');

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
      return NextResponse.redirect(new URL('/login', req.url));
    }
  }

  return NextResponse.next();
}

// Apply middleware for entire routes (except static files)
export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
