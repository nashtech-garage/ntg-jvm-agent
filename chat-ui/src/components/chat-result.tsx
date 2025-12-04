'use client';

import { useEffect, useRef } from 'react';
import Image from 'next/image';
import { ChatMessage } from '@/models/chat-message';
import RichTextPresenter from '@/components/ui/rich-text-presenter';
import TypingIndicator from '@/components/ui/typing-indicator';
import { Constants } from '@/constants/constant';

export default function ChatResult({
  results,
  isTyping = false,
}: Readonly<{
  results: ChatMessage[];
  isTyping: boolean;
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
      <div className="flex h-full flex-col items-center justify-center rounded-2xl border border-slate-200 bg-slate-50 px-6 py-12 text-center shadow-inner shadow-slate-200">
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-sky-500 to-emerald-500 text-lg font-semibold text-white shadow-lg shadow-emerald-300/40">
          *
        </div>
        <p className="text-lg font-semibold text-slate-900">Start the conversation</p>
        <p className="mt-1 max-w-md text-sm text-slate-600">
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
              <div className="max-w-[75%] rounded-2xl border border-sky-100 bg-gradient-to-br from-sky-50 via-emerald-50 to-white p-[1px] shadow-md shadow-slate-200/70">
                <div className="flex flex-col gap-3 rounded-2xl bg-white px-4 py-3 text-slate-800">
                  <div className="flex items-start justify-between gap-3">
                    <p className="font-semibold leading-relaxed text-slate-900">{r.content}</p>
                    <span className="text-[11px] uppercase tracking-[0.18em] text-slate-500">
                      {formatTime(r.createdAt)}
                    </span>
                  </div>
                  {r.medias && r.medias.length > 0 && (
                    <div className="grid grid-cols-2 gap-2 md:grid-cols-3">
                      {r.medias.map((media) => (
                        <div
                          key={media.fileName}
                          className="overflow-hidden rounded-xl border border-slate-200 bg-slate-50"
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
              <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-gradient-to-br from-slate-100 to-white text-sm font-semibold text-sky-600 shadow-sm shadow-slate-200">
                AI
              </div>
              <div className="max-w-[80%] rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-800 shadow-md shadow-slate-200/70">
                <RichTextPresenter content={r.content} />
                <p className="mt-3 text-[11px] uppercase tracking-[0.18em] text-slate-500">
                  Responded - {formatTime(r.createdAt)}
                </p>
              </div>
            </div>
          )}
        </div>
      ))}
      {isTyping && (
        <div className="flex w-fit items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2 text-slate-700 shadow-sm shadow-slate-200">
          <TypingIndicator />
          <span className="text-xs text-slate-500">Thinking...</span>
        </div>
      )}
      <div ref={bottomRef} />
    </div>
  );
}
