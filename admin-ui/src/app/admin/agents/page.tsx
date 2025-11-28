'use client';

import useSWR from 'swr';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { fetcher } from '@/lib/fetcher';
import { Plus, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { AgentListData } from '@/app/types/agent';

export default function AgentTable() {
  const router = useRouter();
  const {
    data: agents,
    error,
    isLoading,
  } = useSWR('/api/agents', fetcher, {
    dedupingInterval: 3000, // avoid duplicate calls
    revalidateOnFocus: false,
  });

  if (isLoading) return <div className="p-6">Loading...</div>;
  if (error) return <div className="p-6 text-red-500">Failed to load agents</div>;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Agents</h1>
        <p className="text-muted-foreground mt-1">
          Manage AI agents, update configurations, and control model behavior.
        </p>
      </div>

      {/* Toolbar: Left buttons + Right search */}
      <div className="flex items-center justify-between">
        {/* Left Buttons */}
        <div className="flex items-center gap-2">
          <Button onClick={() => router.push(`/admin/agents/new`)}>
            <Plus className="h-4 w-4" />
            New Agent
          </Button>

          <Button variant="outline">Import Agent</Button>
        </div>

        {/* Right Search Field */}
        <div className="relative w-64">
          <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input placeholder="Search agents..." className="pl-8" />
        </div>
      </div>

      {/* Table */}
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
                {/* ONLY NAME IS CLICKABLE */}
                <TableCell>
                  <Link
                    href={`/admin/agents/${a.id}`}
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
    </div>
  );
}
