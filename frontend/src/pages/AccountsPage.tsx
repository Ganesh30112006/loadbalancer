import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Layout } from '../components/Layout';
import { api } from '../services/api';
import type { AwsAccount, CreateAccountRequest } from '../types';
import { 
  PlusIcon, 
  CheckCircleIcon, 
  XCircleIcon, 
  ClockIcon,
  CloudIcon,
  ArrowPathIcon,
  MagnifyingGlassIcon,
  ShieldCheckIcon,
  GlobeAltIcon,
  XMarkIcon
} from '@heroicons/react/24/outline';
import { CheckCircleIcon as CheckCircleSolid, XCircleIcon as XCircleSolid, ClockIcon as ClockSolid } from '@heroicons/react/24/solid';
import toast from 'react-hot-toast';
import clsx from 'clsx';

export default function AccountsPage() {
  const queryClient = useQueryClient();
  const [showOnboardModal, setShowOnboardModal] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const { data: accounts, isLoading } = useQuery({
    queryKey: ['accounts'],
    queryFn: api.getAccounts,
    refetchInterval: 10000,
  });

  const validateMutation = useMutation({
    mutationFn: (accountId: string) => api.validateAccount(accountId),
    onSuccess: (result) => {
      if (result.valid) {
        toast.success('Account validated successfully');
      } else {
        toast.error(`Validation failed: ${result.errorMessage}`);
      }
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
    },
    onError: () => {
      toast.error('Failed to validate account');
    },
  });

  const filteredAccounts = accounts?.filter(account => 
    account.accountName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    account.accountId.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Stats
  const stats = {
    total: accounts?.length || 0,
    active: accounts?.filter(a => a.status === 'ACTIVE').length || 0,
    pending: accounts?.filter(a => a.status === 'PENDING_VALIDATION').length || 0,
    failed: accounts?.filter(a => a.status === 'VALIDATION_FAILED').length || 0,
  };

  return (
    <Layout>
    <div className="space-y-6 animate-fade-in">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white flex items-center gap-3">
            <CloudIcon className="h-7 w-7 text-indigo-400" />
            AWS Accounts
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            Manage connected AWS accounts for load balancing control
          </p>
        </div>
        <button
          onClick={() => setShowOnboardModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-500 transition-colors"
        >
          <PlusIcon className="h-4 w-4" />
          Onboard Account
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-4 gap-4">
        <StatCard label="Total Accounts" value={stats.total} icon={CloudIcon} />
        <StatCard label="Active" value={stats.active} color="success" icon={CheckCircleIcon} />
        <StatCard label="Pending" value={stats.pending} color="warning" icon={ClockIcon} />
        <StatCard label="Failed" value={stats.failed} color="error" icon={XCircleIcon} />
      </div>

      {/* Search */}
      <div className="relative max-w-md">
        <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
        <input
          type="text"
          placeholder="Search accounts..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full pl-9 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-sm placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <ArrowPathIcon className="h-8 w-8 text-slate-400 animate-spin" />
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-4">
          {filteredAccounts?.map((account) => (
            <AccountCard 
              key={account.id} 
              account={account}
              onValidate={() => validateMutation.mutate(account.id)}
              isValidating={validateMutation.isPending}
            />
          ))}
        </div>
      )}

      {showOnboardModal && (
        <OnboardAccountModal onClose={() => setShowOnboardModal(false)} />
      )}
    </div>
    </Layout>
  );
}

function StatCard({ 
  label, 
  value, 
  color = 'default',
  icon: Icon
}: { 
  label: string; 
  value: number; 
  color?: 'success' | 'warning' | 'error' | 'default';
  icon: React.ComponentType<{ className?: string }>;
}) {
  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-4">
      <div className="flex items-center justify-between">
        <Icon className={clsx(
          "h-5 w-5",
          color === 'success' && "text-green-400",
          color === 'warning' && "text-amber-400",
          color === 'error' && "text-red-400",
          color === 'default' && "text-slate-400"
        )} />
        <span className={clsx(
          "text-2xl font-semibold",
          color === 'success' && "text-green-400",
          color === 'warning' && "text-amber-400",
          color === 'error' && "text-red-400",
          color === 'default' && "text-white"
        )}>
          {value}
        </span>
      </div>
      <span className="text-xs text-slate-500 mt-2 block">{label}</span>
    </div>
  );
}

function AccountCard({ 
  account, 
  onValidate, 
  isValidating 
}: { 
  account: AwsAccount; 
  onValidate: () => void;
  isValidating: boolean;
}) {
  const StatusIcon = account.status === 'ACTIVE' ? CheckCircleSolid : 
    account.status === 'VALIDATION_FAILED' ? XCircleSolid : ClockSolid;
  
  return (
    <div className="bg-slate-900/50 rounded-xl border border-slate-800 p-5 card-hover">
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className={clsx(
            "p-2.5 rounded-lg",
            account.status === 'ACTIVE' ? "bg-green-500/10" : 
            account.status === 'VALIDATION_FAILED' ? "bg-red-500/10" : "bg-amber-500/10"
          )}>
            <CloudIcon className={clsx(
              "h-5 w-5",
              account.status === 'ACTIVE' ? "text-green-400" : 
              account.status === 'VALIDATION_FAILED' ? "text-red-400" : "text-amber-400"
            )} />
          </div>
          <div>
            <h3 className="text-sm font-medium text-slate-200">{account.accountName}</h3>
            <p className="text-xs text-slate-500 font-mono">{account.accountId}</p>
          </div>
        </div>
        <span className={clsx(
          'badge',
          account.status === 'ACTIVE' && 'badge-success',
          account.status === 'VALIDATION_FAILED' && 'badge-error',
          account.status === 'PENDING_VALIDATION' && 'badge-warning'
        )}>
          <StatusIcon className="h-3 w-3 mr-1" />
          {account.status.replace('_', ' ')}
        </span>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-4">
        <div>
          <span className="text-xs text-slate-500">Regions</span>
          <div className="flex items-center gap-1 mt-1">
            <GlobeAltIcon className="h-4 w-4 text-slate-500" />
            <span className="text-sm text-slate-300">{account.enabledRegions?.length || 0}</span>
          </div>
        </div>
        <div>
          <span className="text-xs text-slate-500">Last Validated</span>
          <p className="text-sm text-slate-300 mt-1">
            {account.lastValidatedAt
              ? new Date(account.lastValidatedAt).toLocaleDateString()
              : 'Never'}
          </p>
        </div>
      </div>

      <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-1 text-xs text-slate-500">
          <ShieldCheckIcon className="h-3 w-3" />
          <span className="truncate max-w-[180px]" title={account.roleArn}>
            {account.roleArn?.split('/').pop() || 'No role configured'}
          </span>
        </div>
        <button
          onClick={(e) => { e.stopPropagation(); onValidate(); }}
          disabled={isValidating}
          className="text-sm text-indigo-400 hover:text-indigo-300 transition-colors disabled:opacity-50"
        >
          {isValidating ? 'Validating...' : 'Validate →'}
        </button>
      </div>
    </div>
  );
}

function OnboardAccountModal({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient();
  const [formData, setFormData] = useState<CreateAccountRequest>({
    accountId: '',
    accountName: '',
    roleArn: '',
    enabledRegions: ['us-east-1'],
  });
  const [onboardingResult, setOnboardingResult] = useState<{
    externalId: string;
    trustPolicy: string;
    instructions: string;
  } | null>(null);

  const onboardMutation = useMutation({
    mutationFn: (request: CreateAccountRequest) => api.onboardAccount(request),
    onSuccess: (result) => {
      setOnboardingResult({
        externalId: result.externalId,
        trustPolicy: result.trustPolicyTemplate,
        instructions: result.iamRoleSetupInstructions,
      });
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      toast.success('Account onboarded! Configure IAM role with the External ID.');
    },
    onError: () => {
      toast.error('Failed to onboard account');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onboardMutation.mutate(formData);
  };

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 animate-fade-in">
      <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto animate-slide-up">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800">
          <h2 className="text-lg font-semibold text-white">
            Onboard AWS Account
          </h2>
          <button 
            onClick={onClose}
            className="p-1 rounded-lg hover:bg-slate-800 text-slate-400 transition-colors"
          >
            <XMarkIcon className="h-5 w-5" />
          </button>
        </div>

        {!onboardingResult ? (
          <form onSubmit={handleSubmit} className="p-6 space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">
                AWS Account ID
              </label>
              <input
                type="text"
                value={formData.accountId}
                onChange={(e) => setFormData({ ...formData, accountId: e.target.value })}
                placeholder="123456789012"
                className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">
                Account Name
              </label>
              <input
                type="text"
                value={formData.accountName}
                onChange={(e) => setFormData({ ...formData, accountName: e.target.value })}
                placeholder="Production Account"
                className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">
                IAM Role ARN
              </label>
              <input
                type="text"
                value={formData.roleArn}
                onChange={(e) => setFormData({ ...formData, roleArn: e.target.value })}
                placeholder="arn:aws:iam::123456789012:role/LoadBalancingRole"
                className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
              />
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
                disabled={onboardMutation.isPending}
                className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-500 disabled:opacity-50 transition-colors"
              >
                {onboardMutation.isPending ? 'Onboarding...' : 'Onboard Account'}
              </button>
            </div>
          </form>
        ) : (
          <div className="p-6 space-y-4">
            <div className="bg-green-500/10 border border-green-500/20 rounded-lg p-4">
              <h3 className="font-medium text-green-400 mb-2">
                External ID Generated
              </h3>
              <code className="text-sm bg-slate-800 text-green-300 px-2 py-1 rounded font-mono">
                {onboardingResult.externalId}
              </code>
            </div>

            <div>
              <h3 className="font-medium text-white mb-2">
                Trust Policy Template
              </h3>
              <pre className="bg-slate-800 p-4 rounded-lg text-sm overflow-x-auto text-slate-300 font-mono">
                {onboardingResult.trustPolicy}
              </pre>
            </div>

            <div>
              <h3 className="font-medium text-white mb-2">
                Setup Instructions
              </h3>
              <pre className="bg-slate-800 p-4 rounded-lg text-sm whitespace-pre-wrap text-slate-300">
                {onboardingResult.instructions}
              </pre>
            </div>

            <div className="flex justify-end pt-4 border-t border-slate-800">
              <button
                onClick={onClose}
                className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-500 transition-colors"
              >
                Done
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
