'use client';

import { useState, useMemo } from 'react';
import { useParams, useRouter } from 'next/navigation';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  Form,
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormMessage,
} from '@/components/ui/form';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from '@/components/ui/alert-dialog';
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group';

import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { KnowledgeSchema, KnowledgeFormValues } from '@/schemas/knowledge-schemas';

import {
  KNOWLEDGE_TYPES,
  KNOWLEDGE_TYPE_OPTIONS,
  KnowledgeSourceType,
} from '@/constants/knowledge-types';

import { FileImportForm } from '@/components/knowledge/forms/file-import-form';
import { UrlImportForm } from '@/components/knowledge/forms/url-import-form';
import { SitemapImportForm } from '@/components/knowledge/forms/sitemap-import-form';
import { InlineImportForm } from '@/components/knowledge/forms/inline-import-form';
import { DatabaseImportForm } from '@/components/knowledge/forms/database-import-form';
import { ApiImportForm } from '@/components/knowledge/forms/api-import-form';

export default function AddKnowledgePage() {
  const router = useRouter();
  const { id: agentId } = useParams();

  // ----------------------------------------------
  // Form Setup
  // ----------------------------------------------
  const form = useForm<KnowledgeFormValues>({
    resolver: zodResolver(KnowledgeSchema),
    defaultValues: {
      sourceType: KNOWLEDGE_TYPES.FILE,
      name: '',
    },
  });

  const type = useWatch({ control: form.control, name: 'sourceType' });
  const isSubmitting = form.formState.isSubmitting;

  const disableSubmit = useMemo(
    () => !form.formState.isValid || isSubmitting,
    [form.formState.isValid, isSubmitting]
  );

  // ----------------------------------------------
  // Type Switch State
  // ----------------------------------------------
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pendingType, setPendingType] = useState<KnowledgeSourceType | null>(null);

  // ----------------------------------------------
  // Default values for each type
  // ----------------------------------------------
  const typeDefaults = {
    [KNOWLEDGE_TYPES.FILE]: {
      sourceType: KNOWLEDGE_TYPES.FILE,
      files: [],
    },

    [KNOWLEDGE_TYPES.WEB_URL]: {
      sourceType: KNOWLEDGE_TYPES.WEB_URL,
      url: '',
    },

    [KNOWLEDGE_TYPES.SITEMAP]: {
      sourceType: KNOWLEDGE_TYPES.SITEMAP,
      sitemapUrl: '',
    },

    [KNOWLEDGE_TYPES.INLINE]: {
      sourceType: KNOWLEDGE_TYPES.INLINE,
      inlineContent: '',
    },

    [KNOWLEDGE_TYPES.DATABASE]: {
      sourceType: KNOWLEDGE_TYPES.DATABASE,
      dbHost: '',
      dbPort: '',
      dbUser: '',
      dbPassword: '',
      dbQuery: '',
    },

    [KNOWLEDGE_TYPES.API]: {
      sourceType: KNOWLEDGE_TYPES.API,
      apiUrl: '',
      apiMethod: '',
    },
  };

  // ----------------------------------------------
  // Type Change Handler
  // ----------------------------------------------
  const applyTypeChange = (newType: KnowledgeSourceType) => {
    const preservedName = form.getValues('name') || '';

    form.reset(
      { ...typeDefaults[newType], name: preservedName },
      { keepErrors: false, keepDirty: false, keepTouched: false }
    );
  };

  // ----------------------------------------------
  // Submit Handler
  // ----------------------------------------------
  const onSubmit = async (values: KnowledgeFormValues) => {
    if (values.sourceType === 'FILE') {
      return submitFileKnowledge(values);
    }
    return submitNonFileKnowledge(values);
  };

  const submitFileKnowledge = async (values: KnowledgeFormValues) => {
    const formData = new FormData();

    values.files?.forEach((item) => {
      formData.append('files', item.file);
    });

    formData.append('name', values.name);

    const res = await fetch(`/api/agents/${agentId}/knowledge`, {
      method: 'POST',
      body: formData,
    });

    if (!res.ok) throw new Error('Failed to upload file knowledge');

    router.push(`/admin/agents/${agentId}/knowledge`);
  };

  const submitNonFileKnowledge = async (values: KnowledgeFormValues) => {
    const payload = {
      name: values.name,
      sourceType: values.sourceType,
      url: values.url,
      sitemapUrl: values.sitemapUrl,
      inlineContent: values.inlineContent,
      dbHost: values.dbHost,
      dbPort: values.dbPort,
      dbUser: values.dbUser,
      dbPassword: values.dbPassword,
      dbQuery: values.dbQuery,
      apiUrl: values.apiUrl,
      apiMethod: values.apiMethod,
    };

    const res = await fetch(`/api/agents/${agentId}/knowledge`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!res.ok) throw new Error('Failed to create knowledge');

    router.push(`/admin/agents/${agentId}/knowledge`);
  };

  // ----------------------------------------------
  // Render
  // ----------------------------------------------
  return (
    <div className="mx-auto max-w-3xl space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Add Knowledge</h1>
        <p className="text-muted-foreground">
          Import knowledge from files, websites, APIs, or databases.
        </p>
      </div>

      {/* Confirm Type Switch */}
      <AlertDialog
        open={confirmOpen}
        onOpenChange={(open) => {
          setConfirmOpen(open);
          if (!open) setPendingType(null);
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Change import type?</AlertDialogTitle>
            <AlertDialogDescription>
              Switching will clear fields for the current type. Continue?
            </AlertDialogDescription>
          </AlertDialogHeader>

          <AlertDialogFooter>
            <AlertDialogCancel>Stay</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                if (pendingType) applyTypeChange(pendingType);
                setConfirmOpen(false);
                setPendingType(null);
              }}
            >
              Continue
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Form */}
      <Form {...form}>
        <Card className="p-4">
          <FormField
            control={form.control}
            name="sourceType"
            render={() => (
              <FormItem>
                <FormLabel>Import Type</FormLabel>

                <ToggleGroup
                  type="single"
                  value={type}
                  className="flex flex-wrap gap-2 mt-2"
                  onValueChange={(v) => {
                    if (!v || v === type) return;

                    const newType = v as KnowledgeSourceType;
                    const current = form.getValues();

                    if (hasTypeSpecificData(type, current)) {
                      setPendingType(newType);
                      setConfirmOpen(true);
                    } else {
                      applyTypeChange(newType);
                    }
                  }}
                >
                  {KNOWLEDGE_TYPE_OPTIONS.map((t) => {
                    const Icon = t.icon;
                    return (
                      <ToggleGroupItem
                        key={t.id}
                        value={t.id}
                        className="px-4 py-2 rounded-md border data-[state=on]:bg-primary data-[state=on]:text-primary-foreground"
                      >
                        <Icon className="w-4 h-4" />
                        {t.label}
                      </ToggleGroupItem>
                    );
                  })}
                </ToggleGroup>

                <FormMessage />
              </FormItem>
            )}
          />
        </Card>

        <Card className="p-6 mt-6">
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
            {/* NAME */}
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Knowledge Name</FormLabel>
                  <FormControl>
                    <Input placeholder="Example: Company Documents" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* TYPE PANELS */}
            {type === KNOWLEDGE_TYPES.FILE && <FileImportForm form={form} />}
            {type === KNOWLEDGE_TYPES.WEB_URL && <UrlImportForm form={form} />}
            {type === KNOWLEDGE_TYPES.SITEMAP && <SitemapImportForm form={form} />}
            {type === KNOWLEDGE_TYPES.INLINE && <InlineImportForm form={form} />}
            {type === KNOWLEDGE_TYPES.DATABASE && <DatabaseImportForm form={form} />}
            {type === KNOWLEDGE_TYPES.API && <ApiImportForm form={form} />}

            <div className="flex justify-end gap-4 pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => router.push(`/admin/agents/${agentId}/knowledge`)}
              >
                Cancel
              </Button>

              <Button type="submit" disabled={disableSubmit}>
                {isSubmitting ? 'Saving...' : 'Save'}
              </Button>
            </div>
          </form>
        </Card>
      </Form>
    </div>
  );
}

// ----------------------------------------------
// Detect any filled data before switching type
// ----------------------------------------------
function hasTypeSpecificData(type: KnowledgeSourceType, values: KnowledgeFormValues): boolean {
  switch (type) {
    case KNOWLEDGE_TYPES.FILE:
      return (values.files?.length ?? 0) > 0;

    case KNOWLEDGE_TYPES.WEB_URL:
      return !!values.url?.trim();

    case KNOWLEDGE_TYPES.SITEMAP:
      return !!values.sitemapUrl?.trim();

    case KNOWLEDGE_TYPES.INLINE:
      return !!values.inlineContent?.trim();

    case KNOWLEDGE_TYPES.DATABASE:
      return !!(
        values.dbHost?.trim() ||
        values.dbPort?.trim() ||
        values.dbUser?.trim() ||
        values.dbPassword?.trim() ||
        values.dbQuery?.trim()
      );

    case KNOWLEDGE_TYPES.API:
      return !!values.apiUrl?.trim() || !!values.apiMethod?.trim();

    default:
      return false;
  }
}
