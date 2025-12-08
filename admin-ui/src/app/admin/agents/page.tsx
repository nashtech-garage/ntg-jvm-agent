'use client';

import { useState, useMemo, useCallback } from 'react';
import useSWR from 'swr';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { fetcher } from '@/utils/fetcher';
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
import { AgentListData } from '@/types/agent';

export default function AgentTable() {
  const router = useRouter();
  const [searchTerm, setSearchTerm] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const apiUrl = useMemo(
    () => (searchQuery ? `/api/agents?name=${encodeURIComponent(searchQuery)}` : '/api/agents'),
    [searchQuery]
  );

  const {
    data: agents,
    error,
    isLoading,
  } = useSWR(apiUrl, fetcher, {
    dedupingInterval: 3000,
    revalidateOnFocus: false,
  });

  // Only show loading state for initial load, not for searches
  const showTableLoading = useMemo(() => isLoading && !agents, [isLoading, agents]);

  const handleSearch = useCallback(() => {
    setSearchQuery(searchTerm);
  }, [searchTerm]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter') {
        handleSearch();
      }
    },
    [handleSearch]
  );

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
          <button
            onClick={handleSearch}
            className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
            aria-label="Search"
          >
            <Search className="h-4 w-4" />
          </button>
          <Input
            placeholder="Search agents..."
            className="pl-8"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={handleKeyDown}
          />
        </div>
      </div>

      {/* Table */}
      <div className="rounded-sm border">
        {showTableLoading ? (
          <div className="p-6 text-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-2"></div>
            <p className="text-gray-500">Loading agents...</p>
          </div>
        ) : (
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
        )}
      </div>
    </div>
  );
}
