'use client';

import { useEffect, useRef, useState } from 'react';
import { Send, Images, CircleX } from 'lucide-react';
import TextareaAutosize from 'react-textarea-autosize';
import { FileSelectInfo } from '../models/file-select-info';
import Image from 'next/image';
import { useChatContext } from '../contexts/ChatContext';

export default function ChatBox({
  onAsk,
}: Readonly<{
  onAsk: (q: string, files: FileSelectInfo[]) => void;
}>) {
  const { agents } = useChatContext();
  const [input, setInput] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrls, setPreviewUrls] = useState<string[]>([]);

  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const selectedImages = useRef<FileSelectInfo[]>([]);

  useEffect(() => {
    textareaRef.current?.focus();

    // Cleanup all URL only when component unmount
    return () => {
      selectedImages.current.forEach((img) => URL.revokeObjectURL(img.url));
    };
  }, []);

  // Create preview when file changed
  useEffect(() => {
    if (!selectedFile) {
      // TODO: Refactor this to avoid disabling the rule
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setPreviewUrls([]);
      selectedImages.current = [];
      return;
    }
    const url = URL.createObjectURL(selectedFile);
    setPreviewUrls((prev) => [...prev, url]);
    selectedImages.current = [
      ...selectedImages.current,
      {
        url,
        file: selectedFile,
      },
    ];
  }, [selectedFile]);

  const handleRemovePreview = (url: string) => {
    const previewFilterList = previewUrls.filter((i) => i !== url);
    const selectedImageFilterList = selectedImages.current.filter((i) => i.url !== url);
    selectedImages.current = selectedImageFilterList;
    setPreviewUrls(previewFilterList);

    // Cleanup image after removing
    URL.revokeObjectURL(url);
  };

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (input.trim()) {
      onAsk(input.trim(), selectedImages.current || []);
      setInput('');
      setSelectedFile(null);
      textareaRef.current?.focus();
      selectedImages.current = [];
      setPreviewUrls([]);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend(e);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
      // Reset the file input so the same file can be selected again
      e.target.value = '';
    }
  };

  return (
    <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm shadow-slate-200">
      {previewUrls.length > 0 && (
        <div className="flex flex-wrap gap-3">
          {previewUrls.map((item) => (
            <div
              key={item}
              className="relative overflow-hidden rounded-xl border border-slate-200 bg-slate-50 p-2 shadow-sm shadow-slate-200/70"
            >
              <button
                onClick={() => handleRemovePreview(item)}
                className="absolute right-2 top-2 rounded-full bg-black/50 p-1 text-white transition hover:bg-black/70"
                aria-label="Remove image"
              >
                <CircleX size={14} />
              </button>
              <Image
                src={item}
                width={200}
                height={120}
                alt="preview"
                className="h-24 w-28 rounded-lg object-cover"
              />
            </div>
          ))}
        </div>
      )}
      <div className="flex items-end gap-3">
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-slate-700 shadow-inner shadow-slate-200 transition hover:-translate-y-0.5 hover:border-sky-300 hover:text-sky-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-300 disabled:opacity-40"
          disabled={previewUrls.length === 3}
          aria-label="Attach images (up to 3)"
        >
          <Images size={18} />
        </button>
        <input
          type="file"
          ref={fileInputRef}
          className="hidden"
          accept="image/*"
          onChange={handleFileChange}
        />
        <TextareaAutosize
          ref={textareaRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Type your message..."
          minRows={1}
          maxRows={8}
          className="flex-1 resize-none rounded-xl border border-slate-200 bg-slate-50 px-3 py-3 text-sm text-slate-900 shadow-inner shadow-slate-200 outline-none transition placeholder:text-slate-400 focus:border-sky-300 focus:ring-2 focus:ring-sky-200"
        />
        <button
          onClick={handleSend}
          disabled={!input.trim() || !agents.length}
          className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-br from-sky-500 to-emerald-500 px-4 py-3 text-sm font-semibold text-white shadow-lg shadow-sky-200/70 transition hover:shadow-sky-300 disabled:opacity-60"
          aria-label="Send message"
        >
          <Send size={18} />
        </button>
      </div>
      <div className="flex items-center justify-between px-1">
        <p className="text-xs text-slate-500">Enter to send - Shift + Enter for new line</p>
        {!agents.length && <p className="text-xs text-amber-600">No agent available yet</p>}
      </div>
    </div>
  );
}
