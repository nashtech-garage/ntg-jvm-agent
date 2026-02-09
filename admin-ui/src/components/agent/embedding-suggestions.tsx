import { Button } from '@/components/ui/button';
import { ProviderModel } from '@/types/model-provider';

export type EmbeddingSuggestionsProps = {
  suggestions: ProviderModel[];
  onSelect: (model: string) => void;
  isVisible: boolean;
};

export function EmbeddingSuggestions({ suggestions, onSelect, isVisible }: EmbeddingSuggestionsProps) {
  if (!isVisible || !suggestions.length) return null;

  return (
    <div className="space-y-2">
      <p className="text-sm text-muted-foreground">Gợi ý embedding:</p>
      <div className="flex flex-wrap gap-2">
        {suggestions.map((model) => (
          <Button
            key={model.name}
            type="button"
            size="sm"
            variant="secondary"
            onClick={() => onSelect(model.name)}
          >
            {model.label || model.name}
          </Button>
        ))}
      </div>
    </div>
  );
}
