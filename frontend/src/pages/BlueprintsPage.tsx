import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import type { Blueprint, CreateBlueprintRequest } from '../types';
import {
  PlusIcon,
  CheckCircleIcon,
  ClockIcon,
  DocumentDuplicateIcon,
  CubeIcon,
  ArrowPathIcon,
  MagnifyingGlassIcon,
  XMarkIcon,
  ServerIcon,
  CloudIcon
} from '@heroicons/react/24/outline';
import { CheckCircleIcon as CheckCircleSolid } from '@heroicons/react/24/solid';
import toast from 'react-hot-toast';
import clsx from 'clsx';

export default function BlueprintsPage() {
  const queryClient = useQueryClient();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');

  const { data: blueprints, isLoading } = useQuery({
    queryKey: ['blueprints'],
    queryFn: api.getBlueprints,
    refetchInterval: 10000,
  });

  const approveMutation = useMutation({
    mutationFn: (id: string) => api.approveBlueprint(id),
    onSuccess: () => {
      toast.success('Blueprint status updated');
      queryClient.invalidateQueries({ queryKey: ['blueprints'] });
    },
    onError: () => {
      toast.error('Failed to update blueprint');
    },
  });

  const filteredBlueprints = blueprints?.filter((bp) => {
    const matchesSearch = bp.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      bp.amiId.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'all' || bp.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const stats = {
    total: blueprints?.length || 0,
    active: blueprints?.filter(bp => bp.status === 'ACTIVE').length || 0,
    deprecated: blueprints?.filter(bp => bp.status === 'DEPRECATED').length || 0,
    draft: blueprints?.filter(bp => bp.status === 'DRAFT').length || 0,
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white flex items-center gap-3">
            <CubeIcon className="h-7 w-7 text-indigo-400" />
            Application Blueprints
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            Pre-approved AMI configurations and launch templates
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >
          <PlusIcon className="h-4 w-4" />
          Create Blueprint
        </button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-4 gap-4">
        <StatCard label="Total Blueprints" value={stats.total} icon={CubeIcon} />
        <StatCard label="Active" value={stats.active} icon={CheckCircleIcon} status="success" />
        <StatCard label="Deprecated" value={stats.deprecated} icon={ClockIcon} status="warning" />
        <StatCard label="Draft" value={stats.draft} icon={DocumentDuplicateIcon} status="neutral" />
      </div>

      {/* Search and Filters */}
      <div className="flex items-center gap-4">
        <div className="flex-1 relative">
          <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
          <input
            type="text"
            placeholder="Search blueprints by name or AMI ID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 bg-slate-900/50 border border-slate-800 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
          />
        </div>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-4 py-2.5 bg-slate-900/50 border border-slate-800 rounded-lg text-slate-200 focus:outline-none focus:border-indigo-500"
        >
          <option value="all">All Status</option>
          <option value="ACTIVE">Active</option>
          <option value="DEPRECATED">Deprecated</option>
          <option value="DRAFT">Draft</option>
        </select>
      </div>

      {/* Blueprints Grid */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <ArrowPathIcon className="h-8 w-8 text-slate-400 animate-spin" />
        </div>
      ) : filteredBlueprints && filteredBlueprints.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredBlueprints.map((blueprint) => (
            <BlueprintCard
              key={blueprint.id}
              blueprint={blueprint}
              onApprove={() => approveMutation.mutate(blueprint.id)}
            />
          ))}
        </div>
      ) : (
        <div className="text-center py-12 bg-slate-900/50 rounded-xl border border-slate-800">
          <CubeIcon className="h-12 w-12 text-slate-700 mx-auto mb-3" />
          <p className="text-slate-500">No blueprints found</p>
          <p className="text-xs text-slate-600 mt-1">Create your first blueprint to get started</p>
        </div>
      )}

      {showCreateModal && (
        <CreateBlueprintModal onClose={() => setShowCreateModal(false)} />
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
  status?: 'success' | 'warning' | 'error' | 'neutral';
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

function BlueprintCard({
  blueprint,
  onApprove,
}: {
  blueprint: Blueprint;
  onApprove: () => void;
}) {
  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-5 hover:border-slate-700 transition-colors">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-start gap-3">
          <div className="p-2 bg-indigo-500/10 rounded-lg">
            <CubeIcon className="h-5 w-5 text-indigo-400" />
          </div>
          <div>
            <h3 className="font-medium text-white">{blueprint.name}</h3>
            <p className="text-xs text-slate-500">Version {blueprint.version}</p>
          </div>
        </div>
        <span className={clsx(
          'badge',
          blueprint.status === 'ACTIVE' && 'badge-success',
          blueprint.status === 'DEPRECATED' && 'badge-warning',
          blueprint.status === 'DRAFT' && 'badge-neutral'
        )}>
          {blueprint.status}
        </span>
      </div>

      {blueprint.description && (
        <p className="text-sm text-slate-400 mb-4 line-clamp-2">
          {blueprint.description}
        </p>
      )}

      <div className="space-y-2 text-sm">
        <div className="flex items-center justify-between py-2 border-t border-slate-800">
          <span className="text-slate-500 flex items-center gap-1.5">
            <CloudIcon className="h-3.5 w-3.5" />
            AMI ID
          </span>
          <span className="text-slate-300 font-mono text-xs">{blueprint.amiId}</span>
        </div>
        <div className="flex items-center justify-between py-2 border-t border-slate-800">
          <span className="text-slate-500 flex items-center gap-1.5">
            <ServerIcon className="h-3.5 w-3.5" />
            Instance Type
          </span>
          <span className="text-slate-300">{blueprint.instanceType}</span>
        </div>
        {blueprint.iamInstanceProfile && (
          <div className="flex items-center justify-between py-2 border-t border-slate-800">
            <span className="text-slate-500">IAM Profile</span>
            <span className="text-slate-300 font-mono text-xs">{blueprint.iamInstanceProfile}</span>
          </div>
        )}
      </div>

      {blueprint.status === 'DRAFT' && (
        <div className="mt-4 flex gap-2 pt-4 border-t border-slate-800">
          <button
            onClick={() => onApprove()}
            className="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 bg-green-600/20 text-green-400 text-sm rounded-lg border border-green-600/30 hover:bg-green-600/30 transition-colors"
          >
            <CheckCircleSolid className="h-4 w-4" />
            Activate
          </button>
        </div>
      )}
    </div>
  );
}

function CreateBlueprintModal({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient();
  
  const { data: accounts } = useQuery({
    queryKey: ['accounts'],
    queryFn: api.getAccounts,
  });

  const [formData, setFormData] = useState<CreateBlueprintRequest>({
    name: '',
    description: '',
    awsAccountId: '',
    amiId: '',
    instanceType: 't3.medium',
    securityGroupIds: [],
    userData: '',
  });

  const createMutation = useMutation({
    mutationFn: (request: CreateBlueprintRequest) => api.createBlueprint(request),
    onSuccess: () => {
      toast.success('Blueprint created successfully');
      queryClient.invalidateQueries({ queryKey: ['blueprints'] });
      onClose();
    },
    onError: () => {
      toast.error('Failed to create blueprint');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate(formData);
  };

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-xl w-full max-w-lg">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800">
          <h2 className="text-lg font-medium text-white flex items-center gap-2">
            <CubeIcon className="h-5 w-5 text-indigo-400" />
            Create Blueprint
          </h2>
          <button onClick={onClose} className="p-1 text-slate-400 hover:text-white transition-colors">
            <XMarkIcon className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
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

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">Name</label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
              placeholder="e.g., Web Server Blueprint"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">Description</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
              placeholder="Describe this blueprint's purpose..."
              rows={3}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">AMI ID</label>
            <input
              type="text"
              value={formData.amiId}
              onChange={(e) => setFormData({ ...formData, amiId: e.target.value })}
              placeholder="ami-0123456789abcdef0"
              className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 font-mono text-sm focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">Instance Type</label>
            <select
              value={formData.instanceType}
              onChange={(e) => setFormData({ ...formData, instanceType: e.target.value })}
              className="w-full px-3 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
            >
              <option value="t3.micro">t3.micro</option>
              <option value="t3.small">t3.small</option>
              <option value="t3.medium">t3.medium</option>
              <option value="t3.large">t3.large</option>
              <option value="m5.large">m5.large</option>
              <option value="m5.xlarge">m5.xlarge</option>
              <option value="c5.large">c5.large</option>
              <option value="c5.xlarge">c5.xlarge</option>
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
              disabled={createMutation.isPending}
              className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors"
            >
              {createMutation.isPending ? 'Creating...' : 'Create Blueprint'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
