import { Reaction } from '@/types/reaction';

export interface ChatMessage {
  id: string;
  content: string;
  medias: ChatMessageMedia[];
  createdAt: string;
  type: number; // 1: user, 2: bot
  reaction: Reaction;
}

export interface ChatMessageMedia {
  contentType: string;
  data: string;
  fileName: string;
}
