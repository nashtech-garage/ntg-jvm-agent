'use client';

import { FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/components/ui/form';
import DropzoneMulti from '@/components/knowledge/dropzone/dropzone-multi';
import { KnowledgeFormValues } from '@/schemas/knowledge-schemas';
import { UseFormReturn } from 'react-hook-form';

interface FileImportFormProps {
  form: UseFormReturn<KnowledgeFormValues>;
}

export function FileImportForm({ form }: Readonly<FileImportFormProps>) {
  return (
    <div className="space-y-6">
      {/* FILES UPLOAD */}
      <FormField
        control={form.control}
        name="files"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Upload Files</FormLabel>
            <FormControl>
              <DropzoneMulti
                files={field.value ?? []}
                onChange={(value) => field.onChange(value)}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  );
}
