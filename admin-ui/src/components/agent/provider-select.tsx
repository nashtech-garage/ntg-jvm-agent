import { cn } from '@/utils/css';
import { ModelProvider } from '@/types/model-provider';

export type ProviderSelectProps = {
  value: string;
  onChange: (value: string) => void;
  providers: ModelProvider[];
};

const selectClass =
  'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 md:text-sm';

export function ProviderSelect({ value, onChange, providers }: ProviderSelectProps) {
  const sorted = [...providers].sort((a, b) => a.displayName.localeCompare(b.displayName));
  return (
    <div className="space-y-2">
      <select
        className={cn(selectClass, 'bg-background')}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      >
        <option value="">Chọn provider...</option>
        {sorted.map((provider) => (
          <option key={provider.id} value={provider.id}>
            {provider.displayName}
          </option>
        ))}
        <option value="custom">Custom</option>
      </select>
    </div>
  );
}
