import Link from 'next/link';
import { ChevronLeft, ChevronRight } from 'lucide-react';

import UsageDateRangePicker from '@/components/usage/usage-date-range-picker';

type Props = {
  from: string;
  to: string;
  latestAggregatedDate?: string;
};

export default function UsageDateNavigator({ from, to, latestAggregatedDate }: Readonly<Props>) {
  const hasFreshness = Boolean(latestAggregatedDate);

  const prev = getPrevMonthRange(from);
  const next = getNextMonthRange(from);

  const prevDisabled = !hasFreshness;
  const nextDisabled =
    !hasFreshness || (latestAggregatedDate != null && next.from > latestAggregatedDate);

  return (
    <div className="flex flex-col gap-2 md:flex-row md:items-center">
      {/* Prev */}
      {prevDisabled ? (
        <DisabledNav label="Previous month">
          <ChevronLeft className="h-4 w-4" />
          <span className="sr-only md:not-sr-only">Prev</span>
        </DisabledNav>
      ) : (
        <NavLink href={`/admin/usage?from=${prev.from}&to=${prev.to}`} label="Previous month">
          <ChevronLeft className="h-4 w-4" />
          <span className="sr-only md:not-sr-only">Prev</span>
        </NavLink>
      )}

      {/* Center */}
      <div className="mx-4">
        <UsageDateRangePicker
          from={from}
          to={to}
          disabled={!hasFreshness}
          latestAggregatedDate={latestAggregatedDate}
        />
      </div>

      {/* Next */}
      {nextDisabled ? (
        <DisabledNav label="Next month">
          <span className="sr-only md:not-sr-only">Next</span>
          <ChevronRight className="h-4 w-4" />
        </DisabledNav>
      ) : (
        <NavLink href={`/admin/usage?from=${next.from}&to=${next.to}`} label="Next month">
          <span className="sr-only md:not-sr-only">Next</span>
          <ChevronRight className="h-4 w-4" />
        </NavLink>
      )}
    </div>
  );
}

/* ---------- Helpers ---------- */

function NavLink({
  href,
  label,
  children,
}: Readonly<{
  href: string;
  label: string;
  children: React.ReactNode;
}>) {
  return (
    <Link
      href={href}
      aria-label={label}
      className="inline-flex h-10 items-center gap-1 rounded-md border px-3 py-1 text-sm hover:bg-muted"
    >
      {children}
    </Link>
  );
}

function DisabledNav({
  label,
  children,
}: Readonly<{
  label: string;
  children: React.ReactNode;
}>) {
  return (
    <span
      aria-disabled="true"
      aria-label={label}
      className="inline-flex h-10 items-center gap-1 rounded-md border px-3 py-1 text-sm text-muted-foreground cursor-not-allowed"
    >
      {children}
    </span>
  );
}

/* ---------- Date utils ---------- */

function getPrevMonthRange(from: string): { from: string; to: string } {
  const [year, month] = from.split('-').map(Number);

  const prev = new Date(year, month - 2, 1); // month is 0-based
  const y = prev.getFullYear();
  const m = prev.getMonth() + 1;

  const lastDay = new Date(y, m, 0).getDate();

  return {
    from: `${y}-${String(m).padStart(2, '0')}-01`,
    to: `${y}-${String(m).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`,
  };
}

function getNextMonthRange(from: string): { from: string; to: string } {
  const [year, month] = from.split('-').map(Number);

  const next = new Date(year, month, 1);
  const y = next.getFullYear();
  const m = next.getMonth() + 1;

  const lastDay = new Date(y, m, 0).getDate();

  return {
    from: `${y}-${String(m).padStart(2, '0')}-01`,
    to: `${y}-${String(m).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`,
  };
}
