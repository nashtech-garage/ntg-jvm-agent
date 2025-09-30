import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function middleware(req: NextRequest) {
  const accessToken = req.cookies.get("access_token");
  const isAuthPage =
    req.nextUrl.pathname.startsWith("/login") ||
    req.nextUrl.pathname.startsWith("/auth");

  // If not login yet and not in login page → redirect to /login
  if (!accessToken && !isAuthPage) {
    return NextResponse.redirect(new URL("/login", req.url));
  }

  return NextResponse.next();
}

// apply middleware for entire routes (except static files)
export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
