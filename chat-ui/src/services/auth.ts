import { SERVER_CONFIG } from '@/constants/site-config';
import { TokenInfo } from '@/models/token';
import logger from '@/utils/logger';

type OAuthTokenResponse = Record<string, unknown> & {
  access_token?: string;
  refresh_token?: string;
  expires_in?: number;
  expires_at?: number;
  id_token?: string;
};

export interface ExchangeAuthCodeParams {
  requestTokenUri: string;
  code: string;
  redirectUri: string;
  codeVerifier?: string;
}

export async function exchangeAuthorizationCode({
  requestTokenUri,
  code,
  redirectUri,
  codeVerifier,
}: ExchangeAuthCodeParams): Promise<OAuthTokenResponse> {
  const basic = Buffer.from(`${SERVER_CONFIG.CLIENT_ID}:${SERVER_CONFIG.CLIENT_SECRET}`).toString(
    'base64'
  );

  const body = new URLSearchParams();
  body.set('grant_type', 'authorization_code');
  body.set('code', code);
  body.set('redirect_uri', redirectUri);
  if (codeVerifier) {
    body.set('code_verifier', codeVerifier);
  }

  const tokenResponse = await fetch(requestTokenUri, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      Authorization: `Basic ${basic}`,
    },
    body: body.toString(),
    cache: 'no-store',
  });

  if (!tokenResponse.ok) {
    const errorText = await tokenResponse.text();
    logger.error('Token exchange failed during OAuth callback', {
      status: tokenResponse.status,
      error: errorText,
    });
    throw new Error('OAuthTokenExchangeFailed');
  }

  return tokenResponse.json();
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
