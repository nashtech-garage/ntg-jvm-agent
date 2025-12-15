'use client';

import { useRouter } from 'next/navigation';
import AgentForm from '@/components/agent/agent-form';
import { AgentFormData } from '@/types/agent';
import { getDefaultAgentAvatar } from '@/utils/avatar';
import { useToaster } from '@/contexts/ToasterContext';

export default function AgentCreatePage() {
  const router = useRouter();
  const { errorToaster } = useToaster();

  async function onSubmit(data: AgentFormData) {
    // Create new object to avoid direct mutation
    const payload = {
      ...data,
      avatar: data.avatar || getDefaultAgentAvatar(data.name),
    };

    const res = await fetch('/api/agents', {
      method: 'POST',
      body: JSON.stringify(payload),
    });

    if (res.ok) {
      router.push('/admin/agents');
      return;
    }

    errorToaster('Failed to create agent');
  }

  return <AgentForm onSubmit={onSubmit} />;
}
