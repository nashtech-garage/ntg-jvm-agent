import * as z from 'zod';
import { MCP_TOOL_AUTHENTICATION_TYPES } from '@/constants/mcp-tool-authentication-types';

// --------------------------------------------------------
// Common fields (ALL OPTIONAL)
// --------------------------------------------------------
const CommonFields = z.object({
  transportType: z.string().optional(),
  baseUrl: z.string().optional(),
  endpoint: z.string().optional(),
  headerName: z.string().optional(),
  token: z.string().optional(),
});

// --------------------------------------------------------
// Type-specific schemas (REQUIRED FIELDS HERE)
// --------------------------------------------------------
export const NoneAuthSchema = CommonFields.extend({
  sourceType: z.literal(MCP_TOOL_AUTHENTICATION_TYPES.NONE),
  transportType: z.string().min(1, 'Transport type is required'),
  baseUrl: z.url('A valid URL is required'),
  endpoint: z.string().startsWith('/', '/ must is first character').min(1, 'Endpoint is required'),
});

export const ApiKeySchema = CommonFields.extend({
  sourceType: z.literal(MCP_TOOL_AUTHENTICATION_TYPES.API_KEY),
  transportType: z.string().min(1, 'Transport type is required'),
  baseUrl: z.url('A valid URL is required'),
  endpoint: z.string().startsWith('/', '/ must is first character').min(1, 'Endpoint is required'),
  token: z.string().min(5, 'API key is required'),
});

export const JWTBearerSchema = CommonFields.extend({
  sourceType: z.literal(MCP_TOOL_AUTHENTICATION_TYPES.BEARER),
  transportType: z.string().min(1, 'Transport type is required'),
  baseUrl: z.url('A valid URL is required'),
  endpoint: z.string().startsWith('/', '/ must is first character').min(1, 'Endpoint is required'),
  token: z.string().min(10, 'Bearer token is required'),
});

export const CustomHeaderSchema = CommonFields.extend({
  sourceType: z.literal(MCP_TOOL_AUTHENTICATION_TYPES.CUSTOM_HEADER),
  transportType: z.string().min(1, 'Transport type is required'),
  baseUrl: z.url('A valid URL is required'),
  endpoint: z.string().startsWith('/', '/ must is first character').min(1, 'Endpoint is required'),
  headerName: z.string().min(1, 'Custom header name is required'),
  token: z.string().min(10, 'Custom header token is required'),
});

// --------------------------------------------------------
// Discriminated union (works perfectly now)
// --------------------------------------------------------
export const McpToolAuthenticationSchema = z.discriminatedUnion('sourceType', [
  NoneAuthSchema,
  ApiKeySchema,
  JWTBearerSchema,
  CustomHeaderSchema,
]);

export type McpToolAuthenticationFormValues = z.infer<typeof McpToolAuthenticationSchema>;
