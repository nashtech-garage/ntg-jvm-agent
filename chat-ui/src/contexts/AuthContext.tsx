'use client';

import React, {
  createContext,
  useContext,
  useMemo,
  ReactNode,
  useCallback,
  useEffect,
} from 'react';
import { SessionProvider, signIn, signOut, useSession } from 'next-auth/react';
import { TokenInfo, UserInfo } from '@/models/token';
import { PAGE_PATH } from '@/constants/url';
import { useRouter } from 'next/navigation';
import logger from '@/utils/logger';
import { LoginErrors } from '@/constants/constant';
import { clearSession } from '@/actions/session';

interface AuthContextType {
  user: UserInfo | null;
  token: TokenInfo | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  signOut: () => Promise<boolean>;
  signIn: typeof signIn;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

interface AuthProviderProps {
  readonly children: ReactNode;
}

function AuthStateProvider({ children }: AuthProviderProps) {
  const router = useRouter();
  const { data: session, status } = useSession();
  const rawUser = session?.user;

  const user: UserInfo | null = useMemo(() => {
    if (!rawUser) {
      return null;
    }

    return {
      sub: rawUser.id ?? 'unknown',
      name: rawUser.name ?? rawUser.preferred_username ?? rawUser.email ?? 'Unknown User',
      email: rawUser.email ?? '',
      roles: rawUser.roles ?? [],
      preferred_username: rawUser.preferred_username,
    };
  }, [rawUser]);

  const token: TokenInfo | null = useMemo(
    () =>
      session?.accessToken && session.accessToken.length > 0
        ? {
            access_token: session.accessToken,
            ...(session.refreshToken ? { refresh_token: session.refreshToken } : {}),
          }
        : null,
    [session]
  );

  const logOut = useCallback(async () => {
    try {
      const result = await signOut({ redirect: false, callbackUrl: PAGE_PATH.LOGIN });
      await clearSession();
      if (result?.url) {
        router.push(result.url);
      }
      return true;
    } catch (error) {
      logger.error('Logout failed:', error);
      return false;
    }
  }, [router]);

  useEffect(() => {
    // Auto sign-out if session error
    if (!!session?.error) {
      logger.error('Session expired or refresh token invalid, signing user out');
      void logOut();
    }
  }, [session?.error, logOut]);

  const contextValue = useMemo(
    () => ({
      user,
      token,
      isLoading: status === 'loading',
      isAuthenticated: status === 'authenticated' && !!user,
      signOut: logOut,
      signIn,
    }),
    [user, token, status, logOut]
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
}

export function AuthProvider({ children }: AuthProviderProps) {
  return (
    <SessionProvider>
      <AuthStateProvider>{children}</AuthStateProvider>
    </SessionProvider>
  );
}
