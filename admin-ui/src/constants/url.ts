export const PAGE_PATH = {
  LOGIN: '/login',
  ADMIN: '/admin',
  FORBIDDEN: '/forbidden',
} as const;

export const API_PATH = {
  AUTH_CALLBACK: (providerId: string) => `/api/auth/callback/${providerId}`,
  USER_BY_ID: (id: string | number) => `/api/users/${id}`,
  AGENT_TOOLS: (agentId: string) => `/api/agents/${agentId}/agent-tools`,
} as const;
