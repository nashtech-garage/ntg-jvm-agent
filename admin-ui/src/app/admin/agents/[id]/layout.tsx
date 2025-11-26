import AgentLayoutClient from "@/app/components/agent/AgentLayoutClient";

export default async function AgentDetailsLayout({ params, children }) {
  const { id } = await params;

  return (
    <AgentLayoutClient id={id}>
      {children}
    </AgentLayoutClient>
  );
}
