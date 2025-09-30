import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { getRefreshToken } from "./app/libs/fetchWithAuth";
import { setTokenIntoCookie } from "./app/libs/util";

export async function middleware(req: NextRequest) {
  const token =
    req.cookies.get("access_token") || req.cookies.get("refresh_token");
  const isAuthPage =
    req.nextUrl.pathname.startsWith("/login") ||
    req.nextUrl.pathname.startsWith("/auth");

  // If not login yet and not in login page → redirect to /login
  if (!token && !isAuthPage) {
    return NextResponse.redirect(new URL("/login", req.url));
  }

  if (!req.cookies.get("access_token") && req.cookies.get("refresh_token")) {
    const tokenInfo = await getRefreshToken(req.cookies.get("refresh_token")?.value || '');

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
      res.headers.set("x-access-token", tokenInfo.access_token);
      setTokenIntoCookie(tokenInfo, res);
      return res;
    } else {
      return NextResponse.redirect(new URL("/login", req.url));
    }
  }
  
  return NextResponse.next();
}

// apply middleware for entire routes (except static files)
export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
