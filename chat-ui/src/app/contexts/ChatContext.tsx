'use client';

import {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
  Dispatch,
  SetStateAction,
  useMemo,
} from 'react';
import { Conversation } from '@/app/models/conversation';
import { ChatMessage } from '@/app/models/chat-message';
import { toast } from 'sonner';

interface ChatContextType {
  conversations: Conversation[];
  chatMessages: ChatMessage[];
  activeConversationId: string | null;
  userName: string;
  setConversations: Dispatch<SetStateAction<Conversation[]>>;
  setChatMessages: Dispatch<SetStateAction<ChatMessage[]>>;
  setActiveConversationId: Dispatch<SetStateAction<string | null>>;
}

const ChatContext = createContext<ChatContextType | null>(null);

export function ChatProvider({ children }: { children: ReactNode }) {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<string | null>(null);
  const [userName, setUserName] = useState<string>('Unknown');

  // Load user info and all conversations on mount
  useEffect(() => {
    const fetchAll = async () => {
      try {
        const [userInfoRes, conversationsRes] = await Promise.all([
          fetch('/api/user'),
          fetch('/api/chat'),
        ]);
        const [userInfoData, conversationsData] = await Promise.all([
          userInfoRes.json(),
          conversationsRes.json(),
        ]);
        setUserName(userInfoData?.sub || 'Unknown user');
        setConversations(conversationsData);
      } catch (error) {
        toast.error(`Error fetching user information or conversations: ${error}`);
      }
    };

    fetchAll();
  }, []);

  const value = useMemo(
    () => ({
      conversations,
      chatMessages,
      activeConversationId,
      userName,
      setConversations,
      setChatMessages,
      setActiveConversationId,
    }),
    [conversations, chatMessages, activeConversationId, userName]
  );

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
}

export const useChatContext = () => {
  const ctx = useContext(ChatContext);
  if (!ctx) throw new Error('useChatContext must be used inside ChatProvider');
  return ctx;
};
