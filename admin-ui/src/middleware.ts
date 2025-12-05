import { NextResponse, type NextRequest } from 'next/server';
import { getSessionToken } from '@/utils/server-utils';
import { PAGE_PATH } from '@/constants/url';
import { hasAdminRole } from '@/utils/user';
import logger from '@/utils/logger';

export async function middleware(request: NextRequest) {
  if (!request.nextUrl.pathname.startsWith('/admin')) {
    return NextResponse.next();
  }

  const token = await getSessionToken(request);
  const loginUrl = new URL(PAGE_PATH.LOGIN, request.url);

  if (!token || !!token.error) {
    logger.error('Unauthorized access attempt to admin route: no session token found');
    return NextResponse.redirect(loginUrl);
  }

  const hasAdmin = hasAdminRole(token.roles ?? []);

  if (!hasAdmin) {
    logger.error('Unauthorized access attempt to admin route: insufficient privileges');
    return NextResponse.redirect(PAGE_PATH.FORBIDDEN);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/admin/:path*'],
};
