import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { getSessionToken } from '@/utils/server-utils';
import { API_PATH, PAGE_PATH } from './constants/url';
import logger from './utils/logger';

export async function middleware(req: NextRequest) {
  const pathName = req.nextUrl.pathname;
  const isAuthPage = [PAGE_PATH.LOGIN, API_PATH.AUTH].some((name) => pathName.startsWith(name));

  if (!isAuthPage) {
    const token = await getSessionToken(req);
    const loginUrl = new URL(PAGE_PATH.LOGIN, req.url);

    // If not logged in and not on an auth page or auth API → redirect to /login
    if (!token) {
      logger.error('No valid session token found, redirecting to login page');
      return NextResponse.redirect(loginUrl);
    }
  }

  return NextResponse.next();
}

// Apply middleware for entire routes (except static files)
export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
