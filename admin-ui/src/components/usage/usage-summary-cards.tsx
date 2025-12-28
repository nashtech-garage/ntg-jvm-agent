type Props = {
  summary: {
    totalTokens: number;
    promptTokens: number;
    completionTokens: number;
  };
};

export default function UsageSummaryCards({ summary }: Readonly<Props>) {
  const items = [
    { label: 'Total Tokens', value: summary.totalTokens },
    { label: 'Prompt Tokens', value: summary.promptTokens },
    { label: 'Completion Tokens', value: summary.completionTokens },
  ];

  return (
    <div className="grid grid-cols-3 gap-3">
      {items.map((item) => (
        <div key={item.label} className="rounded-xl border bg-white p-4 shadow-sm">
          <div className="text-sm text-muted-foreground">{item.label}</div>
          <div className="text-2xl font-bold">{item.value}</div>
        </div>
      ))}
    </div>
  );
}
