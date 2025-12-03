import { NextResponse } from 'next/server';
import logger from '@/utils/logger';

/* Names cover both dev and secure-cookie variants
  so we clear sessions in all environments. */
const NEXT_AUTH_COOKIES = [
  'JSESSIONID',
  'next-auth.session-token',
  '__Secure-next-auth.session-token',
  'next-auth.csrf-token',
  '__Host-next-auth.csrf-token',
  'next-auth.callback-url',
  '__Secure-next-auth.callback-url',
  'next-auth.pkce.code_verifier',
  '__Secure-next-auth.pkce.code_verifier',
  'next-auth.state',
  '__Secure-next-auth.state',
];

export async function POST() {
  const response = NextResponse.json({ success: true });

  NEXT_AUTH_COOKIES.forEach((name) => {
    response.cookies.set({
      name,
      value: '',
      path: '/',
      expires: new Date(0),
    });
  });

  logger.info('User logged out; NextAuth session cookies cleared');

  return response;
}
