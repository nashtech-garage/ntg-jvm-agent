export const PAGE_PATH = {
  LOGIN: '/login',
  ADMIN: '/admin',
  FORBIDDEN: '/forbidden',
} as const;

export const API_PATH = {
  AUTH_CALLBACK: (providerId: string) => `/api/auth/callback/${providerId}`,
  AGENT_KNOWLEDGE: (agentId: string | number) => `/api/agents/${agentId}/knowledge`,
  AGENT_KNOWLEDGE_SEARCH: (agentId: string | number, searchQuery: string) => {
    const params = new URLSearchParams();
    if (searchQuery) params.append('name', searchQuery);
    const query = params.toString();
    return query ? `/api/agents/${agentId}/knowledge?${query}` : `/api/agents/${agentId}/knowledge`;
  },
} as const;

export const BACKEND_PATH_KNOWLEDGE = {
  KNOWLEDGE: '/api/agents',
  KNOWLEDGE_SEARCH: (agentId: string | number, searchQuery: string) => {
    const params = new URLSearchParams();
    if (searchQuery) params.append('name', searchQuery);
    const query = params.toString();
    return query ? `/api/agents/${agentId}/knowledge?${query}` : `/api/agents/${agentId}/knowledge`;
  },
} as const;
