'use client';

import { useRouter } from 'next/navigation';
import AgentForm from '@/components/agent/AgentForm';
import { useAgent } from '@/contexts/AgentContext';
import { AgentDetail, AgentFormData } from '@/types/agent';

export default function AgentEditPage() {
  const router = useRouter();
  const { agent, mutate } = useAgent() as {
    agent: AgentDetail | null;
    mutate: () => void;
  };

  async function onSubmit(data: AgentFormData) {
    if (!agent) return;

    const res = await fetch(`/api/agents/${agent.id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });

    if (res.ok) {
      mutate(); // Refresh SWR data
      router.push(`/admin/agents/${agent.id}`);
      router.refresh(); // Ensure route caches update
      return;
    }

    alert('Failed to update agent');
  }

  if (!agent) return <p>Loading...</p>;

  return <AgentForm onSubmit={onSubmit} initialValues={agent} />;
}
