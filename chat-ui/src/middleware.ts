import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { getSessionToken } from '@/utils/server-utils';
import { API_PATH, PAGE_PATH } from './constants/url';
import logger from './utils/logger';

const handleAuthenticatedRedirect = async (req: NextRequest) => {
  const pathName = req.nextUrl.pathname;
  const isLoginPage = [PAGE_PATH.LOGIN.toString()].includes(pathName);
  const isAuthPage = [PAGE_PATH.LOGIN, API_PATH.AUTH].some((name) => pathName.startsWith(name));
  const token = await getSessionToken(req);

  if (isLoginPage && !!token) {
    const homeUrl = new URL(PAGE_PATH.HOME, req.url);
    // If logged in and on an auth page redirect to home page
    logger.info('Session token found, redirecting to home page');
    return NextResponse.redirect(homeUrl);
  }

  if (!isAuthPage) {
    const loginUrl = new URL(PAGE_PATH.LOGIN, req.url);

    // If not logged in and not on an auth page or auth API redirect to /login
    if (!token || !!token.error) {
      logger.error('No valid session token found, redirecting to login page');
      return NextResponse.redirect(loginUrl);
    }
  }
};

export async function middleware(req: NextRequest) {
  const redirectResponse = await handleAuthenticatedRedirect(req);
  if (redirectResponse) {
    return redirectResponse;
  }

  return NextResponse.next();
}

// Apply middleware for entire routes (except static files)
export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
