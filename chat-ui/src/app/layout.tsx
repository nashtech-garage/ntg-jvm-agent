import '@/styles/globals.css';
import { AuthProvider } from '@/contexts/AuthContext';
import { ToasterProvider } from '@/contexts/ToasterContext';

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
        <ToasterProvider />
      </body>
    </html>
  );
}
