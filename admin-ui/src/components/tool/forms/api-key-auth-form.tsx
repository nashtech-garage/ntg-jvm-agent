'use client';

import { FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { UseFormReturn } from 'react-hook-form';
import { McpToolAuthenticationFormValues } from '@/schemas/mcp-tool-authentication-schemas';
import { Label } from '@/components/ui/label';

export function APIKeyAuthForm({
  form,
}: Readonly<{
  form: UseFormReturn<McpToolAuthenticationFormValues>;
}>) {
  return (
    <>
      <FormField
        control={form.control}
        name="headerName"
        render={() => (
          <FormItem>
            <FormLabel>Header name</FormLabel>
            <FormControl>
              <Input value="X-API-Key" readOnly disabled />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
      <FormField
        control={form.control}
        name="token"
        render={({ field }) => (
          <FormItem>
            <FormLabel>API key</FormLabel>
            <FormControl>
              <Input placeholder="xxxxxxxxxxxxxxx" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </>
  );
}
