'use client';

import { useEffect, useState } from 'react';
import Sidebar from './components/Sidebar';
import ChatBox from './components/ChatBox';
import ChatResult from './components/ChatResult';
import { ChatResponse } from '@/app/models/chat-response';
import { ChatMessage } from './models/chat-message';
import { Conversation } from './models/conversation';
import { toast } from 'sonner';
import { Constants } from './utils/constant';

export default function Page() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [userName, setUserName] = useState<string>('');
  const [isTyping, setIsTyping] = useState<boolean>(false);

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

  const askQuestion = async (
    question: string,
    conversationId: string | null
  ): Promise<ChatResponse | Error> => {
    const res = await fetch(`/api/chat`, {
      method: 'POST',
      body: JSON.stringify({ question, conversationId: conversationId }),
    });
    const jsonResult = await res.json();
    if (!res.ok) {
      return new Error(jsonResult.error);
    }
    return jsonResult;
  };

  const handleAsk = async (q: string) => {
    setIsTyping(true);
    const question = {
      id: `${Date.now()}`,
      content: q,
      createdAt: new Date().toISOString(),
      type: 1,
    };
    /* Show question in screen immediately */
    setChatMessages([...chatMessages, question]);

    /* Call to Orchestrator */
    const result = await askQuestion(q, activeId);

    setIsTyping(false);
    if (result instanceof Error) {
      toast.error(result.message);
      return;
    }

    const { conversation, message: answer } = result;

    setConversations((prev: Conversation[]) => {
      if (activeId === null) {
        // Create new conversation from first chat
        setActiveId(conversation.id);
        setChatMessages([...chatMessages, question, answer]);
        return [conversation, ...prev];
      } else {
        setChatMessages([...chatMessages, question, answer]);
        return [...prev];
      }
    });
  };

  const removeConversation = async (id: string) => {
    const res = await fetch(`/api/chat?conversationId=${id}`, {
      method: 'DELETE',
    });
    const jsonResult = await res.json();
    if (!res.ok) {
      toast.error(jsonResult.error);
      return;
    }

    setConversations((prev) => prev.filter((s) => s.id !== id));
    if (activeId === id) setActiveId(null);
    setChatMessages([]);
    toast.success(Constants.DELETE_CONVERSATION_SUCCESS_MSG);
  };

  const setActiveConversation = async (id: string) => {
    const res = await fetch(`/api/chat?conversationId=${id}`);
    const jsonResult = await res.json();
    if (!res.ok) {
      toast.error(jsonResult.error);
      return;
    }
    setActiveId(id);
    setChatMessages(jsonResult);
  };

  const newChat = () => {
    setActiveId(null);
    setChatMessages([]);
  };

  return (
    <div className="flex h-screen">
      <Sidebar
        conversations={conversations}
        activeConversationId={activeId}
        userName={userName}
        setActiveConversation={setActiveConversation}
        removeConversation={removeConversation}
        newChat={newChat}
      />
      <main className="flex-1 flex flex-col">
        <div className="flex-1 overflow-y-auto p-4">
          <ChatResult results={chatMessages} isTyping={isTyping} />
        </div>
        <ChatBox onAsk={handleAsk} />
      </main>
    </div>
  );
}
