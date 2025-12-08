import NextAuth from 'next-auth';
import { authOptions } from '@/libs/auth-options';
import logger from '@/utils/logger';
import { SERVER_CONFIG } from '@/constants/site-config';

if (!SERVER_CONFIG.NEXTAUTH_SECRET) {
  logger.warn('NEXTAUTH_SECRET environment variable is missing.');
}

const handler = NextAuth(authOptions);

export { handler as GET, handler as POST };
