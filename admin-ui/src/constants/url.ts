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
  AUDIT_HISTORY_ALL: (entity: string) => `/api/audit/${entity}/history`,
  AUDIT_HISTORY: (entity: string, id: string) => `/api/audit/${entity}/${id}/history`,
  AUDIT_REVISION: (entity: string, id: string, revision: number) =>
    `/api/audit/${entity}/${id}/revision/${revision}`,
  AUDIT_ROLLBACK: (entity: string, id: string) => `/api/audit/${entity}/${id}/rollback`,
  AUDIT_AGENT_HISTORY: (id: string) => `/api/audit/agent/${id}/history`,
  AUDIT_AGENT_REVISION: (id: string, revision: number) =>
    `/api/audit/agent/${id}/revision/${revision}`,
  AUDIT_AGENT_ROLLBACK: (id: string) => `/api/audit/agent/${id}/rollback`,
  AUDIT_LOGS: (agentId?: string) =>
    agentId ? `/api/audit/logs?agentId=${encodeURIComponent(agentId)}` : '/api/audit/logs',
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
  AUDIT_HISTORY_ALL: (entity: string) => `/api/v1/audit/${entity}/history`,
  AUDIT_HISTORY: (entity: string, id: string) => `/api/v1/audit/${entity}/${id}/history`,
  AUDIT_REVISION: (entity: string, id: string, revision: number) =>
    `/api/v1/audit/${entity}/${id}/revision/${revision}`,
  AUDIT_ROLLBACK: (entity: string, id: string) => `/api/v1/audit/${entity}/${id}/rollback`,
  AUDIT_AGENT_HISTORY: (id: string) => `/api/v1/audit/agent/${id}/history`,
  AUDIT_AGENT_REVISION: (id: string, revision: number) => `/api/v1/audit/agent/${id}/revision/${revision}`,
  AUDIT_AGENT_ROLLBACK: (id: string) => `/api/v1/audit/agent/${id}/rollback`,
  AUDIT_LOGS: (agentId?: string) =>
    agentId ? `/api/v1/audit/logs?agentId=${encodeURIComponent(agentId)}` : '/api/v1/audit/logs',
} as const;
