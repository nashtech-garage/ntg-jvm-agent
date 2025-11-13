import { NextResponse } from 'next/server';

export async function POST() {
  try {
    const response = NextResponse.json({ message: 'Logged out successfully' });

    // Clear authentication cookies
    response.cookies.delete('access_token');
    response.cookies.delete('refresh_token');
    // Optional: Clear JSESSIONID as additional security measure
    response.cookies.delete('JSESSIONID');

    return response;
  } catch (error) {
    console.error('Logout error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
