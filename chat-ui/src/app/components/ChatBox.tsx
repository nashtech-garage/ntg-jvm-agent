'use client';

import { useEffect, useRef, useState } from 'react';
import { Send } from 'lucide-react';
import TextareaAutosize from 'react-textarea-autosize';

export default function ChatBox({
  onAsk,
}: Readonly<{
  onAsk: (q: string) => void;
}>) {
  const [input, setInput] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    textareaRef.current?.focus();
  }, []);

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (input.trim()) {
      onAsk(input.trim());
      setInput('');
      textareaRef.current?.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend(e);
    }
  };

  return (
    <div className="flex items-end gap-2 p-4 bg-white">
      <TextareaAutosize
        ref={textareaRef}
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Type your message..."
        minRows={1}
        maxRows={8}
        className="flex-1 resize-none rounded-lg border p-2 outline-none focus:ring focus:ring-blue-300"
      />
      <button
        onClick={handleSend}
        disabled={!input.trim()}
        className="bg-blue-600 text-white px-4 py-2 rounded-lg disabled:opacity-50 cursor-pointer"
      >
        <Send size={18} />
      </button>
    </div>
  );
}
