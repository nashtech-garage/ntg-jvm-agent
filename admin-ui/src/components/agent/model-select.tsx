import { Input } from '@/components/ui/input';
import { cn } from '@/utils/css';
import { ProviderModel } from '@/types/model-provider';

export type ModelSelectProps = {
  value: string;
  onChange: (value: string) => void;
  models: ProviderModel[];
  isLoading?: boolean;
  error?: Error;
  disabled?: boolean;
};

const selectClass =
  'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 md:text-sm';

export function ModelSelect({ value, onChange, models, isLoading, error, disabled }: ModelSelectProps) {
  return (
    <div className="space-y-2">
      <select
        className={cn(selectClass, 'bg-background')}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled || isLoading}
      >
        <option value="">Chọn model...</option>
        {models.map((model) => (
          <option key={model.name} value={model.name}>
            {model.label || model.name}
          </option>
        ))}
      </select>
      {isLoading && <p className="text-sm text-muted-foreground">Đang tải danh sách model...</p>}
      {!isLoading && !models.length && (
        <p className="text-sm text-muted-foreground">Không có model cho provider này.</p>
      )}
    </div>
  );
}

export function ModelTextInput({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  return (
    <Input
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder="Nhập tên model"
      autoComplete="off"
    />
  );
}
