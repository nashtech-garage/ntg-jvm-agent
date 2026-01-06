import UsageDateNavigator from '@/components/usage/usage-date-navigator';
import UsageFilterBar from '@/components/usage/usage-filter-bar';

type Props = {
  from: string;
  to: string;
  latestAggregatedDate?: string;
};

export default function UsageControls({ from, to, latestAggregatedDate }: Readonly<Props>) {
  return (
    <div className="flex flex-col gap-3">
      <UsageDateNavigator from={from} to={to} latestAggregatedDate={latestAggregatedDate} />

      <UsageFilterBar />
    </div>
  );
}
