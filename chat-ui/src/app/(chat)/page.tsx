'use client';

import { useRef, useState } from 'react';
import ChatBox from '@/components/chat-box';
import ChatResult from '@/components/chat-result';
import { ChatResponse } from '@/models/chat-response';
import { ChatMessage } from '@/models/chat-message';
import { useRouter } from 'next/navigation';
import { useChatContext } from '@/contexts/ChatContext';
import { FileSelectInfo } from '@/models/file-select-info';
import Header from '@/components/ui/header';
import AgentDropdown from '@/components/agent-dropdown';
import logger from '@/utils/logger';
import { Constants } from '@/constants/constant';
import { REACTION_PATH } from '@/constants/url';
import { Reaction } from '@/types/reaction';
import { useToaster } from '@/contexts/ToasterContext';
import { useChatStream } from '@/hooks/use-chat-stream';
import { customizeFetch } from '@/utils/custom-fetch';
import { ToolCallEvent } from '@/models/tool-call-event';
import { PendingQuestion, QuestionAnswers } from '@/models/user-question';

function buildQuestionMessage(q: string, files: FileSelectInfo[]) {
  return {
    id: `${Date.now()}`,
    content: q,
    medias: files.map((f) => ({
      contentType: f.file.type,
      data: f.url,
      fileName: f.file.name,
    })),
    createdAt: new Date().toISOString(),
    type: Constants.QUESTION_TYPE,
    reaction: Reaction.NONE,
  };
}

function cleanupStreamingMessage(
  setChatMessages: React.Dispatch<React.SetStateAction<ChatMessage[]>>
) {
  setChatMessages((prev) => prev.filter((m) => m.id !== 'streaming'));
}

export default function Page() {
  const { ask, answer, isStreaming } = useChatStream();

  const {
    chatMessages,
    activeConversationId,
    selectedAgent,
    setChatMessages,
    setConversations,
    setActiveConversationId,
    toolCalls,
    todoItems,
    reasoning,
    pendingQuestion,
    setToolCalls,
    setTodoItems,
    setReasoning,
    setPendingQuestion,
    clearAgentActivity,
  } = useChatContext();
  const [isTyping, setIsTyping] = useState<boolean>(false);
  const router = useRouter();
  const { showError } = useToaster();
  const hasStartedStreamingRef = useRef(false);
  const lastUserQuestionRef = useRef('');

  const handleTokenUpdate = (token: string) => {
    if (!hasStartedStreamingRef.current) {
      hasStartedStreamingRef.current = true;

      setIsTyping(false);

      setChatMessages((prev) => [
        ...prev,
        {
          id: 'streaming',
          content: token,
          medias: [],
          createdAt: new Date().toISOString(),
          type: Constants.ANSWER_TYPE,
          reaction: Reaction.NONE,
        },
      ]);

      return;
    }

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

  const handleToolCall = (event: ToolCallEvent) => {
    if (event.todoItems) {
      setTodoItems(event.todoItems);
    }

    setToolCalls((previous) => {
      const index = previous.findIndex((toolCall) => toolCall.id === event.id);
      if (index === -1) return [...previous, event];

      return previous.map((toolCall, currentIndex) => (currentIndex === index ? event : toolCall));
    });
  };

  const handleReasoning = (delta: string) => {
    setReasoning((previous) => previous + delta);
  };

  const handleQuestion = (question: PendingQuestion) => {
    cleanupStreamingMessage(setChatMessages);
    setPendingQuestion(question);
    setIsTyping(false);

    if (!activeConversationId && question.conversationId) {
      setActiveConversationId(question.conversationId);
      setConversations((previous) => [
        {
          id: question.conversationId,
          title: lastUserQuestionRef.current || 'Clarification needed',
          createdAt: new Date().toISOString(),
        },
        ...previous,
      ]);
      router.replace(`/c/${question.conversationId}`);
    }
  };

  const streamHandlers = {
    onToken: handleTokenUpdate,
    onReasoning: handleReasoning,
    onToolCall: handleToolCall,
    onQuestion: handleQuestion,
    onComplete: handleFinalResponse,
    onError: (message: string) => {
      showError(message);
      cleanupStreamingMessage(setChatMessages);
    },
  };

  const handleAsk = async (q: string, files: FileSelectInfo[]) => {
    lastUserQuestionRef.current = q;
    hasStartedStreamingRef.current = false;
    setIsTyping(true);
    clearAgentActivity();
    setPendingQuestion(null);

    // Show question immediately
    const questionMessage = buildQuestionMessage(q, files);
    setChatMessages((prev) => [...prev, questionMessage]);

    try {
      // Start streaming
      await ask(
        {
          question: q,
          conversationId: activeConversationId,
          files,
          agentId: selectedAgent?.id,
        },
        streamHandlers
      );
    } finally {
      // Always reset typing state
      setIsTyping(false);
    }
  };

  const handleAnswerQuestion = async (answers: QuestionAnswers) => {
    if (!pendingQuestion?.conversationId) return;

    const currentPending = pendingQuestion;
    const answerText = Object.values(answers).join('; ');
    const optimisticAnswer = buildQuestionMessage(answerText, []);
    hasStartedStreamingRef.current = false;
    setPendingQuestion(null);
    setIsTyping(true);
    setChatMessages((previous) => [...previous, optimisticAnswer]);

    try {
      await answer(
        {
          conversationId: currentPending.conversationId,
          questionId: currentPending.id,
          answers,
        },
        {
          ...streamHandlers,
          onError: (message: string) => {
            setPendingQuestion(currentPending);
            showError(message);
            setChatMessages((previous) =>
              previous.filter(
                (chatMessage) =>
                  chatMessage.id !== 'streaming' && chatMessage.id !== optimisticAnswer.id
              )
            );
          },
        }
      );
    } finally {
      setIsTyping(false);
    }
  };

  const handleReaction = async (messageId: string, reaction: Reaction) => {
    const current = chatMessages.find((m) => m.id === messageId)?.reaction ?? Reaction.NONE;
    const newReaction = current === reaction ? Reaction.NONE : reaction;

    setChatMessages((prev) =>
      prev.map((m) => (m.id === messageId ? { ...m, reaction: newReaction } : m))
    );
    try {
      const response = await customizeFetch(REACTION_PATH.CHAT_MESSAGE_REACTION(messageId), {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reaction: newReaction }),
      });

      if (!response.ok) {
        const errorResult = await response.json();
        showError(errorResult.error || 'Failed to update reaction');
      }
    } catch (err) {
      logger.error('Failed to react:', err);
      setChatMessages((prev) =>
        prev.map((m) => (m.id === messageId ? { ...m, reaction: current } : m))
      );
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
              <span>Live</span>
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
                toolCalls={toolCalls}
                todoItems={todoItems}
                reasoning={reasoning}
                isStreaming={isStreaming}
                pendingQuestion={pendingQuestion}
                onAnswerQuestion={handleAnswerQuestion}
                onReaction={handleReaction}
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
