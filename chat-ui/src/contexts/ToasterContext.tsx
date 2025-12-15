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

/**
 * Creates namespaced success/error toast helpers.
 * @returns Object with `successToaster` and `errorToaster` that show the toast and return its id.
 */
export function useToaster(options?: ToastOptions) {
  const toasterId = options?.id ?? 'app-toaster';
  const limit = options?.limit ?? true;

  const dismiss = useCallback((id: string) => {
    toast.dismiss(id);
  }, []);

  const showError = useCallback(
    (message: string) => {
      const errorToasterId = `${toasterId}-error`;
      if (limit) {
        dismiss(errorToasterId);
      }
      return toast.error(message, { id: errorToasterId });
    },
    [dismiss, limit, toasterId]
  );

  const showSuccess = useCallback(
    (message: string) => {
      const successToasterId = `${toasterId}-success`;
      if (limit) {
        dismiss(successToasterId);
      }
      return toast.success(message, { id: successToasterId });
    },
    [dismiss, limit, toasterId]
  );

  return {
    dismiss,
    showError,
    showSuccess,
  };
}
