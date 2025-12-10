'use client';

import { FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/components/ui/form';

import { Input } from '@/components/ui/input';
import { UseFormReturn } from 'react-hook-form';
import { KnowledgeFormValues } from '@/schemas/knowledge-schemas';

export function ApiImportForm({
  form,
}: Readonly<{
  form: UseFormReturn<KnowledgeFormValues>;
}>) {
  return (
    <>
      <FormField
        control={form.control}
        name="apiUrl"
        render={({ field }) => (
          <FormItem>
            <FormLabel>API URL</FormLabel>
            <FormControl>
              <Input placeholder="https://api.example.com/data" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      <FormField
        control={form.control}
        name="apiMethod"
        render={({ field }) => (
          <FormItem>
            <FormLabel>HTTP Method</FormLabel>
            <FormControl>
              <Input placeholder="GET / POST" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </>
  );
}
