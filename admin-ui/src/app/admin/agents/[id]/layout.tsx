import AgentLayoutClient from '@/components/agent/agent-layout-client';

export default async function AgentDetailsLayout({
  params,
  children,
}: Readonly<{
  params: Promise<{ id: string }>;
  children: React.ReactNode;
}>) {
  const resolvedParams = await params;

  return <AgentLayoutClient id={resolvedParams.id}>{children}</AgentLayoutClient>;
}
