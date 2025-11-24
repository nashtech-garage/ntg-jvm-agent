'use client';

export default function MaintenancePage() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gray-100 p-6 text-center">
      <div className="bg-white shadow-xl rounded-2xl p-10 max-w-md w-full">
        <h1 className="text-4xl font-bold mb-4">We'll be back soon</h1>
        <p className="text-gray-600 mb-6">
          Our website is currently undergoing scheduled maintenance.
          <br />
          Thank you for your patience.
        </p>
        <div className="flex items-center justify-center">
          <div className="animate-spin rounded-full h-10 w-10 border-t-4 border-gray-800"></div>
        </div>
      </div>
    </div>
  );
}
