'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { useRouter } from 'next/navigation';
import {
  Plus,
  Search,
  RefreshCw,
  MoreVertical,
  ExternalLink,
  Download,
  Database,
  Activity,
} from 'lucide-react';
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
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
} from '@/components/ui/dropdown-menu';
import { useAgent } from '@/contexts/AgentContext';
import { AgentDetail, KnowledgeListData } from '@/types/agent';

const fetcher = (url: string) => fetch(url).then((res) => res.json());

export default function KnowledgePage() {
  const { agent } = useAgent() as { agent: AgentDetail | null };
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [loadingId, setLoadingId] = useState<string | null>(null);

  const {
    data = [],
    isLoading,
    mutate,
  } = useSWR(agent ? `/api/agents/${agent.id}/knowledge` : null, fetcher, {
    refreshInterval: 2000, // auto-refresh to update status live
  });

  const filtered = data.filter((item: KnowledgeListData) => {
    const q = query.trim().toLowerCase();
    return !q || item.name?.toLowerCase().includes(q);
  });

  async function handleResync(knowledgeId: string) {
    if (!agent) return;
    setLoadingId(knowledgeId);

    try {
      await fetch(`/api/agents/${agent.id}/knowledge/${knowledgeId}/resync`, {
        method: 'POST',
      });
      mutate();
    } finally {
      setLoadingId(null);
    }
  }

  return (
    <div className="space-y-6">
      {/* Header Actions */}
      <div className="flex items-center justify-between">
        <Button
          disabled={!agent}
          onClick={() => agent && router.push(`/admin/agents/${agent.id}/knowledge/add`)}
        >
          <Plus className="h-4 w-4 mr-1" />
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
              <TableHead className="w-[60px] text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {filtered.map((item: KnowledgeListData) => {
              const loading = loadingId === item.id;

              return (
                <TableRow key={item.id}>
                  <TableCell>{item.name}</TableCell>
                  <TableCell>{item.type}</TableCell>
                  <TableCell>{item.availableTo}</TableCell>

                  <TableCell>
                    <span className="font-semibold">{item.lastModifiedBy}</span>{' '}
                    <span className="text-xs text-muted-foreground">{item.lastModifiedWhen}</span>
                  </TableCell>

                  <TableCell>
                    <StatusBadge status={item.status} />
                  </TableCell>

                  {/* ACTION MENU */}
                  <TableCell className="text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreVertical className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>

                      <DropdownMenuContent align="end">
                        {/* RESYNC */}
                        <DropdownMenuItem disabled={loading} onClick={() => handleResync(item.id)}>
                          <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
                          Resync
                        </DropdownMenuItem>

                        {/* View Details */}
                        <DropdownMenuItem onClick={() => viewDetails(router, item.id)}>
                          <Activity className="h-4 w-4 mr-2" />
                          View Details
                        </DropdownMenuItem>

                        {/* WEB_URL */}
                        {item.type === 'WEB_URL' && item.sourceUri && (
                          <DropdownMenuItem onClick={() => openUrl(item.sourceUri)}>
                            <ExternalLink className="h-4 w-4 mr-2" />
                            Open URL
                          </DropdownMenuItem>
                        )}

                        {/* FILE */}
                        {item.type === 'FILE' && (
                          <DropdownMenuItem onClick={() => downloadFile(item.id)}>
                            <Download className="h-4 w-4 mr-2" />
                            Download File
                          </DropdownMenuItem>
                        )}

                        {/* API */}
                        {item.type === 'API' && (
                          <DropdownMenuItem onClick={() => testApi(router, item.id)}>
                            <Activity className="h-4 w-4 mr-2" />
                            Test API
                          </DropdownMenuItem>
                        )}

                        {/* DATABASE */}
                        {item.type === 'DATABASE' && (
                          <DropdownMenuItem onClick={() => testDb(router, item.id)}>
                            <Database className="h-4 w-4 mr-2" />
                            Test DB Connection
                          </DropdownMenuItem>
                        )}

                        {/* DELETE */}
                        <DropdownMenuItem
                          className="text-red-600"
                          onClick={() => deleteKnowledge(router, item.id)}
                        >
                          Delete
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}
    </div>
  );
}

function openUrl(url?: string) {
  if (!url) return;
  window.open(url, '_blank', 'noopener,noreferrer');
}

function downloadFile(knowledgeId?: string) {
  if (!knowledgeId) return;
  window.open(`/api/knowledge/${knowledgeId}/download`, '_blank');
}

function testApi(router: ReturnType<typeof useRouter>, knowledgeId?: string) {
  if (!knowledgeId) return;
  router.push(`/admin/knowledge/${knowledgeId}/test-api`);
}

function testDb(router: ReturnType<typeof useRouter>, knowledgeId?: string) {
  if (!knowledgeId) return;
  router.push(`/admin/knowledge/${knowledgeId}/test-db`);
}

function viewDetails(router: ReturnType<typeof useRouter>, knowledgeId?: string) {
  if (!knowledgeId) return;
  router.push(`/admin/knowledge/${knowledgeId}`);
}

function deleteKnowledge(router: ReturnType<typeof useRouter>, knowledgeId?: string) {
  if (!knowledgeId) return;
  router.push(`/admin/knowledge/${knowledgeId}/delete`);
}

function StatusBadge({ status }: Readonly<{ status: string }>) {
  const colors: Record<string, string> = {
    PENDING: 'bg-gray-100 text-gray-700',
    INGESTING: 'bg-yellow-100 text-yellow-800',
    EMBEDDING_PENDING: 'bg-blue-100 text-blue-800',
    READY: 'bg-green-100 text-green-800',
    FAILED: 'bg-red-100 text-red-800',
  };

  return (
    <span className={`px-2 py-1 rounded text-xs font-medium ${colors[status] ?? ''}`}>
      {status.replaceAll('_', ' ')}
    </span>
  );
}
