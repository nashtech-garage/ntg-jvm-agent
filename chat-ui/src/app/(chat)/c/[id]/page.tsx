'use client';

import { useChatContext } from '@/contexts/ChatContext';
import ChatPage from '../../page';
import { useEffect } from 'react';
import { useParams } from 'next/navigation';
import { customizeFetch } from '@/utils/custom-fetch';
import { useToaster } from '@/contexts/ToasterContext';

export default function ConversationPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const { setChatMessages, setActiveConversationId } = useChatContext();
  const { showError } = useToaster();

  useEffect(() => {
    const fetchConversation = async () => {
      try {
        const res = await customizeFetch(`/api/chat?conversationId=${id}`);
        const messages = await res.json();
        setChatMessages(messages);
        setActiveConversationId(id);
      } catch (error) {
        showError(`Error fetching conversation: ${error}`);
      }
    };
    fetchConversation();
  }, [id, setChatMessages, setActiveConversationId, showError]);

  return <ChatPage />;
}
