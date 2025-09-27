"use client";

import { useState } from "react";
import { ChevronLeft, ChevronRight, Trash, SquarePen } from "lucide-react";

interface SidebarProps {
  history: HistoryItem[];
  active: number | null;
  userName: string;
  setActive: (id: number) => void;
  removeHistory: (id: number) => void;
  newChat: () => void;
}

export default function Sidebar({
  history,
  active,
  userName,
  setActive,
  removeHistory,
  newChat,
}: Readonly<SidebarProps>) {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div
      className={`bg-gray-100 text-black h-full flex flex-col transition-all duration-300 ${
        collapsed ? "w-16" : "w-64"
      }`}
    >
      {/* Collapse button */}
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="p-2 hover:bg-gray-150 ml-3"
      >
        {collapsed ? <ChevronRight /> : <ChevronLeft />}
      </button>

      {/* New Chat button */}
      <div className="px-2 mt-2">
        <button
          onClick={newChat}
          className="flex items-center gap-2 w-full px-3 py-2 rounded hover:bg-gray-200"
        >
          <SquarePen size={16} />
          {!collapsed && <span>New Chat</span>}
        </button>
      </div>

      {/* History list */}
      <div className="flex-1 overflow-y-auto space-y-2 mt-4 px-2">
        {history.map((item) => (
          <div
            key={item.id}
            onClick={() => setActive(item.id)}
            className={`flex justify-between items-center px-2 py-2 rounded cursor-pointer ${
              active === item.id ? "bg-gray-200" : "hover:bg-gray-200"
            }`}
          >
            <span className="truncate text-sm">{item.title}</span>
            {!collapsed && (
              <Trash
                size={16}
                onClick={(e) => {
                  e.stopPropagation();
                  removeHistory(item.id);
                }}
                className="hover:text-red-400"
              />
            )}
          </div>
        ))}
      </div>

      {/* Account info */}
      <div className="px-2 mt-2">
        <button
          // onClick={newChat}
          className="flex items-center gap-2 w-full px-3 py-2 rounded hover:bg-gray-200"
        >
          {userName}
        </button>
      </div>
    </div>
  );
}
