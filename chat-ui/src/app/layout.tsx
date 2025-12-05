import { Toaster } from 'sonner';
import '@/styles/globals.css';
import { AuthProvider } from '@/contexts/AuthContext';

export const metadata = {
  title: 'Chat UI',
  description: 'Next.js app with Chat-style UI',
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body className="h-screen flex">
        <main className="flex-1 flex flex-col">
          <AuthProvider>{children}</AuthProvider>
        </main>
        <Toaster richColors position="top-right" duration={1500} />
      </body>
    </html>
  );
}
