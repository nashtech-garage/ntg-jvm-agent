'use client';

import { AppRouterInstance } from 'next/dist/shared/lib/app-router-context.shared-runtime';
import logger from './logger';

export type Options = RequestInit & {
  router?: AppRouterInstance;
};

export async function customizeFetch(url: string | URL | Request, options: Options = {}) {
  try {
    const response = await fetch(url, {
      ...options,
      headers: {
        'From-Handler': 'true',
        ...options.headers,
      },
    });

    await checkStatus(response, options);

    return response;
  } catch (error) {
    logger.error('Fetch failed:', error);
    throw error; // Re-throw so caller can handle it
  }
}

const checkStatus = async (response: Response, options: Options) => {
  const contentType = response.headers.get('Content-Type') || '';

  if (contentType.includes('text/event-stream')) {
    return;
  }

  if (response.ok) {
    const cloneResponse = response.clone();
    const json = await cloneResponse.json();
    const redirectTo = json.redirectTo;
    // manually redirect
    if (redirectTo != undefined && (redirectTo as string).length != 0) {
      const router = options.router;
      if (router) {
        router.push(redirectTo);
      } else {
        window.location.href = redirectTo;
      }
    }
    return;
  }

  // 400 and 500 statuses
  if (!response.ok) {
    throw new Error(`HTTP error! Status: ${response.status}`);
  }
};
