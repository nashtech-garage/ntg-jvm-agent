import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

export async function GET(request: NextRequest) {
    try {
        const cookieStore = cookies();
        const accessToken = (await cookieStore).get('access_token')?.value;

        if (!accessToken) {
            return NextResponse.json({ error: 'No access token found' }, { status: 401 });
        }

        // Decode token to get user info
        const decodedToken = JSON.parse(Buffer.from(accessToken.split('.')[1], 'base64').toString());
        const userInfo = {
            id: decodedToken.sub,
            username: decodedToken.sub,
            roles: decodedToken.roles || [],
        }

        // Check admin role
        if (!userInfo.roles?.some((role: string) =>
            role === 'ADMIN'
        )) {
            return NextResponse.json({ error: 'Access denied. Admin role required.' }, { status: 403 });
        }

        return NextResponse.json({
            user: userInfo,
            token: {
                access_token: accessToken,
            },
        });

    } catch (error) {
        console.error('Auth check error:', error);
        return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
    }
}
