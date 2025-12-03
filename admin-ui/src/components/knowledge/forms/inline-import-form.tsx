'use client';

import { FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/components/ui/form';
import { Textarea } from '@/components/ui/textarea';
import { UseFormReturn } from 'react-hook-form';
import { KnowledgeFormValues } from '@/schemas/knowledge-schemas';

export function InlineImportForm({
  form,
}: Readonly<{
  form: UseFormReturn<KnowledgeFormValues>;
}>) {
  return (
    <FormField
      control={form.control}
      name="inlineContent"
      render={({ field }) => (
        <FormItem>
          <FormLabel>Text Content</FormLabel>
          <FormControl>
            <Textarea rows={6} placeholder="Paste your text..." {...field} />
          </FormControl>
          <FormMessage />
        </FormItem>
      )}
    />
  );
}
