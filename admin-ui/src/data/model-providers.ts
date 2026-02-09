import { ModelProvider, ProviderModel } from '@/types/model-provider';

export const STATIC_MODEL_PROVIDERS: ModelProvider[] = [
  {
    id: 'openai',
    displayName: 'OpenAI',
    defaults: {
      baseUrl: 'https://api.openai.com',
      chatCompletionsPath: '/v1/chat/completions',
      embeddingModel: 'text-embedding-3-large',
    },
    supports: { chat: true, embedding: true },
  },
  {
    id: 'anthropic',
    displayName: 'Anthropic',
    defaults: {
      baseUrl: 'https://api.anthropic.com',
      chatCompletionsPath: '/v1/messages',
      embeddingModel: 'claude-embedding-1',
    },
    supports: { chat: true, embedding: true },
  },
  {
    id: 'azure-openai',
    displayName: 'Azure OpenAI',
    defaults: {
      baseUrl: '',
      chatCompletionsPath: '/openai/deployments/<deployment>/chat/completions?api-version=2024-02-15-preview',
    },
    supports: { chat: true, embedding: true },
  },
  {
    id: 'bedrock',
    displayName: 'Amazon Bedrock',
    defaults: {
      baseUrl: 'https://bedrock.<region>.amazonaws.com',
      chatCompletionsPath: '/model/<model-id>/invoke',
      embeddingModel: 'amazon.titan-embed-text-v2:0',
    },
    supports: { chat: true, embedding: true },
  },
  {
    id: 'ollama',
    displayName: 'Ollama',
    defaults: {
      baseUrl: 'http://localhost:11434',
      chatCompletionsPath: '/api/chat',
      embeddingModel: 'nomic-embed-text',
    },
    supports: { chat: true, embedding: true },
  },
];

const CHAT_MODELS: Record<string, ProviderModel[]> = {
  openai: [
    { name: 'gpt-4.1', label: 'gpt-4.1 (OpenAI)', isDefault: true },
    { name: 'gpt-4.1-mini', label: 'gpt-4.1-mini' },
    { name: 'gpt-3.5-turbo', label: 'gpt-3.5-turbo' },
  ],
  anthropic: [
    { name: 'claude-3-5-sonnet-20241022', label: 'Claude 3.5 Sonnet', isDefault: true },
    { name: 'claude-3-5-haiku-20241022', label: 'Claude 3.5 Haiku' },
  ],
  'azure-openai': [
    { name: 'gpt-5.2', label: 'gpt-5.2', isDefault: true },
    { name: 'gpt-5.1', label: 'gpt-5.1' },
    { name: 'gpt-5.1-chat', label: 'gpt-5.1-chat' },
    { name: 'gpt-5.1-codex', label: 'gpt-5.1-codex' },
    { name: 'gpt-5.1-codex-mini', label: 'gpt-5.1-codex-mini' },
    { name: 'gpt-5.1-codex-max', label: 'gpt-5.1-codex-max' },
    { name: 'gpt-5-pro', label: 'gpt-5-pro' },
    { name: 'gpt-5-codex', label: 'gpt-5-codex' },
    { name: 'gpt-5-mini', label: 'gpt-5-mini' },
    { name: 'gpt-4.1', label: 'gpt-4.1' },
    { name: 'gpt-4.1-mini', label: 'gpt-4.1-mini' },
    { name: 'gpt-4.1-nano', label: 'gpt-4.1-nano' },
    { name: 'o4-mini', label: 'o4-mini' },
    { name: 'o3', label: 'o3' },
    { name: 'o3-mini', label: 'o3-mini' },
    { name: 'o3-deep-research', label: 'o3-deep-research' },
    { name: 'gpt-4.5-preview', label: 'gpt-4.5-preview' },
  ],
  bedrock: [
    { name: 'anthropic.claude-3-5-sonnet-20241022-v1:0', label: 'Claude 3.5 Sonnet (BR)', isDefault: true },
    { name: 'anthropic.claude-3-5-haiku-20241022-v1:0', label: 'Claude 3.5 Haiku (BR)' },
    { name: 'meta.llama3-1-70b-instruct-v1:0', label: 'Llama 3.1 70B (BR)' },
  ],
  ollama: [
    { name: 'llama3.2', label: 'llama3.2', isDefault: true },
    { name: 'llama3.1', label: 'llama3.1' },
    { name: 'mistral', label: 'mistral' },
  ],
};

const EMBEDDING_MODELS: Record<string, ProviderModel[]> = {
  openai: [
    { name: 'text-embedding-3-large', label: 'text-embedding-3-large', isDefault: true },
    { name: 'text-embedding-3-small', label: 'text-embedding-3-small' },
  ],
  anthropic: [
    { name: 'claude-embedding-1', label: 'claude-embedding-1', isDefault: true },
  ],
  'azure-openai': [
    { name: 'text-embedding-3-large', label: 'text-embedding-3-large', isDefault: true },
    { name: 'text-embedding-3-small', label: 'text-embedding-3-small' },
  ],
  bedrock: [
    { name: 'amazon.titan-embed-text-v2:0', label: 'titan-embed-text-v2', isDefault: true },
  ],
  custom: [],
  ollama: [
    { name: 'nomic-embed-text', label: 'nomic-embed-text', isDefault: true },
    { name: 'all-minilm', label: 'all-minilm' },
  ],
};

export function getStaticProviderModels(providerId: string | null, type: 'chat' | 'embedding') {
  if (!providerId) return [];
  if (type === 'chat') return CHAT_MODELS[providerId] ?? [];
  return EMBEDDING_MODELS[providerId] ?? [];
}
