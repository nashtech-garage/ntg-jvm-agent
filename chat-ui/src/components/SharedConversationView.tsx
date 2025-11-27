import React, { useEffect, useState } from 'react';

interface ChatMessage {
  id: string;
  content: string;
  createdAt: string;
  type: number;
  medias: Array<{
    id: string;
    fileName: string;
    contentType: string;
    fileSize: number;
  }>;
}

interface SharedConversationData {
  id: string;
  title: string;
  createdAt: string;
  sharedByUsername: string;
  messages: ChatMessage[];
}

interface SharedConversationViewProps {
  shareToken: string;
}

export default function SharedConversationView({
  shareToken,
}: SharedConversationViewProps) {
  const [data, setData] = useState<SharedConversationData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchSharedConversation = async () => {
      try {
        const response = await fetch(`/api/chat/shared/${shareToken}`);
        if (!response.ok) {
          const errorData = await response.json();
          throw new Error(errorData.message || 'Failed to load shared conversation');
        }
        const data = await response.json();
        setData(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'An error occurred');
      } finally {
        setLoading(false);
      }
    };

    fetchSharedConversation();
  }, [shareToken]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading shared conversation...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md">
          <h2 className="text-xl font-bold text-red-800 mb-2">Error</h2>
          <p className="text-red-700">{error}</p>
        </div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-600">Conversation not found</p>
      </div>
    );
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  };

  return (
    <div className="flex flex-col h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 p-4">
        <div className="max-w-4xl mx-auto">
          <h1 className="text-2xl font-bold text-gray-900">{data.title}</h1>
          <div className="flex gap-4 text-sm text-gray-600 mt-2">
            <span>Shared by: <strong>{data.sharedByUsername}</strong></span>
            <span>Created: {formatDate(data.createdAt)}</span>
          </div>
          <div className="mt-3 p-3 bg-blue-50 border border-blue-200 rounded">
            <p className="text-sm text-blue-800">
              📖 This is a shared conversation. Read-only view.
            </p>
          </div>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto">
        <div className="max-w-4xl mx-auto p-4 space-y-4">
          {data.messages.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-500">No messages in this conversation</p>
            </div>
          ) : (
            data.messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${message.type === 0 ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-xl px-4 py-3 rounded-lg ${
                    message.type === 0
                      ? 'bg-blue-500 text-white rounded-br-none'
                      : 'bg-white text-gray-900 border border-gray-200 rounded-bl-none'
                  }`}
                >
                  <p className="break-words whitespace-pre-wrap">
                    {message.content}
                  </p>
                  <div className="text-xs mt-2 opacity-70">
                    {formatDate(message.createdAt)}
                  </div>

                  {/* Media attachments */}
                  {message.medias && message.medias.length > 0 && (
                    <div className="mt-3 space-y-2">
                      {message.medias.map((media) => (
                        <div
                          key={media.id}
                          className="bg-black bg-opacity-20 rounded p-2 text-sm"
                        >
                          📎 {media.fileName} ({formatFileSize(media.fileSize)})
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Footer */}
      <div className="bg-white border-t border-gray-200 p-4">
        <div className="max-w-4xl mx-auto text-center text-sm text-gray-500">
          <p>This conversation is shared and read-only</p>
        </div>
      </div>
    </div>
  );
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}

