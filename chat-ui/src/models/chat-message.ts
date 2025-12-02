export interface ChatMessage {
  id: string;
  content: string;
  medias: ChatMessageMedia[];
  createdAt: string;
  type: number; // 1: user, 2: bot
}

export interface ChatMessageMedia {
  contentType: string;
  data: string;
  fileName: string;
}
