'use client';

import Link from 'next/link';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { AgentListData } from '@/types/agent';
import { PAGE_PATH } from '@/constants/url';

interface AgentsTableProps {
  agents: AgentListData[] | undefined;
  isLoading: boolean;
}

export function AgentsTable({ agents, isLoading }: AgentsTableProps) {
  if (isLoading) {
    return (
      <div className="rounded-sm border p-6 text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-2"></div>
        <p className="text-gray-500">Loading agents...</p>
      </div>
    );
  }

  return (
    <div className="rounded-sm border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="font-semibold">Name</TableHead>
            <TableHead className="font-semibold">Model</TableHead>
            <TableHead className="font-semibold">Last modified</TableHead>
            <TableHead className="font-semibold">Last published</TableHead>
            <TableHead className="font-semibold">Owner</TableHead>
            <TableHead className="font-semibold">Status</TableHead>
          </TableRow>
        </TableHeader>

        <TableBody>
          {agents?.map((a: AgentListData) => (
            <TableRow key={a.id} className="hover:bg-muted/50 transition">
              <TableCell>
                <Link
                  href={PAGE_PATH.AGENT_DETAIL(a.id)}
                  className="hover:text-blue-600 hover:underline"
                >
                  {a.name}
                </Link>
              </TableCell>

              <TableCell>{a.model}</TableCell>
              <TableCell>
                <span className="font-semibold">{a.lastModifiedBy}</span>{' '}
                <span className="text-xs text-muted-foreground">{a.lastModifiedWhen}</span>
              </TableCell>
              <TableCell>{a.lastPublishedWhen}</TableCell>
              <TableCell>{a.owner}</TableCell>
              <TableCell>{a.status}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
