export type UsageSummary = {
  totalTokens: number;
  promptTokens: number;
  completionTokens: number;
  estimatedTokens: number;
};

export type UsageTimeSeriesPointDto = {
  date: string;
  totalTokens: number;
  promptTokens: number;
  completionTokens: number;
};

export type UsageBreakdownRow = {
  id: string;
  name: string;
  totalTokens: number;
  promptTokens: number;
  completionTokens: number;
};

export type UsageByAgentRowDto = {
  agentId: string;
  agentName: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
};

export type UsageByAgentResponseDto = {
  from: string;
  to: string;
  rows: UsageByAgentRowDto[];
};

export type UsageByUserRowDto = {
  userId: string | null;
  userName: string | null;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
};

export type UsageByUserResponseDto = {
  from: string;
  to: string;
  rows: UsageByUserRowDto[];
};
