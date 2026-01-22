import { Reaction } from '@/types/reaction';

export interface ChatMessage {
  id: string;
  content: string;
  medias: ChatMessageMedia[];
  createdAt: string;
  type: string;
  reaction: Reaction;
}

export interface ChatMessageMedia {
  contentType: string;
  data: string;
  fileName: string;
}
