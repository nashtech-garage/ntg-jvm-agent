"use client";

import { useEffect, useState } from "react";
import Sidebar from "./components/Sidebar";
import ChatBox from "./components/ChatBox";
import SearchResult from "./components/SearchResult";

export default function Page() {
  const [sessions, setSessions] = useState<
    { id: number; title: string; results: Result[] }[]
  >([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [userName, setUserName] = useState<string>('');

  useEffect(() => {
    fetch("/api/user")
      .then((res) => res.json())
      .then((data) => {
        setUserName(data?.sub || 'Unknow user');
      });
  }, []);

  const fetchData = async (query: string): Promise<SearchResponse> => {
    try {
      // fake API
      const res = await fetch(`/api/search?q=${query}`);
      if (!res.ok) {
        throw new Error("Failed to fetch");
      }
      const jsonResult = await res.json();
      return jsonResult;
    } catch(err) {
      console.error(err);
      return {
        id: Date.now(),
        title: `Kết quả cho "${query}" - 1`,
        body: 'Đã có lỗi xảy ra trong quá trình tìm kiếm'
      };
    }
  };

  const handleSearch = async (q: string) => {
    const shortTitle = q.split(" ").slice(0, 10).join(" "); // Lấy 10 chữ đầu tiên cho title
    const { id, body} = await fetchData(q);

    setSessions((prev) => {
      if (activeId === null) {
        // Tạo session mới từ search đầu tiên
        const newSession = {
          id: id,
          title: shortTitle,
          results: [{ id: id, query: q, answer: body }],
        };
        setActiveId(newSession.id);
        return [newSession, ...prev];
      } else {
        // Append vào session đang active
        return prev.map((s) => {
          if (s.id === activeId) {
            const updatedTitle =
              s.results.length === 0 ? shortTitle : s.title; // update title nếu là câu hỏi đầu tiên
            return {
              ...s,
              title: updatedTitle,
              results: [
                ...s.results,
                { id: id, query: q, answer: body },
              ],
            };
          }
          return s;
        });
      }
    });
  };

  const removeHistory = (id: number) => {
    setSessions((prev) => prev.filter((s) => s.id !== id));
    if (activeId === id) setActiveId(null);
  };

  const newChat = () => {
    const newSession = {
      id: Date.now(),
      title: "New Chat",
      results: [],
    };
    setSessions((prev) => [newSession, ...prev]);
    setActiveId(newSession.id);
  };

  const activeSession = sessions.find((s) => s.id === activeId);

  return (
    <div className="flex h-screen">
      <Sidebar
        history={sessions.map((s) => ({ id: s.id, title: s.title }))}
        active={activeId}
        userName={userName}
        setActive={setActiveId}
        removeHistory={removeHistory}
        newChat={newChat}
      />
      <main className="flex-1 flex flex-col">
        <div className="flex-1 overflow-y-auto p-4">
          <SearchResult results={activeSession?.results ?? []} />
        </div>
        <ChatBox onSearch={handleSearch} />
      </main>
    </div>
  );
}
