import 'server-only';
import { SERVER_CONFIG } from '@/constants/site-config';
import { authFetch } from '@/data/client/auth-fetch';

export async function getUsageSummary(params: { from: string; to: string }) {
  const res = await authFetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/usage/summary` +
      `?from=${params.from}&to=${params.to}`
  );

  return res.json();
}

export async function getUsageTimeSeries(params: {
  from: string;
  to: string;
  groupBy: 'DAY' | 'HOUR';
}) {
  const res = await authFetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/usage/timeseries` +
      `?from=${params.from}&to=${params.to}&groupBy=${params.groupBy}`
  );

  return res.json();
}

export async function getUsageByAgent(params: { from: string; to: string }) {
  const res = await authFetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/usage/by-agent` +
      `?from=${params.from}&to=${params.to}`
  );

  return res.json();
}

export async function getUsageByUser(params: { from: string; to: string }) {
  const res = await authFetch(
    `${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/usage/by-user` +
      `?from=${params.from}&to=${params.to}`
  );

  return res.json();
}

export async function getUsageFreshness() {
  const res = await authFetch(`${SERVER_CONFIG.ORCHESTRATOR_SERVER}/api/usage/freshness`);

  return res.json() as Promise<{
    latestAggregatedDate: string;
  }>;
}
