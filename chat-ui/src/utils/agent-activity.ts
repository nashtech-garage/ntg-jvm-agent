export function shouldClearAgentActivity(
  activeConversationId: string | null,
  routeConversationId: string
): boolean {
  return activeConversationId !== routeConversationId;
}
