'use client';

import React, {
  createContext,
  useContext,
  useMemo,
  useCallback,
  useEffect,
  useRef,
  ReactNode,
} from 'react';
import { SessionProvider, signIn, signOut, useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { TokenInfo, UserInfo } from '@/models/token';
import { PAGE_PATH } from '@/constants/url';
import { clearSession } from '@/actions/session';
import logger from '@/utils/logger';

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
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}

interface AuthProviderProps {
  readonly children: ReactNode;
}

function AuthStateProvider({ children }: AuthProviderProps) {
  const router = useRouter();
  const { data: session, status } = useSession();
  const isLoggingOutRef = useRef(false);

  /* ---------------- User ---------------- */
  const rawUser = session?.user;

  const user: UserInfo | null = useMemo(() => {
    if (!rawUser) return null;

    return {
      sub: rawUser.id ?? 'unknown',
      name: rawUser.name ?? rawUser.preferred_username ?? rawUser.email ?? 'Unknown User',
      email: rawUser.email ?? '',
      roles: rawUser.roles ?? [],
      preferred_username: rawUser.preferred_username,
    };
  }, [rawUser]);

  /* ---------------- Token ---------------- */
  const token: TokenInfo | null = useMemo(() => {
    if (!session?.accessToken) return null;

    return {
      access_token: session.accessToken,
      ...(session.refreshToken ? { refresh_token: session.refreshToken } : {}),
    };
  }, [session]);

  /* ---------------- Logout (ONE WAY ONLY) ---------------- */
  const logOut = useCallback(async () => {
    if (isLoggingOutRef.current) return false;
    isLoggingOutRef.current = true;

    try {
      const result = await signOut({
        redirect: false,
        callbackUrl: PAGE_PATH.LOGIN,
      });

      await clearSession();

      if (result?.url) {
        router.push(result.url);
      }

      return true;
    } catch (err) {
      logger.error('Logout failed', err);
      return false;
    } finally {
      isLoggingOutRef.current = false;
    }
  }, [router]);

  /* ---------------- Refresh-token failure ---------------- */
  useEffect(() => {
    if (session?.error !== 'RefreshAccessTokenError') return;
    if (isLoggingOutRef.current) return;

    logger.error('Refresh token invalid or expired. Forcing logout.');

    void logOut();
  }, [session?.error, logOut]);

  /* ---------------- Context ---------------- */
  const value = useMemo<AuthContextType>(
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

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function AuthProvider({ children }: AuthProviderProps) {
  return (
    <SessionProvider>
      <AuthStateProvider>{children}</AuthStateProvider>
    </SessionProvider>
  );
}
