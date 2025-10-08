import { useEffect, useRef } from 'react';
import { ChatMessage } from '../models/chat-message';
import RichTextPresenter from './RichTextPresenter';
import TypingIndicator from './TypingIndicator';
import { Constants } from '../utils/constant';

export default function ChatResult({
  results,
  isTyping = false,
}: Readonly<{
  results: ChatMessage[];
  isTyping: boolean;
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
              <div className="inline-flex bg-gray-100 text-blue-500 p-1 rounded-lg max-w-[75%]">
                <p className="font-medium">{r.content}</p>
              </div>
            </div>
          )}
          {Constants.ANSWER_TYPE === r.type && <RichTextPresenter content={r.content} />}
        </div>
      ))}
      {isTyping && (
        <div className="flex items-center gap-2 p-2 rounded-lg w-fit">
          <TypingIndicator />
        </div>
      )}
      <div ref={bottomRef} />
    </div>
  );
}
