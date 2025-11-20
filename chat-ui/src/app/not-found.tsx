import { Metadata } from 'next';
import Link from 'next/link';
import React from 'react';

export const metadata: Metadata = {
  title: 'Not Found',
  description: 'The page you are looking for does not exist.',
};

const notFound = () => {
  return (
    <div className="flex flex-col gap-6 justify-center items-center h-full bg-gradient-to-b not-dark:from-green-50 dark:from-green-100  to-green-200">
      <h1 className="text-5xl font-bold text-red-600">404 - Not Found</h1>
      <h2 className="text-3xl text-black">The page you are looking for does not exist.</h2>
      <Link
        href={'/'}
        className="text-xl border border-black rounded p-4 cursor-pointer hover:border-white hover:text-white hover:bg-black"
      >
        Go back to home page
      </Link>
    </div>
  );
};

export default notFound;
