import { Key } from 'lucide-react';

export const MCP_TOOL_AUTHENTICATION_TYPES = {
  NONE: 'NONE',
  API_KEY: 'API_KEY',
  BEARER: 'BEARER',
  CUSTOM_HEADER: 'CUSTOM_HEADER',
} as const;

export type McpToolAuthenticationSourceType =
  (typeof MCP_TOOL_AUTHENTICATION_TYPES)[keyof typeof MCP_TOOL_AUTHENTICATION_TYPES];

export const MCP_TOOL_AUTHENTICATION_OPTIONS = [
  { id: MCP_TOOL_AUTHENTICATION_TYPES.NONE, label: 'NONE', icon: Key },
  { id: MCP_TOOL_AUTHENTICATION_TYPES.API_KEY, label: 'API key', icon: Key },
  { id: MCP_TOOL_AUTHENTICATION_TYPES.BEARER, label: 'Bearer', icon: Key },
  { id: MCP_TOOL_AUTHENTICATION_TYPES.CUSTOM_HEADER, label: 'Custom header', icon: Key },
] as const;
