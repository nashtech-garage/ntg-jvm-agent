'use client';

import { useChatContext } from '@/contexts/ChatContext';
import ChatPage from '../../page';
import { useEffect } from 'react';
import { useParams } from 'next/navigation';
import { customizeFetch } from '@/utils/custom-fetch';
import { useToaster } from '@/contexts/ToasterContext';
import { shouldClearAgentActivity } from '@/utils/agent-activity';

export default function ConversationPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const {
    activeConversationId,
    setChatMessages,
    setActiveConversationId,
    setPendingQuestion,
    clearAgentActivity,
  } = useChatContext();
  const { showError } = useToaster();

  useEffect(() => {
    if (shouldClearAgentActivity(activeConversationId, id)) {
      clearAgentActivity();
    }
  }, [activeConversationId, clearAgentActivity, id]);

  useEffect(() => {
    const fetchConversation = async () => {
      try {
        const [res, pendingRes] = await Promise.all([
          customizeFetch(`/api/chat?conversationId=${id}`),
          customizeFetch(`/api/chat/${id}/pending-question`),
        ]);
        const messages = await res.json();
        setChatMessages(messages);
        setPendingQuestion(pendingRes.status === 204 ? null : await pendingRes.json());
        setActiveConversationId(id);
      } catch (error) {
        showError(`Error fetching conversation: ${error}`);
      }
    };
    fetchConversation();
  }, [id, setChatMessages, setActiveConversationId, setPendingQuestion, showError]);

  return <ChatPage />;
}
