import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Layout } from '../components/Layout';
import { Link } from 'react-router-dom';
import { api } from '../services/api';
import type { Service, CreateServiceRequest } from '../types';
import { 
  PlusIcon, 
  PauseIcon,
  ServerStackIcon,
  MagnifyingGlassIcon,
  FunnelIcon,
  GlobeAltIcon,
  ChevronRightIcon,
  ArrowPathIcon,
  CpuChipIcon,
  XMarkIcon
} from '@heroicons/react/24/outline';
import { CheckCircleIcon as CheckCircleSolid, ExclamationTriangleIcon as ExclamationSolid } from '@heroicons/react/24/solid';
import toast from 'react-hot-toast';
import clsx from 'clsx';

export default function ServicesPage() {
  const queryClient = useQueryClient();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [viewMode, setViewMode] = useState<'grid' | 'table'>('grid');

  const { data: services, isLoading } = useQuery({
    queryKey: ['services'],
    queryFn: api.getServices,
    refetchInterval: 10000,
  });

  const toggleAutomationMutation = useMutation({
    mutationFn: ({ serviceId, enabled }: { serviceId: string; enabled: boolean }) =>
      api.setAutomation(serviceId, enabled),
    onSuccess: () => {
      toast.success('Automation setting updated');
      queryClient.invalidateQueries({ queryKey: ['services'] });
    },
    onError: () => {
      toast.error('Failed to update automation');
    },
  });

  const filteredServices = services?.filter(service => {
    const matchesSearch = service.serviceName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      service.displayName?.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'all' || service.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  // Service statistics derived from actual data
  const stats = {
    total: services?.length || 0,
    active: services?.filter(s => s.status === 'ACTIVE').length || 0,
    degraded: services?.filter(s => s.status === 'DEGRADED').length || 0,
    suspended: services?.filter(s => s.status === 'SUSPENDED').length || 0,
  };

  return (
    <Layout>
    <div className="space-y-6 animate-fade-in">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white flex items-center gap-3">
            <ServerStackIcon className="h-7 w-7 text-indigo-400" />
            Services
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            Manage and monitor all registered services
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-500 transition-colors"
        >
          <PlusIcon className="h-4 w-4" />
          Register Service
        </button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-4 gap-4">
        <StatCard label="Total Services" value={stats.total} />
        <StatCard label="Active" value={stats.active} color="success" />
        <StatCard label="Degraded" value={stats.degraded} color="warning" />
        <StatCard label="Suspended" value={stats.suspended} color="neutral" />
      </div>

      {/* Filters */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="relative">
            <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
            <input
              type="text"
              placeholder="Search services..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-64 pl-9 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-sm placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
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
              <option value="ACTIVE">Active</option>
              <option value="DEGRADED">Degraded</option>
              <option value="SUSPENDED">Suspended</option>
              <option value="FAILED">Failed</option>
              <option value="PROVISIONING">Provisioning</option>
            </select>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setViewMode('grid')}
            className={clsx(
              "p-2 rounded-lg transition-colors",
              viewMode === 'grid' ? "bg-slate-700 text-white" : "text-slate-400 hover:bg-slate-800"
            )}
          >
            <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 16 16">
              <rect x="1" y="1" width="6" height="6" rx="1" />
              <rect x="9" y="1" width="6" height="6" rx="1" />
              <rect x="1" y="9" width="6" height="6" rx="1" />
              <rect x="9" y="9" width="6" height="6" rx="1" />
            </svg>
          </button>
          <button
            onClick={() => setViewMode('table')}
            className={clsx(
              "p-2 rounded-lg transition-colors",
              viewMode === 'table' ? "bg-slate-700 text-white" : "text-slate-400 hover:bg-slate-800"
            )}
          >
            <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 16 16">
              <rect x="1" y="2" width="14" height="2" rx="0.5" />
              <rect x="1" y="7" width="14" height="2" rx="0.5" />
              <rect x="1" y="12" width="14" height="2" rx="0.5" />
            </svg>
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <ArrowPathIcon className="h-8 w-8 text-slate-400 animate-spin" />
        </div>
      ) : viewMode === 'grid' ? (
        <div className="grid grid-cols-3 gap-4">
          {filteredServices?.map((service) => (
            <ServiceCard 
              key={service.id} 
              service={service}
              onToggleAutomation={(enabled) => 
                toggleAutomationMutation.mutate({ serviceId: service.id, enabled })
              }
            />
          ))}
        </div>
      ) : (
        <div className="bg-slate-900/50 rounded-xl border border-slate-800 overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-slate-800">
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Service</th>
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Status</th>
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Health</th>
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Regions</th>
                <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Automation</th>
                <th className="px-5 py-3 text-right text-xs font-medium text-slate-400 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {filteredServices?.map((service) => (
                <tr key={service.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="px-5 py-4">
                    <Link to={`/services/${service.id}`} className="flex items-center gap-3 group">
                      <div className="p-2 rounded-lg bg-slate-800">
                        <ServerStackIcon className="h-5 w-5 text-indigo-400" />
                      </div>
                      <div>
                        <span className="text-sm font-medium text-slate-200 group-hover:text-indigo-400 transition-colors">
                          {service.displayName || service.serviceName}
                        </span>
                        <p className="text-xs text-slate-500">{service.serviceName}</p>
                      </div>
                    </Link>
                  </td>
                  <td className="px-5 py-4">
                    <span className={clsx(
                      'badge',
                      service.status === 'ACTIVE' && 'badge-success',
                      service.status === 'DEGRADED' && 'badge-warning',
                      service.status === 'SUSPENDED' && 'badge-neutral',
                      service.status === 'FAILED' && 'badge-error',
                      service.status === 'PROVISIONING' && 'badge-info'
                    )}>
                      {service.status}
                    </span>
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-2">
                      {service.status === 'ACTIVE' ? (
                        <CheckCircleSolid className="h-4 w-4 text-green-500" />
                      ) : (
                        <ExclamationSolid className="h-4 w-4 text-amber-500" />
                      )}
                      <span className="text-sm text-slate-300">{service.status === 'ACTIVE' ? 'Healthy' : 'Degraded'}</span>
                    </div>
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-1">
                      <GlobeAltIcon className="h-4 w-4 text-slate-500" />
                      <span className="text-sm text-slate-400">{service.regions?.length || 0}</span>
                    </div>
                  </td>
                  <td className="px-5 py-4">
                    <button
                      onClick={() =>
                        toggleAutomationMutation.mutate({
                          serviceId: service.id,
                          enabled: !service.automationEnabled,
                        })
                      }
                      className={clsx(
                        'flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium rounded-lg transition-colors',
                        service.automationEnabled
                          ? 'bg-green-500/10 text-green-400 border border-green-500/20'
                          : 'bg-slate-700 text-slate-400 border border-slate-600'
                      )}
                    >
                      {service.automationEnabled ? (
                        <>
                          <CpuChipIcon className="h-3 w-3" />
                          On
                        </>
                      ) : (
                        <>
                          <PauseIcon className="h-3 w-3" />
                          Off
                        </>
                      )}
                    </button>
                  </td>
                  <td className="px-5 py-4 text-right">
                    <Link
                      to={`/services/${service.id}`}
                      className="text-sm text-indigo-400 hover:text-indigo-300 transition-colors"
                    >
                      View Details →
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreateModal && (
        <CreateServiceModal onClose={() => setShowCreateModal(false)} />
      )}
    </div>
    </Layout>
  );
}

function StatCard({ label, value, color = 'default' }: { label: string; value: number; color?: 'success' | 'warning' | 'neutral' | 'default' }) {
  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-4">
      <span className="text-xs text-slate-500 uppercase tracking-wide">{label}</span>
      <span className={clsx(
        "block text-2xl font-semibold mt-1",
        color === 'success' && "text-green-400",
        color === 'warning' && "text-amber-400",
        color === 'neutral' && "text-slate-400",
        color === 'default' && "text-white"
      )}>
        {value}
      </span>
    </div>
  );
}

function ServiceCard({ 
  service, 
  onToggleAutomation: _onToggleAutomation
}: { 
  service: Service;
  onToggleAutomation: (enabled: boolean) => void;
}) {
  return (
    <Link
      to={`/services/${service.id}`}
      className="bg-slate-900/50 rounded-xl border border-slate-800 p-5 card-hover group"
    >
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className={clsx(
            "p-2.5 rounded-lg",
            service.status === 'ACTIVE' ? "bg-green-500/10" : 
            service.status === 'DEGRADED' ? "bg-amber-500/10" : "bg-slate-800"
          )}>
            <ServerStackIcon className={clsx(
              "h-5 w-5",
              service.status === 'ACTIVE' ? "text-green-400" : 
              service.status === 'DEGRADED' ? "text-amber-400" : "text-slate-400"
            )} />
          </div>
          <div>
            <h3 className="text-sm font-medium text-slate-200 group-hover:text-indigo-400 transition-colors">
              {service.displayName || service.serviceName}
            </h3>
            <p className="text-xs text-slate-500">{service.serviceName}</p>
          </div>
        </div>
        <span className={clsx(
          'badge',
          service.status === 'ACTIVE' && 'badge-success',
          service.status === 'DEGRADED' && 'badge-warning',
          service.status === 'SUSPENDED' && 'badge-neutral',
          service.status === 'FAILED' && 'badge-error',
          service.status === 'PROVISIONING' && 'badge-info'
        )}>
          {service.status}
        </span>
      </div>

      <div className="mt-4 grid grid-cols-3 gap-4">
        <div>
          <span className="text-xs text-slate-500">Health</span>
          <div className="flex items-center gap-1 mt-1">
            {service.status === 'ACTIVE' ? (
              <CheckCircleSolid className="h-4 w-4 text-green-500" />
            ) : (
              <ExclamationSolid className="h-4 w-4 text-amber-500" />
            )}
            <span className="text-sm text-slate-300">{service.status === 'ACTIVE' ? 'Healthy' : 'Degraded'}</span>
          </div>
        </div>
        <div>
          <span className="text-xs text-slate-500">Regions</span>
          <p className="text-sm text-slate-300 mt-1">{service.regions?.length || 0}</p>
        </div>
        <div>
          <span className="text-xs text-slate-500">Auto</span>
          <div className="mt-1">
            <span className={clsx(
              "text-sm",
              service.automationEnabled ? "text-green-400" : "text-slate-400"
            )}>
              {service.automationEnabled ? 'On' : 'Off'}
            </span>
          </div>
        </div>
      </div>

      <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          {service.regions?.slice(0, 3).map((region, i) => (
            <span key={i} className="text-xs text-slate-500 bg-slate-800 px-2 py-0.5 rounded">
              {typeof region === 'string' ? region : region.region}
            </span>
          ))}
          {(service.regions?.length || 0) > 3 && (
            <span className="text-xs text-slate-500">+{(service.regions?.length || 0) - 3}</span>
          )}
        </div>
        <ChevronRightIcon className="h-4 w-4 text-slate-500 group-hover:text-indigo-400 transition-colors" />
      </div>
    </Link>
  );
}

function CreateServiceModal({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient();
  
  const { data: accounts } = useQuery({
    queryKey: ['accounts'],
    queryFn: api.getAccounts,
  });

  const { data: blueprints } = useQuery({
    queryKey: ['blueprints'],
    queryFn: api.getBlueprints,
  });

  const { data: policies } = useQuery({
    queryKey: ['policies'],
    queryFn: api.getPolicies,
  });

  const [formData, setFormData] = useState<CreateServiceRequest>({
    awsAccountId: '',
    serviceName: '',
    displayName: '',
    blueprintId: '',
    policyId: '',
    primaryRegion: 'us-east-1',
    regions: ['us-east-1'],
  });

  const createMutation = useMutation({
    mutationFn: (request: CreateServiceRequest) => api.createService(request),
    onSuccess: () => {
      toast.success('Service registered successfully');
      queryClient.invalidateQueries({ queryKey: ['services'] });
      onClose();
    },
    onError: () => {
      toast.error('Failed to register service');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate(formData);
  };

  const availableRegions = [
    'us-east-1',
    'us-east-2',
    'us-west-1',
    'us-west-2',
    'eu-west-1',
    'eu-west-2',
    'eu-central-1',
    'ap-southeast-1',
    'ap-southeast-2',
    'ap-northeast-1',
  ];

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 animate-fade-in">
      <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-2xl w-full max-w-lg animate-slide-up">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800">
          <h2 className="text-lg font-semibold text-white">
            Register Service
          </h2>
          <button 
            onClick={onClose}
            className="p-1 rounded-lg hover:bg-slate-800 text-slate-400 transition-colors"
          >
            <XMarkIcon className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">
              AWS Account
            </label>
            <select
              value={formData.awsAccountId}
              onChange={(e) => setFormData({ ...formData, awsAccountId: e.target.value })}
              className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
            >
              <option value="">Select an account...</option>
              {accounts
                ?.filter((a) => a.status === 'ACTIVE')
                .map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.accountName} ({account.accountId})
                  </option>
                ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">
              Service Name (identifier)
            </label>
            <input
              type="text"
              value={formData.serviceName}
              onChange={(e) => setFormData({ ...formData, serviceName: e.target.value })}
              placeholder="my-api-service"
              className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">
              Display Name
            </label>
            <input
              type="text"
              value={formData.displayName}
              onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
              placeholder="My API Service"
              className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">
              Blueprint
            </label>
            <select
              value={formData.blueprintId}
              onChange={(e) => setFormData({ ...formData, blueprintId: e.target.value })}
              className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
            >
              <option value="">Select a blueprint...</option>
              {blueprints
                ?.filter((b) => b.status === 'ACTIVE')
                .map((blueprint) => (
                  <option key={blueprint.id} value={blueprint.id}>
                    {blueprint.name} (v{blueprint.version})
                  </option>
                ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">
              Policy
            </label>
            <select
              value={formData.policyId}
              onChange={(e) => setFormData({ ...formData, policyId: e.target.value })}
              className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
            >
              <option value="">Select a policy...</option>
              {policies
                ?.filter((p) => p.status === 'ACTIVE')
                .map((policy) => (
                  <option key={policy.id} value={policy.id}>
                    {policy.name}
                  </option>
                ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">
              Regions
            </label>
            <div className="grid grid-cols-2 gap-2 max-h-40 overflow-y-auto p-3 bg-slate-800/50 border border-slate-700 rounded-lg">
              {availableRegions.map((region) => (
                <label key={region} className="flex items-center gap-2 cursor-pointer hover:text-slate-200">
                  <input
                    type="checkbox"
                    checked={formData.regions.includes(region)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setFormData({
                          ...formData,
                          regions: [...formData.regions, region],
                        });
                      } else {
                        setFormData({
                          ...formData,
                          regions: formData.regions.filter((r) => r !== region),
                        });
                      }
                    }}
                    className="w-4 h-4 rounded border-slate-600 bg-slate-700 text-indigo-600 focus:ring-indigo-500 focus:ring-offset-slate-900"
                  />
                  <span className="text-sm text-slate-400">{region}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-slate-300 hover:bg-slate-800 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-500 disabled:opacity-50 transition-colors"
            >
              {createMutation.isPending ? 'Registering...' : 'Register Service'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
