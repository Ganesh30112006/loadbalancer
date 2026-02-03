import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { useMetricsWebSocket } from '../hooks/useWebSocket';
import type { WsMetricsData } from '../types';
import {
  ArrowLeftIcon,
  PlayIcon,
  PauseIcon,
  ArrowPathIcon,
  ExclamationTriangleIcon,
  ChartBarIcon,
  ServerStackIcon,
  GlobeAltIcon,
  Cog6ToothIcon,
  ClockIcon,
  CpuChipIcon,
  BoltIcon
} from '@heroicons/react/24/outline';
import { CheckCircleIcon as CheckCircleSolid } from '@heroicons/react/24/solid';
import {
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Area,
  AreaChart,
} from 'recharts';
import toast from 'react-hot-toast';
import clsx from 'clsx';

type TabType = 'overview' | 'regions' | 'metrics' | 'activity';

export default function ServiceDetailPage() {
  const { serviceId } = useParams<{ serviceId: string }>();
  const queryClient = useQueryClient();
  const [metricsHistory, setMetricsHistory] = useState<WsMetricsData[]>([]);
  const [activeTab, setActiveTab] = useState<TabType>('overview');

  const { data: service, isLoading } = useQuery({
    queryKey: ['services', serviceId],
    queryFn: () => api.getService(serviceId!),
    enabled: !!serviceId,
    refetchInterval: 10000,
  });

  const { data: auditLogs } = useQuery({
    queryKey: ['services', serviceId, 'audit'],
    queryFn: () => api.getAuditLogs(serviceId!, { limit: 20 }),
    enabled: !!serviceId,
    refetchInterval: 10000,
  });

  // Real-time metrics via WebSocket
  const { isConnected, subscribe, unsubscribe } = useMetricsWebSocket({
    onMetrics: (metrics) => {
      setMetricsHistory((prev) => {
        const newHistory = [...prev, metrics];
        return newHistory.slice(-60);
      });
    },
  });

  useEffect(() => {
    if (serviceId && isConnected) {
      subscribe(serviceId);
      return () => unsubscribe(serviceId);
    }
  }, [serviceId, isConnected, subscribe, unsubscribe]);

  const toggleAutomationMutation = useMutation({
    mutationFn: ({ enabled }: { enabled: boolean }) =>
      api.setAutomation(serviceId!, enabled),
    onSuccess: () => {
      toast.success('Automation setting updated');
      queryClient.invalidateQueries({ queryKey: ['services', serviceId] });
    },
  });

  const toggleScalingMutation = useMutation({
    mutationFn: ({ enabled }: { enabled: boolean }) =>
      api.setScalingEnabled(serviceId!, enabled),
    onSuccess: () => {
      toast.success('Scaling setting updated');
      queryClient.invalidateQueries({ queryKey: ['services', serviceId] });
    },
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <ArrowPathIcon className="h-8 w-8 text-slate-400 animate-spin" />
      </div>
    );
  }

  if (!service) {
    return (
      <div className="text-center py-12">
        <ServerStackIcon className="h-12 w-12 text-slate-700 mx-auto mb-3" />
        <p className="text-slate-500">Service not found</p>
      </div>
    );
  }

  const latestMetrics = metricsHistory[metricsHistory.length - 1];

  const tabs = [
    { id: 'overview' as TabType, label: 'Overview', icon: ChartBarIcon },
    { id: 'regions' as TabType, label: 'Regions', icon: GlobeAltIcon },
    { id: 'metrics' as TabType, label: 'Metrics', icon: CpuChipIcon },
    { id: 'activity' as TabType, label: 'Activity', icon: ClockIcon },
  ];

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Link
          to="/services"
          className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
        >
          <ArrowLeftIcon className="h-5 w-5" />
        </Link>
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold text-white">
              {service.displayName || service.serviceName}
            </h1>
            <span className={clsx(
              'badge',
              service.status === 'ACTIVE' && 'badge-success',
              service.status === 'SUSPENDED' && 'badge-neutral',
              service.status === 'DEGRADED' && 'badge-warning',
              service.status === 'FAILED' && 'badge-error',
              service.status === 'PROVISIONING' && 'badge-info'
            )}>
              {service.status}
            </span>
          </div>
          <p className="text-sm text-slate-400 mt-1 font-mono">{service.serviceName}</p>
        </div>
        
        {/* Real-time Status */}
        <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-900/50 rounded-lg border border-slate-800">
          <span className={clsx(
            'h-2 w-2 rounded-full',
            isConnected ? 'bg-green-500 animate-pulse-slow' : 'bg-red-500'
          )} />
          <span className="text-xs text-slate-400">
            {isConnected ? 'Real-time' : 'Disconnected'}
          </span>
        </div>
      </div>

      {/* Controls Bar */}
      <div className="flex items-center gap-4 p-4 bg-slate-900/50 rounded-xl border border-slate-800">
        <button
          onClick={() =>
            toggleAutomationMutation.mutate({ enabled: !service.automationEnabled })
          }
          className={clsx(
            'flex items-center gap-2 px-4 py-2 rounded-lg border transition-colors',
            service.automationEnabled
              ? 'bg-green-500/10 text-green-400 border-green-500/30 hover:bg-green-500/20'
              : 'bg-slate-800 text-slate-300 border-slate-700 hover:bg-slate-700'
          )}
        >
          {service.automationEnabled ? (
            <>
              <PauseIcon className="h-4 w-4" />
              Automation On
            </>
          ) : (
            <>
              <PlayIcon className="h-4 w-4" />
              Automation Off
            </>
          )}
        </button>

        <button
          onClick={() =>
            toggleScalingMutation.mutate({ enabled: !service.scalingEnabled })
          }
          className={clsx(
            'flex items-center gap-2 px-4 py-2 rounded-lg border transition-colors',
            service.scalingEnabled
              ? 'bg-blue-500/10 text-blue-400 border-blue-500/30 hover:bg-blue-500/20'
              : 'bg-slate-800 text-slate-300 border-slate-700 hover:bg-slate-700'
          )}
        >
          <BoltIcon className="h-4 w-4" />
          {service.scalingEnabled ? 'Scaling On' : 'Scaling Off'}
        </button>

        <div className="flex-1" />

        <button className="flex items-center gap-2 px-4 py-2 bg-slate-800 text-slate-300 rounded-lg border border-slate-700 hover:bg-slate-700 transition-colors">
          <Cog6ToothIcon className="h-4 w-4" />
          Configure
        </button>
      </div>

      {/* Tabs */}
      <div className="border-b border-slate-800">
        <nav className="flex gap-1">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={clsx(
                'flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors',
                activeTab === tab.id
                  ? 'border-indigo-500 text-indigo-400'
                  : 'border-transparent text-slate-400 hover:text-slate-200'
              )}
            >
              <tab.icon className="h-4 w-4" />
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      {activeTab === 'overview' && (
        <OverviewTab service={service} latestMetrics={latestMetrics} metricsHistory={metricsHistory} />
      )}
      {activeTab === 'regions' && <RegionsTab service={service} />}
      {activeTab === 'metrics' && <MetricsTab metricsHistory={metricsHistory} />}
      {activeTab === 'activity' && <ActivityTab auditLogs={auditLogs} />}
    </div>
  );
}

function OverviewTab({ service, latestMetrics, metricsHistory }: { service: any; latestMetrics?: WsMetricsData; metricsHistory: WsMetricsData[] }) {
  return (
    <div className="space-y-6">
      {/* Status Cards */}
      <div className="grid grid-cols-4 gap-4">
        <StatusCard
          label="CPU Utilization"
          value={`${latestMetrics?.avgCpu?.toFixed(1) || '--'}%`}
          icon={CpuChipIcon}
          status={latestMetrics?.avgCpu && latestMetrics.avgCpu > 80 ? 'warning' : 'success'}
        />
        <StatusCard
          label="Latency P99"
          value={`${latestMetrics?.latencyP99 || '--'}ms`}
          icon={ClockIcon}
          status={latestMetrics?.latencyP99 && latestMetrics.latencyP99 > 200 ? 'warning' : 'success'}
        />
        <StatusCard
          label="Error Rate"
          value={`${latestMetrics?.errorRate?.toFixed(2) || '--'}%`}
          icon={ExclamationTriangleIcon}
          status={latestMetrics?.errorRate && latestMetrics.errorRate > 1 ? 'error' : 'success'}
        />
        <StatusCard
          label="Regions Active"
          value={`${service.regions?.filter((r: any) => r.status === 'ACTIVE').length || 0}/${service.regions?.length || 0}`}
          icon={GlobeAltIcon}
          status="info"
        />
      </div>

      {/* Mini Charts */}
      {metricsHistory.length > 0 && (
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-4">
            <h3 className="text-sm font-medium text-slate-300 mb-4">CPU Utilization (Last 5 min)</h3>
            <ResponsiveContainer width="100%" height={150}>
              <AreaChart data={metricsHistory}>
                <defs>
                  <linearGradient id="cpuGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="timestamp" tickFormatter={(t) => new Date(t).toLocaleTimeString()} tick={{ fill: '#64748b', fontSize: 10 }} />
                <YAxis domain={[0, 100]} tick={{ fill: '#64748b', fontSize: 10 }} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px' }}
                  labelStyle={{ color: '#94a3b8' }}
                  labelFormatter={(t) => new Date(t).toLocaleTimeString()}
                  formatter={(value: number) => [`${value.toFixed(1)}%`, 'CPU']}
                />
                <Area type="monotone" dataKey="avgCpu" stroke="#6366f1" fill="url(#cpuGradient)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-4">
            <h3 className="text-sm font-medium text-slate-300 mb-4">Latency P99 (Last 5 min)</h3>
            <ResponsiveContainer width="100%" height={150}>
              <AreaChart data={metricsHistory}>
                <defs>
                  <linearGradient id="latencyGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="timestamp" tickFormatter={(t) => new Date(t).toLocaleTimeString()} tick={{ fill: '#64748b', fontSize: 10 }} />
                <YAxis tick={{ fill: '#64748b', fontSize: 10 }} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px' }}
                  labelStyle={{ color: '#94a3b8' }}
                  labelFormatter={(t) => new Date(t).toLocaleTimeString()}
                  formatter={(value: number) => [`${value}ms`, 'P99']}
                />
                <Area type="monotone" dataKey="latencyP99" stroke="#10b981" fill="url(#latencyGradient)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </div>
  );
}

function StatusCard({ label, value, icon: Icon, status }: { label: string; value: string; icon: React.ComponentType<{ className?: string }>; status: 'success' | 'warning' | 'error' | 'info' }) {
  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-4">
      <div className="flex items-center gap-3">
        <div className={clsx(
          "p-2 rounded-lg",
          status === 'success' && "bg-green-500/10",
          status === 'warning' && "bg-amber-500/10",
          status === 'error' && "bg-red-500/10",
          status === 'info' && "bg-blue-500/10"
        )}>
          <Icon className={clsx(
            "h-5 w-5",
            status === 'success' && "text-green-400",
            status === 'warning' && "text-amber-400",
            status === 'error' && "text-red-400",
            status === 'info' && "text-blue-400"
          )} />
        </div>
        <div>
          <span className="text-xs text-slate-500 uppercase tracking-wide">{label}</span>
          <p className="text-xl font-semibold text-white">{value}</p>
        </div>
      </div>
    </div>
  );
}

function RegionsTab({ service }: { service: any }) {
  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 overflow-hidden">
      <table className="w-full">
        <thead>
          <tr className="border-b border-slate-800">
            <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Region</th>
            <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Status</th>
            <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">ASG</th>
            <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">ALB</th>
            <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Traffic</th>
            <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Capacity</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-800">
          {service.regions?.map((region: any) => (
            <tr key={region.id} className="hover:bg-slate-800/30 transition-colors">
              <td className="px-5 py-4 text-sm font-medium text-white flex items-center gap-2">
                <GlobeAltIcon className="h-4 w-4 text-slate-500" />
                {region.region}
              </td>
              <td className="px-5 py-4">
                <span className={clsx(
                  'badge',
                  region.status === 'ACTIVE' && 'badge-success',
                  region.status === 'PROVISIONING' && 'badge-info',
                  region.status === 'DEGRADED' && 'badge-warning',
                  region.status === 'FAILING_OVER' && 'badge-warning',
                  region.status === 'FAILED' && 'badge-error'
                )}>
                  {region.status}
                </span>
              </td>
              <td className="px-5 py-4 text-sm text-slate-400 font-mono">
                {region.asgName || '--'}
              </td>
              <td className="px-5 py-4 text-sm text-slate-400 font-mono">
                {region.albArn ? region.albArn.split('/')[2] : '--'}
              </td>
              <td className="px-5 py-4">
                <div className="flex items-center gap-2">
                  <div className="flex-1 h-2 bg-slate-800 rounded-full overflow-hidden max-w-[80px]">
                    <div 
                      className="h-full bg-indigo-500 rounded-full" 
                      style={{ width: `${region.trafficWeight}%` }}
                    />
                  </div>
                  <span className="text-sm text-slate-300 tabular-nums">{region.trafficWeight}%</span>
                </div>
              </td>
              <td className="px-5 py-4 text-sm text-slate-400">
                <span className="text-white font-medium">{region.desiredCapacity}</span>
                {' '}/ {region.minCapacity}-{region.maxCapacity}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function MetricsTab({ metricsHistory }: { metricsHistory: WsMetricsData[] }) {
  if (metricsHistory.length === 0) {
    return (
      <div className="text-center py-12 bg-slate-900/50 rounded-xl border border-slate-800">
        <ChartBarIcon className="h-12 w-12 text-slate-700 mx-auto mb-3" />
        <p className="text-slate-500">Waiting for metrics data...</p>
        <p className="text-xs text-slate-600 mt-1">Data will appear when the WebSocket connects</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-6">
      <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-5">
        <h3 className="text-sm font-medium text-slate-300 mb-4">CPU Utilization</h3>
        <ResponsiveContainer width="100%" height={250}>
          <AreaChart data={metricsHistory}>
            <defs>
              <linearGradient id="cpuGradientFull" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3}/>
                <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
            <XAxis dataKey="timestamp" tickFormatter={(t) => new Date(t).toLocaleTimeString()} tick={{ fill: '#64748b', fontSize: 10 }} />
            <YAxis domain={[0, 100]} tick={{ fill: '#64748b', fontSize: 10 }} />
            <Tooltip
              contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px' }}
              labelStyle={{ color: '#94a3b8' }}
              labelFormatter={(t) => new Date(t).toLocaleTimeString()}
              formatter={(value: number) => [`${value.toFixed(1)}%`, 'CPU']}
            />
            <Area type="monotone" dataKey="avgCpu" stroke="#6366f1" fill="url(#cpuGradientFull)" strokeWidth={2} />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-5">
        <h3 className="text-sm font-medium text-slate-300 mb-4">Latency P99</h3>
        <ResponsiveContainer width="100%" height={250}>
          <AreaChart data={metricsHistory}>
            <defs>
              <linearGradient id="latencyGradientFull" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#10b981" stopOpacity={0.3}/>
                <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
            <XAxis dataKey="timestamp" tickFormatter={(t) => new Date(t).toLocaleTimeString()} tick={{ fill: '#64748b', fontSize: 10 }} />
            <YAxis tick={{ fill: '#64748b', fontSize: 10 }} />
            <Tooltip
              contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px' }}
              labelStyle={{ color: '#94a3b8' }}
              labelFormatter={(t) => new Date(t).toLocaleTimeString()}
              formatter={(value: number) => [`${value}ms`, 'P99']}
            />
            <Area type="monotone" dataKey="latencyP99" stroke="#10b981" fill="url(#latencyGradientFull)" strokeWidth={2} />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-5">
        <h3 className="text-sm font-medium text-slate-300 mb-4">Request Count</h3>
        <ResponsiveContainer width="100%" height={250}>
          <AreaChart data={metricsHistory}>
            <defs>
              <linearGradient id="requestGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#f59e0b" stopOpacity={0.3}/>
                <stop offset="95%" stopColor="#f59e0b" stopOpacity={0}/>
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
            <XAxis dataKey="timestamp" tickFormatter={(t) => new Date(t).toLocaleTimeString()} tick={{ fill: '#64748b', fontSize: 10 }} />
            <YAxis tick={{ fill: '#64748b', fontSize: 10 }} />
            <Tooltip
              contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px' }}
              labelStyle={{ color: '#94a3b8' }}
              labelFormatter={(t) => new Date(t).toLocaleTimeString()}
              formatter={(value: number) => [value.toLocaleString(), 'Requests']}
            />
            <Area type="monotone" dataKey="requestCount" stroke="#f59e0b" fill="url(#requestGradient)" strokeWidth={2} />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-5">
        <h3 className="text-sm font-medium text-slate-300 mb-4">Error Rate</h3>
        <ResponsiveContainer width="100%" height={250}>
          <AreaChart data={metricsHistory}>
            <defs>
              <linearGradient id="errorGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3}/>
                <stop offset="95%" stopColor="#ef4444" stopOpacity={0}/>
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
            <XAxis dataKey="timestamp" tickFormatter={(t) => new Date(t).toLocaleTimeString()} tick={{ fill: '#64748b', fontSize: 10 }} />
            <YAxis tick={{ fill: '#64748b', fontSize: 10 }} />
            <Tooltip
              contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px' }}
              labelStyle={{ color: '#94a3b8' }}
              labelFormatter={(t) => new Date(t).toLocaleTimeString()}
              formatter={(value: number) => [`${value.toFixed(2)}%`, 'Error Rate']}
            />
            <Area type="monotone" dataKey="errorRate" stroke="#ef4444" fill="url(#errorGradient)" strokeWidth={2} />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

function ActivityTab({ auditLogs }: { auditLogs?: any[] }) {
  if (!auditLogs || auditLogs.length === 0) {
    return (
      <div className="text-center py-12 bg-slate-900/50 rounded-xl border border-slate-800">
        <ClockIcon className="h-12 w-12 text-slate-700 mx-auto mb-3" />
        <p className="text-slate-500">No recent activity</p>
      </div>
    );
  }

  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 divide-y divide-slate-800">
      {auditLogs.slice(0, 15).map((log) => (
        <div key={log.id} className="px-5 py-4 hover:bg-slate-800/30 transition-colors">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className={clsx(
                'badge',
                (log.actionType === 'SCALE_OUT' || log.actionType === 'SCALE_IN') && 'badge-info',
                log.actionType === 'FAILOVER' && 'badge-error',
                !['SCALE_OUT', 'SCALE_IN', 'FAILOVER'].includes(log.actionType) && 'badge-neutral'
              )}>
                {log.actionType.replace('_', ' ')}
              </span>
              <span className="text-sm text-slate-300">{log.region}</span>
            </div>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-1.5">
                {log.executionResult === 'SUCCESS' ? (
                  <CheckCircleSolid className="h-4 w-4 text-green-500" />
                ) : log.executionResult === 'FAILED' ? (
                  <ExclamationTriangleIcon className="h-4 w-4 text-red-500" />
                ) : (
                  <ClockIcon className="h-4 w-4 text-slate-500" />
                )}
                <span className={clsx(
                  'text-xs font-medium',
                  log.executionResult === 'SUCCESS' && 'text-green-400',
                  log.executionResult === 'FAILED' && 'text-red-400',
                  log.executionResult === 'PENDING' && 'text-slate-400'
                )}>
                  {log.executionResult}
                </span>
              </div>
              <span className="text-xs text-slate-500 tabular-nums">
                {new Date(log.timestamp).toLocaleString()}
              </span>
            </div>
          </div>
          {log.decisionRationale && (
            <p className="mt-2 text-sm text-slate-500">
              {log.decisionRationale}
            </p>
          )}
        </div>
      ))}
    </div>
  );
}
