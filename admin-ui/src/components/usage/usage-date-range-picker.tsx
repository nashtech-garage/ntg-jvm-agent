'use client';

import { useRouter, useSearchParams } from 'next/navigation';

type Props = {
  from: string;
  to: string;
  disabled?: boolean;
  latestAggregatedDate?: string;
};

export default function UsageDateRangePicker({
  from,
  to,
  disabled = false,
  latestAggregatedDate,
}: Readonly<Props>) {
  const router = useRouter();
  const params = useSearchParams();

  function update(nextFrom: string, nextTo: string) {
    // invalid range
    if (nextFrom > nextTo) return;

    // freshness limit
    if (latestAggregatedDate && nextTo > latestAggregatedDate) return;

    const next = new URLSearchParams(params.toString());
    next.set('from', nextFrom);
    next.set('to', nextTo);

    router.push(`?${next.toString()}`);
  }

  return (
    <div className="flex gap-4">
      <input
        type="date"
        value={from}
        max={to}
        disabled={disabled}
        onChange={(e) => update(e.target.value, to)}
        className="rounded border px-3 py-2 disabled:cursor-not-allowed disabled:bg-muted"
      />

      <input
        type="date"
        value={to}
        min={from}
        max={latestAggregatedDate}
        disabled={disabled}
        onChange={(e) => update(from, e.target.value)}
        className="rounded border px-3 py-2 disabled:cursor-not-allowed disabled:bg-muted"
      />
    </div>
  );
}
