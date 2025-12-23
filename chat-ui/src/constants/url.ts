export const PAGE_PATH = {
  LOGIN: '/login',
  HOME: '/',
} as const;

export const API_PATH = {
  AUTH: '/api/auth',
  AUTH_CALLBACK: (providerId: string) => `/api/auth/callback/${providerId}`,
} as const;

export const REACTION_PATH = {
  CHAT_MESSAGE_REACTION: (messageId: string) => `/api/chat/messages/${messageId}/reaction`,
  CHAT_MESSAGE_ENDPOINT: (messageId: string) => `/api/conversations/messages/${messageId}/reaction`,
} as const;
