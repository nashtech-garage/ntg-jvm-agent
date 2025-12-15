export const PAGE_PATH = {
  LOGIN: '/login',
  ADMIN: '/admin',
  FORBIDDEN: '/forbidden',
  AGENTS: '/admin/agents',
  AGENT_DETAIL: (id: string | number) => `/admin/agents/${id}`,
  AGENT_NEW: '/admin/agents/new',
} as const;

export const API_PATH = {
  AUTH_CALLBACK: (providerId: string) => `/api/auth/callback/${providerId}`,
  SIGN_OUT: '/api/auth/logout',
  AGENTS: '/api/agents',
  AGENTS_SEARCH: (searchQuery: string) => {
    const params = new URLSearchParams();
    if (searchQuery) params.append('name', searchQuery);
    const query = params.toString();
    return query ? `/api/agents?${query}` : '/api/agents';
  },
  USER_BY_ID: (id: string | number) => `/api/users/${id}`,
} as const;

export const BACKEND_PATH = {
  AGENTS: '/api/agents',
  AGENTS_SEARCH: (searchQuery: string) => {
    const params = new URLSearchParams();
    if (searchQuery) params.append('name', searchQuery);
    const query = params.toString();
    return query ? `/api/agents?${query}` : '/api/agents';
  },
  USER_BY_ID: (id: string | number) => `/api/users/${id}`,
  AGENT_TOOLS: (agentId: string) => `/api/agents/${agentId}/agent-tools`,
} as const;
