export const Constants = {
  THIRTY_DAYS_IN_SECONDS: 60 * 60 * 24 * 30,
};

export enum LoginParams {
  ERROR = 'error',
}

export enum LoginErrors {
  ACCESS_DENIED = 'access_denied',
  ACCESS_DENIED_NEXTAUTH = 'AccessDenied',
  OAUTH_SIGNIN_ERROR = 'OAuthSignin',
  SESSION_EXPIRED = 'SessionExpired',
  REFRESH_TOKEN_MISSING = 'RefreshTokenMissing',
  REFRESH_TOKEN_ERROR = 'RefreshAccessTokenError',
}

export const API_URLS = {
  USERS: '/api/users',
};
