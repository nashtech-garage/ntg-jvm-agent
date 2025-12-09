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
      if (boxRef.current && !boxRef.current.contains(ev.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handle);
    return () => document.removeEventListener('mousedown', handle);
  }, []);

  useEffect(() => {
    const fetchAgents = async () => {
      try {
        const res = await fetch('/api/agent');
        const agents = await res.json();
        if (agents.length) {
          setAgents(agents);
          setSelected(agents[0]);
          setSelectedAgent(agents[0]);
        }
      } catch (error) {
        toast.error(`Error fetching agents: ${error}`);
      }
    };

    if (!agents.length) {
      fetchAgents();
    }
  }, [agents.length, setAgents, setSelectedAgent]);

  return (
    <div ref={boxRef} className="relative inline-block">
      <button
        onClick={toggle}
        className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-900 shadow-sm shadow-slate-200 transition hover:border-sky-300 hover:text-slate-950 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-300"
      >
        {selected?.avatar ? (
          <img
            src={selected.avatar}
            alt={selected.name}
            className="h-5 w-5 rounded-full object-cover"
          />
        ) : (
          <div className="h-5 w-5 rounded-full bg-blue-300 flex items-center justify-center text-xs font-bold text-white">
            {selected?.name?.charAt(0)?.toUpperCase() || 'A'}
          </div>
        )}
        <div className="flex items-center">{selected ? selected.name : 'Select agent'}</div>

        <span className={`transition-transform ${open ? 'rotate-180' : 'rotate-0'}`}>
          <ChevronDown className="h-4 w-4 text-slate-500" />
        </span>
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-64 rounded-xl border border-slate-200 bg-white p-2 shadow-2xl shadow-slate-200">
          {agents.map((agent) => (
            <div
              key={agent.id}
              onClick={() => handleSelect(agent)}
              className="flex items-center gap-3 rounded-lg px-3 py-3 text-sm text-slate-800 transition hover:bg-slate-50"
            >
              {agent.avatar ? (
                <img
                  src={agent.avatar}
                  alt={agent.name}
                  className="h-8 w-8 rounded-full object-cover flex-shrink-0"
                />
              ) : (
                <div className="h-8 w-8 rounded-full bg-blue-300 flex items-center justify-center text-xs font-bold text-white flex-shrink-0">
                  {agent.name?.charAt(0)?.toUpperCase() || 'A'}
                </div>
              )}
              <div className="flex flex-col">
                <span className="font-semibold">{agent.name}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
