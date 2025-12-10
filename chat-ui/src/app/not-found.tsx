import { Metadata } from 'next';
import Link from 'next/link';
import React from 'react';

export const metadata: Metadata = {
  title: 'Not Found',
  description: 'The page you are looking for does not exist.',
};

const notFound = () => {
  return (
    <div className="flex flex-col gap-6 justify-center items-center h-full">
      <h3 className="text-3xl font-bold text-danger-strong">404 - Not Found</h3>
      <h4 className="text-foreground">The page you are looking for does not exist.</h4>
      <Link
        href={'/'}
        className="border border-foreground text-foreground rounded p-4 cursor-pointer transition hover:border-foreground hover:text-inverse hover:bg-foreground"
      >
        Go back to home page
      </Link>
    </div>
  );
};

export default notFound;
