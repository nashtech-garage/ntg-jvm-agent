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
} as const;

export const BACKEND_PATH = {
  AGENTS: '/api/agents',
  AGENTS_SEARCH: (searchQuery: string) => {
    const params = new URLSearchParams();
    if (searchQuery) params.append('name', searchQuery);
    const query = params.toString();
    return query ? `/api/agents?${query}` : '/api/agents';
  },
} as const;
