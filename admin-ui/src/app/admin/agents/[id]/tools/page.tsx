'use client';

import { useState, useCallback } from 'react';
import useSWR from 'swr';
import { useRouter } from 'next/navigation';
import { Plus, Search } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useAgent } from '@/contexts/AgentContext';
import { AgentDetail, AssignmentToolData } from '@/types/agent';
import { fetcher } from '@/utils/fetcher';
import logger from '@/utils/logger';
import { API_PATH } from '@/constants/url';

export default function ToolsPage() {
  const { agent } = useAgent() as {
    agent: AgentDetail | null;
  };
  const router = useRouter();
  const [inputValue, setInputValue] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Build API URL with search parameter
  const apiUrl = agent
    ? `${API_PATH.AGENT_TOOLS(agent.id)}${searchQuery ? `?name=${encodeURIComponent(searchQuery)}` : ''}`
    : null;

  const {
    data: assignmentTools = [],
    isLoading,
    error,
    mutate,
  } = useSWR(apiUrl, fetcher, {
    revalidateOnFocus: false,
  });

  const handleSearchChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
  }, []);

  const handleSearch = useCallback(() => {
    setSearchQuery(inputValue.trim());
  }, [inputValue]);

  const handleSearchKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter') {
        handleSearch();
      }
    },
    [handleSearch]
  );

  const handleAssignTool = async (toolId: string, isAssign: boolean) => {
    if (!agent) return;

    try {
      const result = await fetch(`${API_PATH.AGENT_TOOLS(agent.id)}`, {
        method: isAssign ? 'POST' : 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ toolId }),
      });

      if (!result.ok) {
        toast.error(await result.text());
        return;
      }

      toast.success(isAssign ? 'Tool assigned' : 'Tool unassigned');
      mutate();
    } catch (err) {
      logger.error('Failed to assign tool:', err);
      toast.error('Failed to update tool assignment');
    }
  };

  return (
    <div className="space-y-6">
      {error && <p className="text-red-500">Failed to load tools</p>}
      <Button
        disabled={!agent}
        onClick={() => agent && router.push(`/agents/${agent.id}/tools/new`)}
      >
        <Plus className="h-4 w-4" />
        Add a tool
      </Button>
      <div>
        <p className="font-medium mb-2">List tools available in system</p>
        <div className="flex items-center justify-between">
          <div className="relative w-64">
            <Input
              placeholder="Search tools..."
              className="pr-8"
              value={inputValue}
              onChange={handleSearchChange}
              onKeyDown={handleSearchKeyDown}
            />
            <button
              onClick={handleSearch}
              className="absolute right-2 top-2.5 text-muted-foreground hover:text-foreground transition"
              type="button"
            >
              <Search className="h-4 w-4" />
            </button>
          </div>
        </div>
        {isLoading && <p>Loading...</p>}
        {!isLoading && assignmentTools.length === 0 && <p>No matching tools found.</p>}
        {!isLoading && assignmentTools.length > 0 && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-1/4">Name</TableHead>
                <TableHead>Description</TableHead>
                <TableHead>Assign</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {assignmentTools.map((t: AssignmentToolData) => (
                <TableRow key={t.toolId}>
                  <TableCell>{t.toolName}</TableCell>
                  <TableCell align="left">{t.toolDescription}</TableCell>
                  <TableCell>
                    <Switch
                      checked={t.isAssigned}
                      onCheckedChange={(value) => handleAssignTool(t.toolId, value)}
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>
    </div>
  );
}
