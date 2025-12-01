'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';

export default function AgentTabs({
  agentId,
  agentName,
}: Readonly<{
  agentId: string;
  agentName: string;
}>) {
  const pathname = usePathname();

  const tabs = [
    { href: `/admin/agents/${agentId}`, label: 'Overview' },
    { href: `/admin/agents/${agentId}/knowledge`, label: 'Knowledge' },
    { href: `/admin/agents/${agentId}/tools`, label: 'Tools' },
  ];

  return (
    <div className="lg:col-span-3 border-b">
      {/* Main row */}
      <div className="flex items-center justify-between w-full px-2 py-0">
        {/* LEFT: Avatar + Name */}
        <div className="flex items-center gap-3">
          <Avatar className="h-8 w-8">
            <AvatarImage src="https://github.com/shadcn.png" alt="@shadcn" />
            <AvatarFallback>{agentName?.charAt(0)?.toUpperCase() || 'A'}</AvatarFallback>
          </Avatar>
          <span className="font-semibold text-lg leading-none mr-5">{agentName}</span>
          {tabs.map((tab) => {
            const isActive =
              (tab.href === `/admin/agents/${agentId}` && pathname === tab.href) ||
              (tab.href !== `/admin/agents/${agentId}` && pathname.startsWith(tab.href));

            return (
              <Link
                key={tab.href}
                href={tab.href}
                className={cn(
                  'py-6 px-3 text-sm font-medium',
                  'flex items-center leading-none', // align link text vertically
                  'hover:text-primary transition',
                  isActive ? 'text-primary border-b-2 border-primary mt-1' : 'text-muted-foreground'
                )}
              >
                {tab.label}
              </Link>
            );
          })}
        </div>

        {/* RIGHT: Tabs */}
        <div className="flex gap-4"></div>
      </div>
    </div>
  );
}
