import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
    // Check if user is accessing admin routes
    if (request.nextUrl.pathname.startsWith('/admin')) {
        // Check for authentication token in cookies
        const accessToken = request.cookies.get('access_token');

        if (!accessToken) {
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
    matcher: ['/admin/:path*']
};
