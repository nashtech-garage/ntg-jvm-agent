'use client';

import { use } from 'react';
import AgentAuditLog from '@/components/agent/agent-audit-log';

export default function AgentAuditPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);

  return (
    <div className="space-y-6">
      <AgentAuditLog agentId={id} />
    </div>
  );
}

