'use client';

import { useState, useMemo, useCallback } from 'react';
import useSWR from 'swr';
import { useRouter } from 'next/navigation';
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
import { useAgent } from '@/contexts/AgentContext';
import { AgentDetail, KnowledgeListData } from '@/types/agent';
import { API_PATH } from '@/constants/url';
import { fetcher } from '@/utils/fetcher';

export default function KnowledgePage() {
  const { agent } = useAgent() as {
    agent: AgentDetail | null;
  };
  const router = useRouter();
  const [searchTerm, setSearchTerm] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const apiUrl = useMemo(() => {
    if (!agent) return null;
    return API_PATH.AGENT_KNOWLEDGE_SEARCH(agent.id, searchQuery);
  }, [agent, searchQuery]);

  const { data = [], isLoading } = useSWR(apiUrl, fetcher, {
    keepPreviousData: true,
  });

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

  return (
    <div className="space-y-6">
      {/* Header Actions */}
      <div className="flex items-center justify-between">
        <Button
          disabled={!agent}
          onClick={() => agent && router.push(`/admin/agents/${agent.id}/knowledge/add`)}
        >
          <Plus className="h-4 w-4" />
          Add Knowledge
        </Button>

        <div className="relative w-64">
          <button
            onClick={handleSearch}
            className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
            aria-label="Search"
          >
            <Search className="h-4 w-4" />
          </button>
          <Input
            placeholder="Search knowledge..."
            className="pl-8"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={handleKeyDown}
          />
        </div>
      </div>

      {isLoading && <p>Loading...</p>}
      {!isLoading && data.length === 0 && <p>No knowledge items found.</p>}

      {/* Table */}
      {!isLoading && data.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-1/5">Name</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Available to</TableHead>
              <TableHead>Last modified</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {data.map((item: KnowledgeListData) => (
              <TableRow key={item.id}>
                <TableCell>{item.name}</TableCell>
                <TableCell>{item.type}</TableCell>
                <TableCell>{item.availableTo}</TableCell>
                <TableCell>
                  <span className="font-semibold">{item.lastModifiedBy}</span>{' '}
                  <span className="text-xs text-muted-foreground">{item.lastModifiedWhen}</span>
                </TableCell>
                <TableCell>{item.status}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
