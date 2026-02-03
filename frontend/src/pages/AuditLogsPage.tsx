import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { 
  ClipboardDocumentListIcon,
  FunnelIcon,
  MagnifyingGlassIcon,
  ArrowDownTrayIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  UserIcon,
  CpuChipIcon,
  ServerStackIcon,
  ShieldCheckIcon,
  CloudIcon,
  ArrowPathIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon
} from '@heroicons/react/24/outline';
import clsx from 'clsx';

interface AuditLog {
  id: string;
  timestamp: Date;
  action: string;
  resource: string;
  resourceType: string;
  actor: string;
  automated: boolean;
  details: string;
  severity: string;
  region: string;
}

const actionIcons: Record<string, React.ComponentType<{ className?: string }>> = {
  SCALE_UP: ArrowPathIcon,
  SCALE_DOWN: ArrowPathIcon,
  TRAFFIC_SHIFT: ArrowPathIcon,
  DEPLOYMENT_STARTED: ServerStackIcon,
  DEPLOYMENT_COMPLETED: CheckCircleIcon,
  ROLLBACK_TRIGGERED: ExclamationTriangleIcon,
  AUTOMATION_DISABLED: CpuChipIcon,
  AUTOMATION_ENABLED: CpuChipIcon,
  POLICY_UPDATED: ShieldCheckIcon,
  ACCOUNT_CONNECTED: CloudIcon,
  SLO_VIOLATION: ExclamationTriangleIcon,
  BLUEPRINT_APPLIED: ServerStackIcon,
  REGION_ACTIVATED: ServerStackIcon,
};

export default function AuditLogsPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [severityFilter, setSeverityFilter] = useState<string>('all');
  const [resourceTypeFilter, setResourceTypeFilter] = useState<string>('all');
  const [currentPage, setCurrentPage] = useState(1);

  // Fetch audit logs from API
  const { data: auditLogs = [], isLoading } = useQuery<AuditLog[]>({
    queryKey: ['auditLogs'],
    queryFn: async () => {
      const logs = await api.getAllAuditLogs(200);
      return logs.map((log: any) => ({
        id: log.id,
        timestamp: new Date(log.createdAt),
        action: log.actionType,
        resource: log.resourceName || 'Unknown',
        resourceType: log.actionCategory,
        actor: log.triggeredBy || (log.isManualOverride ? 'Manual' : 'Control Loop'),
        automated: !log.isManualOverride,
        details: log.description,
        severity: log.status === 'FAILED' ? 'ERROR' : log.status === 'IN_PROGRESS' ? 'WARNING' : 'INFO',
        region: log.region || 'Global',
      }));
    },
    refetchInterval: 10000,
  });

  const filteredLogs = auditLogs.filter((log: any) => {
    const matchesSearch = log.action.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.resource.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.details.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.actor.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesSeverity = severityFilter === 'all' || log.severity === severityFilter;
    const matchesResourceType = resourceTypeFilter === 'all' || log.resourceType === resourceTypeFilter;
    return matchesSearch && matchesSeverity && matchesResourceType;
  });

  const formatTimestamp = (date: Date) => {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffMins < 1440) return `${Math.floor(diffMins / 60)}h ago`;
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <ArrowPathIcon className="h-8 w-8 text-indigo-400 animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white flex items-center gap-3">
            <ClipboardDocumentListIcon className="h-7 w-7 text-indigo-400" />
            Audit Logs
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            Complete history of all actions and changes in the control plane
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button className="flex items-center gap-2 px-4 py-2 bg-slate-800 text-slate-200 rounded-lg hover:bg-slate-700 border border-slate-700 transition-colors">
            <ArrowDownTrayIcon className="h-4 w-4" />
            Export
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-4 flex-wrap">
        <div className="relative flex-1 max-w-md">
          <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
          <input
            type="text"
            placeholder="Search logs..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-sm placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <div className="flex items-center gap-2">
          <FunnelIcon className="h-4 w-4 text-slate-500" />
          <select
            value={severityFilter}
            onChange={(e) => setSeverityFilter(e.target.value)}
            className="px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-sm text-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">All Severity</option>
            <option value="INFO">Info</option>
            <option value="WARNING">Warning</option>
            <option value="ERROR">Error</option>
          </select>
          <select
            value={resourceTypeFilter}
            onChange={(e) => setResourceTypeFilter(e.target.value)}
            className="px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-sm text-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">All Resources</option>
            <option value="SERVICE">Services</option>
            <option value="DEPLOYMENT">Deployments</option>
            <option value="POLICY">Policies</option>
            <option value="ACCOUNT">Accounts</option>
            <option value="BLUEPRINT">Blueprints</option>
          </select>
        </div>
      </div>

      {/* Audit Log Timeline */}
      <div className="bg-slate-900/50 rounded-xl border border-slate-800 overflow-hidden">
        <div className="divide-y divide-slate-800">
          {filteredLogs.map((log, index) => {
            const ActionIcon = actionIcons[log.action] || ClipboardDocumentListIcon;
            return (
              <div 
                key={log.id}
                className="flex items-start gap-4 p-4 hover:bg-slate-800/30 transition-colors"
              >
                {/* Timeline indicator */}
                <div className="flex flex-col items-center">
                  <div className={clsx(
                    "w-8 h-8 rounded-full flex items-center justify-center",
                    log.severity === 'ERROR' && "bg-red-500/10",
                    log.severity === 'WARNING' && "bg-amber-500/10",
                    log.severity === 'INFO' && "bg-slate-500/10"
                  )}>
                    <ActionIcon className={clsx(
                      "h-4 w-4",
                      log.severity === 'ERROR' && "text-red-400",
                      log.severity === 'WARNING' && "text-amber-400",
                      log.severity === 'INFO' && "text-slate-400"
                    )} />
                  </div>
                  {index < filteredLogs.length - 1 && (
                    <div className="w-px h-full bg-slate-700 mt-2" />
                  )}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium text-slate-200">
                          {log.action.replace(/_/g, ' ')}
                        </span>
                        <span className={clsx(
                          "badge text-xs",
                          log.severity === 'ERROR' && "badge-error",
                          log.severity === 'WARNING' && "badge-warning",
                          log.severity === 'INFO' && "badge-neutral"
                        )}>
                          {log.severity}
                        </span>
                      </div>
                      <p className="mt-1 text-sm text-slate-400">{log.details}</p>
                      <div className="flex items-center gap-4 mt-2 text-xs text-slate-500">
                        <span className="flex items-center gap-1">
                          <ServerStackIcon className="h-3 w-3" />
                          {log.resource}
                        </span>
                        <span className="flex items-center gap-1">
                          <UserIcon className="h-3 w-3" />
                          {log.actor}
                          {log.automated && (
                            <span className="text-purple-400 ml-1">(Auto)</span>
                          )}
                        </span>
                        <span>{log.region}</span>
                      </div>
                    </div>
                    <span className="text-xs text-slate-500 whitespace-nowrap">
                      {formatTimestamp(log.timestamp)}
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Pagination */}
      <div className="flex items-center justify-between">
        <span className="text-sm text-slate-500">
          Showing {filteredLogs.length} of {auditLogs.length} logs
        </span>
        <div className="flex items-center gap-2">
          <button 
            className="p-2 rounded-lg bg-slate-800 border border-slate-700 text-slate-400 hover:bg-slate-700 disabled:opacity-50 transition-colors"
            disabled={currentPage === 1}
            onClick={() => setCurrentPage(p => p - 1)}
          >
            <ChevronLeftIcon className="h-4 w-4" />
          </button>
          <span className="text-sm text-slate-400 px-3">Page {currentPage}</span>
          <button 
            className="p-2 rounded-lg bg-slate-800 border border-slate-700 text-slate-400 hover:bg-slate-700 disabled:opacity-50 transition-colors"
            onClick={() => setCurrentPage(p => p + 1)}
          >
            <ChevronRightIcon className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Summary Card */}
      <div className="grid grid-cols-4 gap-4">
        <SummaryCard label="Total Actions Today" value="156" />
        <SummaryCard label="Automated Actions" value="89" subLabel="57%" />
        <SummaryCard label="Warnings" value="12" color="warning" />
        <SummaryCard label="Errors" value="3" color="error" />
      </div>
    </div>
  );
}

function SummaryCard({ 
  label, 
  value, 
  subLabel,
  color = 'default'
}: { 
  label: string; 
  value: string;
  subLabel?: string;
  color?: 'default' | 'warning' | 'error';
}) {
  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-4">
      <span className="text-xs text-slate-500 uppercase tracking-wide">{label}</span>
      <div className="flex items-baseline gap-2 mt-1">
        <span className={clsx(
          "text-2xl font-semibold",
          color === 'warning' && "text-amber-400",
          color === 'error' && "text-red-400",
          color === 'default' && "text-white"
        )}>
          {value}
        </span>
        {subLabel && (
          <span className="text-sm text-slate-500">{subLabel}</span>
        )}
      </div>
    </div>
  );
}
