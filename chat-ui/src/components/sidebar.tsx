'use client';

import { useState, useRef, useEffect } from 'react';
import { ChevronLeft, ChevronRight, Trash, SquarePen, MoreVertical, Share2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useChatContext } from '../contexts/ChatContext';
import { toast } from 'sonner';
import { Constants } from '../constants/constant';
import { customizeFetch } from '../utils/custom-fetch';
import { useAuth } from '@/contexts/AuthContext';
import ShareConversationModal from './ShareConversationModal';
import { Button } from './ui/button';

export default function Sidebar() {
  const { signOut } = useAuth();
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
  const [showShareModal, setShowShareModal] = useState(false);
  const [shareConversationId, setShareConversationId] = useState<string | null>(null);
  const [shareConversationTitle, setShareConversationTitle] = useState<string>('');
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

  const shareConversation = (id: string, title: string) => {
    setShareConversationId(id);
    setShareConversationTitle(title);
    setShowShareModal(true);
    setOpenDropdown(null);
  };

  const newChat = () => {
    setActiveConversationId(null);
    setChatMessages([]);
    router.replace(`/`);
  };

  const handleLogout = async () => {
    await signOut();
  };

  return (
    <div
      className={`flex h-full flex-col border-r border-border bg-gradient-to-b from-surface via-surface-muted to-surface-soft text-foreground shadow-sm transition-all duration-300 ${
        collapsed ? 'w-16' : 'w-72'
      }`}
    >
      {/* Brand & collapse */}
      <div className="flex items-center justify-between px-3 py-3">
        {!collapsed && (
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-strong text-sm font-semibold text-primary-foreground shadow-md shadow-[0_10px_26px_color-mix(in_oklab,var(--color-primary)_35%,transparent)]">
              NT
            </div>
            <div className="leading-tight">
              <h2 className="text-lg font-semibold text-foreground">NT Agent</h2>
            </div>
          </div>
        )}
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setCollapsed(!collapsed)}
          className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-border bg-surface text-muted shadow-sm transition hover:-translate-y-0.5 hover:border-primary-border hover:text-primary-strong"
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </Button>
      </div>

      {/* Action wrapper */}
      <div id="action-wrapper">
        <Button
          variant="ghost"
          onClick={newChat}
          className="my-2 group flex w-full items-center justify-start gap-3 rounded-xl border border-primary-border bg-primary-soft px-4 py-6 text-primary-strong focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-border"
        >
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary-soft text-muted">
            <SquarePen size={18} />
          </span>
          {!collapsed && <span className="text-sm font-semibold">New Chat</span>}
        </Button>
      </div>

      {/* History list */}
      <div
        id="history-list"
        className={`mt-4 flex-1 overflow-y-auto px-3 transition-all duration-300 ${
          collapsed ? 'pointer-events-none h-0 opacity-0' : 'opacity-100 space-y-3'
        }`}
      >
        {!collapsed && (
          <div className="px-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-muted-foreground">
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
                <div className="rounded-xl border border-border bg-surface px-3 py-2 shadow-sm shadow-[0_8px_18px_color-mix(in_oklab,var(--color-border)_60%,transparent)]">
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
                    className="w-full rounded-lg border border-border bg-surface-muted px-3 py-2 text-sm text-foreground outline-none ring-primary-border transition focus:border-primary-border focus:ring-2"
                    placeholder="New name..."
                  />
                </div>
              ) : (
                <div
                  onClick={() => changeConversation(item.id)}
                  className={`group relative flex items-center justify-between rounded-xl border px-3 py-2.5 transition shadow-sm ${
                    activeConversationId === item.id
                      ? 'border-primary-border bg-primary-soft shadow-[0_8px_18px_color-mix(in_oklab,var(--color-primary)_20%,transparent)]'
                      : 'border-transparent bg-surface hover:border-border hover:shadow-[0_10px_22px_color-mix(in_oklab,var(--color-border)_45%,transparent)]'
                  }`}
                >
                  <span className="truncate text-sm font-medium text-foreground">{item.title}</span>

                  {/* Dropdown button */}
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={(e) => {
                      e.stopPropagation();
                      setOpenDropdown(openDropdown === item.id ? null : item.id);
                    }}
                    className="rounded-lg p-1 text-muted-foreground transition hover:bg-surface-muted hover:text-foreground"
                    aria-label="Conversation actions"
                  >
                    <MoreVertical size={16} />
                  </Button>

                  {/* Dropdown menu */}
                  {openDropdown === item.id && (
                    <div className="absolute right-0 top-full z-50 mt-2 min-w-max overflow-hidden rounded-lg border border-border bg-surface shadow-lg shadow-[0_14px_32px_color-mix(in_oklab,var(--color-border)_70%,transparent)]">
                      <Button
                        variant="ghost"
                        onClick={(e) => {
                          e.stopPropagation();
                          setRenamingId(item.id);
                          setNewTitle(item.title);
                          setOpenDropdown(null);
                        }}
                        className="flex w-full cursor-pointer items-center gap-2 px-4 py-2 text-sm text-muted transition hover:bg-surface-muted"
                      >
                        <SquarePen size={14} />
                        Rename
                      </Button>
                      <Button
                        variant="ghost"
                        onClick={(e) => {
                          e.stopPropagation();
                          shareConversation(item.id, item.title);
                        }}
                        className="w-full text-left px-4 py-2 text-sm hover:bg-surface-muted flex items-center gap-2 text-primary-strong cursor-pointer"
                      >
                        <Share2 size={14} />
                        Share
                      </Button>
                      <Button
                        variant="ghost"
                        onClick={(e) => {
                          e.stopPropagation();
                          removeConversation(item.id);
                        }}
                        className="flex w-full cursor-pointer items-center gap-2 px-4 py-2 text-sm text-danger transition hover:bg-danger-soft"
                      >
                        <Trash size={14} />
                        Delete
                      </Button>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
      </div>

      {/* Account info & Logout button */}
      <div className="border-t border-border bg-surface/60 p-4 backdrop-blur-sm">
        <div
          className={`mb-4 transition-all duration-300 ${
            !collapsed ? 'opacity-100' : 'opacity-0 h-0 overflow-hidden'
          }`}
        >
          <div className="flex text-sm">
            Welcome:
            <p className="ml-1 font-semibold text-foreground">{userName}</p>
          </div>
        </div>
        <Button
          variant="ghost"
          onClick={handleLogout}
          className="flex w-full items-center justify-center rounded-lg bg-danger px-3 py-2 text-sm font-semibold text-inverse transition hover:bg-danger-strong"
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
        </Button>
      </div>

      {/* Share Modal */}
      {shareConversationId && (
        <ShareConversationModal
          conversationId={shareConversationId}
          conversationTitle={shareConversationTitle}
          isOpen={showShareModal}
          onClose={() => {
            setShowShareModal(false);
            setShareConversationId(null);
            setShareConversationTitle('');
          }}
        />
      )}
    </div>
  );
}
