'use client';

import { useMemo, useState } from 'react';
import { CircleHelp, LoaderCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { PendingQuestion, QuestionAnswers } from '@/models/user-question';

const OTHER_VALUE = '__other__';

export default function UserQuestionCard({
  pending,
  submitting,
  onSubmit,
}: Readonly<{
  pending: PendingQuestion;
  submitting: boolean;
  onSubmit: (answers: QuestionAnswers) => void;
}>) {
  const [selected, setSelected] = useState<Record<string, string[]>>({});
  const [otherText, setOtherText] = useState<Record<string, string>>({});

  const answers = useMemo(() => {
    const result: QuestionAnswers = {};
    for (const question of pending.questions) {
      const values = selected[question.question] ?? [];
      const custom = otherText[question.question]?.trim();
      const labels = values.filter((value) => value !== OTHER_VALUE);
      if (values.includes(OTHER_VALUE) && custom) labels.push(custom);
      if (labels.length) result[question.question] = labels.join(', ');
    }
    return result;
  }, [otherText, pending.questions, selected]);

  const complete = pending.questions.every((question) => Boolean(answers[question.question]));

  const select = (questionText: string, value: string, multiSelect: boolean) => {
    setSelected((current) => {
      const values = current[questionText] ?? [];
      if (!multiSelect) return { ...current, [questionText]: [value] };
      return {
        ...current,
        [questionText]: values.includes(value)
          ? values.filter((candidate) => candidate !== value)
          : [...values, value],
      };
    });
  };

  return (
    <div className="flex items-start gap-3" aria-live="polite" data-testid="pending-question">
      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-primary-border bg-primary-soft text-primary-strong shadow-sm">
        <CircleHelp className="h-5 w-5" aria-hidden="true" />
      </div>
      <form
        className="w-full max-w-[80%] rounded-2xl border border-primary-border bg-surface px-4 py-4 shadow-md"
        onSubmit={(event) => {
          event.preventDefault();
          if (complete) onSubmit(answers);
        }}
      >
        <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-primary-strong">
          Đang chờ bạn trả lời
        </p>
        <div className="mt-3 space-y-5">
          {pending.questions.map((question) => {
            const values = selected[question.question] ?? [];
            return (
              <fieldset key={question.question} className="space-y-2">
                <legend className="text-sm font-semibold text-foreground">
                  <span className="mr-2 rounded-full bg-surface-muted px-2 py-0.5 text-[10px] uppercase tracking-wide text-muted-foreground">
                    {question.header}
                  </span>
                  {question.question}
                </legend>
                <div className="grid gap-2 sm:grid-cols-2">
                  {question.options.map((option) => {
                    const active = values.includes(option.label);
                    return (
                      <label
                        key={option.label}
                        className={
                          active
                            ? 'cursor-pointer rounded-xl border border-primary bg-primary-soft px-3 py-2'
                            : 'cursor-pointer rounded-xl border border-border bg-surface-muted px-3 py-2 hover:border-primary-border'
                        }
                      >
                        <span className="flex items-start gap-2">
                          <input
                            type={question.multiSelect ? 'checkbox' : 'radio'}
                            name={question.question}
                            checked={active}
                            onChange={() =>
                              select(question.question, option.label, question.multiSelect)
                            }
                            className="mt-1"
                          />
                          <span>
                            <span className="block text-sm font-medium text-foreground">
                              {option.label}
                            </span>
                            <span className="block text-xs text-muted-foreground">
                              {option.description}
                            </span>
                          </span>
                        </span>
                      </label>
                    );
                  })}
                  <label className="cursor-pointer rounded-xl border border-border bg-surface-muted px-3 py-2 hover:border-primary-border">
                    <span className="flex items-start gap-2">
                      <input
                        type={question.multiSelect ? 'checkbox' : 'radio'}
                        name={question.question}
                        checked={values.includes(OTHER_VALUE)}
                        onChange={() =>
                          select(question.question, OTHER_VALUE, question.multiSelect)
                        }
                        className="mt-1"
                      />
                      <span className="w-full text-sm font-medium text-foreground">
                        Other
                        {values.includes(OTHER_VALUE) && (
                          <input
                            autoFocus
                            value={otherText[question.question] ?? ''}
                            onChange={(event) =>
                              setOtherText((current) => ({
                                ...current,
                                [question.question]: event.target.value,
                              }))
                            }
                            placeholder="Nhập câu trả lời của bạn"
                            className="mt-2 w-full rounded-lg border border-border bg-surface px-2.5 py-2 text-sm outline-none focus:border-primary-border"
                          />
                        )}
                      </span>
                    </span>
                  </label>
                </div>
              </fieldset>
            );
          })}
        </div>
        <Button
          type="submit"
          disabled={!complete || submitting}
          className="mt-4 inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50"
        >
          {submitting && <LoaderCircle className="h-4 w-4 animate-spin" aria-hidden="true" />}
          Gửi câu trả lời
        </Button>
      </form>
    </div>
  );
}
