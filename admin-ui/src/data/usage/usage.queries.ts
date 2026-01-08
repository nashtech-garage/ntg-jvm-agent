import 'server-only';

import logger from '@/utils/logger';
import { SERVER_CONFIG } from '@/constants/site-config';
import { authFetch } from '@/data/client/auth-fetch';
import { UsageSummary, UsageTimeSeries, UsageByAgentRow, UsageByUserRow } from '@/types/usage';

/* ------------------------------------------------------------------ */
/* Queries                                                            */
/* ------------------------------------------------------------------ */

export async function getUsageSummary(params: { from: string; to: string }) {
  const res = await authFetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/usage/summary?${buildQuery(params)}`,
    { cache: 'no-store' }
  );

  return safeJson<UsageSummary>(res);
}

export async function getUsageTimeSeries(params: {
  from: string;
  to: string;
  groupBy: 'DAY' | 'HOUR';
}) {
  const res = await authFetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/usage/timeseries?${buildQuery(params)}`,
    { cache: 'no-store' }
  );

  return safeJson<UsageTimeSeries>(res);
}

export async function getUsageByAgent(params: { from: string; to: string }) {
  const res = await authFetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/usage/by-agent?${buildQuery(params)}`,
    { cache: 'no-store' }
  );

  return safeJson<{ rows: UsageByAgentRow[] }>(res);
}

export async function getUsageByUser(params: { from: string; to: string }) {
  const res = await authFetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/usage/by-user?${buildQuery(params)}`,
    { cache: 'no-store' }
  );

  return safeJson<{ rows: UsageByUserRow[] }>(res);
}

export async function getUsageFreshness() {
  try {
    const res = await authFetch(`${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/usage/freshness`, {
      cache: 'no-store',
    });

    return (await res.json()) as {
      latestAggregatedDate: string | undefined;
    };
  } catch (err) {
    logger.warn('[usage] freshness unavailable:', err);

    return {
      latestAggregatedDate: undefined,
    };
  }
}

/* ------------------------------------------------------------------ */
/* Helpers                                                            */
/* ------------------------------------------------------------------ */

function buildQuery(params: Record<string, string | undefined>) {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v) qs.set(k, v);
  });
  return qs.toString();
}

async function safeJson<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Usage API error ${res.status}: ${text}`);
  }
  return res.json() as Promise<T>;
}
