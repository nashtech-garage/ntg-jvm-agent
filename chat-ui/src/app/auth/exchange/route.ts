import { NextResponse } from "next/server";

export async function POST(req: Request) {
  const { code, redirect_uri } = await req.json();

  const tokenUrl = `${process.env.NEXT_PUBLIC_AUTH_SERVER}/oauth2/token`;
  const clientId = process.env.CLIENT_ID;
  const clientSecret = process.env.CLIENT_SECRET;

  const basic = Buffer.from(`${clientId}:${clientSecret}`).toString("base64");

  const body = new URLSearchParams();
  body.set("grant_type", "authorization_code");
  body.set("code", code);
  body.set("redirect_uri", redirect_uri);

  const tokenRes = await fetch(tokenUrl, {
    method: "POST",
    headers: {
      Authorization: `Basic ${basic}`,
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: body.toString(),
  });

  if (!tokenRes.ok) {
    const errText = await tokenRes.text();
    return NextResponse.json({ error: errText }, { status: 400 });
  }

  const tokenJson = await tokenRes.json();

  console.log(tokenJson.access_token);
  // Create httpOnly cookies
  const res = NextResponse.json({ success: true });
  res.cookies.set("access_token", tokenJson.access_token, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: tokenJson.expires_in ?? 3600,
  });

  // Keep refresh token on server-side using setting httpOnly cookie
  if (tokenJson.refresh_token) {
    res.cookies.set("refresh_token", tokenJson.refresh_token, {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      path: "/",
      maxAge: 60 * 60 * 24 * 30,
    });
  }

  return res;
}
