'use client';

import { Toaster, toast } from 'react-hot-toast';

interface ToastOptions {
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

export function useToaster() {
  const dismissToaster = (id: string) => {
    toast.dismiss(id);
  };

  const showErrorToast = (message: string, options?: ToastOptions) => {
    const errorToasterId = 'app-error-toast';
    const limit = options?.limit ?? true;
    if (limit) {
      dismissToaster(errorToasterId);
    }
    return toast.error(message, { id: errorToasterId });
  };

  const showSuccessToast = (message: string, options?: ToastOptions) => {
    const successToasterId = 'app-success-toast';
    const limit = options?.limit ?? true;
    if (limit) {
      dismissToaster(successToasterId);
    }
    return toast.success(message, { id: successToasterId });
  };

  return {
    toasterError: showErrorToast,
    toasterSuccess: showSuccessToast,
  };
}
