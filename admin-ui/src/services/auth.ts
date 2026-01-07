import { SERVER_CONFIG } from '@/constants/site-config';
import { TokenInfo } from '@/models/token';
import logger from '@/utils/logger';

export interface ExchangeAuthCodeParams {
  requestTokenUri: string;
  code: string;
  redirectUri: string;
  codeVerifier?: string;
}

export async function getRefreshToken(refreshToken: string): Promise<TokenInfo | null> {
  try {
    const tokenUrl = `${SERVER_CONFIG.AUTH_SERVER}/oauth2/token`;
    const clientId = SERVER_CONFIG.CLIENT_ID;
    const clientSecret = SERVER_CONFIG.CLIENT_SECRET;

    if (!clientId || !clientSecret) {
      logger.error('Missing required environment variables: CLIENT_ID and/or CLIENT_SECRET');
      return null;
    }

    const basic = Buffer.from(`${clientId}:${clientSecret}`).toString('base64');

    const body = new URLSearchParams();
    body.set('grant_type', 'refresh_token');
    body.set('refresh_token', refreshToken);

    const res = await fetch(tokenUrl, {
      method: 'POST',
      headers: {
        Authorization: `Basic ${basic}`,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: body.toString(),
    });

    if (!res.ok) {
      const errorBody = await res.text();
      logger.error(`Token refresh failed: HTTP ${res.status} - ${errorBody}`);
      return null;
    }

    return await res.json();
  } catch (error) {
    logger.error('Token refresh failed:', error);
    return null;
  }
}
