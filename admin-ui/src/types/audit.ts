export type RevisionType = 'ADD' | 'MOD' | 'DEL';

export type AuditHistoryItem = {
  revision: number;
  revisionType: RevisionType;
  entity: any;
};

export type AgentAuditHistory = AuditHistoryItem & {
  entity: {
    id: string;
    name: string;
    model?: string;
    provider?: string;
    temperature?: number;
    maxTokens?: number;
    active?: boolean;
    lastModifiedBy?: string;
    lastModifiedDate?: string;
    [key: string]: any;
  };
};

export type RollbackRequest = {
  revision: number;
  includeSecrets: boolean;
};

export type ComparisonResult = {
  field: string;
  oldValue: any;
  newValue: any;
  changed: boolean;
};

