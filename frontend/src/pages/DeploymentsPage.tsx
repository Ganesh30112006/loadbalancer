import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { 
  RocketLaunchIcon, 
  CheckCircleIcon, 
  XCircleIcon,
  ClockIcon,
  ArrowPathIcon,
  FunnelIcon,
  MagnifyingGlassIcon,
  ArrowUturnLeftIcon,
  EyeIcon
} from '@heroicons/react/24/outline';
import { CheckCircleIcon as CheckCircleSolid, XCircleIcon as XCircleSolid } from '@heroicons/react/24/solid';
import clsx from 'clsx';

interface Deployment {
  id: string;
  serviceName: string;
  version: string;
  previousVersion: string;
  status: string;
  deploymentType: string;
  initiatedBy: string;
  automated: boolean;
  startTime: Date | null;
  endTime: Date | null;
  regions: string[];
  progress: number;
  canaryPercentage: number | null;
  error?: string;
  rollbackReason?: string;
  scheduledFor?: Date;
}

const statusConfig = {
  COMPLETED: { label: 'Completed', color: 'success', icon: CheckCircleSolid },
  IN_PROGRESS: { label: 'In Progress', color: 'info', icon: ArrowPathIcon },
  FAILED: { label: 'Failed', color: 'error', icon: XCircleSolid },
  PENDING: { label: 'Pending', color: 'neutral', icon: ClockIcon },
  ROLLED_BACK: { label: 'Rolled Back', color: 'warning', icon: ArrowUturnLeftIcon },
};

export default function DeploymentsPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [_selectedDeployment, setSelectedDeployment] = useState<string | null>(null);

  // Fetch deployments from API
  const { data: deployments = [], isLoading } = useQuery<Deployment[]>({
    queryKey: ['deploymentWorkflows'],
    queryFn: async () => {
      const workflows = await api.getAllDeploymentWorkflows(50);
      return workflows.map((w: any) => ({
        id: w.id,
        serviceName: w.serviceName || 'Unknown',
        version: w.deploymentVersion || 'N/A',
        previousVersion: w.previousVersion || 'N/A',
        status: w.status,
        deploymentType: w.strategy,
        initiatedBy: w.initiatedBy || 'Control Loop',
        automated: !w.initiatedBy || w.initiatedBy === 'Control Loop',
        startTime: w.startedAt ? new Date(w.startedAt) : null,
        endTime: w.completedAt ? new Date(w.completedAt) : null,
        regions: [w.region || 'us-east-1'],
        progress: w.status === 'COMPLETED' ? 100 : w.status === 'IN_PROGRESS' ? 50 : 0,
        canaryPercentage: w.strategy === 'CANARY' ? w.greenTrafficPercent : null,
        error: w.errorMessage,
        rollbackReason: w.rollbackReason,
      }));
    },
    refetchInterval: 10000,
  });

  const filteredDeployments = deployments.filter((d: any) => {
    const matchesSearch = d.serviceName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      d.version.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'all' || d.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const formatDuration = (start: Date | null, end: Date | null) => {
    if (!start) return '-';
    const endTime = end || new Date();
    const diffMs = endTime.getTime() - start.getTime();
    const minutes = Math.floor(diffMs / 60000);
    if (minutes < 60) return `${minutes}m`;
    return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
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
            <RocketLaunchIcon className="h-7 w-7 text-indigo-400" />
            Deployments
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            Track and manage service deployments across all regions
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-500 transition-colors">
            <RocketLaunchIcon className="h-4 w-4" />
            New Deployment
          </button>
        </div>
      </div>

      {/* Stats Row */}
      <div className="grid grid-cols-5 gap-4">
        <StatCard label="Total Today" value="23" icon={RocketLaunchIcon} />
        <StatCard label="Successful" value="19" color="success" icon={CheckCircleIcon} />
        <StatCard label="In Progress" value="2" color="info" icon={ArrowPathIcon} />
        <StatCard label="Failed" value="1" color="error" icon={XCircleIcon} />
        <StatCard label="Rolled Back" value="1" color="warning" icon={ArrowUturnLeftIcon} />
      </div>

      {/* Filters */}
      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-md">
          <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
          <input
            type="text"
            placeholder="Search deployments..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-sm placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <div className="flex items-center gap-2">
          <FunnelIcon className="h-4 w-4 text-slate-500" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-sm text-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">All Status</option>
            <option value="COMPLETED">Completed</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="FAILED">Failed</option>
            <option value="PENDING">Pending</option>
            <option value="ROLLED_BACK">Rolled Back</option>
          </select>
        </div>
      </div>

      {/* Deployments Table */}
      <div className="bg-slate-900/50 rounded-xl border border-slate-800 overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-800">
              <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Service</th>
              <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Version</th>
              <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Status</th>
              <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Type</th>
              <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Progress</th>
              <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Duration</th>
              <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Initiated By</th>
              <th className="px-5 py-3 text-right text-xs font-medium text-slate-400 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800">
            {filteredDeployments.map((deployment) => {
              const status = statusConfig[deployment.status as keyof typeof statusConfig];
              const StatusIcon = status.icon;
              return (
                <tr 
                  key={deployment.id} 
                  className="hover:bg-slate-800/30 transition-colors cursor-pointer"
                  onClick={() => setSelectedDeployment(deployment.id)}
                >
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-2">
                      <RocketLaunchIcon className="h-4 w-4 text-slate-500" />
                      <span className="text-sm font-medium text-slate-200">{deployment.serviceName}</span>
                    </div>
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-mono text-indigo-400">{deployment.version}</span>
                      <span className="text-slate-600">←</span>
                      <span className="text-xs text-slate-500 font-mono">{deployment.previousVersion}</span>
                    </div>
                  </td>
                  <td className="px-5 py-4">
                    <span className={clsx(
                      'badge',
                      status.color === 'success' && 'badge-success',
                      status.color === 'error' && 'badge-error',
                      status.color === 'warning' && 'badge-warning',
                      status.color === 'info' && 'badge-info',
                      status.color === 'neutral' && 'badge-neutral'
                    )}>
                      <StatusIcon className={clsx(
                        "h-3 w-3 mr-1",
                        deployment.status === 'IN_PROGRESS' && "animate-spin"
                      )} />
                      {status.label}
                    </span>
                  </td>
                  <td className="px-5 py-4">
                    <span className="text-sm text-slate-400">{deployment.deploymentType.replace('_', ' ')}</span>
                    {deployment.canaryPercentage && (
                      <span className="ml-2 text-xs text-purple-400">({deployment.canaryPercentage}%)</span>
                    )}
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-2">
                      <div className="w-24 h-2 bg-slate-700 rounded-full overflow-hidden">
                        <div 
                          className={clsx(
                            "h-full rounded-full transition-all",
                            deployment.status === 'FAILED' ? "bg-red-500" :
                            deployment.status === 'COMPLETED' ? "bg-green-500" :
                            deployment.status === 'ROLLED_BACK' ? "bg-amber-500" :
                            "bg-indigo-500"
                          )}
                          style={{ width: `${deployment.progress}%` }}
                        />
                      </div>
                      <span className="text-xs text-slate-500">{deployment.progress}%</span>
                    </div>
                  </td>
                  <td className="px-5 py-4">
                    <span className="text-sm text-slate-400 tabular-nums">
                      {formatDuration(deployment.startTime, deployment.endTime)}
                    </span>
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-2">
                      <span className="text-sm text-slate-300">{deployment.initiatedBy}</span>
                      {deployment.automated && (
                        <span className="text-xs text-purple-400 bg-purple-500/10 px-1.5 py-0.5 rounded">Auto</span>
                      )}
                    </div>
                  </td>
                  <td className="px-5 py-4 text-right">
                    <div className="flex items-center justify-end gap-2">
                      {deployment.status === 'IN_PROGRESS' && (
                        <button className="p-1.5 rounded-lg hover:bg-slate-700 text-red-400 transition-colors" title="Cancel">
                          <XCircleIcon className="h-4 w-4" />
                        </button>
                      )}
                      {(deployment.status === 'COMPLETED' || deployment.status === 'IN_PROGRESS') && (
                        <button className="p-1.5 rounded-lg hover:bg-slate-700 text-amber-400 transition-colors" title="Rollback">
                          <ArrowUturnLeftIcon className="h-4 w-4" />
                        </button>
                      )}
                      <button className="p-1.5 rounded-lg hover:bg-slate-700 text-slate-400 transition-colors" title="View Details">
                        <EyeIcon className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Quick Actions */}
      <div className="flex items-center justify-between p-4 bg-slate-900/50 rounded-xl border border-slate-800">
        <div className="flex items-center gap-2 text-sm text-slate-400">
          <ClockIcon className="h-4 w-4" />
          <span>1 deployment scheduled for 30 minutes from now</span>
        </div>
        <button className="text-sm text-indigo-400 hover:text-indigo-300 transition-colors">
          View Schedule →
        </button>
      </div>
    </div>
  );
}

function StatCard({ 
  label, 
  value, 
  color = 'default', 
  icon: Icon 
}: { 
  label: string; 
  value: string; 
  color?: 'success' | 'error' | 'warning' | 'info' | 'default';
  icon: React.ComponentType<{ className?: string }>;
}) {
  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-4">
      <div className="flex items-center justify-between">
        <Icon className={clsx(
          "h-5 w-5",
          color === 'success' && "text-green-400",
          color === 'error' && "text-red-400",
          color === 'warning' && "text-amber-400",
          color === 'info' && "text-blue-400",
          color === 'default' && "text-slate-400"
        )} />
        <span className={clsx(
          "text-2xl font-semibold",
          color === 'success' && "text-green-400",
          color === 'error' && "text-red-400",
          color === 'warning' && "text-amber-400",
          color === 'info' && "text-blue-400",
          color === 'default' && "text-white"
        )}>
          {value}
        </span>
      </div>
      <span className="text-xs text-slate-500 mt-2 block">{label}</span>
    </div>
  );
}
