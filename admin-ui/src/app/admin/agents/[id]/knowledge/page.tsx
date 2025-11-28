'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { useRouter } from 'next/navigation';
import { Plus, Search } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/Table';
import { useAgent } from '@/contexts/AgentContext';
import { AgentDetail, KnowledgeListData } from '@/types/agent';

const fetcher = (url: string) => fetch(url).then((res) => res.json());

export default function KnowledgePage() {
  const { agent } = useAgent() as {
    agent: AgentDetail | null;
  };
  const router = useRouter();
  const [query, setQuery] = useState('');

  const { data = [], isLoading } = useSWR(
    agent ? `/api/agents/${agent.id}/knowledge` : null,
    fetcher
  );

  const filtered = data.filter((item: KnowledgeListData) => {
    const q = query.trim().toLowerCase();

    // If blank → return all items
    if (!q) return true;

    return item.name?.toLowerCase().includes(q);
  });

  return (
    <div className="space-y-6">
      {/* Header Actions */}
      <div className="flex items-center justify-between">
        <Button
          disabled={!agent}
          onClick={() => agent && router.push(`/agents/${agent.id}/knowledge/new`)}
        >
          <Plus className="h-4 w-4" />
          Add Knowledge
        </Button>

        <div className="relative w-64">
          <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search knowledge..."
            className="pl-8"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
      </div>

      {isLoading && <p>Loading...</p>}
      {!isLoading && filtered.length === 0 && <p>No knowledge items found.</p>}

      {/* Table */}
      {!isLoading && filtered.length > 0 && (
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
            {filtered.map((k: KnowledgeListData) => (
              <TableRow key={k.id}>
                <TableCell>{k.name}</TableCell>
                <TableCell>{k.type}</TableCell>
                <TableCell>{k.availableTo}</TableCell>
                <TableCell>
                  <span className="font-semibold">{k.lastModifiedBy}</span>{' '}
                  <span className="text-xs text-muted-foreground">{k.lastModifiedWhen}</span>
                </TableCell>
                <TableCell>{k.status}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
