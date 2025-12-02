import { NextResponse } from 'next/server';
import logger from '@/utils/logger';
import { deleteCookies } from '@/utils/server-utils';

export async function POST() {
  try {
    const response = NextResponse.json({ message: 'Logged out successfully' });

    deleteCookies(response);

    return response;
  } catch (error) {
    logger.error('Logout error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
