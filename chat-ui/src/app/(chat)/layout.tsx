import Sidebar from '@/components/sidebar';
import { ChatProvider } from '@/contexts/ChatContext';

export default function ChatLayout({ children }: { children: React.ReactNode }) {
  return (
    <ChatProvider>
      <div className="flex h-screen">
        <Sidebar />
        <div className="flex-1 flex flex-col overflow-x-auto">{children}</div>
      </div>
    </ChatProvider>
  );
}
