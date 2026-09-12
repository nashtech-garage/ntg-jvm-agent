import { Check, Circle, ListTodo, LoaderCircle } from 'lucide-react';
import { TodoItem } from '@/models/tool-call-event';

interface TodoProgressStatusProps {
  items: TodoItem[];
}

function StatusIcon({ status }: Readonly<{ status: TodoItem['status'] }>) {
  if (status === 'completed') {
    return <Check className="h-4 w-4 text-success" aria-hidden="true" />;
  }

  if (status === 'in_progress') {
    return <LoaderCircle className="h-4 w-4 animate-spin text-primary" aria-hidden="true" />;
  }

  return <Circle className="h-4 w-4 text-muted-foreground" aria-hidden="true" />;
}

export default function TodoProgressStatus({ items }: Readonly<TodoProgressStatusProps>) {
  if (items.length === 0) return null;

  const completedCount = items.filter((item) => item.status === 'completed').length;
  const progress = Math.round((completedCount / items.length) * 100);

  return (
    <div className="flex items-start gap-3" aria-live="polite" data-testid="todo-progress">
      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-primary-border bg-primary-soft text-primary-strong shadow-sm shadow-[0_6px_14px_color-mix(in_oklab,var(--color-border)_70%,transparent)]">
        <ListTodo className="h-5 w-5" aria-hidden="true" />
      </div>
      <div className="w-full max-w-[80%] rounded-2xl border border-primary-border bg-surface px-4 py-3 shadow-md shadow-[0_10px_26px_color-mix(in_oklab,var(--color-border)_60%,transparent)]">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-primary-strong">
              Task progress
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              {completedCount} of {items.length} completed
            </p>
          </div>
          <span className="text-sm font-semibold text-foreground">{progress}%</span>
        </div>

        <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-surface-soft">
          <div
            className="h-full rounded-full bg-gradient-to-r from-primary to-success transition-[width] duration-300"
            role="progressbar"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={progress}
            style={{ width: `${progress}%` }}
          />
        </div>

        <ul className="mt-3 space-y-2">
          {items.map((item, index) => (
            <li
              key={`${index}-${item.content}-${item.activeForm}`}
              className="flex items-start gap-2 rounded-xl border border-border bg-surface-muted px-3 py-2"
            >
              <span className="mt-0.5">
                <StatusIcon status={item.status} />
              </span>
              <span
                className={
                  item.status === 'completed'
                    ? 'text-sm text-muted-foreground line-through'
                    : 'text-sm text-foreground'
                }
              >
                {item.status === 'in_progress' ? item.activeForm : item.content}
              </span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
