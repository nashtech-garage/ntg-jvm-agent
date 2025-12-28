'use client';

import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend } from 'recharts';

import type { UsageTimeSeriesPointDto } from '@/types/usage';

type Props = {
  data: UsageTimeSeriesPointDto[];
};

export default function UsageTimeSeriesChart({ data }: Readonly<Props>) {
  return (
    <div className="rounded-xl border bg-white p-4 shadow-sm">
      <h2 className="mb-1 text-lg font-semibold">Tokens Over Time</h2>

      <ResponsiveContainer width="100%" height={320}>
        <LineChart data={data}>
          <XAxis dataKey="date" />
          <YAxis />

          <Tooltip labelFormatter={(label: string) => `Date: ${label}`} />

          <Legend verticalAlign="top" height={32} />

          {/* Total Tokens */}
          <Line
            type="monotone"
            dataKey="totalTokens"
            name="Total Tokens"
            stroke="#2563eb" // blue
            strokeWidth={2.5}
            dot={false}
          />

          {/* Prompt Tokens */}
          <Line
            type="monotone"
            dataKey="promptTokens"
            name="Prompt Tokens"
            stroke="#16a34a" // green
            strokeWidth={2}
            dot={false}
          />

          {/* Completion Tokens */}
          <Line
            type="monotone"
            dataKey="completionTokens"
            name="Completion Tokens"
            stroke="#f97316" // orange
            strokeWidth={2}
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
