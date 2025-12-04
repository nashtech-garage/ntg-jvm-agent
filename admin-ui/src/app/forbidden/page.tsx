'use client';

import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';

export default function ForbiddenPage() {
  const { logOut } = useAuth();
  const router = useRouter();

  const handleLogout = async () => {
    await logOut();
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <div className="mx-auto h-16 w-16 flex items-center justify-center bg-red-100 rounded-full">
            <svg
              className="h-10 w-10 text-red-600"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728L5.636 5.636m12.728 12.728L18.364 5.636M5.636 18.364l12.728-12.728"
              />
            </svg>
          </div>
          <h1 className="mt-6 text-center text-4xl font-extrabold text-gray-900">403</h1>
          <h2 className="mt-2 text-center text-2xl font-bold text-gray-900">Access Forbidden</h2>
          <p className="mt-4 text-center text-sm text-gray-600">
            You don&#39;t have permission to access this resource. Admin role required.
          </p>
          <div className="mt-8 space-y-4">
            <button
              onClick={handleLogout}
              className="w-full flex justify-center py-3 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Sign In with Different Account
            </button>
            <button
              onClick={() => router.back()}
              className="w-full flex justify-center py-3 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Go Back
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
