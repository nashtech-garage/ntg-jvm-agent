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
import { useEffect, useRef, useState, type ReactNode } from 'react';

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
import { Dropdown } from '../dropdown';
import { Provider, ProviderDropDownItem } from '@/types/provider';
import logger from '@/utils/logger';
import { PROVIDER_PATH } from '@/constants/url';
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
  embeddingsPath: z.string().min(1, 'Embeddings API Path is required'),
  embeddingModel: z.string().min(1, 'Embedding Model is required'),
  dimension: z.number().min(1),
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
      embeddingsPath: '/v1/embeddings',
      embeddingModel: '',
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

  const [providers, setProviders] = useState<ProviderDropDownItem[]>([]);
  const [selectedProvider, setSelectedProvider] = useState<string | null>(null);
  const [selectedModelId, setSelectedModelId] = useState<string | null>(null);
  const [selectedEmbeddingModelId, setSelectedEmbeddingModelId] = useState<string | null>(null);

  useEffect(() => {
    async function loadProviders() {
      try {
        const response = await fetch(PROVIDER_PATH.PROVIDERS);

        if (!response.ok) {
          throw new Error(`Failed to load providers: ${response.status}`);
        }

        const data = await response.json();
        setProviders(data);
      } catch (error) {
        logger.error('Error fetching providers:', error);
      }
    }
    loadProviders();
  }, []);

  const [providerDetail, setProviderDetail] = useState<Provider | null>(null);

  const loadProviderDetail = async (id: string): Promise<Provider> => {
    try {
      const res = await fetch(PROVIDER_PATH.PROVIDER_DETAIL(id));

      if (!res.ok) {
        const error = new Error(
          `Failed to load provider detail for id "${id}": ${res.status} ${res.statusText}`
        );
        logger.error(error.message);
        throw error;
      }
      return res.json();
    } catch (err) {
      logger.error('Error fetching provider detail:', err);
      throw err;
    }
  };

  const loadAndSetProviderDetail = async (providerId: string) => {
    const provider = providers.find((p) => p.id === providerId);
    if (!provider?.active) return;

    setSelectedProvider(providerId);
    form.setValue('provider', provider.name);

    const data = await loadProviderDetail(providerId);
    setProviderDetail(data);

    // autofill provider-level fields
    form.setValue('baseUrl', data.baseUrl);
    form.setValue('chatCompletionsPath', data.chatCompletionsPath);
    form.setValue('embeddingsPath', data.embeddingsPath);

    // default model
    if (data.models?.length) {
      form.setValue('model', data.models[0].modelName);
      setSelectedModelId(data.models[0].id);
    }

    // default embedding model
    if (data.embeddingModels?.length) {
      form.setValue('embeddingModel', data.embeddingModels[0].embeddingName);
      setSelectedEmbeddingModelId(data.embeddingModels[0].id);
    }
  };

  useEffect(() => {
    if (!providerDetail) return;

    // sync model dropdown
    const modelName = form.getValues('model');
    if (modelName) {
      const matchedModel = providerDetail.models?.find((m) => m.modelName === modelName);
      if (matchedModel) {
        setSelectedModelId(matchedModel.id);
        form.setValue('model', matchedModel.modelName);
        form.setValue('topP', matchedModel.defaultTopP);
        form.setValue('temperature', matchedModel.defaultTemperature);
        form.setValue('maxTokens', matchedModel.defaultMaxTokens);
        form.setValue('frequencyPenalty', matchedModel.defaultFrequencyPenalty);
        form.setValue('presencePenalty', matchedModel.defaultPresencePenalty);
      }
    }

    // sync embedding model dropdown
    const embeddingName = form.getValues('embeddingModel');
    if (embeddingName) {
      const matchedEmbedding = providerDetail.embeddingModels?.find(
        (e) => e.embeddingName === embeddingName
      );
      if (matchedEmbedding) {
        setSelectedEmbeddingModelId(matchedEmbedding.id);
        form.setValue('embeddingModel', matchedEmbedding.embeddingName);
        form.setValue('dimension', matchedEmbedding.dimension);
      }
    }
  }, [providerDetail, form]);

  const handleProviderChange = async (providerId: string) => {
    try {
      await loadAndSetProviderDetail(providerId);
    } catch (err) {
      logger.error('Failed to fetch provider detail:', err);
    }
  };

  useEffect(() => {
    if (!initialValues?.provider || providers.length === 0) return;

    const matched = providers.find((p) => p.name === initialValues.provider);
    if (!matched) return;

    loadAndSetProviderDetail(matched.id).catch((err) => {
      logger.error('Failed to init provider detail:', err);
    });
  }, [providers, initialValues?.provider]);

  const handleModelChange = (id: string) => {
    setSelectedModelId(id);
    const selected = providerDetail?.models?.find((m) => m.id === id);
    if (!selected) {
      return;
    }
    form.setValue('model', selected.modelName);
    form.setValue('topP', selected.defaultTopP);
    form.setValue('temperature', selected.defaultTemperature);
    form.setValue('maxTokens', selected.defaultMaxTokens);
    form.setValue('frequencyPenalty', selected.defaultFrequencyPenalty);
    form.setValue('presencePenalty', selected.defaultPresencePenalty);
  };

  const handleEmbeddingModelChange = (id: string) => {
    setSelectedEmbeddingModelId(id);
    const selected = providerDetail?.embeddingModels?.find((e) => e.id === id);
    if (!selected) return;
    form.setValue('embeddingModel', selected.embeddingName);
    form.setValue('dimension', selected.dimension);
  };

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
                    <Dropdown
                      label="Provider"
                      options={providers.map((p) => ({
                        label: p.name,
                        value: p.id,
                      }))}
                      value={selectedProvider}
                      onChange={handleProviderChange}
                    />

                    <TextField<AgentFormValues>
                      form={form}
                      name="baseUrl"
                      label="Base URL"
                      readOnly
                    />
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
                    readOnly
                  />
                </TabsContent>

                {/* ------------------- MODEL ------------------- */}
                <TabsContent value="model" className="space-y-6 pt-4">
                  {/* ------------------- Provider Models ------------------- */}
                  <h3 className="text-lg font-semibold">Provider Models</h3>

                  <Dropdown
                    label="Model"
                    options={
                      providerDetail?.models?.map((m) => ({
                        label: m.modelName,
                        value: m.id,
                      })) ?? []
                    }
                    value={selectedModelId}
                    onChange={handleModelChange}
                  />

                  {/* ------------------- Embedding Configuration ------------------- */}
                  <h3 className="text-lg font-semibold">Embedding Configuration</h3>
                  <TwoColumn>
                    <Dropdown
                      label="Embedding Model"
                      options={
                        providerDetail?.embeddingModels?.map((e) => ({
                          label: e.embeddingName,
                          value: e.id,
                        })) ?? []
                      }
                      value={selectedEmbeddingModelId}
                      onChange={handleEmbeddingModelChange}
                    />

                    <NumberField<AgentFormValues>
                      form={form}
                      name="dimension"
                      label="Vector Dimension"
                    />
                  </TwoColumn>
                  <TextField<AgentFormValues>
                    form={form}
                    name="embeddingsPath"
                    label="Embeddings API Path"
                    readOnly
                  />

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
  readOnly?: boolean;
}

export function TextField<T extends object>({
  form,
  name,
  label,
  type = 'text',
  readOnly = false,
}: Readonly<TextFieldProps<T>>) {
  return (
    <FormField
      control={form.control}
      name={name} // now fully type-safe
      render={({ field }) => (
        <FormItem>
          <FormLabel>{label}</FormLabel>
          <FormControl>
            <Input
              type={type}
              {...field}
              readOnly={readOnly}
              onChange={readOnly ? undefined : field.onChange}
              className={readOnly ? 'bg-muted cursor-not-allowed' : ''}
            />
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
