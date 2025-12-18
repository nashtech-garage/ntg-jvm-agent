export type Provider = {
  id: string;
  name: string;
  description: string;
  active: boolean;
  baseUrl: string;
  chatCompletionsPath: string;
  embeddingsPath: string;
  models: ProviderModel[];
  embeddingModels: ProviderEmbeddingModel[];
};

export type ProviderDropDownItem = Pick<Provider, 'id' | 'name' | 'active'>;

export type ProviderModel = Readonly<{
  id: string;
  modelName: string;
  contextWindow: number;
  defaultTemperature: number;
  defaultTopP: number;
  defaultMaxTokens: number;
  defaultFrequencyPenalty: number;
  defaultPresencePenalty: number;
  defaultDimension: number;
  settings: Record<string, unknown>;
  active: boolean;
}>;

export type ProviderEmbeddingModel = Readonly<{
  id: string;
  embeddingName: string;
  dimension: number;
  settings: Record<string, unknown>;
  active: boolean;
}>;
