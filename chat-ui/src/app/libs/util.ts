import { cookies } from "next/headers";
import { NextResponse } from "next/server";

export function setTokenIntoCookie(tokenInfo: any, res: NextResponse) {
  if (tokenInfo.access_token) {
    res.cookies.set("access_token", tokenInfo.access_token, {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      path: "/",
      maxAge: tokenInfo.expires_in ?? 3600,
    });
  }

  // Keep refresh token on server-side using setting httpOnly cookie
  if (tokenInfo.refresh_token) {
    res.cookies.set("refresh_token", tokenInfo.refresh_token, {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      path: "/",
      maxAge: 60 * 60 * 24 * 30,
    });
  }

  return res;
}

/**
 * Get access token from both cookie and custom header
 * 
 * @param {Request} req
 *
 * @return {Promise<string|undefined>}
 */
export async function getAccessToken(req: Request): Promise<string|undefined> {
  const headerToken = req.headers.get("x-access-token");
  const cookieToken  = (await cookies()).get('access_token')?.value;
  return (headerToken || cookieToken);
}