export interface UserQuestionOption {
  label: string;
  description: string;
}

export interface UserQuestion {
  question: string;
  header: string;
  options: UserQuestionOption[];
  multiSelect: boolean;
}

export interface PendingQuestion {
  id: string;
  conversationId: string;
  questions: UserQuestion[];
  expiresAt: string;
}

export type QuestionAnswers = Record<string, string>;
