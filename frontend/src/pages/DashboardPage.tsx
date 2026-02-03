import { useEffect, useState, useCallback } from 'react';
import { useLoadBalancingStore } from '../store/loadBalancingStore';
import { useMetricsWebSocket } from '../hooks/useWebSocket';
import { Layout } from '../components/Layout';
import { api } from '../services/api';
import { 
  ArrowPathIcon, 
  ArrowTrendingUpIcon,
  ArrowTrendingDownIcon,
  ServerStackIcon,
  GlobeAltIcon,
  ClockIcon,
  BoltIcon,
  ShieldCheckIcon,
  CpuChipIcon,
  PauseIcon,
  ArrowUturnLeftIcon,
  InformationCircleIcon
} from '@heroicons/react/24/outline';
import { CheckCircleIcon as CheckCircleSolid, ExclamationTriangleIcon as ExclamationSolid } from '@heroicons/react/24/solid';
import clsx from 'clsx';
import { AreaChart, Area, LineChart, Line, XAxis, YAxis, ResponsiveContainer, Tooltip, CartesianGrid } from 'recharts';
import type { Service, ControlLoopStatus, AuditLogEntry, DashboardData } from '../types';

export default function DashboardPage() {
  const { 
    dashboardData,
    loadingDashboard,
    refreshDashboard
  } = useLoadBalancingStore();
  
  const [services, setServices] = useState<Service[]>([]);
  const [controlLoopStatus, setControlLoopStatus] = useState<ControlLoopStatus | null>(null);
  const [recentActions, setRecentActions] = useState<AuditLogEntry[]>([]);
  const [loadingData, setLoadingData] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  // Handle real-time dashboard updates from WebSocket
  const handleDashboardUpdate = useCallback((data: DashboardData) => {
    if (data.services) {
      setServices(data.services);
    }
    if (data.controlLoop) {
      setControlLoopStatus({
        isLeader: data.controlLoop.isLeader,
        leaderId: '',
        status: data.controlLoop.status,
        lastDecisionAt: null,
        pendingActions: 0
      });
    }
    if (data.recentActions) {
      setRecentActions(data.recentActions.map(log => ({
        id: log.id,
        action: log.action,
        serviceId: log.serviceId || '',
        serviceName: log.serviceName || '',
        details: log.details || '',
        timestamp: log.timestamp,
        status: log.status as 'SUCCESS' | 'FAILURE' | 'PENDING',
        userId: log.userId
      })));
    }
    setLastUpdated(new Date(data.timestamp));
    if (loadingData) {
      setLoadingData(false);
    }
  }, [loadingData]);

  const { isConnected, subscribeDashboard, unsubscribeDashboard } = useMetricsWebSocket({
    onDashboardUpdate: handleDashboardUpdate
  });

  // Fetch initial dashboard data and subscribe to updates
  const fetchInitialData = useCallback(async () => {
    setLoadingData(true);
    try {
      const [servicesData, controlLoop, auditLogs] = await Promise.all([
        api.getServices(),
        api.getControlLoopStatus().catch(() => null),
        api.getRecentAuditLogs(5).catch(() => [])
      ]);
      setServices(servicesData);
      setControlLoopStatus(controlLoop);
      setRecentActions(auditLogs || []);
      setLastUpdated(new Date());
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error);
    } finally {
      setLoadingData(false);
    }
  }, []);

  // Subscribe to dashboard updates when connected
  useEffect(() => {
    fetchInitialData();
    refreshDashboard();
  }, [fetchInitialData, refreshDashboard]);

  // Subscribe/unsubscribe to dashboard WebSocket updates
  useEffect(() => {
    if (isConnected) {
      subscribeDashboard();
    }
    return () => {
      if (isConnected) {
        unsubscribeDashboard();
      }
    };
  }, [isConnected, subscribeDashboard, unsubscribeDashboard]);

  const statusData = dashboardData || {
    serviceStatus: 'UNKNOWN',
    healthStatus: 'UNKNOWN',
    automationEnabled: false,
    scalingEnabled: false,
    metrics: { avgCpu: 0, latencyP99: 0, errorRate: 0, requestsPerMinute: 0 },
    capacity: { healthyInstances: 0, unhealthyInstances: 0, regionCount: 0 },
    regions: [],
    sloViolations: [],
  };

  const hasData = dashboardData !== null || services.length > 0;

  // Generate chart data from actual metrics
  const chartData = statusData.regions.map((r: any) => ({
    time: r.region,
    cpu: r.avgCpu || 0,
    latency: r.latencyP99 || 0,
    requests: r.healthyInstances * 1000 || 0,
  }));

  return (
    <Layout>
    <div className="space-y-6 animate-fade-in">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white flex items-center gap-3">
            Dashboard
            {loadingData || loadingDashboard ? (
              <span className="flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium bg-blue-500/10 text-blue-400 border border-blue-500/20">
                <ArrowPathIcon className="w-3 h-3 animate-spin" />
                Loading
              </span>
            ) : hasData ? (
              <span className="flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium bg-green-500/10 text-green-400 border border-green-500/20">
                <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-live" />
                Live
              </span>
            ) : (
              <span className="flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium bg-slate-500/10 text-slate-400 border border-slate-500/20">
                <span className="w-1.5 h-1.5 rounded-full bg-slate-500" />
                No Data
              </span>
            )}
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            Real-time infrastructure health and performance metrics
          </p>
        </div>
        <div className="flex items-center gap-3">
          {/* Auto-refresh indicator */}
          <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-slate-800/50 border border-slate-700">
            <span className={clsx(
              "w-2 h-2 rounded-full",
              isConnected ? "bg-green-500 animate-pulse" : "bg-amber-500"
            )} />
            <span className="text-xs text-slate-400">
              {isConnected ? 'Connected' : 'Connecting...'}
            </span>
          </div>
          <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-slate-800/50 border border-slate-700">
            <ClockIcon className="h-4 w-4 text-slate-400" />
            <span className="text-xs text-slate-400">
              Updated: {lastUpdated.toLocaleTimeString()}
            </span>
          </div>
        </div>
      </div>

      {/* Top Status Bar - Fleet Health Summary */}
      <div className="grid grid-cols-5 gap-4">
        <StatusCard
          icon={ServerStackIcon}
          label="Services Active"
          value={services.length > 0 ? `${services.filter(s => s.status === 'ACTIVE').length}/${services.length}` : '0'}
          status={services.length === 0 ? 'warning' : services.filter(s => s.status === 'ACTIVE').length === services.length ? 'success' : 'warning'}
        />
        <StatusCard
          icon={GlobeAltIcon}
          label="Regions Healthy"
          value={statusData.capacity.regionCount > 0 ? `${statusData.capacity.regionCount}/${statusData.capacity.regionCount}` : '0'}
          status={statusData.capacity.regionCount > 0 ? 'success' : 'warning'}
        />
        <StatusCard
          icon={BoltIcon}
          label="Instances"
          value={`${statusData.capacity.healthyInstances}`}
          subValue={statusData.capacity.unhealthyInstances > 0 ? `${statusData.capacity.unhealthyInstances} unhealthy` : undefined}
          status={statusData.capacity.unhealthyInstances > 0 ? 'warning' : statusData.capacity.healthyInstances > 0 ? 'success' : 'warning'}
        />
        <StatusCard
          icon={ShieldCheckIcon}
          label="SLO Status"
          value={statusData.sloViolations?.length === 0 ? 'All Met' : `${statusData.sloViolations?.length || 0} Violations`}
          status={statusData.sloViolations?.length === 0 ? 'success' : 'warning'}
        />
        <StatusCard
          icon={CpuChipIcon}
          label="Control Loop"
          value={controlLoopStatus?.isLeader ? 'Active' : 'Standby'}
          status={controlLoopStatus?.isLeader ? 'success' : 'warning'}
        />
      </div>

      {/* Main Metrics Grid */}
      <div className="grid grid-cols-4 gap-4">
        <MetricCard
          label="CPU Utilization"
          value={statusData.metrics.avgCpu.toFixed(1)}
          unit="%"
          status={statusData.metrics.avgCpu > 80 ? 'warning' : 'normal'}
          sparklineData={chartData.length > 0 ? chartData.map(d => d.cpu) : undefined}
        />
        <MetricCard
          label="P99 Latency"
          value={statusData.metrics.latencyP99}
          unit="ms"
          status={statusData.metrics.latencyP99 > 500 ? 'warning' : 'normal'}
          sparklineData={chartData.length > 0 ? chartData.map(d => d.latency) : undefined}
        />
        <MetricCard
          label="Error Rate"
          value={(statusData.metrics.errorRate * 100).toFixed(3)}
          unit="%"
          status={statusData.metrics.errorRate > 0.01 ? 'warning' : 'normal'}
        />
        <MetricCard
          label="Requests/min"
          value={statusData.metrics.requestsPerMinute > 0 ? (statusData.metrics.requestsPerMinute / 1000).toFixed(1) : '0'}
          unit="K"
          status="normal"
          sparklineData={chartData.length > 0 ? chartData.map(d => d.requests) : undefined}
        />
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-5">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-slate-200">CPU & Memory Utilization</h3>
            <div className="flex items-center gap-4 text-xs">
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-indigo-500" />
                CPU
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-500" />
                Memory
              </span>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <AreaChart data={chartData}>
              <defs>
                <linearGradient id="cpuGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="memoryGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis dataKey="time" tick={{ fill: '#64748b', fontSize: 10 }} axisLine={{ stroke: '#334155' }} />
              <YAxis tick={{ fill: '#64748b', fontSize: 10 }} axisLine={{ stroke: '#334155' }} domain={[0, 100]} />
              <Tooltip 
                contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px' }}
                labelStyle={{ color: '#e2e8f0' }}
              />
              <Area type="monotone" dataKey="cpu" stroke="#6366f1" fillOpacity={1} fill="url(#cpuGradient)" />
              <Area type="monotone" dataKey="memory" stroke="#10b981" fillOpacity={1} fill="url(#memoryGradient)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-5">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-slate-200">Request Latency (P99)</h3>
            <span className="text-xs text-slate-500">Last 24 hours</span>
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis dataKey="time" tick={{ fill: '#64748b', fontSize: 10 }} axisLine={{ stroke: '#334155' }} />
              <YAxis tick={{ fill: '#64748b', fontSize: 10 }} axisLine={{ stroke: '#334155' }} />
              <Tooltip 
                contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px' }}
                labelStyle={{ color: '#e2e8f0' }}
              />
              <Line type="monotone" dataKey="latency" stroke="#f59e0b" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Region Health Table */}
      <div className="bg-slate-900/50 rounded-xl border border-slate-800 overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800">
          <h3 className="text-sm font-medium text-slate-200">Region Health</h3>
          <div className="flex items-center gap-2">
            {statusData.regions.length > 0 ? (
              <>
                <span className="text-xs text-slate-500">
                  {statusData.regions.every(r => r.status === 'ACTIVE') ? 'All regions operational' : 'Some regions degraded'}
                </span>
                {statusData.regions.every(r => r.status === 'ACTIVE') 
                  ? <CheckCircleSolid className="h-4 w-4 text-green-500" />
                  : <ExclamationSolid className="h-4 w-4 text-amber-500" />}
              </>
            ) : (
              <span className="text-xs text-slate-500">No regions configured</span>
            )}
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-slate-800">
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Region</th>
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Status</th>
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Traffic %</th>
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Instances</th>
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">CPU</th>
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">P99 Latency</th>
                <th className="px-5 py-3 text-right text-xs font-medium text-slate-400 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {statusData.regions.length > 0 ? (
                statusData.regions.map((region) => (
                  <tr key={region.region} className="hover:bg-slate-800/30 transition-colors">
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-2">
                        <GlobeAltIcon className="h-4 w-4 text-slate-500" />
                        <span className="text-sm font-medium text-slate-200">{region.region}</span>
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      <span className={clsx(
                        'badge',
                        region.status === 'ACTIVE' ? 'badge-success' : 'badge-warning'
                      )}>
                        {region.status}
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-2">
                        <div className="w-20 h-2 bg-slate-700 rounded-full overflow-hidden">
                          <div 
                            className="h-full bg-indigo-500 rounded-full"
                            style={{ width: `${region.trafficWeight}%` }}
                          />
                        </div>
                        <span className="text-sm text-slate-400">{region.trafficWeight}%</span>
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      <span className="text-sm text-slate-300">
                        <span className="text-green-400">{region.healthyInstances}</span>
                        <span className="text-slate-500">/{region.totalInstances}</span>
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      <span className={clsx(
                        'text-sm tabular-nums',
                        region.avgCpu > 80 ? 'text-amber-400' : 'text-slate-300'
                      )}>
                        {region.avgCpu.toFixed(1)}%
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      <span className="text-sm text-slate-300 tabular-nums">{region.latencyP99}ms</span>
                    </td>
                    <td className="px-5 py-4 text-right">
                      <button className="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
                        Details →
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} className="px-5 py-8 text-center">
                    <div className="flex flex-col items-center gap-2">
                      <GlobeAltIcon className="h-8 w-8 text-slate-600" />
                      <span className="text-sm text-slate-500">No regions configured</span>
                      <span className="text-xs text-slate-600">Deploy services to see region data</span>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* System Info / Recent Actions */}
      <div className="grid grid-cols-2 gap-4">
        {/* System Information */}
        <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-5">
          <div className="flex items-center gap-2 mb-4">
            <InformationCircleIcon className="h-5 w-5 text-blue-400" />
            <h3 className="text-sm font-medium text-slate-200">System Information</h3>
          </div>
          <div className="space-y-3">
            <div className="flex items-center justify-between p-3 bg-slate-800/50 rounded-lg border border-slate-700">
              <span className="text-sm text-slate-400">Total Services</span>
              <span className="text-sm font-medium text-white">{services.length}</span>
            </div>
            <div className="flex items-center justify-between p-3 bg-slate-800/50 rounded-lg border border-slate-700">
              <span className="text-sm text-slate-400">Active Regions</span>
              <span className="text-sm font-medium text-white">{statusData.capacity.regionCount}</span>
            </div>
            <div className="flex items-center justify-between p-3 bg-slate-800/50 rounded-lg border border-slate-700">
              <span className="text-sm text-slate-400">Control Loop Status</span>
              <span className={clsx(
                "text-sm font-medium",
                controlLoopStatus?.isLeader ? "text-green-400" : "text-amber-400"
              )}>
                {controlLoopStatus?.isLeader ? 'Leader' : 'Standby'}
              </span>
            </div>
          </div>
        </div>

        {/* Recent Actions from Audit Log */}
        <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-5">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-slate-200">Recent Actions</h3>
            <a href="/audit-logs" className="text-xs text-indigo-400 hover:text-indigo-300">View all</a>
          </div>
          <div className="space-y-2">
            {recentActions.length > 0 ? (
              recentActions.slice(0, 4).map((action) => (
                <ActionItem
                  key={action.id}
                  action={action.description}
                  time={formatTimeAgo(action.createdAt || new Date().toISOString())}
                  type={getActionType(action.category)}
                  automated={!action.isManualOverride}
                />
              ))
            ) : (
              <div className="text-center py-6 text-slate-500">
                <ClockIcon className="h-8 w-8 mx-auto mb-2 opacity-50" />
                <p className="text-sm">No recent actions</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Quick Actions Bar */}
      <div className="flex items-center justify-between p-4 bg-slate-900/50 rounded-xl border border-slate-800">
        <div className="flex items-center gap-4">
          <button className="override-btn">
            <PauseIcon className="h-4 w-4" />
            Pause Automation
          </button>
          <button className="rollback-btn">
            <ArrowUturnLeftIcon className="h-4 w-4" />
            Rollback Last Action
          </button>
        </div>
        <div className="flex items-center gap-2 text-xs text-slate-500">
          <ClockIcon className="h-4 w-4" />
          {recentActions.length > 0 
            ? `Last action: ${formatTimeAgo(recentActions[0]?.createdAt || new Date().toISOString())}`
            : 'No recent actions'}
        </div>
      </div>
    </div>
    </Layout>
  );
}

// Component: Status Card
function StatusCard({ 
  icon: Icon,
  label, 
  value,
  subValue,
  status,
  trend
}: { 
  icon: React.ComponentType<{ className?: string }>;
  label: string; 
  value: string;
  subValue?: string;
  status: 'success' | 'warning' | 'error';
  trend?: { direction: 'up' | 'down'; value: string };
}) {
  return (
    <div className={clsx(
      "bg-slate-900/50 rounded-xl border p-4 transition-all",
      status === 'success' && "border-slate-800 hover:border-green-500/30",
      status === 'warning' && "border-amber-500/30 glow-amber",
      status === 'error' && "border-red-500/30 glow-red"
    )}>
      <div className="flex items-start justify-between">
        <div className={clsx(
          "p-2 rounded-lg",
          status === 'success' && "bg-green-500/10",
          status === 'warning' && "bg-amber-500/10",
          status === 'error' && "bg-red-500/10"
        )}>
          <Icon className={clsx(
            "h-5 w-5",
            status === 'success' && "text-green-400",
            status === 'warning' && "text-amber-400",
            status === 'error' && "text-red-400"
          )} />
        </div>
        {status === 'success' && <CheckCircleSolid className="h-4 w-4 text-green-500" />}
        {status === 'warning' && <ExclamationSolid className="h-4 w-4 text-amber-500" />}
        {status === 'error' && <ExclamationSolid className="h-4 w-4 text-red-500" />}
      </div>
      <div className="mt-3">
        <span className="text-xs text-slate-500 uppercase tracking-wide">{label}</span>
        <div className="flex items-baseline gap-2 mt-1">
          <span className="text-xl font-semibold text-white">{value}</span>
          {trend && (
            <span className={clsx(
              "flex items-center text-xs",
              trend.direction === 'up' ? "text-green-400" : "text-slate-400"
            )}>
              {trend.direction === 'up' ? <ArrowTrendingUpIcon className="h-3 w-3 mr-0.5" /> : <ArrowTrendingDownIcon className="h-3 w-3 mr-0.5" />}
              {trend.value}
            </span>
          )}
        </div>
        {subValue && <span className="text-xs text-amber-400">{subValue}</span>}
      </div>
    </div>
  );
}

// Component: Metric Card
function MetricCard({ 
  label, 
  value,
  unit,
  status,
  trend,
  sparklineData
}: { 
  label: string; 
  value: string | number;
  unit: string;
  status: 'normal' | 'warning';
  trend?: { direction: 'up' | 'down'; value: string };
  sparklineData?: number[];
}) {
  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-4">
      <span className="text-xs text-slate-500 uppercase tracking-wide">{label}</span>
      <div className="flex items-baseline gap-1 mt-2">
        <span className={clsx(
          "metric-value",
          status === 'warning' ? "text-amber-400" : "text-white"
        )}>
          {value}
        </span>
        <span className="text-lg text-slate-500">{unit}</span>
      </div>
      {trend && (
        <div className={clsx(
          "flex items-center gap-1 mt-1 text-xs",
          trend.direction === 'up' ? "text-green-400" : "text-red-400"
        )}>
          {trend.direction === 'up' ? (
            <ArrowTrendingUpIcon className="h-3 w-3" />
          ) : (
            <ArrowTrendingDownIcon className="h-3 w-3" />
          )}
          {trend.value}
        </div>
      )}
      {sparklineData && (
        <div className="mt-3 h-10">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={sparklineData.map((v, i) => ({ value: v, i }))}>
              <defs>
                <linearGradient id={`spark-${label}`} x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                </linearGradient>
              </defs>
              <Area 
                type="monotone" 
                dataKey="value" 
                stroke="#6366f1" 
                strokeWidth={1.5}
                fill={`url(#spark-${label})`} 
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}

// Helper: Format time ago
function formatTimeAgo(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);
  
  if (seconds < 60) return 'just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

// Helper: Get action type from category
function getActionType(category: string): 'scaling' | 'traffic' | 'config' {
  if (category?.includes('SCALING') || category?.includes('CAPACITY')) return 'scaling';
  if (category?.includes('TRAFFIC') || category?.includes('DEPLOYMENT')) return 'traffic';
  return 'config';
}

// Component: Action Item
function ActionItem({ 
  action, 
  time, 
  type, 
  automated 
}: { 
  action: string; 
  time: string; 
  type: 'scaling' | 'traffic' | 'config';
  automated?: boolean;
}) {
  return (
    <div className="flex items-center gap-3 p-2 rounded-lg hover:bg-slate-800/30 transition-colors">
      <div className={clsx(
        "w-2 h-2 rounded-full",
        type === 'scaling' && "bg-indigo-500",
        type === 'traffic' && "bg-emerald-500",
        type === 'config' && "bg-slate-500"
      )} />
      <div className="flex-1 min-w-0">
        <span className="text-sm text-slate-300 truncate block">{action}</span>
        {automated && (
          <span className="text-xs text-purple-400">Auto</span>
        )}
      </div>
      <span className="text-xs text-slate-500 whitespace-nowrap">{time}</span>
    </div>
  );
}

