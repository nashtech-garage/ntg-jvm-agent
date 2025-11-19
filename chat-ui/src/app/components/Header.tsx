'use client';

import React from 'react';

export default function Header({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <header className="sticky top-0 z-50 bg-[white] border-b border-b-gray-300">{children}</header>
  );
}
