import { useMemo } from 'react';

import { ModelProviderResponse, ProviderDefaults } from '@/types/model-provider';
import { STATIC_MODEL_PROVIDERS } from '@/data/model-providers';

export function useModelProviders() {
  const providers = STATIC_MODEL_PROVIDERS;

  const defaultsMap = useMemo(() => {
    return new Map<string, ProviderDefaults>(
      providers.map((provider) => [provider.id, provider.defaults])
    );
  }, [providers]);

  return {
    providers,
    defaultsMap,
    isLoading: false,
    error: undefined,
    mutate: () => Promise.resolve(),
  };
}
