'use client';

import { useState, useMemo, useCallback } from 'react';
import useSWR from 'swr';
import { useRouter } from 'next/navigation';
import { fetcher } from '@/utils/fetcher';
import { Plus, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { API_PATH, PAGE_PATH } from '@/constants/url';
import { AgentsTable } from '@/components/agent/agents-table';

export default function AgentTable() {
  const router = useRouter();
  const [searchTerm, setSearchTerm] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const apiUrl = useMemo(() => API_PATH.AGENTS_SEARCH(searchQuery), [searchQuery]);

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
    setSearchQuery(searchTerm.trim());
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
          <Button onClick={() => router.push(PAGE_PATH.AGENT_NEW)}>
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
      <AgentsTable agents={agents} isLoading={showTableLoading} />
    </div>
  );
}
