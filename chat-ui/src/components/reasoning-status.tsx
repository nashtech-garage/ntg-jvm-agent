import { BrainCircuit, Check, LoaderCircle } from 'lucide-react';

interface ReasoningStatusProps {
  content: string;
  isStreaming: boolean;
}

export default function ReasoningStatus({ content, isStreaming }: Readonly<ReasoningStatusProps>) {
  if (!content) return null;

  return (
    <div className="flex items-start gap-3" aria-live="polite">
      <div
        className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-border bg-gradient-to-br from-surface-muted to-surface text-primary-strong shadow-sm shadow-[0_6px_14px_color-mix(in_oklab,var(--color-border)_70%,transparent)]"
        aria-hidden="true"
      >
        <BrainCircuit className="h-5 w-5" />
      </div>
      <div className="max-w-[80%] rounded-2xl border border-border bg-surface px-4 py-3 shadow-md shadow-[0_10px_26px_color-mix(in_oklab,var(--color-border)_60%,transparent)]">
        <div className="mb-2 flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.18em] text-muted-foreground">
          <span>Model reasoning</span>
          {isStreaming ? (
            <LoaderCircle className="h-3.5 w-3.5 animate-spin text-primary" aria-hidden="true" />
          ) : (
            <Check className="h-3.5 w-3.5 text-success" aria-hidden="true" />
          )}
        </div>
        <p className="max-h-64 overflow-y-auto whitespace-pre-wrap text-sm leading-relaxed text-muted">
          {content}
        </p>
      </div>
    </div>
  );
}
