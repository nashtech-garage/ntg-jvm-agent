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
import {
  McpToolAuthenticationFormValues,
  McpToolAuthenticationSchema,
} from '@/schemas/mcp-tool-authentication-schemas';
import {
  MCP_TOOL_AUTHENTICATION_OPTIONS,
  MCP_TOOL_AUTHENTICATION_TYPES,
  McpToolAuthenticationSourceType,
} from '@/constants/mcp-tool-authentication-types';
import { APIKeyAuthForm } from '@/components/tool/forms/api-key-auth-form';
import { JWTBearerAuthForm } from '@/components/tool/forms/jwt-bearer-auth-form';
import { CustomHeaderAuthForm } from '@/components/tool/forms/custom-header-auth-form';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Label } from '@/components/ui/label';

export default function AddToolPage() {
  const router = useRouter();
  const { id: agentId } = useParams();

  // ----------------------------------------------
  // Form Setup
  // ----------------------------------------------
  const form = useForm<McpToolAuthenticationFormValues>({
    resolver: zodResolver(McpToolAuthenticationSchema),
    defaultValues: {
      sourceType: MCP_TOOL_AUTHENTICATION_TYPES.NONE,
      transportType: 'SSE',
      baseUrl: '',
      endpoint: '',
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
  const [pendingType, setPendingType] = useState<McpToolAuthenticationSourceType | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>('');

  // ----------------------------------------------
  // Default values for each type
  // ----------------------------------------------
  const typeDefaults = {
    [MCP_TOOL_AUTHENTICATION_TYPES.NONE]: {
      sourceType: MCP_TOOL_AUTHENTICATION_TYPES.NONE,
    },

    [MCP_TOOL_AUTHENTICATION_TYPES.API_KEY]: {
      sourceType: MCP_TOOL_AUTHENTICATION_TYPES.API_KEY,
    },

    [MCP_TOOL_AUTHENTICATION_TYPES.BEARER]: {
      sourceType: MCP_TOOL_AUTHENTICATION_TYPES.BEARER,
    },

    [MCP_TOOL_AUTHENTICATION_TYPES.CUSTOM_HEADER]: {
      sourceType: MCP_TOOL_AUTHENTICATION_TYPES.CUSTOM_HEADER,
    },
  };

  // ----------------------------------------------
  // Type Change Handler
  // ----------------------------------------------
  const applyTypeChange = (newType: McpToolAuthenticationSourceType) => {
    const preservedTransportType = form.getValues('transportType');
    const preservedBaseUrl = form.getValues('baseUrl') || '';
    const preservedEndpoint = form.getValues('endpoint') || '';

    form.reset(
      {
        ...typeDefaults[newType],
        baseUrl: preservedBaseUrl,
        endpoint: preservedEndpoint,
        transportType: preservedTransportType,
      },
      { keepErrors: false, keepDirty: false, keepTouched: false }
    );
  };

  // ----------------------------------------------
  // Submit Handler
  // ----------------------------------------------
  const onSubmit = async (values: McpToolAuthenticationFormValues) => {
    const payload = {
      transportType: values.transportType,
      baseUrl: values.baseUrl,
      endpoint: values.endpoint,
      authorization: {
        type: values.sourceType,
        headerName: values.headerName,
        token: values.token,
      },
    };

    const res = await fetch(`/api/agents/${agentId}/tools`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      setErrorMessage(await res.text());
      return;
    }

    router.push(`/admin/agents/${agentId}/tools`);
  };

  // ----------------------------------------------
  // Render
  // ----------------------------------------------
  return (
    <div className="mx-auto max-w-3xl space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Add Tool</h1>
        <p className="text-muted-foreground">Add more tool from MCP Server.</p>
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
                <FormLabel>Authentication Type</FormLabel>

                <ToggleGroup
                  type="single"
                  value={type}
                  className="flex flex-wrap gap-2 mt-2"
                  onValueChange={(v) => {
                    if (!v || v === type) return;

                    const newType = v as McpToolAuthenticationSourceType;
                    const current = form.getValues();

                    if (hasTypeSpecificData(type, current)) {
                      setPendingType(newType);
                      setConfirmOpen(true);
                    } else {
                      applyTypeChange(newType);
                    }
                  }}
                >
                  {MCP_TOOL_AUTHENTICATION_OPTIONS.map((t) => {
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
            <FormField
              control={form.control}
              name="transportType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Transport type</FormLabel>
                  <FormControl>
                    <RadioGroup value={field.value} onValueChange={field.onChange}>
                      <div className="flex items-center gap-3">
                        <RadioGroupItem value="SSE" id="sse" />
                        <Label htmlFor="sse">SSE</Label>
                      </div>
                      <div className="flex items-center gap-3">
                        <RadioGroupItem value="Streamable" id="streamable" disabled />
                        <Label htmlFor="streamable">Streamable HTTP</Label>
                      </div>
                    </RadioGroup>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="baseUrl"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Base URL</FormLabel>
                  <FormControl>
                    <Input placeholder="Example: https://mcpserver.com" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="endpoint"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Endpoint</FormLabel>
                  <FormControl>
                    <Input placeholder="Example:/sse" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* TYPE PANELS */}
            {type === MCP_TOOL_AUTHENTICATION_TYPES.NONE}
            {type === MCP_TOOL_AUTHENTICATION_TYPES.API_KEY && <APIKeyAuthForm form={form} />}
            {type === MCP_TOOL_AUTHENTICATION_TYPES.BEARER && <JWTBearerAuthForm form={form} />}
            {type === MCP_TOOL_AUTHENTICATION_TYPES.CUSTOM_HEADER && (
              <CustomHeaderAuthForm form={form} />
            )}

            <div className="flex justify-end gap-4 pt-4">
              <div>
                <p className="text-red-700">{errorMessage}</p>
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={() => router.push(`/admin/agents/${agentId}/tools`)}
              >
                Cancel
              </Button>

              <Button type="submit" disabled={disableSubmit}>
                {isSubmitting ? 'Verifying and saving...' : 'Verify and Save'}
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
function hasTypeSpecificData(
  type: McpToolAuthenticationSourceType,
  values: McpToolAuthenticationFormValues
): boolean {
  switch (type) {
    case MCP_TOOL_AUTHENTICATION_TYPES.API_KEY:
      return !!values.token?.trim();

    case MCP_TOOL_AUTHENTICATION_TYPES.BEARER:
      return !!values.token?.trim();

    case MCP_TOOL_AUTHENTICATION_TYPES.CUSTOM_HEADER:
      return !!values.headerName?.trim() || !!values.token?.trim();

    default:
      return false;
  }
}
