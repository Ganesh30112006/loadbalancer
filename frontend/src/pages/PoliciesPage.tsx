import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import type { Policy, CreatePolicyRequest } from '../types';
import {
  PlusIcon,
  PencilIcon,
  TrashIcon,
  ShieldCheckIcon,
  XMarkIcon,
  ArrowPathIcon,
  CpuChipIcon,
  ClockIcon,
  ExclamationTriangleIcon,
  CurrencyDollarIcon,
  ChartBarIcon,
  BoltIcon,
  MagnifyingGlassIcon,
  AdjustmentsHorizontalIcon
} from '@heroicons/react/24/outline';
import toast from 'react-hot-toast';
import clsx from 'clsx';

export default function PoliciesPage() {
  const queryClient = useQueryClient();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingPolicy, setEditingPolicy] = useState<Policy | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  const { data: policies, isLoading } = useQuery({
    queryKey: ['policies'],
    queryFn: api.getPolicies,
    refetchInterval: 10000,
  });

  const deleteMutation = useMutation({
    mutationFn: api.deletePolicy,
    onSuccess: () => {
      toast.success('Policy deleted');
      queryClient.invalidateQueries({ queryKey: ['policies'] });
    },
    onError: () => {
      toast.error('Failed to delete policy');
    },
  });

  const filteredPolicies = policies?.filter((policy) =>
    policy.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const stats = {
    total: policies?.length || 0,
    active: policies?.filter(p => p.status === 'ACTIVE').length || 0,
    deprecated: policies?.filter(p => p.status === 'DEPRECATED').length || 0,
    draft: policies?.filter(p => p.status === 'DRAFT').length || 0,
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white flex items-center gap-3">
            <ShieldCheckIcon className="h-7 w-7 text-indigo-400" />
            Policies
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            SLO targets, scaling rules, and cost guardrails
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >
          <PlusIcon className="h-4 w-4" />
          Create Policy
        </button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-4 gap-4">
        <StatCard label="Total Policies" value={stats.total} icon={ShieldCheckIcon} />
        <StatCard label="Active" value={stats.active} icon={ShieldCheckIcon} status="success" />
        <StatCard label="Deprecated" value={stats.deprecated} icon={ClockIcon} status="warning" />
        <StatCard label="Draft" value={stats.draft} icon={BoltIcon} />
      </div>

      {/* Search */}
      <div className="relative max-w-md">
        <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
        <input
          type="text"
          placeholder="Search policies..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 bg-slate-900/50 border border-slate-800 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
        />
      </div>

      {/* Policies Grid */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <ArrowPathIcon className="h-8 w-8 text-slate-400 animate-spin" />
        </div>
      ) : filteredPolicies && filteredPolicies.length > 0 ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {filteredPolicies.map((policy) => (
            <PolicyCard
              key={policy.id}
              policy={policy}
              onEdit={() => setEditingPolicy(policy)}
              onDelete={() => {
                if (confirm('Are you sure you want to delete this policy?')) {
                  deleteMutation.mutate(policy.id);
                }
              }}
            />
          ))}
        </div>
      ) : (
        <div className="text-center py-12 bg-slate-900/50 rounded-xl border border-slate-800">
          <ShieldCheckIcon className="h-12 w-12 text-slate-700 mx-auto mb-3" />
          <p className="text-slate-500">No policies found</p>
          <p className="text-xs text-slate-600 mt-1">Create your first policy to define SLO targets</p>
        </div>
      )}

      {(showCreateModal || editingPolicy) && (
        <PolicyModal
          policy={editingPolicy}
          onClose={() => {
            setShowCreateModal(false);
            setEditingPolicy(null);
          }}
        />
      )}
    </div>
  );
}

function StatCard({
  label,
  value,
  icon: Icon,
  status,
}: {
  label: string;
  value: number;
  icon: React.ComponentType<{ className?: string }>;
  status?: 'success' | 'warning' | 'error';
}) {
  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-4">
      <div className="flex items-center gap-3">
        <div className={clsx(
          "p-2 rounded-lg",
          status === 'success' && "bg-green-500/10",
          status === 'warning' && "bg-amber-500/10",
          status === 'error' && "bg-red-500/10",
          !status && "bg-slate-700"
        )}>
          <Icon className={clsx(
            "h-5 w-5",
            status === 'success' && "text-green-400",
            status === 'warning' && "text-amber-400",
            status === 'error' && "text-red-400",
            !status && "text-slate-400"
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

function PolicyCard({
  policy,
  onEdit,
  onDelete,
}: {
  policy: Policy;
  onEdit: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-5 hover:border-slate-700 transition-colors">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-start gap-3">
          <div className="p-2 bg-indigo-500/10 rounded-lg">
            <ShieldCheckIcon className="h-5 w-5 text-indigo-400" />
          </div>
          <div>
            <h3 className="font-medium text-white">{policy.name}</h3>
            <span className={clsx(
              'badge mt-1',
              policy.status === 'ACTIVE' && 'badge-success',
              policy.status === 'DEPRECATED' && 'badge-warning',
              policy.status === 'DRAFT' && 'badge-neutral'
            )}>
              {policy.status}
            </span>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <button
            onClick={onEdit}
            className="p-2 text-slate-400 hover:text-indigo-400 hover:bg-slate-800 rounded-lg transition-colors"
          >
            <PencilIcon className="h-4 w-4" />
          </button>
          <button
            onClick={onDelete}
            className="p-2 text-slate-400 hover:text-red-400 hover:bg-slate-800 rounded-lg transition-colors"
          >
            <TrashIcon className="h-4 w-4" />
          </button>
        </div>
      </div>

      {policy.description && (
        <p className="text-sm text-slate-400 mb-4">{policy.description}</p>
      )}

      {/* SLO Config */}
      {policy.sloConfig && (
        <div className="mb-4">
          <h4 className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-2 flex items-center gap-1.5">
            <ChartBarIcon className="h-3.5 w-3.5" />
            SLO Configuration
          </h4>
          <div className="grid grid-cols-3 gap-2">
            {policy.sloConfig.latencyP99TargetMs && (
              <div className="bg-slate-800/50 p-2.5 rounded-lg border border-slate-700">
                <div className="flex items-center gap-1.5 text-xs text-slate-500 mb-1">
                  <ClockIcon className="h-3 w-3" />
                  Latency P99
                </div>
                <div className="text-sm font-medium text-white">
                  &lt;{policy.sloConfig.latencyP99TargetMs}ms
                </div>
              </div>
            )}
            {policy.sloConfig.maxErrorRate && (
              <div className="bg-slate-800/50 p-2.5 rounded-lg border border-slate-700">
                <div className="flex items-center gap-1.5 text-xs text-slate-500 mb-1">
                  <ExclamationTriangleIcon className="h-3 w-3" />
                  Error Rate
                </div>
                <div className="text-sm font-medium text-white">
                  &lt;{(policy.sloConfig.maxErrorRate * 100).toFixed(2)}%
                </div>
              </div>
            )}
            {policy.sloConfig.targetAvailability && (
              <div className="bg-slate-800/50 p-2.5 rounded-lg border border-slate-700">
                <div className="flex items-center gap-1.5 text-xs text-slate-500 mb-1">
                  <CpuChipIcon className="h-3 w-3" />
                  Availability
                </div>
                <div className="text-sm font-medium text-white">
                  {(policy.sloConfig.targetAvailability * 100).toFixed(2)}%
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Scaling Rules Summary */}
      {policy.scalingRules && (
        <div className="flex items-center justify-between py-3 border-t border-slate-800">
          <div className="flex items-center gap-2 text-sm text-slate-400">
            <AdjustmentsHorizontalIcon className="h-4 w-4" />
            Scaling
          </div>
          <span className="text-sm text-slate-300">
            {policy.scalingRules.minInstances || 1} - {policy.scalingRules.maxInstances || 100} instances
          </span>
        </div>
      )}

      {/* Cost Config */}
      {policy.costConfig && policy.costConfig.monthlyBudgetLimit && (
        <div className="flex items-center justify-between py-3 border-t border-slate-800">
          <div className="flex items-center gap-2 text-sm text-slate-400">
            <CurrencyDollarIcon className="h-4 w-4" />
            Monthly Budget
          </div>
          <span className="text-sm text-slate-300">
            ${policy.costConfig.monthlyBudgetLimit.toLocaleString()}
          </span>
        </div>
      )}

      {/* Deployment Config */}
      {policy.deploymentConfig && (
        <div className="flex items-center justify-between py-3 border-t border-slate-800">
          <div className="flex items-center gap-2 text-sm text-slate-400">
            <BoltIcon className="h-4 w-4" />
            Deployment Strategy
          </div>
          <span className="badge badge-info">
            {policy.deploymentConfig.strategy || 'ROLLING'}
          </span>
        </div>
      )}
    </div>
  );
}

function PolicyModal({
  policy,
  onClose,
}: {
  policy: Policy | null;
  onClose: () => void;
}) {
  const queryClient = useQueryClient();
  const isEditing = !!policy;

  const { data: accounts } = useQuery({
    queryKey: ['accounts'],
    queryFn: api.getAccounts,
  });

  const [formData, setFormData] = useState<CreatePolicyRequest>({
    name: policy?.name || '',
    description: policy?.description || '',
    awsAccountId: policy?.awsAccountId || '',
    sloConfig: policy?.sloConfig || {
      targetAvailability: 0.999,
      latencyP99TargetMs: 200,
      latencyP95TargetMs: 100,
      maxErrorRate: 0.01,
      minHealthyInstances: 2,
    },
    scalingRules: policy?.scalingRules || {
      minInstances: 2,
      maxInstances: 50,
      cpuScaleOutThreshold: 70,
      cpuScaleInThreshold: 30,
      scaleOutCooldownSeconds: 300,
      scaleInCooldownSeconds: 300,
      maxScaleOutStep: 5,
      maxScaleInStep: 2,
      predictiveScalingEnabled: false,
    },
    costConfig: policy?.costConfig || {
      monthlyBudgetLimit: 10000,
      dailySpendLimit: 500,
      costAlertThreshold: 80,
      spotInstancePercentage: 20,
      reservedInstanceCoverage: 50,
      enableRightsizing: true,
    },
    deploymentConfig: policy?.deploymentConfig || {
      strategy: 'ROLLING',
      canaryPercentage: 10,
      canaryDurationMinutes: 15,
      bakeTimeMinutes: 10,
      rollbackOnAlarm: true,
      autoRollbackThreshold: 5,
      requireApproval: false,
      approvalTimeoutMinutes: 60,
    },
  });

  const createMutation = useMutation({
    mutationFn: (request: CreatePolicyRequest) => api.createPolicy(request),
    onSuccess: () => {
      toast.success('Policy created successfully');
      queryClient.invalidateQueries({ queryKey: ['policies'] });
      onClose();
    },
    onError: () => {
      toast.error('Failed to create policy');
    },
  });

  const updateMutation = useMutation({
    mutationFn: (request: CreatePolicyRequest) => api.updatePolicy(policy!.id, request),
    onSuccess: () => {
      toast.success('Policy updated successfully');
      queryClient.invalidateQueries({ queryKey: ['policies'] });
      onClose();
    },
    onError: () => {
      toast.error('Failed to update policy');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (isEditing) {
      updateMutation.mutate(formData);
    } else {
      createMutation.mutate(formData);
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 sticky top-0 bg-slate-900 z-10">
          <h2 className="text-lg font-medium text-white flex items-center gap-2">
            <ShieldCheckIcon className="h-5 w-5 text-indigo-400" />
            {isEditing ? 'Edit Policy' : 'Create Policy'}
          </h2>
          <button onClick={onClose} className="p-1 text-slate-400 hover:text-white transition-colors">
            <XMarkIcon className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* AWS Account */}
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">AWS Account</label>
            <select
              value={formData.awsAccountId}
              onChange={(e) => setFormData({ ...formData, awsAccountId: e.target.value })}
              className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
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

          {/* Name */}
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">Policy Name</label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
              placeholder="e.g., Production SLO Policy"
              required
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">Description</label>
            <textarea
              value={formData.description || ''}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
              placeholder="Describe this policy..."
              rows={2}
            />
          </div>

          {/* SLO Config */}
          <div>
            <h3 className="text-sm font-medium text-white mb-3 flex items-center gap-2">
              <ChartBarIcon className="h-4 w-4 text-indigo-400" />
              SLO Configuration
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs text-slate-400 mb-1.5">Target Availability</label>
                <input
                  type="number"
                  step="0.001"
                  value={formData.sloConfig?.targetAvailability || 0.999}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      sloConfig: {
                        ...formData.sloConfig,
                        targetAvailability: Number(e.target.value),
                      },
                    })
                  }
                  className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-xs text-slate-400 mb-1.5">Latency P99 Target (ms)</label>
                <input
                  type="number"
                  value={formData.sloConfig?.latencyP99TargetMs || 200}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      sloConfig: {
                        ...formData.sloConfig,
                        latencyP99TargetMs: Number(e.target.value),
                      },
                    })
                  }
                  className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-xs text-slate-400 mb-1.5">Max Error Rate</label>
                <input
                  type="number"
                  step="0.001"
                  value={formData.sloConfig?.maxErrorRate || 0.01}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      sloConfig: {
                        ...formData.sloConfig,
                        maxErrorRate: Number(e.target.value),
                      },
                    })
                  }
                  className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-xs text-slate-400 mb-1.5">Min Healthy Instances</label>
                <input
                  type="number"
                  value={formData.sloConfig?.minHealthyInstances || 2}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      sloConfig: {
                        ...formData.sloConfig,
                        minHealthyInstances: Number(e.target.value),
                      },
                    })
                  }
                  className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>
            </div>
          </div>

          {/* Scaling Rules */}
          <div>
            <h3 className="text-sm font-medium text-white mb-3 flex items-center gap-2">
              <BoltIcon className="h-4 w-4 text-indigo-400" />
              Scaling Configuration
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs text-slate-400 mb-1.5">Min Instances</label>
                <input
                  type="number"
                  value={formData.scalingRules?.minInstances || 2}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      scalingRules: {
                        ...formData.scalingRules,
                        minInstances: Number(e.target.value),
                      },
                    })
                  }
                  className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-xs text-slate-400 mb-1.5">Max Instances</label>
                <input
                  type="number"
                  value={formData.scalingRules?.maxInstances || 50}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      scalingRules: {
                        ...formData.scalingRules,
                        maxInstances: Number(e.target.value),
                      },
                    })
                  }
                  className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-xs text-slate-400 mb-1.5">CPU Scale Out Threshold (%)</label>
                <input
                  type="number"
                  value={formData.scalingRules?.cpuScaleOutThreshold || 70}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      scalingRules: {
                        ...formData.scalingRules,
                        cpuScaleOutThreshold: Number(e.target.value),
                      },
                    })
                  }
                  className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-xs text-slate-400 mb-1.5">CPU Scale In Threshold (%)</label>
                <input
                  type="number"
                  value={formData.scalingRules?.cpuScaleInThreshold || 30}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      scalingRules: {
                        ...formData.scalingRules,
                        cpuScaleInThreshold: Number(e.target.value),
                      },
                    })
                  }
                  className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>
            </div>
          </div>

          {/* Cost Config */}
          <div>
            <h3 className="text-sm font-medium text-white mb-3 flex items-center gap-2">
              <CurrencyDollarIcon className="h-4 w-4 text-indigo-400" />
              Cost Configuration
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs text-slate-400 mb-1.5">Monthly Budget ($)</label>
                <input
                  type="number"
                  value={formData.costConfig?.monthlyBudgetLimit || 10000}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      costConfig: {
                        ...formData.costConfig,
                        monthlyBudgetLimit: Number(e.target.value),
                      },
                    })
                  }
                  className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-xs text-slate-400 mb-1.5">Daily Spend Limit ($)</label>
                <input
                  type="number"
                  value={formData.costConfig?.dailySpendLimit || 500}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      costConfig: {
                        ...formData.costConfig,
                        dailySpendLimit: Number(e.target.value),
                      },
                    })
                  }
                  className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>
            </div>
          </div>

          {/* Deployment Strategy */}
          <div>
            <h3 className="text-sm font-medium text-white mb-3 flex items-center gap-2">
              <AdjustmentsHorizontalIcon className="h-4 w-4 text-indigo-400" />
              Deployment Strategy
            </h3>
            <select
              value={formData.deploymentConfig?.strategy || 'ROLLING'}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  deploymentConfig: {
                    ...formData.deploymentConfig,
                    strategy: e.target.value as 'ROLLING' | 'BLUE_GREEN' | 'CANARY',
                  },
                })
              }
              className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
            >
              <option value="ROLLING">Rolling</option>
              <option value="BLUE_GREEN">Blue/Green</option>
              <option value="CANARY">Canary</option>
            </select>
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
              disabled={isPending}
              className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors"
            >
              {isPending ? 'Saving...' : isEditing ? 'Update Policy' : 'Create Policy'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
