'use client';

import { useEffect, useRef, useState } from 'react';
import { Send, Images, CircleX } from 'lucide-react';
import TextareaAutosize from 'react-textarea-autosize';
import { FileSelectInfo } from '../models/file-select-info';
import Image from 'next/image';
import { useChatContext } from '../contexts/ChatContext';
import { Button } from './ui/button';

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
    <div className="flex flex-col gap-3 rounded-2xl border border-border bg-surface px-4 py-3 shadow-sm shadow-[0_6px_18px_color-mix(in_oklab,var(--color-border)_80%,transparent)]">
      {previewUrls.length > 0 && (
        <div className="flex flex-wrap gap-3">
          {previewUrls.map((item) => (
            <div
              key={item}
              className="relative overflow-hidden rounded-xl border border-border bg-surface-muted p-2 shadow-sm shadow-[0_4px_12px_color-mix(in_oklab,var(--color-border)_70%,transparent)]"
            >
              <Button
                variant="ghost"
                size="icon"
                onClick={() => handleRemovePreview(item)}
                className="absolute right-2 top-2 rounded-full bg-overlay p-1 text-inverse transition hover:bg-[color-mix(in_oklab,var(--color-overlay)_80%,transparent)]"
                aria-label="Remove image"
              >
                <CircleX size={14} />
              </Button>
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
        <Button
          type="button"
          variant="ghost"
          size="icon"
          onClick={() => fileInputRef.current?.click()}
          className="flex h-11 w-11 items-center justify-center rounded-xl border border-primary/30 bg-primary/5 text-primary shadow-inner shadow-primary/20 transition hover:border-primary/60 hover:bg-primary/10 hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/50 disabled:opacity-40"
          disabled={previewUrls.length === 3}
          aria-label="Attach images (up to 3)"
        >
          <Images size={18} />
        </Button>
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
          className="flex-1 resize-none rounded-xl border border-border bg-surface-muted px-3 py-3 text-sm text-foreground shadow-inner shadow-[0_0_0_1px_color-mix(in_oklab,var(--color-border)_80%,transparent)] outline-none transition placeholder:text-muted-foreground focus:border-primary-border focus:ring-2 focus:ring-primary-border"
        />
        <Button
          type="button"
          variant="ghost"
          onClick={handleSend}
          disabled={!input.trim() || !agents.length}
          className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-[0_10px_30px_color-mix(in_oklab,var(--color-primary)_28%,transparent)] transition hover:bg-primary-strong hover:shadow-[0_12px_34px_color-mix(in_oklab,var(--color-primary)_32%,transparent)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/50 disabled:opacity-60"
          aria-label="Send message"
        >
          <Send size={18} />
        </Button>
      </div>
      <div className="flex items-center justify-between px-1">
        <p className="text-xs text-muted-foreground">Enter to send - Shift + Enter for new line</p>
        {!agents.length && <p className="text-xs text-warning">No agent available yet</p>}
      </div>
    </div>
  );
}
