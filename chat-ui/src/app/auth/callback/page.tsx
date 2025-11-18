'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function CallbackPage() {
  const router = useRouter();

  useEffect(() => {
    const q = new URL(location.href).searchParams;
    const code = q.get('code');
    if (!code) {
      return;
    }

    // POST to Authorization server to exchange token
    fetch('/api/auth/exchange', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, redirect_uri: `${location.origin}/auth/callback` }),
      credentials: 'include',
    })
      .then((r) => {
        if (r.ok) {
          // login success — redirect to home page
          router.replace('/');
        } else {
          // handle error
        }
      })
      .catch(console.error);
  }, [router]);

  return <div className='flex justify-between items-center'>Finishing sign in...</div>;
}
