'use client';

import React from 'react';

export default function Header({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <header className="px-6 pt-4">
      <div className="border-b border-gray-200 bg-gray-50">{children}</div>
    </header>
  );
}
