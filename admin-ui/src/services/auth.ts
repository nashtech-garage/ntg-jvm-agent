import { SERVER_CONFIG } from '@/constants/site-config';
import { API_PATH } from '@/constants/url';
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

export async function signOut({
  serverUri = API_PATH.SIGN_OUT,
}: {
  serverUri?: string;
}): Promise<void> {
  try {
    const res = await fetch(serverUri, {
      method: 'POST',
      cache: 'no-store',
    });

    if (!res.ok) {
      logger.error(`Logout API failed during signOut callback: HTTP ${res.status}`);
    }
  } catch (error) {
    logger.error('Logout API call failed during signOut callback', error);
  }
}
