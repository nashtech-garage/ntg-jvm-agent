import Sidebar from '../components/Sidebar';
import { ChatProvider } from '../contexts/ChatContext';

export default function ChatLayout({ children }: { children: React.ReactNode }) {
  return (
    <ChatProvider>
      <div className="flex h-screen">
        <Sidebar />
        <div className="flex-1 flex flex-col">{children}</div>
      </div>
    </ChatProvider>
  );
}
