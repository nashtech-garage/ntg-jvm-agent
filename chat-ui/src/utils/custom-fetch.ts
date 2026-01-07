'use client';

import { AppRouterInstance } from 'next/dist/shared/lib/app-router-context.shared-runtime';
import logger from './logger';

/**
 * Custom HTTP error that preserves status and backend payload.
 */
export class HttpError extends Error {
  readonly status: number;
  readonly payload?: unknown;

  constructor(status: number, message: string, payload?: unknown) {
    super(message);
    // Explicitly set the error name for clearer stack traces and logging.
    this.name = 'HttpError';
    this.status = status;
    this.payload = payload;
  }
}

export type Options = RequestInit & {
  router?: AppRouterInstance;
};

export async function customizeFetch(
  url: string | URL | Request,
  options: Options = {}
): Promise<Response> {
  try {
    const response = await fetch(url, {
      ...options,
      headers: {
        'From-Handler': 'true',
        ...options.headers,
      },
    });

    await handleResponse(response, options);
    return response;
  } catch (error) {
    logger.error('Fetch failed:', error);
    throw error;
  }
}

/* ─────────────────────────────────────────────── */
/* Helpers                                         */
/* ─────────────────────────────────────────────── */

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function isEventStream(response: Response): boolean {
  return response.headers.get('Content-Type')?.includes('text/event-stream') ?? false;
}

async function parseJsonBody(response: Response): Promise<unknown> {
  const contentType = response.headers.get('Content-Type') ?? '';
  if (!contentType.includes('application/json')) return null;

  try {
    return await response.clone().json();
  } catch {
    return null;
  }
}

function handleRedirect(body: unknown, options: Options): void {
  if (!isObject(body) || typeof body.redirectTo !== 'string') return;

  const { router } = options;
  if (router) {
    router.push(body.redirectTo);
  } else {
    globalThis.location.href = body.redirectTo;
  }
}

function extractErrorMessage(response: Response, body: unknown): string {
  if (isObject(body)) {
    if (typeof body.message === 'string') {
      return body.message;
    }
    if (typeof body.error === 'string') {
      return body.error;
    }
  }

  return `HTTP ${response.status} ${response.statusText}`;
}

function buildHttpError(response: Response, body: unknown): HttpError {
  const message = extractErrorMessage(response, body);
  return new HttpError(response.status, message, body);
}

/* ─────────────────────────────────────────────── */
/* Core logic                                      */
/* ─────────────────────────────────────────────── */

async function handleResponse(response: Response, options: Options): Promise<void> {
  /**
   * Allow Server-Sent Events to stream uninterrupted
   */
  if (isEventStream(response)) {
    return;
  }

  const body = await parseJsonBody(response);

  /**
   * Success path
   */
  if (response.ok) {
    handleRedirect(body, options);
    return;
  }

  /**
   * Error path
   */
  throw buildHttpError(response, body);
}
