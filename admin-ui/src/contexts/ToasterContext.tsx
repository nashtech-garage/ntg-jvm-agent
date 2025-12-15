'use client';

import { Toaster, toast } from 'react-hot-toast';

interface ToastOptions {
  id?: string;
  limit?: boolean;
}

export function ToasterProvider() {
  return (
    <Toaster
      position="top-right"
      gutter={8}
      reverseOrder={false}
      toastOptions={{
        duration: 3000,
        className: 'app-toast',
        success: {
          className: 'app-toast app-toast-success',
        },
        error: {
          duration: 4500,
          className: 'app-toast app-toast-error',
        },
      }}
    />
  );
}

export function useToaster(options?: ToastOptions) {
  const toasterId = options?.id ?? 'app-toaster';
  const limit = options?.limit ?? true;

  const dismissToaster = (id: string) => {
    toast.dismiss(id);
  };

  const errorToaster = (message: string) => {
    const errorToasterId = `${toasterId}-error`;
    if (limit) {
      dismissToaster(errorToasterId);
    }
    return toast.error(message, { id: errorToasterId });
  };

  const successToaster = (message: string) => {
    const successToasterId = `${toasterId}-success`;
    if (limit) {
      dismissToaster(successToasterId);
    }
    return toast.success(message, { id: successToasterId });
  };

  return {
    errorToaster,
    successToaster,
  };
}
