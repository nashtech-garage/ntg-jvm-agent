'use client';

import { Toaster } from 'react-hot-toast';

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
