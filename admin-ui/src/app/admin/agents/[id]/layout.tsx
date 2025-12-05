import AgentLayoutClient from '@/components/agent/agent-layout-client';

interface AgentDetailsLayoutProps {
  params: Promise<{ id: string }>;
  children: React.ReactNode;
}

export default async function AgentDetailsLayout({ params, children }: AgentDetailsLayoutProps) {
  const resolvedParams = await params;

  return <AgentLayoutClient id={resolvedParams.id}>{children}</AgentLayoutClient>;
}
