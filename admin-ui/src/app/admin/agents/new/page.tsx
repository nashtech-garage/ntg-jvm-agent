'use client';

import { useRouter } from 'next/navigation';
import AgentForm from '@/components/agent/agent-form';
import { AgentFormData } from '@/types/agent';
import { getDefaultAgentAvatar } from '@/utils/avatar';

export default function AgentCreatePage() {
  const router = useRouter();

  async function onSubmit(data: AgentFormData) {
    // Set default avatar if not provided
    if (!data.avatar) {
      data.avatar = getDefaultAgentAvatar(data.name);
    }

    const res = await fetch('/api/agents', {
      method: 'POST',
      body: JSON.stringify(data),
    });

    if (res.ok) {
      router.push('/admin/agents');
      return;
    }

    alert('Failed to create agent');
  }

  return <AgentForm onSubmit={onSubmit} />;
}
