'use client';

import { useEffect, useRef } from 'react';
import Image from 'next/image';
import { ChatMessage } from '@/models/chat-message';
import RichTextPresenter from '@/components/ui/rich-text-presenter';
import TypingIndicator from '@/components/ui/typing-indicator';
import { Constants } from '@/constants/constant';
import { ThumbsUp, ThumbsDown } from 'lucide-react';
import { Reaction } from '@/types/reaction';
import clsx from 'clsx';

export default function ChatResult({
  results,
  isTyping = false,
  agentAvatar,
  agentName,
  onReaction,
}: Readonly<{
  results: ChatMessage[];
  isTyping: boolean;
  agentAvatar?: string;
  agentName?: string;
  onReaction?: (messageId: string, reaction: Reaction) => void;
}>) {
  const bottomRef = useRef<HTMLDivElement | null>(null);

  const formatTime = (value: string) => {
    try {
      return new Intl.DateTimeFormat('en', { hour: '2-digit', minute: '2-digit' }).format(
        new Date(value)
      );
    } catch {
      return '';
    }
  };

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [isTyping, results]);

  if (results.length === 0) {
    return (
      <div className="flex h-full flex-col items-center justify-center rounded-2xl border border-border bg-surface-muted px-6 py-12 text-center">
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-primary text-lg font-semibold text-primary-foreground">
          *
        </div>
        <p className="text-lg font-semibold text-foreground">Start the conversation</p>
        <p className="mt-1 max-w-md text-sm text-muted-foreground">
          Ask a question, drop in a few reference images, and let the agent craft a response in
          seconds.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {results.map((r) => (
        <div key={r.id}>
          {Constants.QUESTION_TYPE === r.type && (
            <div className="flex justify-end">
              <div className="max-w-[75%] rounded-2xl border border-primary-border bg-gradient-to-br from-primary-soft via-success-soft to-surface p-[1px] shadow-md shadow-[0_10px_26px_color-mix(in_oklab,var(--color-border)_60%,transparent)]">
                <div className="flex flex-col gap-3 rounded-2xl bg-surface px-4 py-3 text-foreground">
                  <div className="flex items-start justify-between gap-3">
                    <p className="font-semibold leading-relaxed text-foreground">{r.content}</p>
                    <span className="text-[11px] uppercase tracking-[0.18em] text-muted-foreground">
                      {formatTime(r.createdAt)}
                    </span>
                  </div>
                  {r.medias && r.medias.length > 0 && (
                    <div className="grid grid-cols-2 gap-2 md:grid-cols-3">
                      {r.medias.map((media) => (
                        <div
                          key={media.fileName}
                          className="overflow-hidden rounded-xl border border-border bg-surface-muted"
                        >
                          <Image
                            src={media.data}
                            alt={media.fileName}
                            width={200}
                            height={200}
                            className="h-full w-full object-cover transition duration-200 hover:scale-[1.02]"
                          />
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
          {Constants.ANSWER_TYPE === r.type && (
            <div className="flex items-start gap-3">
              <div
                className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-border bg-gradient-to-br from-surface-muted to-surface text-sm font-semibold text-primary-strong shadow-sm shadow-[0_6px_14px_color-mix(in_oklab,var(--color-border)_70%,transparent)]"
                aria-label="AI Assistant"
              >
                <div aria-hidden="true">AI</div>
              </div>
              <div className="max-w-[80%] rounded-2xl border border-border bg-surface px-4 py-3 text-foreground shadow-md shadow-[0_10px_26px_color-mix(in_oklab,var(--color-border)_60%,transparent)]">
                <RichTextPresenter content={r.content} />
                <p className="mt-3 flex items-center justify-between text-[11px] uppercase tracking-[0.18em] text-slate-500">
                  <span>Responded - {formatTime(r.createdAt)}</span>

                  <span className="flex items-center gap-2">
                    <button
                      onClick={() => onReaction?.(r.id, Reaction.LIKE)}
                      className={clsx('transition hover:text-sky-600', {
                        'text-sky-600': r.reaction === 'LIKE',
                      })}
                    >
                      <ThumbsUp size={16} strokeWidth={2} />
                    </button>
                    <button
                      onClick={() => onReaction?.(r.id, Reaction.DISLIKE)}
                      className={clsx('transition hover:text-rose-600', {
                        'text-rose-600': r.reaction === 'DISLIKE',
                      })}
                    >
                      <ThumbsDown size={16} strokeWidth={2} />
                    </button>
                  </span>
                </p>
              </div>
            </div>
          )}
        </div>
      ))}
      {isTyping && (
        <div className="flex w-fit items-center gap-2 rounded-full border border-border bg-surface px-4 py-2 text-muted shadow-sm shadow-[0_6px_14px_color-mix(in_oklab,var(--color-border)_70%,transparent)]">
          {agentAvatar ? (
            <Image
              src={agentAvatar}
              alt={agentName || 'Agent'}
              width={24}
              height={24}
              className="h-6 w-6 rounded-full object-cover flex-shrink-0"
            />
          ) : (
            <div className="h-6 w-6 rounded-full bg-avatar flex items-center justify-center text-xs font-bold text-inverse flex-shrink-0">
              {agentName?.charAt(0)?.toUpperCase() || 'A'}
            </div>
          )}
          <TypingIndicator />
          <span className="text-xs text-muted-foreground">Thinking...</span>
        </div>
      )}
      <div ref={bottomRef} />
    </div>
  );
}
