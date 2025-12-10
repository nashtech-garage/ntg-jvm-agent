'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import ChatResult from '@/components/chat-result';
import { ChatMessage } from '@/models/chat-message';
import { toast } from 'sonner';
import { ChevronLeft } from 'lucide-react';
import Link from 'next/link';

interface SharedConversation {
  id: string;
  title: string;
  createdAt: string;
  sharedByUsername: string;
  messages: ChatMessage[];
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
          const errorData = (await res.json()) as { error?: string };
          toast.error(errorData.error || 'Failed to load shared conversation');
          setIsLoading(false);
          return;
        }

        const data = (await res.json()) as SharedConversation;
        setConversation(data);
      } catch (error) {
        toast.error(
          `Error loading conversation: ${error instanceof Error ? error.message : 'Unknown error'}`
        );
      } finally {
        setIsLoading(false);
      }
    };

    fetchSharedConversation();
  }, [shareToken]);

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="text-center">
          <div className="mb-4 h-8 w-8 animate-spin rounded-full border-4 border-primary-strong border-t-transparent mx-auto"></div>
          <p className="text-muted-foreground">Loading shared conversation...</p>
        </div>
      </div>
    );
  }

  if (!conversation) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="text-center">
          <p className="mb-4 text-lg font-semibold text-foreground">
            Conversation not found or has expired
          </p>
          <Link
            href="/login"
            className="inline-flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:bg-primary-strong"
          >
            <ChevronLeft size={16} />
            Back to Login
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col bg-background">
      <div className="border-b border-border bg-surface px-6 py-4 shadow-[0_10px_26px_color-mix(in_oklab,var(--color-border)_60%,transparent)]">
        <div className="flex items-center gap-4">
          <Link href="/login" className="text-muted-foreground hover:text-foreground">
            <ChevronLeft size={24} />
          </Link>
          <div className="flex-1">
            <h1 className="text-xl font-semibold text-foreground">{conversation.title}</h1>
            <p className="text-sm text-muted-foreground">
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
