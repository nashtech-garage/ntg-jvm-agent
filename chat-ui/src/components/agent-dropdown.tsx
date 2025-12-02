'use client';

import { useEffect, useRef, useState } from 'react';
import { Agent } from '../models/agent';
import { ChevronDown } from 'lucide-react';
import { toast } from 'sonner';
import { useChatContext } from '../contexts/ChatContext';

export default function AgentDropdown() {
  const { agents, setAgents, setSelectedAgent } = useChatContext();
  const [open, setOpen] = useState<boolean>(false);
  const [selected, setSelected] = useState<Agent | null>(agents[0]);
  const boxRef = useRef<HTMLDivElement | null>(null);

  const toggle = () => setOpen(!open);

  const handleSelect = (agent: Agent) => {
    setSelected(agent);
    setSelectedAgent(agent);
    setOpen(false);
  };

  useEffect(() => {
    const handle = (ev: MouseEvent) => {
      if (boxRef.current && !boxRef.current.contains(ev.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handle);

    const fetchAgents = async () => {
      try {
        const res = await fetch('/api/agent');
        const fetchedAgents = await res.json();
        if (fetchedAgents.length) {
          setAgents(fetchedAgents);
          setSelected(fetchedAgents[0]);
          setSelectedAgent(fetchedAgents[0]);
        }
      } catch (error) {
        toast.error(`Error fetching agents: ${error}`);
      }
    };

    if (!agents.length) {
      fetchAgents();
    }
    return () => document.removeEventListener('mousedown', handle);
  }, [agents.length, setAgents, setSelectedAgent]);

  return (
    <div ref={boxRef} className="relative inline-block">
      <button
        onClick={toggle}
        className="flex items-center py-2 pl-4 bg-[white] text-black rounded-xl hover:bg-[#00000012] transition min-w-4 justify-between cursor-pointer"
      >
        {selected && <div className="flex items-center mr-1">{selected.name}</div>}

        <span className={`transition-transform ${open ? 'rotate-180' : 'rotate-0'}`}>
          <ChevronDown className="w-4 h-4 text-gray-400" />
        </span>
      </button>

      {open && (
        <div className="absolute mt-2 w-60 bg-[white] border border-[gray] rounded-xl shadow-2xl p-2 z-50">
          {agents.map((agent) => (
            <div
              key={agent.id}
              onClick={() => handleSelect(agent)}
              className="flex items-start gap-3 px-3 py-3 rounded-lg cursor-pointer hover:bg-[#00000012] transition"
            >
              <div className="flex flex-col">
                <span className="text-black font-medium">{agent.name}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
