'use client';

import { Circle } from 'lucide-react';

export default function TypingIndicator() {
  return (
    <div className="flex items-center gap-1">
      <Circle className="w-2 h-2 text-green-400 animate-bounce [animation-delay:-0.3s]" />
      <Circle className="w-2 h-2 text-blue-400 animate-bounce [animation-delay:-0.15s]" />
      <Circle className="w-2 h-2 text-red-400 animate-bounce" />
    </div>
  );
}
