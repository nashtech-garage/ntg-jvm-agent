'use client';

import { useEffect, useState } from 'react';

interface SystemSettings {
  id: string;
  siteName: string;
  maintenanceMode: boolean;
  maximumUser: number;
  sessionTimeout: number;
  userRegistration: boolean;
  emailVerification: boolean;
  maximumSizeFileUpload: number;
  allowedFileTypes: string;
}

export default function AdminSettings() {
  const [settings, setSettings] = useState<SystemSettings>({
    id: '',
    siteName: '',
    maintenanceMode: false,
    maximumUser: 0,
    sessionTimeout: 0,
    userRegistration: true,
    emailVerification: false,
    maximumSizeFileUpload: 0,
    allowedFileTypes: '',
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    const fetchSystemSettings = async () => {
      try {
        setLoading(true);
        const response = await fetch(`/api/settings`);

        if (!response.ok) {
          throw new Error('Failed to fetch system settings');
        }

        const data: SystemSettings = await response.json();
        setSettings(data);
      } catch (error) {
        console.error('Failed to fetch  system settings:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchSystemSettings();
  }, []);

  const handleInputChange = (field: keyof SystemSettings, value: string | number | boolean) => {
    setSettings((prev) => ({
      ...prev,
      [field]: value,
    }));
    setSaved(false);
  };

  const handleSaveSettings = async () => {
    setSaving(true);
    try {
      const payload = {
        id: settings.id,
        siteName: settings.siteName,
        maintenanceMode: settings.maintenanceMode,
        maximumUser: settings.maximumUser,
        sessionTimeout: settings.sessionTimeout,
        userRegistration: settings.userRegistration,
        emailVerification: settings.emailVerification,
        maximumSizeFileUpload: settings.maximumSizeFileUpload,
        allowedFileTypes: settings.allowedFileTypes,
      };
      const id = settings.id;
      const res = await fetch(`/api/settings/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      });
      const data = await res.json();
      setSettings(data);
      setSaved(true);
    } catch (error) {
      console.error('Failed to save settings:', error);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">System Settings</h1>
          <p className="text-gray-600 mt-1">Configure system-wide settings and preferences</p>
        </div>
        <div className="flex items-center space-x-3">
          {saved && (
            <span className="text-green-600 text-sm flex items-center gap-1">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M5 13l4 4L19 7"
                />
              </svg>
              Settings saved successfully
            </span>
          )}
          <button
            onClick={handleSaveSettings}
            disabled={saving}
            className="bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white px-6 py-2 rounded-lg transition-colors flex items-center gap-2"
          >
            {saving ? (
              <>
                <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  ></circle>
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  ></path>
                </svg>
                Saving...
              </>
            ) : (
              <>
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"
                  />
                </svg>
                Save Changes
              </>
            )}
          </button>
        </div>
      </div>

      {/* General Settings */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">General Settings</h2>
        </div>
        <div className="p-6 space-y-6">
          <div>
            <label htmlFor="siteName" className="block text-sm font-medium text-gray-700 mb-2">
              Site Name
            </label>
            <input
              type="text"
              id="siteName"
              value={settings.siteName}
              onChange={(e) => handleInputChange('siteName', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <div className="flex items-center">
            <input
              type="checkbox"
              id="maintenanceMode"
              checked={settings.maintenanceMode}
              onChange={(e) => handleInputChange('maintenanceMode', e.target.checked)}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label htmlFor="maintenanceMode" className="ml-2 block text-sm text-gray-900">
              Enable Maintenance Mode
            </label>
          </div>
          {settings.maintenanceMode && (
            <div className="ml-6 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p className="text-sm text-yellow-800">
                ⚠️ When enabled, only administrators can access the system.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* User Management Settings */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">User Management</h2>
        </div>
        <div className="p-6 space-y-6">
          <div>
            <label htmlFor="maximumUser" className="block text-sm font-medium text-gray-700 mb-2">
              Maximum Users
            </label>
            <input
              type="number"
              id="maximumUser"
              value={settings.maximumUser}
              onChange={(e) => handleInputChange('maximumUser', parseInt(e.target.value))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <div>
            <label
              htmlFor="sessionTimeout"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              Session Timeout (seconds)
            </label>
            <input
              type="number"
              id="sessionTimeout"
              value={settings.sessionTimeout}
              onChange={(e) => handleInputChange('sessionTimeout', parseInt(e.target.value))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <div className="flex items-center">
            <input
              type="checkbox"
              id="userRegistration"
              checked={settings.userRegistration}
              onChange={(e) => handleInputChange('userRegistration', e.target.checked)}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label htmlFor="userRegistration" className="ml-2 block text-sm text-gray-900">
              Enable User Registration
            </label>
          </div>

          <div className="flex items-center">
            <input
              type="checkbox"
              id="emailVerification"
              checked={settings.emailVerification}
              onChange={(e) => handleInputChange('emailVerification', e.target.checked)}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label htmlFor="emailVerification" className="ml-2 block text-sm text-gray-900">
              Require Email Verification
            </label>
          </div>
        </div>
      </div>

      {/* File Upload Settings */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">File Upload Settings</h2>
        </div>
        <div className="p-6 space-y-6">
          <div>
            <label
              htmlFor="maximumSizeFileUpload"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              Maximum File Upload Size (MB)
            </label>
            <input
              type="number"
              id="maximumSizeFileUpload"
              value={settings.maximumSizeFileUpload}
              onChange={(e) => handleInputChange('maximumSizeFileUpload', parseInt(e.target.value))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <div>
            <label
              htmlFor="allowedFileTypes"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              Allowed File Types
            </label>
            <input
              type="text"
              id="allowedFileTypes"
              value={settings.allowedFileTypes}
              onChange={(e) => handleInputChange('allowedFileTypes', e.target.value)}
              placeholder="jpg, png, pdf, doc, docx"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <p className="mt-1 text-sm text-gray-500">Enter file extensions separated by commas</p>
          </div>
        </div>
      </div>

      {/* Security Settings */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Security Settings</h2>
        </div>
        <div className="p-6 space-y-6">
          <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <h3 className="font-medium text-blue-900 mb-2">OAuth Configuration</h3>
            <p className="text-sm text-blue-700 mb-3">
              OAuth settings are configured via environment variables for security.
            </p>
            <div className="space-y-2 text-sm">
              <div>
                <strong>Client ID:</strong> {process.env.NEXT_PUBLIC_CLIENT_ID || 'Not configured'}
              </div>
              <div>
                <strong>Auth Server:</strong>{' '}
                {process.env.NEXT_PUBLIC_AUTH_SERVER || 'Not configured'}
              </div>
              <div>
                <strong>Scope:</strong> {process.env.NEXT_PUBLIC_SCOPE || 'Not configured'}
              </div>
            </div>
          </div>

          <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
            <h3 className="font-medium text-yellow-900 mb-2">Security Recommendations</h3>
            <ul className="text-sm text-yellow-700 space-y-1">
              <li>• Enable HTTPS in production</li>
              <li>• Regularly rotate client secrets</li>
              <li>• Monitor failed login attempts</li>
              <li>• Keep session timeouts reasonable</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
