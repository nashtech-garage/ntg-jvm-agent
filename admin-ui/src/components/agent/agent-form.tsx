'use client';

import {
  useForm,
  type FieldValues,
  type ControllerRenderProps,
  type Path,
  type UseFormReturn,
} from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRef, useState, type ReactNode } from 'react';

import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';

import {
  Form,
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormMessage,
} from '@/components/ui/form';

import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Switch } from '@/components/ui/switch';
import { Button } from '@/components/ui/button';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';

import { AgentFormParams } from '@/types/agent';

import type * as monaco from 'monaco-editor';
import dynamic from 'next/dynamic';
import AvatarUpload from './avatar-upload';
const Monaco = dynamic(() => import('@monaco-editor/react'), { ssr: false });

// Zod schema for full Agent
const formSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().optional(),
  avatar: z.string().optional(),
  active: z.boolean(),
  provider: z.string().min(1, 'Provider is required'),
  model: z.string().min(1, 'Model is required'),
  apiKey: z.string().min(1, 'API Key is required'),
  baseUrl: z.string().min(1, 'Base URL is required'),
  chatCompletionsPath: z.string().min(1, 'Chat Completions Path is required'),
  temperature: z.number().min(0).max(2),
  maxTokens: z.number().min(1),
  topP: z.number().min(0).max(1),
  frequencyPenalty: z.number().min(-2).max(2),
  presencePenalty: z.number().min(-2).max(2),
  settings: z.any().optional(),
});
export type AgentFormValues = z.infer<typeof formSchema>;

// ---------------------- Main Form Component ----------------------
export default function AgentForm({ onSubmit, initialValues }: Readonly<AgentFormParams>) {
  const form = useForm<AgentFormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: initialValues || {
      name: '',
      description: '',
      avatar: undefined,
      active: true,
      provider: '',
      model: '',
      apiKey: '',
      baseUrl: '',
      chatCompletionsPath: '/v1/chat/completions',
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
      <h1 className="text-2xl font-bold">{isEdit ? 'Edit Agent' : 'Create Agent'}</h1>

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
                  <div className="flex justify-center mb-6">
                    <FormField
                      control={form.control}
                      name="avatar"
                      render={({ field }) => (
                        <AvatarUpload
                          currentAvatar={field.value}
                          agentName={form.getValues('name')}
                          onAvatarChange={field.onChange}
                        />
                      )}
                    />
                  </div>
                  <TextField<AgentFormValues> form={form} name="name" label="Name" />
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
                    <TextField<AgentFormValues> form={form} name="provider" label="Provider" />
                    <TextField<AgentFormValues> form={form} name="baseUrl" label="Base URL" />
                  </TwoColumn>
                  <TextField<AgentFormValues>
                    form={form}
                    name="apiKey"
                    label="API Key"
                    type="password"
                  />
                  <TextField<AgentFormValues>
                    form={form}
                    name="chatCompletionsPath"
                    label="Chat Completions Path"
                  />
                </TabsContent>

                {/* ------------------- MODEL ------------------- */}
                <TabsContent value="model" className="space-y-6 pt-4">
                  {/* ------------------- Provider Models ------------------- */}
                  <h3 className="text-lg font-semibold">Provider Models</h3>
                  <TextField<AgentFormValues> form={form} name="model" label="Model" />

                  {/* ------------------- Generation Parameters ------------------- */}
                  <h3 className="text-lg font-semibold">Generation Parameters</h3>
                  <TwoColumn>
                    <NumberField<AgentFormValues>
                      form={form}
                      name="topP"
                      label="Top P"
                      step="0.05"
                    />
                    <NumberField<AgentFormValues>
                      form={form}
                      name="temperature"
                      label="Temperature"
                      step="0.1"
                    />
                  </TwoColumn>
                  <TwoColumn>
                    <NumberField<AgentFormValues> form={form} name="maxTokens" label="Max Tokens" />
                    <NumberField<AgentFormValues>
                      form={form}
                      name="frequencyPenalty"
                      label="Frequency Penalty"
                      step="0.1"
                    />
                    <NumberField<AgentFormValues>
                      form={form}
                      name="presencePenalty"
                      label="Presence Penalty"
                      step="0.1"
                    />
                  </TwoColumn>

                  {/* ------------------- Advanced Settings ------------------- */}
                  <h3 className="text-lg font-semibold">Advanced Settings</h3>
                  <FormField
                    control={form.control}
                    name="settings"
                    render={({ field }) => (
                      <MonacoField<AgentFormValues, 'settings'>
                        field={field}
                        label="Settings (JSON)"
                      />
                    )}
                  />
                </TabsContent>
              </Tabs>

              <div className="pt-4 flex justify-end">
                <Button type="submit">{isEdit ? 'Update Agent' : 'Create Agent'}</Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  );
}

// ---------------------- MonacoField Component ----------------------

interface MonacoFieldProps<T extends FieldValues, N extends Path<T>> {
  field: ControllerRenderProps<T, N>;
  label?: string;
}

export function MonacoField<T extends FieldValues, N extends Path<T>>({
  field,
  label,
}: Readonly<MonacoFieldProps<T, N>>) {
  const [editorValue, setEditorValue] = useState(JSON.stringify(field.value ?? {}, null, 2));
  const [jsonError, setJsonError] = useState<string | null>(null);

  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);

  const handleEditorChange = (value: string = '{}') => {
    setEditorValue(value);

    try {
      // Update form value immediately
      const parsed = JSON.parse(value);
      field.onChange(parsed);
      setJsonError(null);
    } catch {
      setJsonError('Invalid JSON');
    }
  };

  return (
    <FormItem>
      {label && <FormLabel>{label}</FormLabel>}
      <FormControl>
        <div className="rounded-md border">
          <Monaco
            height="250px"
            defaultLanguage="json"
            value={editorValue}
            onChange={handleEditorChange}
            onMount={(editor) => {
              editorRef.current = editor;
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

interface TwoColumnProps {
  children: ReactNode;
}

export function TwoColumn({ children }: Readonly<TwoColumnProps>) {
  return <div className="grid grid-cols-1 md:grid-cols-2 gap-4">{children}</div>;
}

interface TextFieldProps<T extends object> {
  form: UseFormReturn<T>;
  name: Path<T>; // <-- use Path<T> instead of keyof T
  label: string;
  type?: string;
}

export function TextField<T extends object>({
  form,
  name,
  label,
  type = 'text',
}: Readonly<TextFieldProps<T>>) {
  return (
    <FormField
      control={form.control}
      name={name} // now fully type-safe
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

interface NumberFieldProps<T extends object> {
  form: UseFormReturn<T>;
  name: Path<T>; // <-- Use Path<T> instead
  label: string;
  step?: string | number;
}

export function NumberField<T extends object>({
  form,
  name,
  label,
  step = 1,
}: Readonly<NumberFieldProps<T>>) {
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
              value={field.value ?? ''}
              onChange={(e) => field.onChange(Number(e.target.value))}
            />
          </FormControl>
          <FormMessage />
        </FormItem>
      )}
    />
  );
}
