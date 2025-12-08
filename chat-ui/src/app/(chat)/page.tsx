'use client';

import { useState } from 'react';
import ChatBox from '@/components/chat-box';
import ChatResult from '@/components/chat-result';
import { ChatResponse } from '@/models/chat-response';
import { toast } from 'sonner';
import { useRouter } from 'next/navigation';
import { useChatContext } from '@/contexts/ChatContext';
import { FileSelectInfo } from '@/models/file-select-info';
import { customizeFetch } from '@/utils/custom-fetch';
import Header from '@/components/ui/header';
import AgentDropdown from '@/components/agent-dropdown';

export default function Page() {
  const {
    chatMessages,
    activeConversationId,
    selectedAgent,
    setChatMessages,
    setConversations,
    setActiveConversationId,
  } = useChatContext();
  const [isTyping, setIsTyping] = useState<boolean>(false);
  const router = useRouter();

  const askQuestion = async (
    question: string,
    conversationId: string | null,
    files: FileSelectInfo[]
  ): Promise<ChatResponse | Error> => {
    const formData = new FormData();
    formData.append('question', question);
    if (conversationId) {
      formData.append('conversationId', conversationId);
    }
    if (files && files.length) {
      for (const file of files) {
        formData.append('files', file.file);
      }
    }
    if (selectedAgent?.id) {
      formData.append('agentId', selectedAgent.id);
    }

    const res = await customizeFetch(`/api/chat`, {
      method: 'POST',
      body: formData,
    });
    const jsonResult = await res.json();
    if (!res.ok) {
      return new Error(jsonResult.error);
    }
    return jsonResult;
  };

  const handleAsk = async (q: string, files: FileSelectInfo[]) => {
    setIsTyping(true);
    const question = {
      id: `${Date.now()}`,
      content: q,
      medias: files.map((f) => ({
        contentType: f.file.type,
        data: f.url,
        fileName: f.file.name,
      })),
      createdAt: new Date().toISOString(),
      type: 1,
    };
    /* Show question in screen immediately */
    setChatMessages([...chatMessages, question]);

    /* Call to Orchestrator */
    const result = await askQuestion(q, activeConversationId, files);

    setIsTyping(false);
    if (result instanceof Error) {
      toast.error(result.message);
      return;
    }

    const { conversation, message: answer } = result;

    if (!activeConversationId) {
      setActiveConversationId(conversation.id);
      setConversations((prev) => [conversation, ...prev]);
      router.replace(`/c/${conversation.id}`);
    }
    setChatMessages((prev) => [...prev, answer]);
  };

  return (
    <div className="flex h-screen">
      <main className="flex-1 flex flex-col">
        <div>
          <Header>
            <AgentDropdown />
          </Header>
        </div>
        <div className="flex-1 overflow-y-auto p-4">
          <ChatResult
            results={chatMessages}
            isTyping={isTyping}
            agentAvatar={selectedAgent?.avatar}
            agentName={selectedAgent?.name}
          />
        </div>
        <ChatBox onAsk={handleAsk} />
      </main>
    </div>
  );
}
