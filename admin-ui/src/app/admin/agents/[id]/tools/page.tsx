"use client";

import { useState } from "react";
import useSWR, { mutate } from "swr";
import { useRouter } from "next/navigation";
import { Plus, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useAgent } from "@/app/contexts/AgentContext";
import { AgentDetail, ToolListData } from "@/app/types/agent";

const fetcher = (url: string) => fetch(url).then(res => res.json());

export default function ToolsPage() {
  const { agent } = useAgent() as {
    agent: AgentDetail | null;
  };
  const router = useRouter();
  const [query, setQuery] = useState("");

  const { data = [], isLoading } = useSWR(
    agent ? `/api/agents/${agent.id}/tools` : null,
    fetcher
  );

  const filtered = data.filter((tool: ToolListData) => {
    const q = query.toLowerCase();

    // If blank → return all items
    if (!q) return true;

    return (
      tool?.toolName?.toLowerCase().includes(q)
    );
  });

  const handleToggle = async (toolId: string, enabled: boolean) => {
    if (!agent) return;

    try {
      await fetch(`/api/agents/${agent.id}/tools/${toolId}/toggle`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ enabled }),
      });

      // Refresh the table
      mutate(`/api/agents/${agent.id}/tools`);
    } catch (err) {
      console.error("Failed to toggle:", err);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Actions */}
      <div className="flex items-center justify-between">
        <Button
          disabled={!agent}
          onClick={() =>
            agent && router.push(`/agents/${agent.id}/tools/new`)
          }
        >
          <Plus className="h-4 w-4" />
          Add a tool
        </Button>

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

      {isLoading && <p>Loading...</p>}
      {!isLoading && filtered.length === 0 && (
        <p>No matching tools found.</p>
      )}

      {/* Table */}
      {!isLoading && filtered.length > 0 && (
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
            {filtered.map((t: ToolListData) => (
              <TableRow key={t.toolId}>
                <TableCell>{t.toolName}</TableCell>
                <TableCell>{t.toolType}</TableCell>
                <TableCell>{t.availableTo}</TableCell>
                <TableCell>
                  <span className="font-semibold">{t.lastModifiedBy}</span>{" "}
                  <span className="text-xs text-muted-foreground">{t.lastModifiedWhen}</span>
                </TableCell>

                {/* Toggle Enabled */}
                <TableCell>
                  <Switch
                    checked={t.enabled}
                    onCheckedChange={(value) => handleToggle(t.toolId, value)}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>

        </Table>
      )}
    </div>
  );
}
