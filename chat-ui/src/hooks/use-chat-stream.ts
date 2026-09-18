'use client';

import { useCallback, useRef, useState } from 'react';
import { ChatResponse } from '@/models/chat-response';
import { FileSelectInfo } from '@/models/file-select-info';
import { customizeFetch } from '@/utils/custom-fetch';
import { QuestionAnswers } from '@/models/user-question';
import { dispatchSseEvent, parseSseFrame, type StreamHandlers } from '@/hooks/sse-events';

function buildChatFormData(
  question: string,
  conversationId: string | null,
  files: FileSelectInfo[],
  agentId?: string
): FormData {
  const formData = new FormData();
  formData.append('question', question);

  if (conversationId) {
    formData.append('conversationId', conversationId);
  }

  if (files?.length) {
    files.forEach((f) => formData.append('files', f.file));
  }

  if (agentId) {
    formData.append('agentId', agentId);
  }

  return formData;
}

async function parseSseStream<TComplete>(
  reader: ReadableStreamDefaultReader<string>,
  handlers: StreamHandlers<TComplete>,
  parseComplete: (raw: string) => TComplete
) {
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    if (!value) continue;

    buffer += value;

    const frames = buffer.split('\n\n');
    buffer = frames.pop() ?? '';

    for (const frame of frames) {
      const parsed = parseSseFrame(frame);
      const shouldStop = dispatchSseEvent(parsed, handlers, parseComplete);

      if (shouldStop) return;
    }
  }

  handlers.onError('Stream ended without completion');
}

export function useChatStream() {
  const controllerRef = useRef<AbortController | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);

  const abort = useCallback(() => {
    controllerRef.current?.abort();
    controllerRef.current = null;
    setIsStreaming(false);
  }, []);

  const consume = useCallback(
    async (request: () => Promise<Response>, handlers: StreamHandlers<ChatResponse>) => {
      abort(); // cancel any previous stream
      setIsStreaming(true);

      const controller = new AbortController();
      controllerRef.current = controller;

      try {
        const res = await request();

        if (!res.body) {
          throw new Error(`Request failed with status ${res.status}`);
        }

        const reader = res.body.pipeThrough(new TextDecoderStream()).getReader();

        await parseSseStream<ChatResponse>(
          reader,
          {
            onToken: handlers.onToken,
            onReasoning: handlers.onReasoning,
            onToolCall: handlers.onToolCall,
            onQuestion: handlers.onQuestion,
            onComplete: handlers.onComplete,
            onError: handlers.onError,
          },
          (raw) => JSON.parse(raw) as ChatResponse
        );
      } catch (err: unknown) {
        if (err instanceof DOMException && err.name === 'AbortError') {
          return;
        }

        if (err instanceof Error) {
          handlers.onError(err.message);
        } else {
          handlers.onError('Streaming failed');
        }
      } finally {
        setIsStreaming(false);
        controllerRef.current = null;
      }
    },
    [abort]
  );

  const ask = useCallback(
    async (
      params: {
        question: string;
        conversationId: string | null;
        files: FileSelectInfo[];
        agentId?: string;
      },
      handlers: StreamHandlers<ChatResponse>
    ) =>
      consume(() => {
        const formData = buildChatFormData(
          params.question,
          params.conversationId,
          params.files,
          params.agentId
        );
        return customizeFetch('/api/chat', {
          method: 'POST',
          body: formData,
          signal: controllerRef.current?.signal,
        });
      }, handlers),
    [consume]
  );

  const answer = useCallback(
    async (
      params: { conversationId: string; questionId: string; answers: QuestionAnswers },
      handlers: StreamHandlers<ChatResponse>
    ) =>
      consume(
        () =>
          customizeFetch(`/api/chat/${params.conversationId}/answer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ questionId: params.questionId, answers: params.answers }),
            signal: controllerRef.current?.signal,
          }),
        handlers
      ),
    [consume]
  );

  return {
    ask,
    answer,
    abort,
    isStreaming,
  };
}
