'use client';

import { useEffect, useState } from 'react';
import logger from '@/utils/logger';

interface LogEntry {
  id: string;
  timestamp: string;
  level: 'info' | 'warn' | 'error' | 'debug';
  message: string;
  source: string;
  userId?: string;
  metadata?: Record<string, unknown>;
}

export default function AdminLogs() {
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState({
    level: 'all',
    source: 'all',
    search: '',
  });

  useEffect(() => {
    const fetchLogs = async () => {
      try {
        // Mock logs data - replace with actual API call
        const mockLogs: LogEntry[] = [
          {
            id: '1',
            timestamp: '2024-01-16T10:30:00Z',
            level: 'info',
            message: 'User authentication successful',
            source: 'auth-service',
            userId: 'user123',
            metadata: { ip: '192.168.1.1', userAgent: 'Mozilla/5.0...' },
          },
          {
            id: '2',
            timestamp: '2024-01-16T10:25:00Z',
            level: 'warn',
            message: 'High memory usage detected: 85%',
            source: 'system',
            metadata: { memoryUsage: 85, threshold: 80 },
          },
          {
            id: '3',
            timestamp: '2024-01-16T10:20:00Z',
            level: 'error',
            message: 'Failed to connect to external API',
            source: 'api-client',
            metadata: { endpoint: 'https://api.example.com', statusCode: 500 },
          },
          {
            id: '4',
            timestamp: '2024-01-16T10:15:00Z',
            level: 'info',
            message: 'Database backup completed successfully',
            source: 'backup-service',
            metadata: { backupSize: '2.5GB', duration: '5m 32s' },
          },
          {
            id: '5',
            timestamp: '2024-01-16T10:10:00Z',
            level: 'debug',
            message: 'OAuth token refresh completed',
            source: 'auth-service',
            userId: 'user456',
            metadata: { tokenType: 'refresh', expiresIn: 3600 },
          },
        ];
        setLogs(mockLogs);
      } catch (error) {
        logger.error('Failed to fetch logs:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchLogs();
  }, []);

  const filteredLogs = logs.filter((log) => {
    const matchesLevel = filter.level === 'all' || log.level === filter.level;
    const matchesSource = filter.source === 'all' || log.source === filter.source;
    const matchesSearch =
      filter.search === '' ||
      log.message.toLowerCase().includes(filter.search.toLowerCase()) ||
      log.source.toLowerCase().includes(filter.search.toLowerCase());

    return matchesLevel && matchesSource && matchesSearch;
  });

  const getLevelColor = (level: LogEntry['level']) => {
    switch (level) {
      case 'error':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'warn':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'info':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'debug':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  const uniqueSources = [...new Set(logs.map((log) => log.source))];

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
          <h1 className="text-3xl font-bold text-gray-900">System Logs</h1>
          <p className="text-gray-600 mt-1">Monitor system activity and troubleshoot issues</p>
        </div>
        <button className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
          Export Logs
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label htmlFor="search" className="block text-sm font-medium text-gray-700 mb-2">
              Search
            </label>
            <div className="relative">
              <input
                type="text"
                id="search"
                value={filter.search}
                onChange={(e) => setFilter((prev) => ({ ...prev, search: e.target.value }))}
                placeholder="Search logs..."
                className="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <svg
                className="absolute left-3 top-2.5 h-5 w-5 text-gray-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
            </div>
          </div>

          <div>
            <label htmlFor="level-filter" className="block text-sm font-medium text-gray-700 mb-2">
              Log Level
            </label>
            <select
              id="level-filter"
              value={filter.level}
              onChange={(e) => setFilter((prev) => ({ ...prev, level: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">All Levels</option>
              <option value="error">Error</option>
              <option value="warn">Warning</option>
              <option value="info">Info</option>
              <option value="debug">Debug</option>
            </select>
          </div>

          <div>
            <label htmlFor="source-filter" className="block text-sm font-medium text-gray-700 mb-2">
              Source
            </label>
            <select
              id="source-filter"
              value={filter.source}
              onChange={(e) => setFilter((prev) => ({ ...prev, source: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">All Sources</option>
              {uniqueSources.map((source) => (
                <option key={source} value={source}>
                  {source}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-end">
            <button
              onClick={() => setFilter({ level: 'all', source: 'all', search: '' })}
              className="w-full bg-gray-200 hover:bg-gray-300 text-gray-800 px-4 py-2 rounded-lg transition-colors"
            >
              Clear Filters
            </button>
          </div>
        </div>
      </div>

      {/* Log Entries */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200">
        <div className="divide-y divide-gray-200">
          {filteredLogs.map((log) => (
            <div key={log.id} className="p-6 hover:bg-gray-50">
              <div className="flex items-start space-x-4">
                <div className="flex-shrink-0">
                  <span
                    className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full border ${getLevelColor(
                      log.level
                    )}`}
                  >
                    {log.level.toUpperCase()}
                  </span>
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-2">
                    <div className="text-sm text-gray-500">
                      {formatTimestamp(log.timestamp)} • {log.source}
                      {log.userId && <span className="ml-2 text-blue-600">User: {log.userId}</span>}
                    </div>
                  </div>

                  <p className="text-sm text-gray-900 mb-2">{log.message}</p>

                  {log.metadata && Object.keys(log.metadata).length > 0 && (
                    <details className="text-xs text-gray-600">
                      <summary className="cursor-pointer hover:text-gray-800">
                        View metadata
                      </summary>
                      <pre className="mt-2 p-2 bg-gray-100 rounded border overflow-x-auto">
                        {JSON.stringify(log.metadata, null, 2)}
                      </pre>
                    </details>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {filteredLogs.length === 0 && (
        <div className="text-center py-12">
          <div className="text-gray-500 text-lg">No logs found matching your criteria.</div>
        </div>
      )}

      {/* Log Summary */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Log Summary</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <div className="text-center">
            <div className="text-2xl font-bold text-red-600">
              {logs.filter((log) => log.level === 'error').length}
            </div>
            <div className="text-gray-600">Errors</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-yellow-600">
              {logs.filter((log) => log.level === 'warn').length}
            </div>
            <div className="text-gray-600">Warnings</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-blue-600">
              {logs.filter((log) => log.level === 'info').length}
            </div>
            <div className="text-gray-600">Info</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-gray-600">
              {logs.filter((log) => log.level === 'debug').length}
            </div>
            <div className="text-gray-600">Debug</div>
          </div>
        </div>
      </div>
    </div>
  );
}
