export function getDefaultAgentAvatar(agentName: string): string {
  return agentName?.charAt(0)?.toUpperCase() || 'A';
}
