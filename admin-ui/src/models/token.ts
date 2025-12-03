export interface TokenInfo {
  access_token: string;
  refresh_token?: string;
  expires_in?: number;
  token_type?: string;
  scope?: string;
}

export interface UserInfo {
  sub: string;
  name: string;
  email: string;
  roles: string[];
  preferred_username?: string;
}

export interface JWTPayload {
  sub: string;
  name: string;
  email: string;
  roles: string[];
  exp: number;
  iat: number;
  iss: string;
  aud: string;
}
