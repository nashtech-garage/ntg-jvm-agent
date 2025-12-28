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

type UsagePageProps = {
  searchParams: Promise<{
    from?: string;
    to?: string;
  }>;
};

export default async function UsagePage({ searchParams }: Readonly<UsagePageProps>) {
  const params = await searchParams;

  const freshness = await getUsageFreshness();
  const freshnessRange = getMonthRangeFromDate(freshness.latestAggregatedDate);

  const from = params.from ?? freshnessRange.from;
  const to = params.to ?? freshnessRange.to;

  const [summary, timeSeries, byAgent, byUser] = await Promise.all([
    getUsageSummary({ from, to }),
    getUsageTimeSeries({ from, to, groupBy: 'DAY' }),
    getUsageByAgent({ from, to }),
    getUsageByUser({ from, to }),
  ]);

  return (
    <div className="space-y-6 p-6">
      <header className="flex flex-col gap-2">
        <h1 className="text-2xl font-semibold">Token Usage</h1>

        <UsageControls from={from} to={to} latestAggregatedDate={freshness.latestAggregatedDate} />

        <p className="text-xs text-muted-foreground">
          Usage data is aggregated daily. Latest complete date:{' '}
          <span className="font-medium">{freshness.latestAggregatedDate}</span>.
        </p>
      </header>

      <UsageSummaryCards summary={summary} />

      <UsageTimeSeriesChart data={timeSeries.points ?? []} />

      <UsageByAgentTable rows={byAgent.rows} />

      <UsageByUserTable rows={byUser.rows} />
    </div>
  );
}

/**
 * Returns YYYY-MM-DD strings for the current calendar month.
 * Safe for backend LocalDate usage.
 */
function getCurrentMonthRange(): { from: string; to: string } {
  const now = new Date();

  const year = now.getFullYear();
  const month = now.getMonth(); // 0-based

  const from = new Date(year, month, 1);
  const to = new Date(year, month + 1, 0);

  const format = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(
      2,
      '0'
    )}`;

  return {
    from: format(from),
    to: format(to),
  };
}

function getMonthRangeFromDate(date: string): { from: string; to: string } {
  const [year, month] = date.split('-').map(Number);

  const from = `${year}-${String(month).padStart(2, '0')}-01`;
  const to = date; // latest aggregated date

  return { from, to };
}
