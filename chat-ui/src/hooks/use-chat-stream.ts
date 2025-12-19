'use client';

import { useCallback, useRef, useState } from 'react';
import { ChatResponse } from '@/models/chat-response';
import { FileSelectInfo } from '@/models/file-select-info';
import { customizeFetch } from '@/utils/custom-fetch';

type StreamHandlers<TComplete> = {
  onToken: (token: string) => void;
  onComplete: (final: TComplete) => void;
  onError: (message: string) => void;
};

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

function parseSseFrame(frame: string): { event?: string; data?: string } {
  let event: string | undefined;
  const dataLines: string[] = [];

  for (const line of frame.split('\n')) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5));
    }
  }

  return {
    event,
    data: dataLines.length ? dataLines.join('\n') : undefined,
  };
}

function dispatchSseEvent<TComplete>(
  parsed: { event?: string; data?: string },
  handlers: StreamHandlers<TComplete>,
  parseComplete: (raw: string) => TComplete
): boolean {
  if (!parsed.event) return false;

  switch (parsed.event) {
    case 'message': {
      if (parsed.data) {
        handlers.onToken(parsed.data);
      }
      return false;
    }

    case 'complete': {
      if (!parsed.data) {
        handlers.onError('Empty completion payload');
        return true;
      }

      try {
        const parsedData = parseComplete(parsed.data);
        handlers.onComplete(parsedData);
      } catch {
        handlers.onError('Invalid completion payload');
      }
      return true;
    }

    case 'error': {
      handlers.onError(parsed.data ?? 'Unexpected server error');
      return true;
    }

    default:
      return false;
  }
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

  const ask = useCallback(
    async (
      params: {
        question: string;
        conversationId: string | null;
        files: FileSelectInfo[];
        agentId?: string;
      },
      handlers: StreamHandlers<ChatResponse>
    ) => {
      abort(); // cancel any previous stream
      setIsStreaming(true);

      const controller = new AbortController();
      controllerRef.current = controller;

      try {
        const formData = buildChatFormData(
          params.question,
          params.conversationId,
          params.files,
          params.agentId
        );

        const res = await customizeFetch('/api/chat', {
          method: 'POST',
          body: formData,
          signal: controller.signal,
        });

        if (!res.body) {
          throw new Error(`Request failed with status ${res.status}`);
        }

        const reader = res.body.pipeThrough(new TextDecoderStream()).getReader();

        await parseSseStream<ChatResponse>(
          reader,
          {
            onToken: handlers.onToken,
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

  return {
    ask,
    abort,
    isStreaming,
  };
}
