import { useMemo } from 'react';

import { getStaticProviderModels } from '@/data/model-providers';

export function useProviderModels(providerId: string | null, type: 'chat' | 'embedding') {
  const models = useMemo(() => getStaticProviderModels(providerId, type), [providerId, type]);

  return {
    models,
    isLoading: false,
    error: undefined,
    mutate: () => Promise.resolve(),
  };
}
