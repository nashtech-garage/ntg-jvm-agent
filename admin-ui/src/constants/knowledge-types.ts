import { FileText, Globe, Map, Pencil, Database, PlugZap } from 'lucide-react';

export const KNOWLEDGE_TYPES = {
  FILE: 'FILE',
  WEB_URL: 'WEB_URL',
  SITEMAP: 'SITEMAP',
  INLINE: 'INLINE',
  DATABASE: 'DATABASE',
  API: 'API',
} as const;

export type KnowledgeSourceType = (typeof KNOWLEDGE_TYPES)[keyof typeof KNOWLEDGE_TYPES];

export const KNOWLEDGE_TYPE_OPTIONS = [
  { id: KNOWLEDGE_TYPES.FILE, label: 'Files', icon: FileText },
  { id: KNOWLEDGE_TYPES.WEB_URL, label: 'Web URL', icon: Globe },
  { id: KNOWLEDGE_TYPES.SITEMAP, label: 'Sitemap', icon: Map },
  { id: KNOWLEDGE_TYPES.INLINE, label: 'Manual', icon: Pencil },
  { id: KNOWLEDGE_TYPES.DATABASE, label: 'Database', icon: Database },
  { id: KNOWLEDGE_TYPES.API, label: 'API', icon: PlugZap },
] as const;
