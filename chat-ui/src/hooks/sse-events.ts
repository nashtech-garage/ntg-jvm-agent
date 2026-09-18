import type { ToolCallEvent } from '../models/tool-call-event';
import type { PendingQuestion } from '../models/user-question';

export type StreamHandlers<TComplete> = {
  onToken: (token: string) => void;
  onReasoning: (delta: string) => void;
  onToolCall: (event: ToolCallEvent) => void;
  onQuestion: (question: PendingQuestion) => void;
  onComplete: (final: TComplete) => void;
  onError: (message: string) => void;
};

export function parseSseFrame(frame: string): { event?: string; data?: string } {
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

export function dispatchSseEvent<TComplete>(
  parsed: { event?: string; data?: string },
  handlers: StreamHandlers<TComplete>,
  parseComplete: (raw: string) => TComplete
): boolean {
  if (!parsed.event) return false;

  switch (parsed.event) {
    case 'message': {
      if (parsed.data) handlers.onToken(parsed.data);
      return false;
    }
    case 'reasoning': {
      if (parsed.data) handlers.onReasoning(parsed.data);
      return false;
    }
    case 'complete': {
      if (!parsed.data) {
        handlers.onError('Empty completion payload');
        return true;
      }
      try {
        handlers.onComplete(parseComplete(parsed.data));
      } catch {
        handlers.onError('Invalid completion payload');
      }
      return true;
    }
    case 'tool': {
      if (!parsed.data) {
        handlers.onError('Empty tool event payload');
        return true;
      }
      try {
        handlers.onToolCall(JSON.parse(parsed.data) as ToolCallEvent);
      } catch {
        handlers.onError('Invalid tool event payload');
        return true;
      }
      return false;
    }
    case 'question': {
      if (!parsed.data) {
        handlers.onError('Empty question payload');
        return true;
      }
      try {
        handlers.onQuestion(JSON.parse(parsed.data) as PendingQuestion);
      } catch {
        handlers.onError('Invalid question payload');
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
