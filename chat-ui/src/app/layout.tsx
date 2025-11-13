import './globals.css';
import { Toaster } from 'sonner';

export const metadata = {
  title: 'Chat UI',
  description: 'Next.js app with Chat-style UI',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="h-screen flex">
        <main className="flex-1 flex flex-col">{children}</main>
        <Toaster richColors position="top-right" duration={1500} />
      </body>
    </html>
  );
}
