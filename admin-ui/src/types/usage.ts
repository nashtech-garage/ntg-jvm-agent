export type UsageSummary = {
  totalTokens: number;
  promptTokens: number;
  completionTokens: number;
  estimatedTokens: number;
};

export type UsageTimeSeriesPoint = {
  date: string;
  totalTokens: number;
  promptTokens: number;
  completionTokens: number;
};

export type UsageTimeSeries = {
  points: UsageTimeSeriesPoint[];
};

export type UsageByAgentRow = {
  agentId: string;
  agentName: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
};

export type UsageByUserRow = {
  userId: string | null;
  userName: string | null;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
};
