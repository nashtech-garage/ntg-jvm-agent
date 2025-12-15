'use client';

import { useEffect, useRef, useState } from 'react';
import Image from 'next/image';
import { Agent } from '../models/agent';
import { ChevronDown } from 'lucide-react';
import { useChatContext } from '../contexts/ChatContext';
import { Button } from './ui/button';
import { useToaster } from '@/contexts/ToasterContext';

export default function AgentDropdown() {
  const { agents, setAgents, setSelectedAgent } = useChatContext();
  const { showError } = useToaster();
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
        showError(`Error fetching agents: ${error}`);
      }
    };

    if (!agents.length) {
      fetchAgents();
    }
  }, [agents.length, showError, setAgents, setSelectedAgent]);

  return (
    <div ref={boxRef} className="relative inline-block">
      <Button
        variant="ghost"
        onClick={toggle}
        className="flex items-center gap-2 rounded-xl border border-border bg-surface px-4 py-5 text-sm font-medium text-foreground shadow-sm shadow-[0_6px_16px_color-mix(in_oklab,var(--color-border)_70%,transparent)] transition hover:border-primary-border hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-border"
      >
        {selected?.avatar ? (
          <Image
            src={selected.avatar}
            alt={selected.name}
            className="h-5 w-5 rounded-full object-cover"
          />
        ) : (
          <div className="h-5 w-5 rounded-full bg-avatar flex items-center justify-center text-xs font-bold text-inverse">
            {selected?.name?.charAt(0)?.toUpperCase() || 'A'}
          </div>
        )}
        <div className="flex items-center">{selected ? selected.name : 'Select agent'}</div>

        <span className={`transition-transform ${open ? 'rotate-180' : 'rotate-0'}`}>
          <ChevronDown className="h-4 w-4 text-muted-foreground" />
        </span>
      </Button>

      {open && (
        <div className="absolute right-0 mt-2 w-64 rounded-xl border border-border bg-surface p-2 shadow-2xl shadow-[0_14px_34px_color-mix(in_oklab,var(--color-border)_75%,transparent)]">
          {agents.map((agent) => (
            <div
              key={agent.id}
              onClick={() => handleSelect(agent)}
              className="flex items-center gap-2 rounded-lg px-2 py-1 text-sm text-foreground transition hover:bg-surface-muted"
            >
              {agent.avatar ? (
                <Image
                  src={agent.avatar}
                  alt={agent.name}
                  className="h-8 w-8 rounded-full object-cover flex-shrink-0"
                />
              ) : (
                <div className="h-8 w-8 rounded-full bg-avatar flex items-center justify-center text-xs font-bold text-inverse flex-shrink-0">
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
