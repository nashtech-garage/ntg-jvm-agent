"use client";

import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";

import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "@/components/ui/card";

import {
  Form,
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormMessage,
} from "@/components/ui/form";

import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";

import dynamic from "next/dynamic";
const Monaco = dynamic(() => import("@monaco-editor/react"), { ssr: false });

// Zod schema for full Agent
const formSchema = z.object({
  name: z.string().min(1, "Name is required"),
  description: z.string().optional(),
  active: z.boolean(),
  provider: z.string().min(1, "Provider is required"),
  model: z.string().min(1, "Model is required"),
  apiKey: z.string().min(1, "API Key is required"),
  baseUrl: z.string().min(1, "Base URL is required"),
  chatCompletionsPath: z.string().min(1, "Chat Completions Path is required"),
  embeddingsPath: z.string().min(1, "Embeddings API Path is required"),
  embeddingModel: z.string().min(1, "Embedding Model is required"),
  dimension: z.number().min(1),
  temperature: z.number().min(0).max(2),
  maxTokens: z.number().min(1),
  topP: z.number().min(0).max(1),
  frequencyPenalty: z.number().min(-2).max(2),
  presencePenalty: z.number().min(-2).max(2),
  settings: z.any().optional(),
});

// ---------------------- Main Form Component ----------------------
export default function AgentForm({ onSubmit, initialValues }: any) {
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: initialValues || {
      name: "",
      description: "",
      active: true,
      provider: "",
      model: "",
      apiKey: "",
      baseUrl: "",
      chatCompletionsPath: "/v1/chat/completions",
      embeddingsPath: "/v1/embeddings",
      embeddingModel: "",
      dimension: 1536,
      temperature: 0.7,
      maxTokens: 2048,
      topP: 1,
      frequencyPenalty: 0,
      presencePenalty: 0,
      settings: {},
    },
  });

  // Detect if it's create or edit
  const isEdit = !!initialValues?.id;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">
        {isEdit ? "Edit Agent" : "Create Agent"}
      </h1>

      <Card>
        <CardHeader className="p-3">
          <CardTitle></CardTitle>
        </CardHeader>

        <CardContent>
          <Form {...form}>
            <form className="space-y-6" onSubmit={form.handleSubmit(onSubmit)}>
              <Tabs defaultValue="general">
                <TabsList>
                  <TabsTrigger value="general">General</TabsTrigger>
                  <TabsTrigger value="provider">Provider</TabsTrigger>
                  <TabsTrigger value="model">Model</TabsTrigger>
                </TabsList>

                {/* ------------------- GENERAL ------------------- */}
                <TabsContent value="general" className="space-y-4 pt-4">
                  <TextField form={form} name="name" label="Name" />
                  <FormField
                    control={form.control}
                    name="description"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Description</FormLabel>
                        <FormControl>
                          <Textarea placeholder="Agent description..." {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="active"
                    render={({ field }) => (
                      <FormItem className="flex items-center gap-3">
                        <FormLabel>Active</FormLabel>
                        <FormControl>
                          <Switch checked={field.value} onCheckedChange={field.onChange} />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                </TabsContent>

                {/* ------------------- PROVIDER ------------------- */}
                <TabsContent value="provider" className="space-y-4 pt-4">
                  <TwoColumn>
                    <TextField form={form} name="provider" label="Provider" />
                    <TextField form={form} name="baseUrl" label="Base URL" />
                  </TwoColumn>
                  <TextField form={form} name="apiKey" label="API Key" type="password" />
                  <TextField form={form} name="chatCompletionsPath" label="Chat Completions Path" />
                </TabsContent>

                {/* ------------------- MODEL ------------------- */}
                <TabsContent value="model" className="space-y-6 pt-4">
                  {/* ------------------- Provider Models ------------------- */}
                  <h3 className="text-lg font-semibold">Provider Models</h3>
                  <TextField form={form} name="model" label="Model" />

                  {/* ------------------- Embedding Configuration ------------------- */}
                  <h3 className="text-lg font-semibold">Embedding Configuration</h3>
                  <TwoColumn>
                    <TextField form={form} name="embeddingModel" label="Embedding Model" />
                    <NumberField form={form} name="dimension" label="Vector Dimension" />
                  </TwoColumn>
                  <TextField form={form} name="embeddingsPath" label="Embeddings API Path" />

                  {/* ------------------- Generation Parameters ------------------- */}
                  <h3 className="text-lg font-semibold">Generation Parameters</h3>
                  <TwoColumn>
                    <NumberField form={form} name="topP" label="Top P" step="0.05" />
                    <NumberField form={form} name="temperature" label="Temperature" step="0.1" />
                  </TwoColumn>
                  <TwoColumn>
                    <NumberField form={form} name="maxTokens" label="Max Tokens" />
                    <NumberField form={form} name="frequencyPenalty" label="Frequency Penalty" step="0.1" />
                    <NumberField form={form} name="presencePenalty" label="Presence Penalty" step="0.1" />
                  </TwoColumn>

                  {/* ------------------- Advanced Settings ------------------- */}
                  <h3 className="text-lg font-semibold">Advanced Settings</h3>
                  <FormField
                    control={form.control}
                    name="settings"
                    render={({ field }) => <MonacoField field={field} label="Settings (JSON)" />}
                  />
                </TabsContent>
              </Tabs>

              <div className="pt-4 flex justify-end">
                <Button type="submit">
                  {isEdit ? "Update Agent" : "Create Agent"}
                </Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  );
}

// ---------------------- MonacoField Component ----------------------

function MonacoField({
  field,
  label,
}: Readonly<{
  field: any;
  label: string;
}>) {
  const [editorValue, setEditorValue] = useState(
    JSON.stringify(field.value ?? {}, null, 2)
  );
  const [jsonError, setJsonError] = useState<string | null>(null);

  return (
    <FormItem>
      <FormLabel>{label}</FormLabel>
      <FormControl>
        <div className="rounded-md border">
          <Monaco
            height="250px"
            defaultLanguage="json"
            value={editorValue}
            onChange={(v) => setEditorValue(v || "{}")}
            onBlur={() => {
              try {
                field.onChange(JSON.parse(editorValue));
                setJsonError(null);
              } catch {
                setJsonError("Invalid JSON");
              }
            }}
            options={{ minimap: { enabled: false }, fontSize: 14 }}
          />
        </div>
      </FormControl>
      {jsonError && <p className="text-red-500 text-sm">{jsonError}</p>}
      <FormMessage />
    </FormItem>
  );
}

// ---------------------- Reusable Components ----------------------

function TwoColumn({ children }) {
  return <div className="grid grid-cols-1 md:grid-cols-2 gap-4">{children}</div>;
}

function TextField({ form, name, label, type = "text" }) {
  return (
    <FormField
      control={form.control}
      name={name}
      render={({ field }) => (
        <FormItem>
          <FormLabel>{label}</FormLabel>
          <FormControl>
            <Input type={type} {...field} />
          </FormControl>
          <FormMessage />
        </FormItem>
      )}
    />
  );
}

function NumberField({ form, name, label, step = "1" }) {
  return (
    <FormField
      control={form.control}
      name={name}
      render={({ field }) => (
        <FormItem>
          <FormLabel>{label}</FormLabel>
          <FormControl>
            <Input
              type="number"
              step={step}
              value={field.value ?? ""}
              onChange={(e) => field.onChange(Number(e.target.value))}
            />
          </FormControl>
          <FormMessage />
        </FormItem>
      )}
    />
  );
}
