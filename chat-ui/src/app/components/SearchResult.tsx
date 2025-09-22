export default function SearchResult({
  results,
}: Readonly<{
  results: Result[];
}>) {
  if (results.length === 0) {
    return <p className="text-gray-500">Search results will appear here ...</p>;
  }

  return (
    <div className="space-y-4">
      {results.map((r, i) => (
        <div key={r.id} className="p-3 rounded bg-gray-50">
          <p className="font-medium text-blue-600">You: {r.query}</p>
          <p className="mt-1">Answer: {r.answer}</p>
        </div>
      ))}
    </div>
  );
}
