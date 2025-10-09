'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from './contexts/AuthContext';

export default function HomePage() {
  const router = useRouter();
  const { isLoading, isAuthenticated, hasAdminRole } = useAuth();

  useEffect(() => {
    if (!isLoading) {
      if (isAuthenticated && hasAdminRole) {
        // User is authenticated and has admin role, redirect to admin dashboard
        router.push('/admin');
      } else {
        // User is not authenticated or doesn't have admin role, redirect to login
        router.push('/login');
      }
    }
  }, [router, isLoading, isAuthenticated, hasAdminRole]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        <p className="mt-4 text-gray-600">
          {isLoading ? 'Loading...' : 'Redirecting to Admin Portal...'}
        </p>
      </div>
    </div>
  );
}
