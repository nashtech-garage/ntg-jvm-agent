'use client';

import { useCallback } from 'react';
import { Toaster, toast } from 'react-hot-toast';

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
  return (
    <Toaster
      position="top-right"
      gutter={8}
      toastOptions={{
        duration: 3000,
        className: 'app-toast',
        success: {
          className: 'app-toast app-toast-success',
        },
        error: {
          className: 'app-toast app-toast-error',
        },
      }}
    />
  );
}

export function useToaster(options?: ToastOptions) {
  const { id, limit } = options || { id: 'app-toaster', limit: true };

  const dismiss = useCallback((id: string) => {
    toast.dismiss(id);
  }, []);

  const showError = useCallback(
    (message: string) => {
      const errorToasterId = `${id}-error`;
      if (limit) {
        dismiss(errorToasterId);
      }
      return toast.error(message, { id: errorToasterId });
    },
    [dismiss, limit, id]
  );

  const showSuccess = useCallback(
    (message: string) => {
      const successToasterId = `${id}-success`;
      if (limit) {
        dismiss(successToasterId);
      }
      return toast.success(message, { id: successToasterId });
    },
    [dismiss, limit, id]
  );

  return {
    dismiss,
    showError,
    showSuccess,
  };
}
