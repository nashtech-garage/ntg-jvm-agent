'use client';

import { SITE_CONFIG } from '@/constants/site-config';

export default function LoginPage() {
  const startOAuth = () => {
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: `${SITE_CONFIG.CLIENT_ID_PUBLIC}`,
      redirect_uri: `${location.origin}/auth/callback`,
      scope: `${SITE_CONFIG.SCOPE}`,
    });

    window.location.href = `${SITE_CONFIG.AUTH_SERVER}/oauth2/authorize?${params.toString()}`;
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center">
      <h3 className="mb-5">Welcome to ntg-jvm-agent project</h3>
      <div className="border border-red-500 p-2 bg-green-500 rounded text-white text-center">
        <button onClick={startOAuth}>Login with Auth Server</button>
      </div>
    </div>
  );
}
