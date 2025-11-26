"use client";

import { useRouter } from "next/navigation";
import AgentForm from "@/app/components/agent/AgentForm";
import { useAgent } from "@/app/contexts/AgentContext";

export default function AgentEditPage() {
  const router = useRouter();
  const { agent, mutate } = useAgent();

  async function onSubmit(data: any) {
    const res = await fetch(`/api/agents/${agent.id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    });

    if (res.ok) {
      await mutate(); // Refresh SWR data
      router.push(`/admin/agents/${agent.id}?updated=1`);
      router.refresh(); // Ensure route caches update
      return;
    }

    alert("Failed to update agent");
  }

  return <AgentForm onSubmit={onSubmit} initialValues={agent} />;
}
