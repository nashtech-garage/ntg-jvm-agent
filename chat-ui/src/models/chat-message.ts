export interface ChatMessage {
  id: string;
  content: string;
  medias: ChatMessageMedia[];
  createdAt: string;
  type: number; // 1: user, 2: bot
  citations?: Citation[];
}

export interface Citation {
  chunkId: string;
  fileName: string;
  filePath: string;
  charStart: number;
  charEnd: number;
}

export interface ChatMessageMedia {
  contentType: string;
  data: string;
  fileName: string;
}
