'use client';

import { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { UserInfo } from '@/models/token';
import { useAuth } from '@/contexts/AuthContext';

interface SidebarProps {
  user: UserInfo;
}

export default function Sidebar({ user }: Readonly<SidebarProps>) {
  const pathname = usePathname();
  const { logOut } = useAuth();
  const [isOpen, setIsOpen] = useState(true);

  const handleLogout = async () => {
    await logOut();
  };

  const navItems = [
    {
      href: '/admin',
      label: 'Dashboard',
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="lucide lucide-layout-dashboard-icon lucide-layout-dashboard"
        >
          <rect width="7" height="9" x="3" y="3" rx="1" />
          <rect width="7" height="5" x="14" y="3" rx="1" />
          <rect width="7" height="9" x="14" y="12" rx="1" />
          <rect width="7" height="5" x="3" y="16" rx="1" />
        </svg>
      ),
    },
    {
      href: '/admin/agents',
      label: 'Agents',
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="lucide lucide-hat-glasses-icon lucide-hat-glasses"
        >
          <path d="M14 18a2 2 0 0 0-4 0" />
          <path d="m19 11-2.11-6.657a2 2 0 0 0-2.752-1.148l-1.276.61A2 2 0 0 1 12 4H8.5a2 2 0 0 0-1.925 1.456L5 11" />
          <path d="M2 11h20" />
          <circle cx="17" cy="18" r="3" />
          <circle cx="7" cy="18" r="3" />
        </svg>
      ),
    },
    {
      href: '/admin/users',
      label: 'Users',
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="lucide lucide-users-icon lucide-users"
        >
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
          <path d="M16 3.128a4 4 0 0 1 0 7.744" />
          <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
          <circle cx="9" cy="7" r="4" />
        </svg>
      ),
    },
    {
      href: '/admin/settings',
      label: 'Settings',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
          />
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
          />
        </svg>
      ),
    },
    {
      href: '/admin/analytics',
      label: 'Analytics',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
          />
        </svg>
      ),
    },
    {
      href: '/admin/logs',
      label: 'System Logs',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
          />
        </svg>
      ),
    },
  ];

  return (
    <div
      className={`bg-gray-900 text-white transition-all duration-300 flex flex-col h-screen ${
        isOpen ? 'w-64' : 'w-16'
      }`}
    >
      {/* Header */}
      <div className="p-4 border-b border-gray-700">
        <div className="flex items-center justify-between">
          <h1
            className={`font-bold text-xl transition-opacity duration-300 ${
              isOpen ? 'opacity-100' : 'opacity-0 w-0 overflow-hidden hidden'
            }`}
          >
            Admin Portal
          </h1>
          <button
            onClick={() => setIsOpen(!isOpen)}
            className="p-2 rounded-lg hover:bg-gray-700 transition-colors flex-shrink-0"
            title={isOpen ? 'Collapse sidebar' : 'Expand sidebar'}
          >
            <svg
              className={`w-4 h-4 transition-transform duration-300 ${isOpen ? 'rotate-180' : ''}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 19l-7-7 7-7"
              />
            </svg>
          </button>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 py-4 overflow-y-auto">
        {navItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={`flex items-center px-4 py-3 text-sm hover:bg-gray-700 transition-all duration-200 group ${
              pathname === item.href || (item.href !== '/admin' && pathname.startsWith(item.href))
                ? 'bg-gray-700 border-r-4 border-blue-500'
                : ''
            }`}
            title={isOpen ? undefined : item.label}
          >
            <div className="flex-shrink-0 mr-3">{item.icon}</div>
            <span
              className={`transition-all duration-300 ${
                isOpen
                  ? 'opacity-100 translate-x-0'
                  : 'opacity-0 -translate-x-2 w-0 overflow-hidden'
              }`}
            >
              {item.label}
            </span>
          </Link>
        ))}
      </nav>

      {/* User Info & Logout */}
      <div className="border-t border-gray-700 p-4">
        <div
          className={`mb-4 transition-all duration-300 ${
            isOpen ? 'opacity-100' : 'opacity-0 h-0 overflow-hidden'
          }`}
        >
          <div className="text-sm text-gray-300">Logged in as:</div>
          <div className="font-medium truncate">{user.name}</div>
          <div className="font-medium truncate">{user.sub}</div>
          <div className="text-xs text-gray-400 truncate">{user.email}</div>
          <div className="text-xs text-blue-400">Roles: {user.roles.join(', ')}</div>
        </div>
        <button
          onClick={handleLogout}
          className={`flex items-center w-full px-3 py-2 text-sm bg-red-600 hover:bg-red-700 rounded-lg transition-colors ${
            isOpen ? 'justify-start' : 'justify-center'
          }`}
          title={isOpen ? undefined : 'Logout'}
        >
          <svg
            className="w-4 h-4 flex-shrink-0"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
            />
          </svg>
          <span
            className={`ml-2 transition-all duration-300 ${
              isOpen ? 'opacity-100 translate-x-0' : 'opacity-0 -translate-x-2 w-0 overflow-hidden'
            }`}
          >
            Logout
          </span>
        </button>
      </div>
    </div>
  );
}
