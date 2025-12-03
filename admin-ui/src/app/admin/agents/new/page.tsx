'use client';

import { useRouter } from 'next/navigation';
import AgentForm from '@/components/agent/agent-form';
import { AgentFormData } from '@/types/agent';
import { toast } from 'react-hot-toast';

export default function AgentCreatePage() {
  const router = useRouter();

  async function onSubmit(data: AgentFormData) {
    const res = await fetch('/api/agents', {
      method: 'POST',
      body: JSON.stringify(data),
    });

    if (res.ok) {
      router.push('/admin/agents');
      return;
    }

    toast.error('Failed to create agent');
  }

  return <AgentForm onSubmit={onSubmit} />;
}
