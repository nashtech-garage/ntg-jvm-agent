'use client';

import { useState, useRef, useEffect } from 'react';
import { ChevronLeft, ChevronRight, Trash, SquarePen, MoreVertical } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useChatContext } from '../contexts/ChatContext';
import { toast } from 'sonner';
import { Constants } from '../constants/constant';
import { customizeFetch } from '../utils/custom-fetch';
import logger from '@/utils/logger';

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
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);
  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [newTitle, setNewTitle] = useState('');
  const dropdownRefs = useRef<{ [key: string]: HTMLDivElement | null }>({});
  const router = useRouter();

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (openDropdown) {
        const currentRef = dropdownRefs.current[openDropdown];
        if (currentRef && !currentRef.contains(event.target as Node)) {
          setOpenDropdown(null);
        }
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [openDropdown]);

  const changeConversation = async (id: string) => {
    router.push(`/c/${id}`);
  };

  const renameConversation = async (id: string) => {
    if (!newTitle.trim()) {
      toast.error('Conversation name cannot be empty');
      return;
    }

    try {
      const res = await fetch(`/api/chat?conversationId=${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: newTitle.trim() }),
      });

      const jsonResult = await res.json();
      if (!res.ok) {
        toast.error(jsonResult.error || 'Failed to rename conversation');
        return;
      }

      setConversations((prev) =>
        prev.map((conv) => (conv.id === id ? { ...conv, title: newTitle.trim() } : conv))
      );
      setRenamingId(null);
      setNewTitle('');
      setOpenDropdown(null);
      toast.success('Conversation renamed successfully');
    } catch (error) {
      toast.error(`Error renaming conversation: ${error}`);
    }
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
    setOpenDropdown(null);
    toast.success(Constants.DELETE_CONVERSATION_SUCCESS_MSG);
  };

  const newChat = () => {
    setActiveConversationId(null);
    setChatMessages([]);
    router.replace(`/`);
  };

  const handleLogout = async () => {
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
      router.replace('/login');
    } catch (error) {
      logger.error('Logout failed:', error);
      toast.error('Logout failed');
    }
  };

  return (
    <div
      className={`flex h-full flex-col border-r border-slate-200 bg-gradient-to-b from-white via-slate-50 to-slate-100 text-slate-800 shadow-sm transition-all duration-300 ${
        collapsed ? 'w-16' : 'w-72'
      }`}
    >
      {/* Brand & collapse */}
      <div className="flex items-center justify-between px-3 py-3">
        {!collapsed && (
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-sky-600 text-sm font-semibold text-white shadow-md shadow-sky-200">
              NT
            </div>
            <div className="leading-tight">
              <h2 className="text-lg font-semibold text-slate-800">NT Agent</h2>
            </div>
          </div>
        )}
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 shadow-sm transition hover:-translate-y-0.5 hover:border-sky-200 hover:text-sky-700"
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </button>
      </div>

      {/* Action wrapper */}
      <div id="action-wrapper">
        <button
          onClick={newChat}
          className="group flex w-full items-center gap-3 rounded-xl border border-sky-100 bg-sky-50 px-4 py-3 text-sky-800 shadow-sm shadow-sky-100 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-200"
        >
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-gray-100 text-gray-700 transition group-hover:bg-sky-50 group-hover:text-sky-700">
            <SquarePen size={18} />
          </span>
          {!collapsed && <span className="text-sm font-semibold">New Chat</span>}
        </button>
      </div>

      {/* History list */}
      <div
        id="history-list"
        className={`mt-4 flex-1 overflow-y-auto px-3 transition-all duration-300 ${
          collapsed ? 'pointer-events-none h-0 opacity-0' : 'opacity-100 space-y-3'
        }`}
      >
        {!collapsed && (
          <div className="px-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500">
            Recent chats
          </div>
        )}
        {!collapsed &&
          conversations.map((item) => (
            <div
              key={item.id}
              ref={(el) => {
                if (el) dropdownRefs.current[item.id] = el;
              }}
              className="relative"
            >
              {/* Rename input */}
              {renamingId === item.id ? (
                <div className="rounded-xl border border-slate-200 bg-white px-3 py-2 shadow-sm shadow-slate-100">
                  <input
                    autoFocus
                    type="text"
                    value={newTitle}
                    onChange={(e) => setNewTitle(e.target.value)}
                    onBlur={() => {
                      if (newTitle.trim()) {
                        renameConversation(item.id);
                      } else {
                        setRenamingId(null);
                        setNewTitle('');
                      }
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        renameConversation(item.id);
                      } else if (e.key === 'Escape') {
                        setRenamingId(null);
                        setNewTitle('');
                      }
                    }}
                    className="w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm outline-none ring-sky-200 transition focus:border-sky-300 focus:ring-2"
                    placeholder="New name..."
                  />
                </div>
              ) : (
                <div
                  onClick={() => changeConversation(item.id)}
                  className={`group relative flex items-center justify-between rounded-xl border px-3 py-2.5 transition shadow-sm ${
                    activeConversationId === item.id
                      ? 'border-sky-200 bg-white shadow-sky-100'
                      : 'border-transparent bg-white hover:-translate-y-0.5 hover:border-slate-200 hover:shadow-md'
                  }`}
                >
                  <span className="truncate text-sm font-medium text-slate-800">{item.title}</span>

                  {/* Dropdown button */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setOpenDropdown(openDropdown === item.id ? null : item.id);
                    }}
                    className="rounded-lg p-1 text-slate-500 transition hover:bg-slate-100 hover:text-slate-800"
                    aria-label="Conversation actions"
                  >
                    <MoreVertical size={16} />
                  </button>

                  {/* Dropdown menu */}
                  {openDropdown === item.id && (
                    <div className="absolute right-0 top-full z-50 mt-2 min-w-max overflow-hidden rounded-lg border border-slate-200 bg-white shadow-lg shadow-slate-200/70">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setRenamingId(item.id);
                          setNewTitle(item.title);
                          setOpenDropdown(null);
                        }}
                        className="flex w-full cursor-pointer items-center gap-2 px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-50"
                      >
                        <SquarePen size={14} />
                        Rename
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          removeConversation(item.id);
                        }}
                        className="flex w-full cursor-pointer items-center gap-2 px-4 py-2 text-sm text-red-600 transition hover:bg-red-50"
                      >
                        <Trash size={14} />
                        Delete
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
      </div>

      {/* Account info & Logout button */}
      <div className="border-t border-slate-200 bg-white/60 p-4 backdrop-blur-sm">
        <div
          className={`mb-4 transition-all duration-300 ${
            !collapsed ? 'opacity-100' : 'opacity-0 h-0 overflow-hidden'
          }`}
        >
          <div className="flex text-sm">
            Welcome:
            <p className="ml-1 font-semibold text-slate-800">{userName}</p>
          </div>
        </div>
        <button
          onClick={handleLogout}
          className="flex w-full items-center justify-center rounded-lg bg-red-500 px-3 py-2 text-sm font-semibold text-white shadow-sm shadow-red-200 transition hover:-translate-y-0.5 hover:bg-red-600"
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
            className={`transition-all duration-300 ${
              !collapsed
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
