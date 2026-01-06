export default function UsageEmptyState() {
  return (
    <div className="rounded-lg border border-dashed p-10 text-center">
      <h3 className="text-lg font-medium">No usage data yet</h3>
      <p className="mt-2 text-sm text-muted-foreground">
        Token usage will appear here after the first aggregation job runs.
      </p>
    </div>
  );
}
