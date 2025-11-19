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
    <div className="flex h-screen">
      <main className="flex-1 flex flex-col">
        <div>
          <Header>
            <AgentDropdown />
          </Header>
        </div>
        <div className="flex-1 overflow-y-auto p-4">
          <ChatResult results={chatMessages} isTyping={isTyping} />
        </div>
        <ChatBox onAsk={handleAsk} />
      </main>
    </div>
  );
}
