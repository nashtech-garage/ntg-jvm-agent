'use client';

import { useEffect } from 'react';
import { AlertTriangle } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import logger from '@/utils/logger';

interface ErrorPageProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function ErrorPage({ error, reset }: ErrorPageProps) {
  useEffect(() => {
    // Log for monitoring; uses shared logger to stay consistent with the app
    logger.error('Unhandled application error:', error);
  }, [error]);

  return (
    <main className="min-h-screen flex items-center justify-center bg-primary-foreground px-4">
      <Card className="w-full max-w-lg border-0 shadow-none">
        <CardHeader className="space-y-3 text-center">
          <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-amber-100 text-amber-500">
            <AlertTriangle className="size-6" />
          </div>
          <CardTitle className="text-2xl">Something went wrong</CardTitle>
          <CardDescription className="text-base">
            An unexpected error occurred. The issue was logged. You can try again or go back.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {error?.message ? (
            <div className="rounded-lg border border-dashed bg-surface-soft p-3 text-sm text-muted-foreground">
              <p className="font-medium text-foreground">Error detail</p>
              <p className="mt-1 wrap-break-words">{error.message}</p>
            </div>
          ) : null}
          <div className="flex flex-col gap-3 sm:flex-row sm:justify-center">
            <Button onClick={reset}>Try again</Button>
            <Button variant="outline" onClick={() => window.history.back()}>
              Go back
            </Button>
          </div>
        </CardContent>
      </Card>
    </main>
  );
}
