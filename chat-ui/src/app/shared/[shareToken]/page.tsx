'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import ChatResult from '@/app/components/ChatResult';
import { ChatResponse } from '@/app/models/chat-response';
import { toast } from 'sonner';
import { ChevronLeft } from 'lucide-react';
import Link from 'next/link';

interface SharedConversation {
  id: string;
  title: string;
  createdAt: string;
  sharedByUsername: string;
  messages: any[];
}

export default function SharedConversationPage() {
  const params = useParams();
  const shareToken = params.shareToken as string;
  const [conversation, setConversation] = useState<SharedConversation | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchSharedConversation = async () => {
      try {
        const res = await fetch(`/api/chat/shared/${shareToken}`);
        if (!res.ok) {
          const error = await res.json();
          toast.error(error.error || 'Failed to load shared conversation');
          setIsLoading(false);
          return;
        }

        const data = await res.json();
        setConversation(data);
      } catch (error) {
        toast.error(`Error loading conversation: ${error}`);
      } finally {
        setIsLoading(false);
      }
    };

    fetchSharedConversation();
  }, [shareToken]);

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <div className="mb-4 h-8 w-8 animate-spin rounded-full border-4 border-blue-600 border-t-transparent mx-auto"></div>
          <p>Loading shared conversation...</p>
        </div>
      </div>
    );
  }

  if (!conversation) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <p className="mb-4 text-lg font-semibold">Conversation not found or has expired</p>
          <Link
            href="/login"
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <ChevronLeft size={16} />
            Back to Login
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col">
      <div className="border-b border-gray-200 bg-white px-6 py-4">
        <div className="flex items-center gap-4">
          <Link href="/login" className="text-gray-600 hover:text-gray-900">
            <ChevronLeft size={24} />
          </Link>
          <div className="flex-1">
            <h1 className="text-xl font-semibold">{conversation.title}</h1>
            <p className="text-sm text-gray-500">
              Shared by {conversation.sharedByUsername}
            </p>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        <ChatResult results={conversation.messages} isTyping={false} />
      </div>
    </div>
  );
}

