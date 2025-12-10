'use client';

import { Circle } from 'lucide-react';

export default function TypingIndicator() {
  return (
    <div className="flex items-center gap-1">
      <Circle className="w-2 h-2 text-success animate-bounce [animation-delay:-0.3s]" />
      <Circle className="w-2 h-2 text-primary animate-bounce [animation-delay:-0.15s]" />
      <Circle className="w-2 h-2 text-danger animate-bounce" />
    </div>
  );
}
