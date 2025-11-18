'use client';

import { useState } from 'react';
import { ChevronLeft, ChevronRight, Trash, SquarePen } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useChatContext } from '../contexts/ChatContext';
import { toast } from 'sonner';
import { Constants } from '../utils/constant';
import { customizeFetch } from '../utils/custom-fetch';

export default function Sidebar() {
  const {
    conversations,
    activeConversationId,
    userName,
    setActiveConversationId,
    setChatMessages,
    setConversations,
  } = useChatContext();
  const [collapsed, setCollapsed] = useState(false);
  const router = useRouter();

  const changeConversation = async (id: string) => {
    router.push(`/c/${id}`);
  };

  const removeConversation = async (id: string) => {
    const res = await customizeFetch(`/api/chat?conversationId=${id}`, { method: 'DELETE' });
    const jsonResult = await res.json();
    if (!res.ok) {
      toast.error(jsonResult.error);
      return;
    }

    setConversations((prev) => prev.filter((s) => s.id !== id));
    if (activeConversationId === id) {
      setActiveConversationId(null);
      setChatMessages([]);
      router.replace(`/`);
    }
    toast.success(Constants.DELETE_CONVERSATION_SUCCESS_MSG);
  };

  const newChat = () => {
    setActiveConversationId(null);
    setChatMessages([]);
    router.replace(`/`);
  };

  const handleLogout = async () => {
    try {
      await fetch('/auth/logout', { method: 'POST' });
      router.replace('/login');
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  return (
    <div
      className={`bg-gray-100 text-black h-full flex flex-col transition-all duration-300 ${collapsed ? 'w-16' : 'w-64'
        }`}
    >
      {/* Collapse button */}
      <button onClick={() => setCollapsed(!collapsed)} className="p-2 hover:bg-gray-150 ml-3">
        {collapsed ? <ChevronRight /> : <ChevronLeft />}
      </button>

      {/* Action wrapper */}
      <div id="action-wrapper" className="px-2 mt-2">
        <button
          onClick={newChat}
          className="flex items-center gap-2 w-full px-3 py-2 rounded hover:bg-gray-200 cursor-pointer"
        >
          <SquarePen size={16} />
          {!collapsed && <span>New Chat</span>}
        </button>
      </div>

      {/* History list */}
      <div
        id="history-list"
        className={`flex-1 overflow-y-auto mt-4 px-2 transition-all duration-300 ${collapsed ? 'opacity-0 pointer-events-none h-0' : 'opacity-100 space-y-2'
          }`}
      >
        {!collapsed &&
          conversations.map((item) => (
            <div
              key={item.id}
              onClick={() => changeConversation(item.id)}
              className={`flex justify-between items-center px-2 py-2 rounded cursor-pointer ${
                activeConversationId === item.id ? 'bg-gray-200' : 'hover:bg-gray-200'
              }`}
            >
              <span className="truncate text-sm">{item.title}</span>
              <Trash
                size={16}
                onClick={(e) => {
                  e.stopPropagation();
                  removeConversation(item.id);
                }}
                className="hover:text-red-400"
              />
            </div>
          ))}
      </div>

      {/* Account info & Logout button */}
      <div className="border-t border-gray-700 gap-2 p-4">
        <div
          className={`mb-4 transition-all duration-300 ${!collapsed ? 'opacity-100' : 'opacity-0 h-0 overflow-hidden'
            }`}
        >
          <div className="text-sm flex">
            Welcome:
            <p className="ml-1 text-green-700">{userName}</p>
          </div>
        </div>
        <button
          onClick={handleLogout}
          className="flex items-center content-center w-full px-3 py-2 text-sm bg-red-600 hover:bg-red-700 rounded-lg transition-colors text-white justify-center"
          title={collapsed ? 'Logout' : undefined}
        >
          <svg
            className="w-4 h-4 flex-shrink-0"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
            />
          </svg>
          <span
            className={`transition-all duration-300 ${!collapsed
              ? 'opacity-100 translate-x-0'
              : 'opacity-0 -translate-x-2 w-0 overflow-hidden'
              }`}
          >
            Logout
          </span>
        </button>
      </div>
    </div>
  );
}
