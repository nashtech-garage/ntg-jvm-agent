"use client";

import { useState } from "react";
import { Send } from "lucide-react";

export default function SearchBox({
  onSearch,
}: Readonly<{
  onSearch: (q: string) => void;
}>) {
  const [input, setInput] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (input.trim()) {
      onSearch(input.trim());
      setInput("");
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="flex items-center gap-2 p-3 border-t"
    >
      <input
        type="text"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        placeholder="Type your question..."
        className="flex-1 px-3 py-2 rounded border border-gray-300 focus:outline-none"
      />
      <button
        type="submit"
        className="p-2 bg-blue-600 text-white rounded hover:bg-blue-500"
      >
        <Send size={18} />
      </button>
    </form>
  );
}