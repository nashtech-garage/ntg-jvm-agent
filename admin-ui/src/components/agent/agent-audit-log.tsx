'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { fetcher } from '@/utils/fetcher';
import { API_PATH } from '@/constants/url';
import { AgentAuditHistory, ComparisonResult } from '@/types/audit';
import { Button } from '@/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { AlertCircle, Clock, Eye, GitCompare, RotateCcw } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { Alert, AlertDescription } from '@/components/ui/alert';

interface AgentAuditLogProps {
  agentId: string;
}

export default function AgentAuditLog({ agentId }: AgentAuditLogProps) {
  const [selectedRevision, setSelectedRevision] = useState<AgentAuditHistory | null>(null);
  const [compareRevision, setCompareRevision] = useState<AgentAuditHistory | null>(null);
  const [showRollbackModal, setShowRollbackModal] = useState(false);
  const [rollbackConfirmation, setRollbackConfirmation] = useState('');
  const [includeSecrets, setIncludeSecrets] = useState(false);
  const [rollbackInProgress, setRollbackInProgress] = useState(false);

  const {
    data: history,
    error,
    isLoading,
    mutate,
  } = useSWR<AgentAuditHistory[]>(API_PATH.AUDIT_AGENT_HISTORY(agentId), fetcher, {
    revalidateOnFocus: false,
  });

  const getRevisionTypeColor = (type: string) => {
    switch (type) {
      case 'ADD':
        return 'bg-green-500';
      case 'MOD':
        return 'bg-blue-500';
      case 'DEL':
        return 'bg-red-500';
      default:
        return 'bg-gray-500';
    }
  };

  const getRevisionTypeLabel = (type: string) => {
    switch (type) {
      case 'ADD':
        return 'Created';
      case 'MOD':
        return 'Modified';
      case 'DEL':
        return 'Deleted';
      default:
        return type;
    }
  };

  const compareRevisions = (
    current: AgentAuditHistory,
    previous: AgentAuditHistory | null
  ): ComparisonResult[] => {
    if (!previous) return [];

    const currentEntity = current.entity;
    const previousEntity = previous.entity;
    const results: ComparisonResult[] = [];

    const fieldsToCompare = [
      'name',
      'model',
      'provider',
      'temperature',
      'maxTokens',
      'active',
      'baseUrl',
      'description',
    ];

    fieldsToCompare.forEach((field) => {
      const oldValue = previousEntity[field];
      const newValue = currentEntity[field];
      const changed = JSON.stringify(oldValue) !== JSON.stringify(newValue);

      if (changed) {
        results.push({
          field,
          oldValue,
          newValue,
          changed,
        });
      }
    });

    return results;
  };

  const handleRollback = async () => {
    if (!selectedRevision || rollbackConfirmation !== 'ROLLBACK') return;

    setRollbackInProgress(true);
    try {
      const response = await fetch(API_PATH.AUDIT_AGENT_ROLLBACK(agentId), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          revision: selectedRevision.revision,
          includeSecrets,
        }),
      });

      if (!response.ok) {
        throw new Error('Rollback failed');
      }

      // Refresh the audit history
      await mutate();

      // Close modal and reset state
      setShowRollbackModal(false);
      setRollbackConfirmation('');
      setIncludeSecrets(false);
      setSelectedRevision(null);

      alert('Rollback completed successfully!');
    } catch (error) {
      console.error('Rollback error:', error);
      alert('Rollback failed. Please try again.');
    } finally {
      setRollbackInProgress(false);
    }
  };

  const formatDate = (dateString: string | undefined) => {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateString;
    }
  };

  if (error) {
    return (
      <div className="p-6 text-center">
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>Failed to load audit history</AlertDescription>
        </Alert>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="p-6 text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
        <p className="text-gray-600">Loading audit history...</p>
      </div>
    );
  }

  const sortedHistory = [...(history || [])].sort((a, b) => b.revision - a.revision);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold">Audit History</h2>
          <p className="text-sm text-gray-600 mt-1">
            Complete change history for this agent with rollback capabilities
          </p>
        </div>
        <div className="flex items-center gap-2 text-sm text-gray-500">
          <Clock className="h-4 w-4" />
          <span>{sortedHistory.length} revisions</span>
        </div>
      </div>

      <div className="border rounded-lg overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-24">Revision</TableHead>
              <TableHead className="w-32">Change Type</TableHead>
              <TableHead>Changed By</TableHead>
              <TableHead>Changed At</TableHead>
              <TableHead>Summary</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {sortedHistory.map((item, index) => {
              const previousRevision = sortedHistory[index + 1] || null;
              const changes = compareRevisions(item, previousRevision);

              return (
                <TableRow key={item.revision}>
                  <TableCell className="font-mono font-semibold">{item.revision}</TableCell>
                  <TableCell>
                    <Badge className={getRevisionTypeColor(item.revisionType)}>
                      {getRevisionTypeLabel(item.revisionType)}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="font-medium">{item.entity.lastModifiedBy || 'System'}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{formatDate(item.entity.lastModifiedDate)}</div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">
                      {item.revisionType === 'ADD' && (
                        <span>Created agent &quot;{item.entity.name}&quot;</span>
                      )}
                      {item.revisionType === 'MOD' && (
                        <span>
                          {changes.length > 0
                            ? `Modified ${changes.length} field${changes.length > 1 ? 's' : ''}: ${changes.map((c) => c.field).join(', ')}`
                            : 'Modified agent'}
                        </span>
                      )}
                      {item.revisionType === 'DEL' && (
                        <span>Deleted/disabled agent</span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setSelectedRevision(item)}
                        title="View snapshot"
                      >
                        <Eye className="h-4 w-4" />
                      </Button>
                      {previousRevision && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setSelectedRevision(item);
                            setCompareRevision(previousRevision);
                          }}
                          title="Compare with previous"
                        >
                          <GitCompare className="h-4 w-4" />
                        </Button>
                      )}
                      {item.revisionType !== 'ADD' && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setSelectedRevision(item);
                            setShowRollbackModal(true);
                          }}
                          title="Rollback to this revision"
                        >
                          <RotateCcw className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>

      {/* View Snapshot Dialog */}
      <Dialog open={!!selectedRevision && !compareRevision} onOpenChange={() => setSelectedRevision(null)}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Revision {selectedRevision?.revision} Snapshot</DialogTitle>
            <DialogDescription>
              View the complete state of the agent at this revision
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label className="text-xs text-gray-500">Revision Type</Label>
                <div className="mt-1">
                  <Badge className={getRevisionTypeColor(selectedRevision?.revisionType || '')}>
                    {getRevisionTypeLabel(selectedRevision?.revisionType || '')}
                  </Badge>
                </div>
              </div>
              <div>
                <Label className="text-xs text-gray-500">Changed By</Label>
                <div className="mt-1 font-medium">
                  {selectedRevision?.entity.lastModifiedBy || 'System'}
                </div>
              </div>
            </div>
            <div className="border-t pt-4">
              <pre className="bg-gray-50 p-4 rounded-lg text-xs overflow-x-auto">
                {JSON.stringify(selectedRevision?.entity, null, 2)}
              </pre>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Compare Dialog */}
      <Dialog
        open={!!selectedRevision && !!compareRevision}
        onOpenChange={() => {
          setSelectedRevision(null);
          setCompareRevision(null);
        }}
      >
        <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              Compare Revisions: {compareRevision?.revision} → {selectedRevision?.revision}
            </DialogTitle>
            <DialogDescription>Changes between revisions</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            {selectedRevision && compareRevision && (
              <div>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Field</TableHead>
                      <TableHead>Old Value (Rev {compareRevision.revision})</TableHead>
                      <TableHead>New Value (Rev {selectedRevision.revision})</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {compareRevisions(selectedRevision, compareRevision).map((change) => (
                      <TableRow key={change.field}>
                        <TableCell className="font-medium">{change.field}</TableCell>
                        <TableCell className="bg-red-50">
                          <code className="text-sm">{JSON.stringify(change.oldValue)}</code>
                        </TableCell>
                        <TableCell className="bg-green-50">
                          <code className="text-sm">{JSON.stringify(change.newValue)}</code>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* Rollback Confirmation Dialog */}
      <Dialog open={showRollbackModal} onOpenChange={setShowRollbackModal}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Rollback to Revision {selectedRevision?.revision}</DialogTitle>
            <DialogDescription>
              This will update the current agent configuration and create a new audit revision.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                Warning: This action will modify the agent&apos;s current configuration. The rollback
                itself will be recorded as a new revision in the audit log.
              </AlertDescription>
            </Alert>

            {selectedRevision && compareRevision && (
              <div className="border rounded-lg p-4 space-y-2">
                <p className="text-sm font-medium">Fields that will change:</p>
                <ul className="text-sm space-y-1">
                  {compareRevisions(
                    sortedHistory[0],
                    selectedRevision
                  ).map((change) => (
                    <li key={change.field} className="text-gray-600">
                      • {change.field}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <div className="flex items-center space-x-2">
              <Checkbox
                id="includeSecrets"
                checked={includeSecrets}
                onCheckedChange={(checked) => setIncludeSecrets(checked === true)}
              />
              <Label htmlFor="includeSecrets" className="text-sm cursor-pointer">
                Include secrets (apiKey) - Use with caution
              </Label>
            </div>

            <div className="space-y-2">
              <Label htmlFor="confirmation">
                Type <strong>ROLLBACK</strong> to confirm
              </Label>
              <Input
                id="confirmation"
                value={rollbackConfirmation}
                onChange={(e) => setRollbackConfirmation(e.target.value)}
                placeholder="Type ROLLBACK"
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                setShowRollbackModal(false);
                setRollbackConfirmation('');
                setIncludeSecrets(false);
              }}
            >
              Cancel
            </Button>
            <Button
              onClick={handleRollback}
              disabled={rollbackConfirmation !== 'ROLLBACK' || rollbackInProgress}
              className="bg-blue-600 hover:bg-blue-700"
            >
              {rollbackInProgress ? 'Rolling back...' : 'Confirm Rollback'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

