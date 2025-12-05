'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Plus, Search, Info } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useAgent } from '@/contexts/AgentContext';
import { AgentDetail, ToolInfo, ToolListData } from '@/types/agent';
import logger from '@/utils/logger';

export default function ToolsPage() {
  const { agent } = useAgent() as {
    agent: AgentDetail | null;
  };
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [assignedTools, setAssignedTools] = useState<ToolListData[]>([]);
  const [availableTools, setAvailableTools] = useState<ToolInfo[]>([]);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        if (!agent) return;
        const [availableToolsRes, assignedToolsRes] = await Promise.all([
          fetch(`/api/agents/${agent.id}/tools`),
          fetch(`/api/agents/${agent.id}/agent-tools`),
        ]);
        const [availableToolsOriginal, assignedTools] = await Promise.all<
          [Promise<ToolInfo[]>, Promise<ToolListData[]>]
        >([availableToolsRes.json(), assignedToolsRes.json()]);
        setAssignedTools(assignedTools);
        setAvailableTools(
          availableToolsOriginal.map((tool) => ({
            ...tool,
            active: assignedTools.some((item) => item.toolId === tool.id),
          })) ?? []
        );

        setIsLoading(false);
      } catch (error) {
        logger.error(`Error fetching user information or conversations: ${error}`);
        setIsLoading(false);
      }
    };

    fetchAll();
  }, [agent]);

  const filteredAvailableTools = availableTools.filter((tool: ToolInfo) => {
    const q = query.toLowerCase();

    // If blank → return all items
    if (!q) return true;

    return tool?.name?.toLowerCase().includes(q);
  });

  const handleAssignTool = async (toolId: string, isAssign: boolean) => {
    if (!agent) return;

    try {
      const result = await fetch(`/api/agents/${agent.id}/agent-tools`, {
        method: isAssign ? 'POST' : 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ toolId }),
      });

      if (!result.ok) {
        toast.error(await result.text());
        return;
      }

      setAvailableTools(
        availableTools.map((tool) => {
          if (tool.id === toolId) {
            tool.active = isAssign;
            return tool;
          }
          return tool;
        })
      );

      // Refresh assigned tool table
      await refreshAssignTools();
    } catch (err) {
      logger.error('Failed to assign tool:', err);
    }
  };

  const handleStateAsignedTool = async (toolId: string, status: boolean) => {
    if (!agent) return;

    try {
      const result = await fetch(`/api/agents/${agent.id}/agent-tools`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ toolId, status }),
      });

      if (!result.ok) {
        toast.error(await result.text());
        return;
      }
      setAssignedTools(
        assignedTools.map((tool) => {
          if (tool.toolId === toolId) {
            tool.enabled = status;
            return tool;
          }
          return tool;
        })
      );
    } catch (err) {
      logger.error('Failed to change state of assigned tool:', err);
    }
  };

  const refreshAssignTools = async () => {
    if (!agent) return;
    const res = await fetch(`/api/agents/${agent.id}/agent-tools`);
    setAssignedTools(await res.json());
  };

  return (
    <div className="space-y-6">
      {isLoading && <p>Loading...</p>}
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
            <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search tools..."
              className="pl-8"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>
        </div>
        {!isLoading && filteredAvailableTools.length === 0 && <p>No matching tools found.</p>}
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-1/4">Name</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Description</TableHead>
              <TableHead>Config</TableHead>
              <TableHead>Assign</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredAvailableTools.map((t: ToolInfo) => (
              <TableRow key={t.id}>
                <TableCell>{t.name}</TableCell>
                <TableCell>{t.type}</TableCell>
                <TableCell align="center">
                  <Tooltip>
                    <TooltipTrigger>
                      <Info size={18} color="green" />
                    </TooltipTrigger>
                    <TooltipContent>
                      <div className="whitespace-pre-wrap">{t.description}</div>
                    </TooltipContent>
                  </Tooltip>
                </TableCell>
                <TableCell>{JSON.stringify(t.config)}</TableCell>

                {/* Toggle Enabled */}
                <TableCell>
                  <Switch
                    checked={t.active}
                    onCheckedChange={(value) => handleAssignTool(t.id, value)}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Assigned tool table */}
      <div>
        <p className="font-medium mb-2">List assigned tools</p>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-1/4">Name</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Available to</TableHead>
              <TableHead>Last modified</TableHead>
              <TableHead>Enabled</TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {assignedTools.map((t: ToolListData) => (
              <TableRow key={t.toolId}>
                <TableCell>{t.toolName}</TableCell>
                <TableCell>{t.toolType}</TableCell>
                <TableCell>{t.availableTo}</TableCell>
                <TableCell>
                  <span className="font-semibold">{t.lastModifiedBy}</span>{' '}
                  <span className="text-xs text-muted-foreground">{t.lastModifiedWhen}</span>
                </TableCell>

                {/* Toggle Enabled */}
                <TableCell>
                  <Switch
                    checked={t.enabled}
                    onCheckedChange={(value) => handleStateAsignedTool(t.toolId, value)}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
