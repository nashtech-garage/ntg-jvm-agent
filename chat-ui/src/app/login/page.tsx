'use client';

import { PUBLIC_CONFIG } from '@/constants/site-config';
import { LockKeyhole, LogIn } from 'lucide-react';

export default function LoginPage() {
  const startOAuth = () => {
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: `${PUBLIC_CONFIG.CLIENT_ID}`,
      redirect_uri: `${location.origin}/auth/callback`,
      scope: `${PUBLIC_CONFIG.SCOPE}`,
    });

    window.location.href = `${PUBLIC_CONFIG.AUTH_SERVER}/oauth2/authorize?${params.toString()}`;
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <div className="mx-auto h-12 w-12 flex items-center justify-center rounded-full bg-blue-100">
            <LockKeyhole className="h-6 w-6 text-blue-600" />
          </div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">NT Agent Chat</h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            Sign in to access the NT Agent Chat
          </p>
        </div>

        <div className="mt-8 space-y-6">
          <button
            type="button"
            onClick={startOAuth}
            className="group relative flex w-full justify-center gap-2 rounded-md bg-blue-600 px-4 py-3 text-sm font-medium text-white shadow-sm transition hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <LogIn className="h-5 w-5" />
            Sign in
          </button>
        </div>
      </div>
    </div>
  );
}
