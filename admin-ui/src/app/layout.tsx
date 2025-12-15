import type { Metadata } from 'next';
import { AuthProvider } from '@/contexts/AuthContext';
import '@/styles/globals.css';
import { ToasterProvider } from '@/contexts/ToasterContext';

export const metadata: Metadata = {
  title: 'NTG Admin Portal',
  description: 'Administration portal for NTG JVM Agent',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="antialiased">
        <ToasterProvider />
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
