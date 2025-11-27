"use client";

import { useRouter } from "next/navigation";
import AgentForm from "@/app/components/agent/AgentForm";
import { AgentFormData } from "@/app/types/agent";

export default function AgentCreatePage() {
  const router = useRouter();

  async function onSubmit(data: AgentFormData) {
    const res = await fetch("/api/agents", {
      method: "POST",
      body: JSON.stringify(data),
    });

    if (res.ok) {
      router.push("/admin/agents");
      return;
    }

    alert("Failed to create agent");
  }

  return <AgentForm onSubmit={onSubmit} />;
}
