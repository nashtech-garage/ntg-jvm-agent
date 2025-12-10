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
import logger from '@/utils/logger';

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
    files: FileSelectInfo[],
    onToken: (token: string) => void,
    onComplete: (finalResult: ChatResponse) => void
  ): Promise<ChatResponse | Error> => {
    const formData = new FormData();
    formData.append('question', question);
    if (conversationId) {
      formData.append('conversationId', conversationId);
    }
    if (files?.length) {
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

    if (!res.body) {
      let errorMessage = `Request failed with status ${res.status}`;
      try {
        const errorText = await res.text();
        if (errorText) {
          errorMessage += `: ${errorText}`;
        }
      } catch (err) {
        logger.error('Failed to parse error response body', err);
      }
      toast.error(errorMessage);
      return new Error(errorMessage);
    }

    const reader = res.body.pipeThrough(new TextDecoderStream()).getReader();
    let buffer = '';

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      if (!value) continue;

      buffer += value;

      const events = buffer.split('\n\n');

      buffer = events.pop() || '';

      for (const event of events) {
        const lines = event.split('\n');
        let eventName = '';
        const dataLines: string[] = [];

        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventName = line.replace('event:', '').trim();
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5));
          }
        }

        const data = dataLines.join('\n');

        if (eventName === 'message') {
          onToken(data);
        }

        if (eventName === 'complete') {
          const finalJson = JSON.parse(data);
          onComplete(finalJson);
          return finalJson;
        }

        if (eventName === 'error') {
          toast.error(data || 'Unexpected server error');
          return new Error(data);
        }
      }
    }

    return new Error('Stream ended without complete event');
  };

  const handleTokenUpdate = (token: string) => {
    setChatMessages((prev) =>
      prev.map((msg) => (msg.id === 'streaming' ? { ...msg, content: msg.content + token } : msg))
    );
  };

  const handleFinalResponse = (finalResponse: ChatResponse) => {
    const { conversation, message } = finalResponse;

    // Update active conversation
    if (!activeConversationId) {
      setActiveConversationId(conversation.id);
      setConversations((prev) => [conversation, ...prev]);
      router.replace(`/c/${conversation.id}`);
    }

    if (message) {
      setChatMessages((prev) => prev.map((msg) => (msg.id === 'streaming' ? message : msg)));
    } else {
      setChatMessages((prev) => prev.filter((msg) => msg.id !== 'streaming'));
    }

    setIsTyping(false);
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

    const tempMessage = {
      id: 'streaming',
      content: '',
      medias: [],
      createdAt: new Date().toISOString(),
      type: 2,
    };

    setChatMessages((prev) => [...prev, tempMessage]);

    /* Call to Orchestrator */
    const result = await askQuestion(
      q,
      activeConversationId,
      files,
      handleTokenUpdate,
      handleFinalResponse
    );
    if (result instanceof Error) {
      toast.error(result.message);
      setIsTyping(false);
      setChatMessages((prev) => prev.filter((msg) => msg.id !== 'streaming'));
    }
  };

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <Header>
        <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-6 py-4">
          <div>
            <h2 className="text-xl font-semibold text-foreground">Ask anything</h2>
            <p className="text-sm text-muted-foreground">
              Share details, attach context, and get concise answers.
            </p>
          </div>
          <div className="flex flex-col items-end gap-2 sm:flex-row sm:items-center sm:gap-3">
            <div className="hidden items-center gap-2 rounded-full border border-border bg-surface px-3 py-1 text-xs font-medium text-success sm:flex">
              <span className="h-2 w-2 rounded-full bg-success shadow-[0_0_0_6px_color-mix(in_oklab,var(--color-success)_20%,transparent)]" />
              Live
            </div>
            <AgentDropdown />
          </div>
        </div>
      </Header>

      <main className="flex flex-1 min-h-0 flex-col bg-background">
        <div className="mx-auto flex h-full w-full max-w-6xl flex-1 flex-col gap-4 px-6 pb-5 pt-4">
          <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-border bg-surface shadow-[0_12px_30px_color-mix(in_oklab,var(--color-border)_65%,transparent)]">
            <div className="flex-1 overflow-y-auto px-5 pb-5 pt-4">
              <ChatResult
                results={chatMessages}
                isTyping={isTyping}
                agentAvatar={selectedAgent?.avatar}
                agentName={selectedAgent?.name}
              />
            </div>
          </div>
          <div className="rounded-xl border border-border bg-surface shadow-[0_12px_30px_color-mix(in_oklab,var(--color-border)_65%,transparent)]">
            <div className="px-4 pb-4 pt-3">
              <ChatBox onAsk={handleAsk} />
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
