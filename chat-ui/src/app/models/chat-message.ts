export interface ChatMessage {
  id: string;
  content: string;
  createdAt: string;
  type: number; // 1: user, 2: bot
}
