import { Check, LoaderCircle } from 'lucide-react';
import { ToolCallEvent } from '@/models/tool-call-event';

interface ToolCallStatusProps {
  toolCalls: ToolCallEvent[];
}

export default function ToolCallStatus({ toolCalls }: Readonly<ToolCallStatusProps>) {
  if (toolCalls.length === 0) return null;

  return (
    <div className="flex items-start gap-3" aria-live="polite">
      <div
        className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-border bg-gradient-to-br from-surface-muted to-surface text-sm font-semibold text-primary-strong shadow-sm shadow-[0_6px_14px_color-mix(in_oklab,var(--color-border)_70%,transparent)]"
        aria-hidden="true"
      >
        AI
      </div>
      <div className="max-w-[80%] rounded-2xl border border-border bg-surface px-4 py-3 shadow-md shadow-[0_10px_26px_color-mix(in_oklab,var(--color-border)_60%,transparent)]">
        <p className="mb-2 text-[11px] font-medium uppercase tracking-[0.18em] text-muted-foreground">
          Agent activity
        </p>
        <div className="flex flex-wrap gap-2">
          {toolCalls.map((toolCall) => {
            const isCompleted = toolCall.phase === 'COMPLETED';

            return (
              <div
                key={toolCall.id}
                className={
                  isCompleted
                    ? 'inline-flex items-center gap-1.5 rounded-full border border-success-border bg-success-soft px-2.5 py-1 text-xs text-foreground'
                    : 'inline-flex items-center gap-1.5 rounded-full border border-border-strong bg-surface-muted px-2.5 py-1 text-xs text-muted'
                }
              >
                {isCompleted ? (
                  <Check className="h-3.5 w-3.5 text-success" aria-hidden="true" />
                ) : (
                  <LoaderCircle
                    className="h-3.5 w-3.5 animate-spin text-primary"
                    aria-hidden="true"
                  />
                )}
                <span>{isCompleted ? toolCall.name : `Calling ${toolCall.name}...`}</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
