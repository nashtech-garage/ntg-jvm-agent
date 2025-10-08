import { ChatMessage } from './chat-message';
import { Conversation } from './conversation';

export interface ChatResponse {
  conversation: Conversation;
  message: ChatMessage;
}
