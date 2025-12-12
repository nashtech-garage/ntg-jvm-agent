'use client';

import { FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { UseFormReturn } from 'react-hook-form';
import { McpToolAuthenticationFormValues } from '@/schemas/mcp-tool-authentication-schemas';

export function CustomHeaderAuthForm({
  form,
}: Readonly<{
  form: UseFormReturn<McpToolAuthenticationFormValues>;
}>) {
  return (
    <>
      <FormField
        control={form.control}
        name="headerName"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Custom header name</FormLabel>
            <FormControl>
              <Input placeholder="custom-header-name" {...field} />
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
            <FormLabel>Custom header value</FormLabel>
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
