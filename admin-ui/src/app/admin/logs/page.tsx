'use client';

import { useEffect, useMemo, useState } from 'react';
import { fetchAuditLogs } from '@/services/audit';
import { AuditLog } from '@/models/audit';
import logger from '@/utils/logger';

type EntityFilter = 'all' | 'Agent' | 'Tool' | 'Conversation' | 'SystemSetting';
type RevisionFilter = 'all' | 'ADD' | 'MOD' | 'DEL';

const isRecord = (value: unknown): value is Record<string, unknown> =>
  value !== null && typeof value === 'object' && !Array.isArray(value);

export default function AdminLogs() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState({
    search: '',
    entity: 'all' as EntityFilter,
    revision: 'all' as RevisionFilter,
    agentId: 'all',
  });
  const [snapshotLog, setSnapshotLog] = useState<AuditLog | null>(null);
  const [compareLog, setCompareLog] = useState<{ current: AuditLog; previous: AuditLog } | null>(
    null
  );

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchAuditLogs(filters.agentId !== 'all' ? filters.agentId : undefined);
        setLogs(data);
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Unable to load system logs';
        setError(message);
      } finally {
        setLoading(false);
      }
    };

    load().catch((err) => logger.error('System logs load failure', err));
  }, [filters.agentId]);

  const agentOptions = useMemo(
    () =>
      Array.from(
        new Set(
          logs
            .filter((log) => log.entityType === 'Agent' && log.entityId)
            .map((log) => log.entityId as string)
        )
      ),
    [logs]
  );

  const filteredLogs = useMemo(() => {
    const query = filters.search.trim().toLowerCase();

    return logs.filter((log) => {
      const matchesEntity = filters.entity === 'all' || log.entityType === filters.entity;
      const matchesRevision = filters.revision === 'all' || log.revisionType === filters.revision;
      const matchesAgent =
        filters.agentId === 'all' || (log.entityType === 'Agent' && log.entityId === filters.agentId);

      const text = [
        log.entityType,
        log.entityId,
        log.username,
        log.revisionType,
        JSON.stringify(log.payload ?? {}),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      const matchesSearch = query === '' || text.includes(query);

      return matchesEntity && matchesRevision && matchesAgent && matchesSearch;
    });
  }, [filters.agentId, filters.entity, filters.revision, filters.search, logs]);

  const formatTimestamp = (timestamp?: string) => {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  const revisionColor = (revisionType: string) => {
    switch (revisionType) {
      case 'ADD':
        return 'bg-emerald-100 text-emerald-800 border-emerald-200';
      case 'MOD':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'DEL':
        return 'bg-rose-100 text-rose-800 border-rose-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const summary = useMemo(() => {
    return ['ADD', 'MOD', 'DEL'].map((type) => ({
      type,
      count: logs.filter((log) => log.revisionType === type).length,
    }));
  }, [logs]);
  const changeSummaryMap = useMemo(() => {
    const map = new Map<string, string>();
    const prevMap = new Map<string, AuditLog>();

    const diffFields = (prevPayload: unknown, currPayload: unknown): string[] => {
      if (!isRecord(currPayload)) return [];
      const prev = isRecord(prevPayload) ? prevPayload : {};
      const keys = new Set([...Object.keys(prev), ...Object.keys(currPayload)]);
      const changed: string[] = [];
      keys.forEach((key) => {
        if (key === 'id') return; // skip stable id
        const beforeVal = prev[key];
        const afterVal = currPayload[key];
        if (JSON.stringify(beforeVal) !== JSON.stringify(afterVal)) {
          changed.push(key);
        }
      });
      return changed;
    };

    const grouped = new Map<string, AuditLog[]>();
    logs.forEach((log) => {
      const key = `${log.entityType}:${log.entityId ?? 'na'}`;
      if (!grouped.has(key)) grouped.set(key, []);
      grouped.get(key)!.push(log);
    });

    grouped.forEach((list, key) => {
      const sorted = [...list].sort((a, b) => a.revision - b.revision);
      sorted.forEach((log, idx) => {
        const prev = sorted[idx - 1];
        const changedFields = diffFields(prev?.payload, log.payload);

        if (prev) {
          prevMap.set(`${key}:${log.revision}`, prev);
        }

        let summaryText = 'Updated';
        if (log.revisionType === 'ADD') summaryText = 'Created';
        else if (log.revisionType === 'DEL') summaryText = 'Deleted';
        else if (log.revisionType === 'MOD') {
          summaryText =
            changedFields.length > 0
              ? `Edited ${changedFields.length} field(s): ${changedFields.join(', ')}`
              : 'Updated';
        }

        map.set(`${key}:${log.revision}`, summaryText);
      });
    });

    return { summaryMap: map, prevMap };
  }, [logs]);

  const renderSummary = (log: AuditLog) => {
    const key = `${log.entityType}:${log.entityId ?? 'na'}:${log.revision}`;
    return changeSummaryMap.summaryMap.get(key) ?? 'Updated';
  };

  const getPreviousLog = (log: AuditLog): AuditLog | undefined => {
    const key = `${log.entityType}:${log.entityId ?? 'na'}:${log.revision}`;
    return changeSummaryMap.prevMap.get(key);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">System Logs</h1>
          <p className="text-gray-600 mt-1">Unified audit logs from orchestrator</p>
        </div>
      </div>

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
                value={filters.search}
                onChange={(e) => setFilters((prev) => ({ ...prev, search: e.target.value }))}
                placeholder="Search by entity, user, payload..."
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
            <label htmlFor="entity-filter" className="block text-sm font-medium text-gray-700 mb-2">
              Entity
            </label>
            <select
              id="entity-filter"
              value={filters.entity}
              onChange={(e) => setFilters((prev) => ({ ...prev, entity: e.target.value as EntityFilter }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">All</option>
              <option value="Agent">Agent</option>
              <option value="Tool">Tool</option>
              <option value="Conversation">Conversation</option>
              <option value="SystemSetting">System Setting</option>
            </select>
          </div>

          <div>
            <label htmlFor="revision-filter" className="block text-sm font-medium text-gray-700 mb-2">
              Revision type
            </label>
            <select
              id="revision-filter"
              value={filters.revision}
              onChange={(e) => setFilters((prev) => ({ ...prev, revision: e.target.value as RevisionFilter }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">All</option>
              <option value="ADD">ADD</option>
              <option value="MOD">MOD</option>
              <option value="DEL">DEL</option>
            </select>
          </div>

          <div>
            <label htmlFor="agent-filter" className="block text-sm font-medium text-gray-700 mb-2">
              Agent
            </label>
            <select
              id="agent-filter"
              value={filters.agentId}
              onChange={(e) => setFilters((prev) => ({ ...prev, agentId: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">All</option>
              {agentOptions.map((id) => (
                <option key={id} value={id}>
                  {id}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="flex justify-end mt-4">
          <button
            onClick={() =>
              setFilters({
                search: '',
                entity: 'all',
                revision: 'all',
                agentId: 'all',
              })
            }
            className="bg-gray-200 hover:bg-gray-300 text-gray-800 px-4 py-2 rounded-lg transition-colors"
          >
            Reset filters
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-rose-50 border border-rose-200 text-rose-800 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200">
          <div className="divide-y divide-gray-200">
            {filteredLogs.map((log) => (
              <div key={`${log.entityType}-${log.revision}-${log.entityId ?? 'na'}`} className="p-6 hover:bg-gray-50">
                <div className="flex items-start gap-4">
                  <div className="flex-shrink-0">
                    <span
                      className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full border ${revisionColor(
                        log.revisionType
                      )}`}
                    >
                      {log.revisionType}
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-2">
                      <div className="text-sm text-gray-600">
                        {log.entityType}
                        {log.entityId && <span className="ml-2 text-gray-800 font-medium">{log.entityId}</span>}
                        {log.username && <span className="ml-3 text-blue-700">User: {log.username}</span>}
                      </div>
                      <div className="text-xs text-gray-500">{formatTimestamp(log.timestamp)}</div>
                    </div>

                    <p className="text-sm text-gray-900 mb-2">Revision #{log.revision}</p>

                    {log.payload && (
                      <details className="text-xs text-gray-700">
                        <summary className="cursor-pointer hover:text-gray-900">View payload</summary>
                        <pre className="mt-2 p-3 bg-gray-100 rounded border overflow-x-auto text-xs leading-snug">
                          {JSON.stringify(log.payload, null, 2)}
                        </pre>
                      </details>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Table view similar to mock history */}
      {!loading && filteredLogs.length > 0 && (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200">
          <div className="px-6 pt-6 pb-3 flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold text-gray-900">History: agent (all)</h2>
              <p className="text-sm text-gray-500">Showing revisions in table view</p>
            </div>
            <span className="text-sm text-gray-600">{filteredLogs.length} revisions</span>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700">Revision</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700">Change Type</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700">Changed By</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700">Changed At</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700">Summary</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {filteredLogs.map((log) => (
                  <tr key={`table-${log.entityType}-${log.revision}-${log.entityId ?? 'na'}`}>
                    <td className="px-6 py-4 text-sm text-gray-900">{log.revision}</td>
                    <td className="px-6 py-4 text-sm">
                      <span
                        className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full border ${revisionColor(
                          log.revisionType
                        )}`}
                      >
                        {log.revisionType}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-700">{log.username ?? 'System'}</td>
                    <td className="px-6 py-4 text-sm text-gray-500">{formatTimestamp(log.timestamp)}</td>
                    <td className="px-6 py-4 text-sm text-gray-800">
                      <div className="font-medium text-gray-900">
                        {log.entityType}
                        {log.entityId && <span className="ml-2 text-gray-700">{log.entityId}</span>}
                      </div>
                      <div className="text-xs text-gray-600 truncate max-w-md">{renderSummary(log)}</div>
                    </td>
                    <td className="px-6 py-4 text-sm text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => setSnapshotLog(log)}
                          className="inline-flex items-center justify-center w-10 h-10 rounded-lg border border-gray-200 hover:border-gray-300 hover:bg-gray-50"
                          title="View snapshot"
                        >
                          <span className="sr-only">View snapshot</span>
                          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                            />
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M2.458 12C3.732 7.943 7.523 5 12 5c4.477 0 8.268 2.943 9.542 7-1.274 4.057-5.065 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
                            />
                          </svg>
                        </button>
                        <button
                          onClick={() => {
                            const prev = getPreviousLog(log);
                            if (prev) setCompareLog({ current: log, previous: prev });
                          }}
                          disabled={!getPreviousLog(log)}
                          className={`inline-flex items-center justify-center w-10 h-10 rounded-lg border ${
                            getPreviousLog(log)
                              ? 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                              : 'border-gray-100 text-gray-300 cursor-not-allowed'
                          }`}
                          title="Compare with previous"
                        >
                          <span className="sr-only">Compare</span>
                          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M17 8l4 4m0 0l-4 4m4-4H7a4 4 0 01-4-4V5"
                            />
                          </svg>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!loading && filteredLogs.length === 0 && !error && (
        <div className="text-center py-12 bg-white border border-dashed border-gray-200 rounded-lg">
          <div className="text-gray-500 text-lg">No logs match the filters.</div>
        </div>
      )}

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Summary</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          {summary.map((item) => (
            <div key={item.type} className="text-center">
              <div className="text-2xl font-bold text-gray-900">{item.count}</div>
              <div className="text-gray-600">{item.type}</div>
            </div>
          ))}
          <div className="text-center">
            <div className="text-2xl font-bold text-gray-900">{logs.length}</div>
            <div className="text-gray-600">Total</div>
          </div>
        </div>
      </div>

      {snapshotLog && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[80vh] flex flex-col">
            <div className="flex items-center justify-between px-6 py-4 border-b">
              <div>
                <div className="text-sm text-gray-500">Revision {snapshotLog.revision} Snapshot</div>
                <div className="text-lg font-semibold text-gray-900">Full entity state</div>
                <div className="mt-1 inline-flex px-2 py-1 text-xs font-semibold rounded-full border border-amber-200 text-amber-700">
                  {snapshotLog.revisionType}
                </div>
              </div>
              <button
                onClick={() => setSnapshotLog(null)}
                className="w-9 h-9 inline-flex items-center justify-center rounded-lg hover:bg-gray-100"
                title="Close"
              >
                <span className="sr-only">Close</span>
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="px-6 pb-6 pt-4 overflow-auto">
              <pre className="bg-gray-50 border border-gray-200 rounded-lg p-4 text-xs leading-snug whitespace-pre-wrap">
                {JSON.stringify(snapshotLog.payload, null, 2)}
              </pre>
            </div>
          </div>
        </div>
      )}

      {compareLog && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[80vh] flex flex-col">
            <div className="flex items-center justify-between px-6 py-4 border-b">
              <div>
                <div className="text-sm text-gray-500">Compare: Rev {compareLog.previous.revision}  Rev {compareLog.current.revision}</div>
                <div className="text-lg font-semibold text-gray-900">Differences between adjacent revisions</div>
              </div>
              <button
                onClick={() => setCompareLog(null)}
                className="w-9 h-9 inline-flex items-center justify-center rounded-lg hover:bg-gray-100"
                title="Close"
              >
                <span className="sr-only">Close</span>
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="px-6 pb-6 pt-4 overflow-auto">
              <table className="min-w-full divide-y divide-gray-200 text-sm">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-2 text-left font-semibold text-gray-700">Field</th>
                    <th className="px-4 py-2 text-left font-semibold text-gray-700">Old (Rev {compareLog.previous.revision})</th>
                    <th className="px-4 py-2 text-left font-semibold text-gray-700">New (Rev {compareLog.current.revision})</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {(() => {
                    const prevPayload = isRecord(compareLog.previous.payload) ? compareLog.previous.payload : {};
                    const currPayload = isRecord(compareLog.current.payload) ? compareLog.current.payload : {};
                    const fields = Array.from(new Set([...Object.keys(prevPayload), ...Object.keys(currPayload)])).filter(
                      (f) => f !== 'id'
                    );
                    return fields.map((field) => {
                      const oldVal = prevPayload[field];
                      const newVal = currPayload[field];
                      const changed = JSON.stringify(oldVal) !== JSON.stringify(newVal);
                      return (
                        <tr key={field} className={changed ? 'bg-amber-50' : ''}>
                          <td className="px-4 py-2 font-medium text-gray-800">{field}</td>
                          <td className="px-4 py-2 text-gray-700 align-top whitespace-pre-wrap text-xs">
                            {oldVal === undefined ? '—' : JSON.stringify(oldVal, null, 2)}
                          </td>
                          <td className="px-4 py-2 text-gray-700 align-top whitespace-pre-wrap text-xs">
                            {newVal === undefined ? '—' : JSON.stringify(newVal, null, 2)}
                          </td>
                        </tr>
                      );
                    });
                  })()}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
