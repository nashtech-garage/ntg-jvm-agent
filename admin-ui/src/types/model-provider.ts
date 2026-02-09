export type ProviderSupports = {
  chat: boolean;
  embedding: boolean;
};

export type ProviderDefaults = {
  baseUrl: string;
  chatCompletionsPath: string;
  embeddingModel?: string;
};

export type ModelProvider = {
  id: string;
  displayName: string;
  icon?: string;
  defaults: ProviderDefaults;
  supports: ProviderSupports;
};

export type ModelProviderResponse = {
  providers: ModelProvider[];
};

export type ProviderModel = {
  name: string;
  label?: string;
  contextWindow?: number;
  isDefault?: boolean;
};

export type ProviderModelsResponse = {
  models: ProviderModel[];
};
