'use client';

import { useChatContext } from '@/app/contexts/ChatContext';
import ChatPage from '../../page';
import { useEffect } from 'react';
import { toast } from 'sonner';
import { useParams } from 'next/navigation';

export default function ConversationPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const { setChatMessages, setActiveConversationId } = useChatContext();

  useEffect(() => {
    const fetchConversation = async () => {
      try {
        const res = await fetch(`/api/chat?conversationId=${id}`);
        const messages = await res.json();
        setChatMessages(messages);
        setActiveConversationId(id);
      } catch (error) {
        toast.error(`Error fetching conversation: ${error}`);
      }
    };
    fetchConversation();
  }, [id, setChatMessages, setActiveConversationId]);

  return <ChatPage />;
}
