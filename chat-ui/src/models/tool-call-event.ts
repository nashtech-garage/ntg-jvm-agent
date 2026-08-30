export type ToolCallPhase = 'STARTED' | 'COMPLETED';

export interface ToolCallEvent {
  id: string;
  name: string;
  phase: ToolCallPhase;
}
