import 'server-only';

import {
  getUsageSummary,
  getUsageTimeSeries,
  getUsageByAgent,
  getUsageByUser,
  getUsageFreshness,
} from '@/data/usage/usage.queries';

import UsageSummaryCards from '@/components/usage/usage-summary-cards';
import UsageTimeSeriesChart from '@/components/usage/usage-time-series-chart';
import UsageByAgentTable from '@/components/usage/usage-by-agent-table';
import UsageByUserTable from '@/components/usage/usage-by-user-table';
import UsageControls from '@/components/usage/usage-controls';
import UsageEmptyState from '@/components/usage/usage-empty-state';

type UsagePageProps = {
  searchParams: Promise<{
    from?: string;
    to?: string;
  }>;
};

export default async function UsagePage({ searchParams }: Readonly<UsagePageProps>) {
  const params = await searchParams;

  const freshness = await getUsageFreshness();

  const freshnessRange = freshness.latestAggregatedDate
    ? getMonthRangeFromDate(freshness.latestAggregatedDate)
    : getMonthRangeFromDate(new Date().toISOString().slice(0, 10));

  const from = params.from ?? freshnessRange.from;
  const to = params.to ?? freshnessRange.to;

  const [summary, timeSeries, byAgent, byUser] = await Promise.all([
    getUsageSummary({ from, to }),
    getUsageTimeSeries({ from, to, groupBy: 'DAY' }),
    getUsageByAgent({ from, to }),
    getUsageByUser({ from, to }),
  ]);

  const hasData = (summary?.totalTokens ?? 0) > 0;

  return (
    <div className="space-y-6 p-6">
      <header className="flex flex-col gap-2">
        <h1 className="text-2xl font-semibold">Token Usage</h1>

        <UsageControls from={from} to={to} latestAggregatedDate={freshness.latestAggregatedDate} />

        <p className="text-xs text-muted-foreground">
          Usage data is aggregated daily.
          {freshness.latestAggregatedDate ? (
            <>
              {' '}
              Latest complete date:{' '}
              <span className="font-medium">{freshness.latestAggregatedDate}</span>.
            </>
          ) : (
            <> No usage data has been aggregated yet.</>
          )}
        </p>
      </header>

      {hasData ? (
        <>
          <UsageSummaryCards summary={summary} />
          <UsageTimeSeriesChart data={timeSeries.points ?? []} />
          <UsageByAgentTable rows={byAgent.rows ?? []} />
          <UsageByUserTable rows={byUser.rows ?? []} />
        </>
      ) : (
        <UsageEmptyState />
      )}
    </div>
  );
}

function getMonthRangeFromDate(date: string): { from: string; to: string } {
  const [year, month] = date.split('-').map(Number);

  return {
    from: `${year}-${String(month).padStart(2, '0')}-01`,
    to: date,
  };
}
