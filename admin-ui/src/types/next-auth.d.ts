import type { DefaultSession, DefaultUser } from 'next-auth';

declare module 'next-auth' {
  interface User extends DefaultUser {
    roles?: string[];
    preferred_username?: string;
  }

  interface Session {
    accessToken?: string;
    refreshToken?: string;
    user?: DefaultSession['user'] & {
      id: string;
      roles?: string[];
      preferred_username?: string;
    };
  }
}

declare module 'next-auth/jwt' {
  interface JWT {
    accessToken?: string;
    refreshToken?: string;
    expiresAt?: number;
    roles?: string[];
    error?: string;
  }
}
