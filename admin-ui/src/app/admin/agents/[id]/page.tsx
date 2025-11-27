"use client";

import { useRouter } from "next/navigation";
import { Plus, Edit } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAgent } from "@/app/contexts/AgentContext";
import { AgentDetail } from "@/app/types/agent";

export default function AgentOverviewPage() {
  const { agent } = useAgent() as {
    agent: AgentDetail | null;
  };
  const router = useRouter();

  return (
    <div className="space-y-6 w-full">

      {/* Agent Details */}
      <Card className="w-full rounded-sm">
        <CardHeader className="flex flex-row justify-between items-center p-3">
          <CardTitle>Details</CardTitle>
          <Button
            variant="outline"
            size="sm"
            disabled={!agent}
            onClick={() => agent && router.push(`/admin/agents/${agent.id}/edit`)}
          >
            <Edit className="h-4 w-4" />
            Edit
          </Button>
        </CardHeader>

        <CardContent className="space-y-4 p-3">
          {/* Name with avatar */}
          <div className="flex items-center gap-3">
            <Avatar className="h-10 w-10">
              <AvatarImage src="https://github.com/shadcn.png" alt="@shadcn" />
              <AvatarFallback>
                {agent?.name?.charAt(0)?.toUpperCase() || "A"}
              </AvatarFallback>
            </Avatar>
            <div className="flex flex-col">
              <span className="text-sm"><strong>Name</strong></span>
              <span className="font-sm">{agent?.name}</span>
            </div>
          </div>

          {/* Description */}
          <div className="flex flex-col">
            <span className="text-sm"><strong>Description</strong></span>
            <span className="font-sm">{agent?.description || "-"}</span>
          </div>

          {/* Model */}
          <div className="flex flex-col">
            <span className="text-sm"><strong>{`Agent's model`}</strong></span>
            <span className="font-sm">{agent?.model}</span>
          </div>

        </CardContent>
      </Card>

      {/* Instructions Section */}
      <Card className="w-full rounded-sm">
        <CardHeader className="flex flex-row justify-between items-center p-3">
          <div>
            <CardTitle>Instructions</CardTitle>
          </div>
          <Button
            variant="outline"
            size="sm"
            disabled={!agent}
            onClick={() => agent && router.push(`/agents/${agent.id}/edit`)}
          >
            <Edit className="h-4 w-4" />
            Edit
          </Button>
        </CardHeader>
        <CardContent className="p-3">
          <p>Instruction content</p>
        </CardContent>
      </Card>

      {/* Knowledge Section */}
      <Card className="w-full rounded-sm">
        <CardHeader className="flex flex-row justify-between items-center p-3">
          <div>
            <CardTitle>Knowledge</CardTitle>
            <p className="text-muted-foreground text-sm mt-1">Add data, files and other resources to inform and improve AI-generated responses.</p>
          </div>
          <Button
            variant="outline"
            size="sm"
            disabled={!agent}
            onClick={() => agent && router.push(`/agents/${agent.id}/knowledge/new`)}
          >
            <Plus className="h-4 w-4" />
            Add Knowledge
          </Button>
        </CardHeader>
        <CardContent className="p-3">
          <Button variant="outline" disabled={!agent} onClick={() => agent && router.push(`/agents/${agent.id}/knowledge`)}>
            Manage Knowledge
          </Button>
        </CardContent>
      </Card>

      {/* Tools Section */}
      <Card className="w-full rounded-sm">
        <CardHeader className="flex flex-row justify-between items-center p-3">
          <div>
            <CardTitle>Tools</CardTitle>
            <p className="text-muted-foreground text-sm mt-1">Add tools to empower the AI to complete specific tasks for improved engagement.</p>
          </div>
          <Button
            variant="outline"
            size="sm"
            disabled={!agent}
            onClick={() => agent && router.push(`/agents/${agent.id}/tools/new`)}
          >
            <Plus className="h-4 w-4" />
            Add Tool
          </Button>
        </CardHeader>
        <CardContent className="p-3">
          <Button variant="outline" disabled={!agent} onClick={() => agent && router.push(`/agents/${agent.id}/tools`)}>
            Manage Tools
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
