import { Metadata } from 'next';
import Link from 'next/link';
import React from 'react';

export const metadata: Metadata = {
  title: 'Not Found',
  description: 'The page you are looking for does not exist.',
};

const notFound = () => {
  return (
    <div className="flex flex-col gap-6 justify-center items-center h-full bg-gradient-to-b from-primary-soft to-success-soft">
      <h1 className="text-5xl font-bold text-danger-strong">404 - Not Found</h1>
      <h2 className="text-3xl text-foreground">The page you are looking for does not exist.</h2>
      <Link
        href={'/'}
        className="text-xl border border-foreground text-foreground rounded p-4 cursor-pointer transition hover:border-foreground hover:text-inverse hover:bg-foreground"
      >
        Go back to home page
      </Link>
    </div>
  );
};

export default notFound;
