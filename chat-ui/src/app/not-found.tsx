import { Metadata } from 'next';
import Link from 'next/link';
import { SearchX } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export const metadata: Metadata = {
  title: 'Page not found',
  description: 'The page you are looking for does not exist.',
};

export default function NotFoundPage() {
  return (
    <main className="min-h-screen flex items-center justify-center bg-surface-soft px-4">
      <Card className="w-full max-w-lg border-0 shadow-none bg-background">
        <CardHeader className="space-y-3 text-center">
          <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-slate-100 text-slate-600 dark:bg-slate-800/60 dark:text-slate-100">
            <SearchX className="size-6" />
          </div>
          <CardTitle className="text-2xl">404 - Page Not Found</CardTitle>
          <CardDescription className="text-base">
            The page you are looking for doesn&apos;t exist or may have been moved. Check the URL or
            head back to the dashboard.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-center justify-center gap-2 text-sm text-muted-foreground">
            <span className="rounded-full bg-surface-soft px-3 py-1 font-mono text-xs uppercase tracking-[0.2em]">
              404
            </span>
            <span className="wrap-break-words">Requested page could not be located.</span>
          </div>
          <div className="flex justify-center">
            <Button asChild>
              <Link href="/">Back to home</Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    </main>
  );
}
