"use client";

import { useRouter } from "next/navigation";
import AgentForm from "@/app/components/agent/AgentForm";

export default function AgentCreatePage() {
  const router = useRouter();

  async function onSubmit(data: any) {
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
