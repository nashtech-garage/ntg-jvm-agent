'use client';

import { FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/components/ui/form';

import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';

import { UseFormReturn } from 'react-hook-form';
import { KnowledgeFormValues } from '@/schemas/knowledge-schemas';

export function DatabaseImportForm({
  form,
}: Readonly<{
  form: UseFormReturn<KnowledgeFormValues>;
}>) {
  return (
    <>
      {[
        ['dbHost', 'Host'],
        ['dbPort', 'Port'],
        ['dbUser', 'User'],
        ['dbPassword', 'Password'],
      ].map(([fieldName, label]) => (
        <FormField
          key={fieldName}
          control={form.control}
          name={fieldName as keyof KnowledgeFormValues}
          render={({ field }) => (
            <FormItem>
              <FormLabel>{label}</FormLabel>
              <FormControl>
                <Input
                  name={field.name}
                  ref={field.ref}
                  onBlur={field.onBlur}
                  onChange={field.onChange}
                  value={Array.isArray(field.value) ? '' : (field.value ?? '')}
                  type={fieldName === 'dbPassword' ? 'password' : 'text'}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      ))}

      <FormField
        control={form.control}
        name="dbQuery"
        render={({ field }) => (
          <FormItem>
            <FormLabel>SQL Query</FormLabel>
            <FormControl>
              <Textarea rows={4} placeholder="SELECT * FROM ..." {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </>
  );
}
