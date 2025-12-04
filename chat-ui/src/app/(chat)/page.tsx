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
    <div className="flex min-h-screen flex-col bg-gray-100">
      <main className="flex min-h-screen flex-col">
        <Header>
          <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-6 py-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-gray-500">
                NT Agent
              </p>
              <h2 className="text-xl font-semibold text-gray-900">Ask anything</h2>
              <p className="text-sm text-gray-600">
                Share details, attach context, and get concise answers.
              </p>
            </div>
            <div className="flex flex-col items-end gap-2 sm:flex-row sm:items-center sm:gap-3">
              <div className="hidden items-center gap-2 rounded-full border border-gray-200 bg-white px-3 py-1 text-xs font-medium text-emerald-600 shadow-sm sm:flex">
                <span className="h-2 w-2 rounded-full bg-emerald-500 shadow-[0_0_0_6px_rgba(16,185,129,0.2)]" />
                Live
              </div>
              <AgentDropdown />
            </div>
          </div>
        </Header>

        <div className="flex-1 bg-gray-100">
          <div className="mx-auto flex h-full min-h-[calc(100vh-96px)] w-full max-w-6xl flex-col gap-4 px-6 pb-5 pt-4">
            <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white">
              <div className="flex-1 overflow-y-auto px-5 pb-5 pt-4">
                <ChatResult results={chatMessages} isTyping={isTyping} />
              </div>
            </div>
            <div className="rounded-xl border border-gray-200 bg-white">
              <div className="px-4 pb-4 pt-3">
                <ChatBox onAsk={handleAsk} />
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
