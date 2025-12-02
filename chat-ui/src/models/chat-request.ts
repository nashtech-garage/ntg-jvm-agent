import { FileSelectInfo } from './file-select-info';

export interface ChatRequest {
  question: string;
  conversationId?: string;
  files: FileSelectInfo[];
}
