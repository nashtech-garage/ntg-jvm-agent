'use client';

import React, { ReactNode } from 'react';
import useSWR from 'swr';
import { fetcher } from '@/utils/fetcher';
import AgentTabs from '@/components/agent/agent-tabs';
import TestAgentPanel from '@/components/agent/test-agent-panel';
import { AgentContext } from '@/contexts/AgentContext';

interface AgentLayoutClientProps {
  id: string;
  children: ReactNode;
}

export default function AgentLayoutClient({ id, children }: Readonly<AgentLayoutClientProps>) {
  const {
    data: agent,
    error,
    mutate,
    isLoading,
  } = useSWR(
    `/api/agents/${id}`,
    fetcher,
    { revalidateOnFocus: false } // avoid annoying page focus refreshes
  );

  const value = React.useMemo(() => ({ agent, mutate }), [agent, mutate]);

  if (error) return <div className="p-6">Failed to load agent.</div>;
  if (isLoading || !agent) return <div className="p-6">Loading agent...</div>;

  return (
    <AgentContext.Provider value={value}>
      <div className="h-screen grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-3 sticky top-0 z-10 bg-background border-b">
          <AgentTabs agentId={id} agentName={agent.name} agentAvatar={agent.avatar} />
        </div>

        <div className="lg:col-span-2 h-[calc(100vh-48px)] pr-4 border-r space-y-6">{children}</div>

        <div className="lg:col-span-1 h-screen sticky top-0 space-y-6">
          <TestAgentPanel agentId={id} />
        </div>
      </div>
    </AgentContext.Provider>
  );
}
