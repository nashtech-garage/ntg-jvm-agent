import React, { useState } from 'react';

interface ShareModalProps {
  conversationId: string;
  conversationTitle: string;
  isOpen: boolean;
  onClose: () => void;
}

export default function ShareConversationModal({
  conversationId,
  conversationTitle,
  isOpen,
  onClose,
}: ShareModalProps) {
  const [expiryDays, setExpiryDays] = useState(7);
  const [loading, setLoading] = useState(false);
  const [shareUrl, setShareUrl] = useState('');
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  const handleShare = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setShareUrl('');

    try {
      const response = await fetch(`/api/chat/share?conversationId=${conversationId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          expiryDays,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to share conversation');
      }

      const data = await response.json();
      // Construct the correct share URL from shareToken
      const shareLink = `/shared/${data.shareToken}`;
      setShareUrl(shareLink);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleCopyUrl = () => {
    if (shareUrl) {
      const fullUrl = `${window.location.origin}${shareUrl}`;
      navigator.clipboard.writeText(fullUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-lg p-6 w-full max-w-md">
        <h2 className="text-2xl font-bold mb-4">Share Conversation</h2>
        <p className="text-gray-600 mb-4">
          Share &quot;<strong>{conversationTitle}</strong>&quot; with others
        </p>

        {shareUrl ? (
          <div className="space-y-4">
            <div className="bg-green-50 border border-green-200 rounded p-4">
              <p className="text-sm text-green-800 mb-2">
                ✓ Share link created successfully!
              </p>
              <div className="flex gap-2">
                <input
                  type="text"
                  readOnly
                  value={`${window.location.origin}${shareUrl}`}
                  className="flex-1 px-3 py-2 border rounded text-sm bg-white"
                />
                <button
                  onClick={handleCopyUrl}
                  className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 text-sm"
                >
                  {copied ? 'Copied!' : 'Copy'}
                </button>
              </div>
            </div>
            <button
              onClick={onClose}
              className="w-full px-4 py-2 bg-gray-500 text-white rounded hover:bg-gray-600"
            >
              Done
            </button>
          </div>
        ) : (
          <form onSubmit={handleShare} className="space-y-4">

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Expiry Days
              </label>
              <select
                value={expiryDays}
                onChange={(e) => setExpiryDays(Number(e.target.value))}
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value={1}>1 day</option>
                <option value={3}>3 days</option>
                <option value={7}>7 days</option>
                <option value={30}>30 days</option>
                <option value={90}>90 days</option>
              </select>
            </div>

            {error && (
              <div className="bg-red-50 border border-red-200 rounded p-3">
                <p className="text-sm text-red-700">{error}</p>
              </div>
            )}

            <div className="flex gap-2">
              <button
                type="submit"
                disabled={loading}
                className="flex-1 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 disabled:opacity-50"
              >
                {loading ? 'Sharing...' : 'Create Share Link'}
              </button>
              <button
                type="button"
                onClick={onClose}
                className="flex-1 px-4 py-2 bg-gray-500 text-white rounded hover:bg-gray-600"
              >
                Cancel
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

