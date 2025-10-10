'use client';

import React, { createContext, useContext, useEffect, useState, ReactNode, useMemo } from 'react';
import { UserInfo, TokenInfo } from '../models/token';

interface AuthContextType {
  user: UserInfo | null;
  token: TokenInfo | null;
  login: (code: string, redirectUri: string) => Promise<boolean>;
  logout: () => void;
  isLoading: boolean;
  isAuthenticated: boolean;
  hasAdminRole: boolean;
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

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [token, setToken] = useState<TokenInfo | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const isAuthenticated = !!user && !!token;
  const hasAdminRole = user?.roles?.includes('ADMIN') || user?.roles?.includes('admin') || false;

  useEffect(() => {
    // Check for existing token on mount
    checkExistingAuth();
  }, []);

  const checkExistingAuth = async () => {
    try {
      setIsLoading(true);

      // Try to get user info from stored token
      const response = await fetch('/api/auth/me', {
        credentials: 'include',
      });

      if (response.ok) {
        const userData = await response.json();
        setUser(userData.user);
        setToken(userData.token);
      }
    } catch (error) {
      console.error('Auth check failed:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const login = async (code: string, redirectUri: string): Promise<boolean> => {
    try {
      setIsLoading(true);

      const response = await fetch('/api/auth/exchange', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ code, redirect_uri: redirectUri }),
      });

      if (response.ok) {
        const data = await response.json();
        setUser(data.user);
        setToken(data.token);
        return true;
      }

      return false;
    } catch (error) {
      console.error('Login failed:', error);
      return false;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    fetch('/api/auth/logout', {
      method: 'POST',
      credentials: 'include',
    })
      .catch((error) => {
        console.error('Logout failed:', error);
      })
      .finally(() => {
        setUser(null);
        setToken(null);
      });
  };

  const contextValue = useMemo(
    () => ({
      user,
      token,
      login,
      logout,
      isLoading,
      isAuthenticated,
      hasAdminRole,
    }),
    [user, token, login, logout, isLoading, isAuthenticated, hasAdminRole]
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
}
