'use client';

import { useCallback } from 'react';
import { Toaster, toast } from 'sonner';

/**
 * Options to control a toast namespace and duplication behavior.
 * @property id Optional base identifier to namespace success/error toasts; set when multiple screens/components coexist to avoid collisions.
 * @property limit When true (default), dismisses an existing toast with the same derived id before showing a new one; set false to allow stacking.
 */
interface ToastOptions {
  id?: string;
  limit?: boolean;
}

export function ToasterProvider() {
  return <Toaster richColors position="top-right" duration={1500} />;
}

export function useToaster(options?: ToastOptions) {
  const { id = 'app-toaster', limit = true } = options || {};

  const dismiss = useCallback((id: string) => {
    toast.dismiss(id);
  }, []);

  const showError = useCallback(
    (message: string) => {
      if (limit) {
        const errorToasterId = `${id}-error`;
        dismiss(errorToasterId);
        return toast.error(message, { id: errorToasterId });
      }
      return toast.error(message);
    },
    [dismiss, limit, id]
  );

  const showSuccess = useCallback(
    (message: string) => {
      if (limit) {
        const successToasterId = `${id}-success`;
        dismiss(successToasterId);
        return toast.success(message, { id: successToasterId });
      }
      return toast.success(message);
    },
    [dismiss, limit, id]
  );

  return {
    dismiss,
    showError,
    showSuccess,
  };
}
