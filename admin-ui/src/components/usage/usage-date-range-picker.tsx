'use client';

import { useRouter, useSearchParams } from 'next/navigation';

export default function UsageDateRangePicker({
  from,
  to,
}: Readonly<{
  from: string;
  to: string;
}>) {
  const router = useRouter();
  const params = useSearchParams();

  function update(key: string, value: string) {
    const next = new URLSearchParams(params.toString());
    next.set(key, value);
    router.push(`?${next.toString()}`);
  }

  return (
    <div className="flex gap-4">
      <input
        type="date"
        value={from}
        onChange={(e) => update('from', e.target.value)}
        className="rounded border px-3 py-2"
      />
      <input
        type="date"
        value={to}
        onChange={(e) => update('to', e.target.value)}
        className="rounded border px-3 py-2"
      />
    </div>
  );
}
