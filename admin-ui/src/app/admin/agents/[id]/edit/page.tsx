'use client';

import { useRouter } from 'next/navigation';
import AgentForm from '@/components/agent/agent-form';
import { useAgent } from '@/contexts/AgentContext';
import { AgentDetail, AgentFormData } from '@/types/agent';
import { useToaster } from '@/contexts/ToasterContext';

export default function AgentEditPage() {
  const { toasterError } = useToaster();
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

    toasterError('Failed to update agent');
  }

  if (!agent) return <p>Loading...</p>;

  return <AgentForm onSubmit={onSubmit} initialValues={agent} />;
}
