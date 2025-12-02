'use client';

import { createContext, useContext } from 'react';

export const AgentContext = createContext({
  agent: null,
  mutate: () => {},
});

export function useAgent() {
  return useContext(AgentContext);
}
