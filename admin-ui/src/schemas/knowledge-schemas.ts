import * as z from 'zod';
import { KNOWLEDGE_TYPES } from '@/constants/knowledge-types';

// --------------------------------------------------------
// File item schema
// --------------------------------------------------------
export const FileItemSchema = z.object({
  id: z.string(), // UUID
  file: z.instanceof(File),
});

export type FileItem = z.infer<typeof FileItemSchema>;

// --------------------------------------------------------
// Common fields (ALL OPTIONAL)
// --------------------------------------------------------
const CommonFields = z.object({
  files: z.array(FileItemSchema).optional(),

  url: z.string().optional(),
  sitemapUrl: z.string().optional(),
  inlineContent: z.string().optional(),

  dbHost: z.string().optional(),
  dbPort: z.string().optional(),
  dbUser: z.string().optional(),
  dbPassword: z.string().optional(),
  dbQuery: z.string().optional(),

  apiUrl: z.string().optional(),
  apiMethod: z.string().optional(),
});

// --------------------------------------------------------
// Type-specific schemas (REQUIRED FIELDS HERE)
// --------------------------------------------------------
export const FileImportSchema = CommonFields.extend({
  sourceType: z.literal(KNOWLEDGE_TYPES.FILE),
  files: z.array(FileItemSchema).min(1, 'At least one file is required'),
});

export const UrlImportSchema = CommonFields.extend({
  sourceType: z.literal(KNOWLEDGE_TYPES.WEB_URL),
  url: z.string().url('A valid URL is required'),
});

export const SitemapImportSchema = CommonFields.extend({
  sourceType: z.literal(KNOWLEDGE_TYPES.SITEMAP),
  sitemapUrl: z.string().url('A valid sitemap URL is required'),
});

export const InlineImportSchema = CommonFields.extend({
  sourceType: z.literal(KNOWLEDGE_TYPES.INLINE),
  inlineContent: z.string().min(1, 'Content cannot be empty'),
});

export const DatabaseImportSchema = CommonFields.extend({
  sourceType: z.literal(KNOWLEDGE_TYPES.DATABASE),
  dbHost: z.string().min(1, 'DB host is required'),
  dbPort: z.string().min(1, 'DB port is required'),
  dbUser: z.string().min(1, 'DB user is required'),
  dbPassword: z.string().min(1, 'DB password is required'),
  dbQuery: z.string().min(1, 'DB query is required'),
});

export const ApiImportSchema = CommonFields.extend({
  sourceType: z.literal(KNOWLEDGE_TYPES.API),
  apiUrl: z.string().min(1, 'API URL is required'),
  apiMethod: z.string().min(1, 'HTTP method is required'),
});

// --------------------------------------------------------
// Discriminated union (works perfectly now)
// --------------------------------------------------------
export const KnowledgeSchema = z.discriminatedUnion('sourceType', [
  FileImportSchema,
  UrlImportSchema,
  SitemapImportSchema,
  InlineImportSchema,
  DatabaseImportSchema,
  ApiImportSchema,
]);

export type KnowledgeFormValues = z.infer<typeof KnowledgeSchema>;
