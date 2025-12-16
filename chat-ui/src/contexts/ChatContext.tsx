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
import { Conversation } from '@/models/conversation';
import { ChatMessage } from '@/models/chat-message';
import { Agent } from '../models/agent';
import { useToaster } from './ToasterContext';

interface ChatContextType {
  conversations: Conversation[];
  chatMessages: ChatMessage[];
  activeConversationId: string | null;
  userName: string;
  agents: Agent[];
  selectedAgent: Agent | null;
  setConversations: Dispatch<SetStateAction<Conversation[]>>;
  setChatMessages: Dispatch<SetStateAction<ChatMessage[]>>;
  setActiveConversationId: Dispatch<SetStateAction<string | null>>;
  setAgents: Dispatch<SetStateAction<Agent[]>>;
  setSelectedAgent: Dispatch<SetStateAction<Agent | null>>;
}

const ChatContext = createContext<ChatContextType | null>(null);

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
    return {
      userName: userInfoData?.sub || 'Unknown user',
      conversations: conversationsData,
    };
  } catch (error) {
    return {
      error: `Error fetching user information or conversations: ${error}`,
      userName: 'Unknown user',
      conversations: [] as Conversation[],
    };
  }
};

export function ChatProvider({ children }: Readonly<{ children: ReactNode }>) {
  const { showError } = useToaster();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<string | null>(null);
  const [userName, setUserName] = useState<string>('Unknown');
  const [agents, setAgents] = useState<Agent[]>([]);
  const [selectedAgent, setSelectedAgent] = useState<Agent | null>(null);

  // Load user info and all conversations on mount
  useEffect(() => {
    fetchAll().then(({ userName, conversations, error }) => {
      if (error) {
        showError(error);
      } else {
        setUserName(userName);
        setConversations(conversations);
      }
    });
  }, [showError]);

  const value = useMemo(
    () => ({
      conversations,
      chatMessages,
      activeConversationId,
      userName,
      agents,
      selectedAgent,
      setConversations,
      setChatMessages,
      setActiveConversationId,
      setAgents,
      setSelectedAgent,
    }),
    [conversations, chatMessages, activeConversationId, userName, agents, selectedAgent]
  );

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
}

export const useChatContext = () => {
  const ctx = useContext(ChatContext);
  if (!ctx) throw new Error('useChatContext must be used inside ChatProvider');
  return ctx;
};
