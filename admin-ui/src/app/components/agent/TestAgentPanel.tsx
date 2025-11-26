"use client";

import { useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";

export default function TestAgentPanel({ agentId }: { agentId: string }) {
  const [input, setInput] = useState("");
  const [response, setResponse] = useState("");

  async function handleTest() {
    if (!input.trim()) return;

    const res = await fetch(`/api/agents/${agentId}/test`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ input }),
    });

    const data = await res.json();
    setResponse(data.output || "No response");
  }

  return (
    <Card className="h-fit rounded-sm">
      <CardHeader>
        <CardTitle>Test Your Agent</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <Textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask something..."
        />
        <Button className="w-full" onClick={handleTest}>
          Send
        </Button>

        {response && (
          <div className="mt-4 p-3 rounded-xl border bg-muted">
            <p>{response}</p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
