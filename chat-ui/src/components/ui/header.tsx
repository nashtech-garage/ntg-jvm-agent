'use client';

import React from 'react';

export default function Header({ children }: Readonly<{ children: React.ReactNode }>) {
  return <header className="pt-4">{children}</header>;
}
