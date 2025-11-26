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
    <div className="flex flex-col gap-2 p-4 bg-white">
      {previewUrls && (
        <div className="flex-1 flex">
          {previewUrls.map((item) => (
            <div key={item} className="ml-20">
              <button
                onClick={() => handleRemovePreview(item)}
                className="bg-black/50 text-white rounded-full p-0"
              >
                <CircleX />
              </button>
              <Image
                src={item}
                width={200}
                height={20}
                alt="preview"
                className="max-h-40 rounded-md"
              />
            </div>
          ))}
        </div>
      )}
      <div className="flex items-end gap-2">
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="bg-green-200 text-gray-700 px-2 py-2 rounded-lg cursor-pointer disabled:opacity-50"
          disabled={previewUrls.length === 3}
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
          className="flex-1 resize-none rounded-lg border p-2 outline-none focus:ring focus:ring-blue-300"
        />
        <button
          onClick={handleSend}
          disabled={!input.trim() || !agents.length}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg disabled:opacity-50 cursor-pointer"
        >
          <Send size={18} />
        </button>
      </div>
    </div>
  );
}
