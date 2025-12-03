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
}
