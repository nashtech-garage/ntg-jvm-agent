export type AgentDetail = {
  id: string;
  name: string;
  description?: string;
  active: boolean;
  provider: string;
  model: string;
  apiKey: string;
  baseUrl: string;
  chatCompletionsPath: string;
  embeddingsPath: string;
  embeddingModel: string;
  dimension: number;
  temperature: number;
  maxTokens: number;
  topP: number;
  frequencyPenalty: number;
  presencePenalty: number;
  settings?: AgentSettings;
};

export type AgentListData = {
  id: string;
  name: string;
  model: string;
  lastModifiedBy: string;
  lastModifiedWhen: string;
  lastPublishedWhen?: string;
  owner: string;
  status: string;
};

export type AgentFormData = {
  name: string;
  description?: string;
  active: boolean;
  provider: string;
  model: string;
  apiKey: string;
  baseUrl: string;
  chatCompletionsPath: string;
  embeddingsPath: string;
  embeddingModel: string;
  dimension: number;
  temperature: number;
  maxTokens: number;
  topP: number;
  frequencyPenalty: number;
  presencePenalty: number;
  settings?: AgentSettings;
};

export type AgentSettings = {
  [key: string]: string;
};

export type AgentFormParams = {
  onSubmit: (data: AgentFormData) => void | Promise<void>;
  initialValues?: AgentDetail;
};

export type KnowledgeListData = {
  id: string;
  name: string;
  type: string;
  availableTo: string;
  lastModifiedBy: string;
  lastModifiedWhen: string;
  status: string;
};

export type ToolListData = {
  toolId: string;
  toolName: string;
  toolType: string;
  availableTo: string;
  lastModifiedBy: string;
  lastModifiedWhen: string;
  enabled: boolean;
};

export type AssignmentToolData = {
  toolId: string;
  toolName: string;
  toolDescription: string;
  isAssigned: boolean;
};
