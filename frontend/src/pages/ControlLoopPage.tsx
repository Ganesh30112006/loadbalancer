import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { Layout } from '../components/Layout';
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  PauseIcon,
  ArrowPathIcon,
  CpuChipIcon,
  EyeIcon,
  AdjustmentsHorizontalIcon,
  LightBulbIcon,
  BoltIcon,
  ClockIcon,
  ServerStackIcon,
  ArrowUturnLeftIcon
} from '@heroicons/react/24/outline';
import { CheckCircleIcon as CheckCircleSolid, PlayIcon as PlaySolid } from '@heroicons/react/24/solid';
import clsx from 'clsx';

export default function ControlLoopPage() {
  const { data: status, isLoading } = useQuery({
    queryKey: ['controlLoopStatus'],
    queryFn: api.getControlLoopStatus,
    refetchInterval: 10000,
  });

  const { data: recentDecisions } = useQuery({
    queryKey: ['recentDecisions'],
    queryFn: () => api.getAuditLogs('all', { limit: 20 }),
    refetchInterval: 10000,
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <ArrowPathIcon className="h-8 w-8 text-slate-400 animate-spin" />
      </div>
    );
  }

  const controlLoopStatus = status || {
    isLeader: false,
    instanceId: 'unknown',
    activeServicesCount: 0,
    lastCheckedAt: null,
  };

  return (
    <Layout>
    <div className="space-y-6 animate-fade-in">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white flex items-center gap-3">
            <CpuChipIcon className="h-7 w-7 text-indigo-400" />
            Control Loop
            <span className="flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium bg-green-500/10 text-green-400 border border-green-500/20">
              <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-live" />
              Active
            </span>
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            OODA loop-based autonomous infrastructure control
          </p>
        </div>
        <div className="flex items-center gap-3">
          {/* Auto-update indicator */}
          <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-slate-800/50 border border-slate-700">
            <ArrowPathIcon className="h-4 w-4 text-green-400 animate-spin" />
            <span className="text-xs text-slate-400">Auto-updating</span>
          </div>
          <button className="flex items-center gap-2 px-4 py-2 bg-amber-500/10 text-amber-400 rounded-lg border border-amber-500/30 hover:bg-amber-500/20 transition-colors">
            <PauseIcon className="h-4 w-4" />
            Pause Loop
          </button>
        </div>
      </div>

      {/* Status Cards */}
      <div className="grid grid-cols-4 gap-4">
        <StatusCard
          label="Status"
          value={controlLoopStatus.isLeader ? 'Running' : 'Standby'}
          icon={controlLoopStatus.isLeader ? PlaySolid : PauseIcon}
          status={controlLoopStatus.isLeader ? 'success' : 'neutral'}
        />
        <StatusCard
          label="Leader Node"
          value={controlLoopStatus.isLeader ? 'This Instance' : 'Other'}
          icon={CheckCircleIcon}
          status={controlLoopStatus.isLeader ? 'success' : 'warning'}
        />
        <StatusCard
          label="Active Services"
          value={controlLoopStatus.activeServicesCount.toString()}
          icon={ServerStackIcon}
          status="info"
        />
        <StatusCard
          label="Last Cycle"
          value={controlLoopStatus.lastCheckedAt ? new Date(controlLoopStatus.lastCheckedAt).toLocaleTimeString() : 'Never'}
          icon={ClockIcon}
          status="neutral"
        />
      </div>

      {/* OODA Loop Visualization */}
      <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-medium text-white">OODA Loop Phases</h2>
          <div className="flex items-center gap-2 text-xs text-slate-500">
            <ArrowPathIcon className="h-3 w-3" />
            Cycle time: ~30s
          </div>
        </div>
        <div className="grid grid-cols-4 gap-4">
          <OODAPhaseCard
            phase="Observe"
            icon={EyeIcon}
            description="Collect metrics from CloudWatch"
            status="active"
            metrics={['CPU Utilization', 'Request Latency', 'Error Rate', 'Request Count']}
            color="blue"
          />
          <OODAPhaseCard
            phase="Orient"
            icon={AdjustmentsHorizontalIcon}
            description="Analyze metrics against SLO targets"
            status="active"
            metrics={['SLO Comparison', 'Trend Analysis', 'Anomaly Detection']}
            color="purple"
          />
          <OODAPhaseCard
            phase="Decide"
            icon={LightBulbIcon}
            description="Determine scaling actions"
            status="active"
            metrics={['Rule Evaluation', 'Guardrail Check', 'Cost Analysis']}
            color="amber"
          />
          <OODAPhaseCard
            phase="Act"
            icon={BoltIcon}
            description="Execute infrastructure changes"
            status="active"
            metrics={['ASG Scaling', 'Traffic Shifting', 'Failover']}
            color="green"
          />
        </div>
      </div>

      {/* Recent Decisions */}
      <div className="bg-slate-900/50 rounded-xl border border-slate-800 overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800">
          <h3 className="text-sm font-medium text-slate-200">Recent Decisions</h3>
          <span className="text-xs text-slate-500">Last 20 actions</span>
        </div>
        {recentDecisions && recentDecisions.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-slate-800">
                  <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Time</th>
                  <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Category</th>
                  <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Action</th>
                  <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Status</th>
                  <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Description</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {recentDecisions.map((decision) => (
                  <tr key={decision.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="px-5 py-4 text-sm text-slate-400 tabular-nums">
                      {new Date(decision.createdAt).toLocaleTimeString()}
                    </td>
                    <td className="px-5 py-4">
                      <span className="text-sm text-slate-300">{decision.category}</span>
                    </td>
                    <td className="px-5 py-4">
                      <span className={clsx(
                        'badge',
                        decision.actionType === 'SCALE_OUT' && 'badge-success',
                        decision.actionType === 'SCALE_IN' && 'badge-info',
                        decision.actionType === 'FAILOVER' && 'badge-error',
                        !['SCALE_OUT', 'SCALE_IN', 'FAILOVER'].includes(decision.actionType) && 'badge-neutral'
                      )}>
                        {decision.actionType.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-1.5">
                        {decision.status === 'SUCCESS' ? (
                          <CheckCircleSolid className="h-4 w-4 text-green-500" />
                        ) : decision.status === 'FAILED' ? (
                          <ExclamationTriangleIcon className="h-4 w-4 text-red-500" />
                        ) : (
                          <ClockIcon className="h-4 w-4 text-slate-500" />
                        )}
                        <span className={clsx(
                          'text-sm',
                          decision.status === 'SUCCESS' && 'text-green-400',
                          decision.status === 'FAILED' && 'text-red-400',
                          !['SUCCESS', 'FAILED'].includes(decision.status) && 'text-slate-400'
                        )}>
                          {decision.status}
                        </span>
                      </div>
                    </td>
                    <td className="px-5 py-4 text-sm text-slate-500 max-w-xs truncate">
                      {decision.description}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="px-6 py-12 text-center">
            <CpuChipIcon className="h-12 w-12 text-slate-700 mx-auto mb-3" />
            <p className="text-slate-500">No recent decisions</p>
            <p className="text-xs text-slate-600 mt-1">The control loop will make decisions as needed</p>
          </div>
        )}
      </div>

      {/* Quick Actions */}
      <div className="flex items-center justify-between p-4 bg-slate-900/50 rounded-xl border border-slate-800">
        <div className="flex items-center gap-4">
          <button className="flex items-center gap-2 px-4 py-2 bg-slate-800 text-slate-300 rounded-lg border border-slate-700 hover:bg-slate-700 transition-colors">
            <ArrowUturnLeftIcon className="h-4 w-4" />
            Rollback Last Action
          </button>
          <button className="flex items-center gap-2 px-4 py-2 bg-slate-800 text-slate-300 rounded-lg border border-slate-700 hover:bg-slate-700 transition-colors">
            Force Cycle Now
          </button>
        </div>
        <div className="text-xs text-slate-500">
          Next cycle in: <span className="text-slate-400 tabular-nums">~28s</span>
        </div>
      </div>
    </div>
    </Layout>
  );
}

function StatusCard({ 
  label, 
  value, 
  icon: Icon,
  status 
}: { 
  label: string; 
  value: string;
  icon: React.ComponentType<{ className?: string }>;
  status: 'success' | 'warning' | 'error' | 'info' | 'neutral';
}) {
  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-4">
      <div className="flex items-center justify-between">
        <div className={clsx(
          "p-2 rounded-lg",
          status === 'success' && "bg-green-500/10",
          status === 'warning' && "bg-amber-500/10",
          status === 'error' && "bg-red-500/10",
          status === 'info' && "bg-blue-500/10",
          status === 'neutral' && "bg-slate-700"
        )}>
          <Icon className={clsx(
            "h-5 w-5",
            status === 'success' && "text-green-400",
            status === 'warning' && "text-amber-400",
            status === 'error' && "text-red-400",
            status === 'info' && "text-blue-400",
            status === 'neutral' && "text-slate-400"
          )} />
        </div>
      </div>
      <div className="mt-3">
        <span className="text-xs text-slate-500 uppercase tracking-wide">{label}</span>
        <p className={clsx(
          "text-lg font-semibold mt-0.5",
          status === 'success' && "text-green-400",
          status === 'warning' && "text-amber-400",
          status === 'error' && "text-red-400",
          status === 'info' && "text-white",
          status === 'neutral' && "text-slate-300"
        )}>
          {value}
        </p>
      </div>
    </div>
  );
}

function OODAPhaseCard({
  phase,
  icon: Icon,
  description,
  status,
  metrics,
  color,
}: {
  phase: string;
  icon: React.ComponentType<{ className?: string }>;
  description: string;
  status: 'active' | 'inactive';
  metrics: string[];
  color: 'blue' | 'purple' | 'amber' | 'green';
}) {
  const colorClasses = {
    blue: { bg: 'bg-blue-500/10', border: 'border-blue-500/20', text: 'text-blue-400', dot: 'bg-blue-500' },
    purple: { bg: 'bg-purple-500/10', border: 'border-purple-500/20', text: 'text-purple-400', dot: 'bg-purple-500' },
    amber: { bg: 'bg-amber-500/10', border: 'border-amber-500/20', text: 'text-amber-400', dot: 'bg-amber-500' },
    green: { bg: 'bg-green-500/10', border: 'border-green-500/20', text: 'text-green-400', dot: 'bg-green-500' },
  };

  const colors = colorClasses[color];

  return (
    <div className={clsx(
      'rounded-xl border p-4 transition-all',
      status === 'active' ? `${colors.bg} ${colors.border}` : 'bg-slate-800/50 border-slate-700'
    )}>
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <Icon className={clsx("h-5 w-5", status === 'active' ? colors.text : 'text-slate-500')} />
          <h3 className="font-medium text-white">{phase}</h3>
        </div>
        <span className={clsx(
          'h-2 w-2 rounded-full',
          status === 'active' ? `${colors.dot} animate-pulse-slow` : 'bg-slate-600'
        )} />
      </div>
      <p className="text-sm text-slate-400 mb-3">{description}</p>
      <ul className="space-y-1">
        {metrics.map((metric) => (
          <li key={metric} className="flex items-center gap-2 text-xs text-slate-500">
            <span className="w-1 h-1 rounded-full bg-slate-600" />
            {metric}
          </li>
        ))}
      </ul>
    </div>
  );
}
