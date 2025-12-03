'use client';

import React, { createContext, useContext, useMemo, ReactNode, useCallback } from 'react';
import { SessionProvider, signOut, useSession } from 'next-auth/react';
import { TokenInfo, UserInfo } from '@/models/token';
import { hasAdminRole as hasAdmin } from '@/utils/user';
import { signOut as signOutApp } from '@/services/auth';
import { API_PATH, PAGE_PATH } from '@/constants/url';
import { useRouter } from 'next/navigation';
import logger from '@/utils/logger';

interface AuthContextType {
  user: UserInfo | null;
  token: TokenInfo | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  hasAdminRole: boolean;
  logOut: () => Promise<boolean>;
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

  const roles = useMemo(() => user?.roles ?? [], [user]);
  const hasAdminRole = hasAdmin(roles);

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
      await signOutApp({ serverUri: API_PATH.SIGN_OUT });
      if (result?.url) {
        router.push(result.url);
      }
      return true;
    } catch (error) {
      logger.error('Logout failed:', error);
      return false;
    }
  }, [router]);

  const contextValue = useMemo(
    () => ({
      user,
      token,
      isLoading: status === 'loading',
      isAuthenticated: status === 'authenticated' && !!user,
      hasAdminRole,
      logOut,
    }),
    [user, token, status, hasAdminRole, logOut]
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
