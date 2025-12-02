'use client';

import { useEffect, useState, Suspense, useMemo } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import logger from '@/utils/logger';

function CallbackPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const code = searchParams.get('code');
  const state = searchParams.get('state') || '/admin';
  const errorParam = searchParams.get('error');

  const urlError = useMemo(() => {
    if (errorParam) {
      return `Authentication failed: ${errorParam}`;
    }

    if (!code) {
      return 'No authorization code received';
    }

    return null;
  }, [code, errorParam]);

  const [requestError, setRequestError] = useState<string | null>(null);
  const error = requestError ?? urlError;

  useEffect(() => {
    if (error || !code) {
      return;
    }

    // Exchange authorization code for tokens
    const controller = new AbortController();

    fetch('/api/auth/exchange', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        code,
        redirect_uri: `${window.location.origin}/auth/callback`,
      }),
      credentials: 'include',
      signal: controller.signal,
    })
      .then((response) => {
        if (response.ok) {
          // Login successful, redirect to intended destination
          window.location.href = `${window.location.origin}/${state}`;
        } else if (response.status === 403) {
          // redirect to forbidden page if user lacks admin role
          router.replace('/forbidden');
        } else {
          setRequestError('Authentication failed. Please try again.');
        }
      })
      .catch((err) => {
        if (controller.signal.aborted) {
          return;
        }
        logger.error('Authentication error:', err);
        setRequestError('Authentication failed. Please try again.');
      });

    return () => controller.abort();
  }, [code, error, router, state]);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="max-w-md w-full space-y-8">
          <div className="text-center">
            <div className="mx-auto h-12 w-12 flex items-center justify-center bg-red-100 rounded-full">
              <svg
                className="h-8 w-8 text-red-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L5.082 16.5c-.77.833.192 2.5 1.732 2.5z"
                />
              </svg>
            </div>
            <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
              Authentication Error
            </h2>
            <p className="mt-2 text-center text-sm text-red-600">{error}</p>
            <div className="mt-6">
              <button
                onClick={() => router.push('/login')}
                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                Try Again
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        <p className="mt-4 text-gray-600">Completing authentication...</p>
      </div>
    </div>
  );
}

export default function CallbackPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Loading...</p>
          </div>
        </div>
      }
    >
      <CallbackPageContent />
    </Suspense>
  );
}
