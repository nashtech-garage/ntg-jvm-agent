'use client';

import { FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { UseFormReturn } from 'react-hook-form';
import { KnowledgeFormValues } from '@/schemas/knowledge-schemas';

export function SitemapImportForm({
  form,
}: Readonly<{
  form: UseFormReturn<KnowledgeFormValues>;
}>) {
  return (
    <FormField
      control={form.control}
      name="sitemapUrl"
      render={({ field }) => (
        <FormItem>
          <FormLabel>Sitemap URL</FormLabel>
          <FormControl>
            <Input placeholder="https://example.com/sitemap.xml" {...field} />
          </FormControl>
          <FormMessage />
        </FormItem>
      )}
    />
  );
}
