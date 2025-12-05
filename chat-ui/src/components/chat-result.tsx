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
  agentAvatar,
  agentName,
}: Readonly<{
  results: ChatMessage[];
  isTyping: boolean;
  agentAvatar?: string;
  agentName?: string;
}>) {
  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    /* Scroll to bottom element when ask new question */
    if (isTyping) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [isTyping]);

  if (results.length === 0) {
    return <p className="text-gray-500">Search results will appear here ...</p>;
  }

  return (
    <div className="space-y-4">
      {results.map((r) => (
        <div key={r.id} className="">
          {Constants.QUESTION_TYPE === r.type && (
            <div className="flex justify-end">
              <div className="flex flex-col gap-2 bg-gray-100 text-blue-500 p-1 rounded-lg max-w-[75%]">
                <p className="font-medium">{r.content}</p>
                {r.medias && r.medias.length > 0 && (
                  <div>
                    {r.medias.map((media) => (
                      <Image
                        key={media.fileName}
                        src={media.data}
                        alt={media.fileName}
                        width={200}
                        height={200}
                        className="max-h-40 rounded-md"
                      />
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
          {Constants.ANSWER_TYPE === r.type && (
            <div>
              <RichTextPresenter content={r.content} />
            </div>
          )}
        </div>
      ))}
      {isTyping && (
        <div className="flex items-center gap-2 p-2 rounded-lg w-fit">
          {agentAvatar ? (
            <img
              src={agentAvatar}
              alt={agentName || 'Agent'}
              className="w-6 h-6 rounded-full object-cover flex-shrink-0"
            />
          ) : (
            <div className="w-6 h-6 rounded-full bg-blue-300 flex items-center justify-center text-xs font-bold text-white flex-shrink-0">
              {agentName?.charAt(0)?.toUpperCase() || 'A'}
            </div>
          )}
          <TypingIndicator />
        </div>
      )}
      <div ref={bottomRef} />
    </div>
  );
}
