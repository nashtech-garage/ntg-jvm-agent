export type ToolCallPhase = 'STARTED' | 'COMPLETED';
export type TodoStatus = 'pending' | 'in_progress' | 'completed';

export interface TodoItem {
  content: string;
  status: TodoStatus;
  activeForm: string;
}

export interface ToolCallEvent {
  id: string;
  name: string;
  phase: ToolCallPhase;
  todoItems?: TodoItem[];
}
