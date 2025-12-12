'use client';

import { useEffect, useState } from 'react';
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
import logger from '@/utils/logger';

export default function ToolsPage() {
  const { agent } = useAgent() as {
    agent: AgentDetail | null;
  };
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [assignmentTools, setAssignmentTools] = useState<AssignmentToolData[]>([]);

  useEffect(() => {
    if (!agent) return;
    const load = async () => {
      try {
        const res = await fetch(`/api/agents/${agent.id}/agent-tools`);
        const data = await res.json();
        setAssignmentTools(data);
      } catch (error) {
        logger.error(`Error fetching user information: ${error}`);
      } finally {
        setIsLoading(false);
      }
    };

    load();
  }, [agent]);

  const filteredAvailableTools = assignmentTools.filter((tool: AssignmentToolData) => {
    const q = query.toLowerCase();

    // If blank → return all items
    if (!q) return true;

    return tool?.toolName?.toLowerCase().includes(q);
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

      setAssignmentTools(
        assignmentTools.map((tool) => {
          if (tool.toolId === toolId) {
            tool.isAssigned = isAssign;
            return tool;
          }
          return tool;
        })
      );
    } catch (err) {
      logger.error('Failed to assign tool:', err);
    }
  };

  return (
    <div className="space-y-6">
      {isLoading && <p>Loading...</p>}
      <Button
        disabled={!agent}
        onClick={() => agent && router.push(`/admin/agents/${agent.id}/tools/add`)}
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
              <TableHead>Description</TableHead>
              <TableHead>Assign</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredAvailableTools.map((t: AssignmentToolData) => (
              <TableRow key={t.toolId}>
                <TableCell>{t.toolName}</TableCell>
                <TableCell align="left">{t.toolDescription}</TableCell>
                {/* Toggle Enabled */}
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
      </div>
    </div>
  );
}
