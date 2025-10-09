'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { UserInfo } from '@/app/models/token';
import Sidebar from './Sidebar';

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  // mock user for layout purpose
  const user: UserInfo = {
    sub: '1234567890',
    name: 'Admin User',
    email: 'admin@example.com',
    roles: ['admin'],
  };
  return (
    <div className="min-h-screen bg-gray-50">
      <div className="flex h-screen overflow-hidden">
        <Sidebar user={user} />
        <main className="flex-1 overflow-y-auto">
          <div className="p-6 lg:p-8 max-w-7xl mx-auto">{children}</div>
        </main>
      </div>
    </div>
  );
}
