import AgentLayoutClient from '@/components/agent/agent-layout-client';

export default async function AgentDetailsLayout({
  params,
  children,
}: Readonly<{
  params: Promise<{ id: string }>;
  children: React.ReactNode | Promise<React.ReactNode>;
}>) {
  const resolvedParams = await params;
  const resolvedChildren = await children;

  return <AgentLayoutClient id={resolvedParams.id}>{resolvedChildren}</AgentLayoutClient>;
}
