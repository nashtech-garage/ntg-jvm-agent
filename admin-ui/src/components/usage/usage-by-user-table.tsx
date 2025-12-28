'use client';

import type { UsageByUserRowDto } from '@/types/usage';

type Props = {
  rows: UsageByUserRowDto[];
};

export default function UsageByUserTable({ rows }: Readonly<Props>) {
  return (
    <div className="rounded-xl border bg-white p-4 shadow-sm">
      <h2 className="mb-4 text-lg font-semibold">Usage by User</h2>

      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="border-b">
              <th className="px-2 py-2 text-left font-medium">User</th>
              <th className="px-2 py-2 text-right font-medium">Prompt</th>
              <th className="px-2 py-2 text-right font-medium">Completion</th>
              <th className="px-2 py-2 text-right font-medium">Total</th>
            </tr>
          </thead>

          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-2 py-6 text-center text-muted-foreground">
                  No usage data found.
                </td>
              </tr>
            ) : (
              rows.map((row, idx) => (
                <tr key={idx} className="border-b last:border-b-0">
                  <td className="px-2 py-2 font-mono">{row.userName ?? 'SYSTEM'}</td>
                  <td className="px-2 py-2 text-right">{row.promptTokens}</td>
                  <td className="px-2 py-2 text-right">{row.completionTokens}</td>
                  <td className="px-2 py-2 text-right">{row.totalTokens}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
