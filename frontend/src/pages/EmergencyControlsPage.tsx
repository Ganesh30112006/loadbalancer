import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ExclamationTriangleIcon,
  StopIcon,
  PlayIcon,
  ShieldExclamationIcon,
  ClockIcon,
  UserIcon,
  ServerStackIcon,
  ArrowPathIcon
} from '@heroicons/react/24/outline';
import { EmergencyOverride, Service } from '../types';
import { api } from '../services/api';
import { useLoadBalancingStore } from '../store/loadBalancingStore';
import toast from 'react-hot-toast';

export function EmergencyControlsPage() {
  const queryClient = useQueryClient();
  const { user } = useLoadBalancingStore();
  const [showPauseModal, setShowPauseModal] = useState(false);
  const [showCapacityModal, setShowCapacityModal] = useState(false);
  const [selectedScope, setSelectedScope] = useState<'service' | 'account' | 'global'>('service');
  const [selectedService, setSelectedService] = useState('');
  const [pauseReason, setPauseReason] = useState('');
  const [pauseDuration, setPauseDuration] = useState(60);
  const [capacityMin, setCapacityMin] = useState(10);
  const [capacityMax, setCapacityMax] = useState(50);
  const [confirming, setConfirming] = useState(false);

  // Fetch real services from API
  const { data: services = [] } = useQuery<Service[]>({
    queryKey: ['services'],
    queryFn: api.getServices,
  });

  // Fetch active overrides from API
  const { data: activeOverrides = [], isLoading: loadingOverrides } = useQuery<EmergencyOverride[]>({
    queryKey: ['activeOverrides'],
    queryFn: api.getActiveOverrides,
    refetchInterval: 10000,
  });

  const pauseMutation = useMutation({
    mutationFn: async () => {
      if (selectedScope === 'global') {
        return api.pauseGlobalAutomation(pauseReason, user?.email || 'unknown');
      } else {
        return api.pauseServiceAutomation(selectedService, pauseReason, user?.email || 'unknown');
      }
    },
    onSuccess: () => {
      toast.success('Automation paused successfully');
      queryClient.invalidateQueries({ queryKey: ['activeOverrides'] });
      setShowPauseModal(false);
      setConfirming(false);
      setPauseReason('');
    },
    onError: () => {
      toast.error('Failed to pause automation');
    },
  });

  const capacityMutation = useMutation({
    mutationFn: async () => {
      return api.setCapacityOverride(selectedService, capacityMax, `Capacity locked: ${capacityMin}-${capacityMax}`, user?.email || 'unknown');
    },
    onSuccess: () => {
      toast.success('Capacity override applied');
      queryClient.invalidateQueries({ queryKey: ['activeOverrides'] });
      setShowCapacityModal(false);
      setConfirming(false);
    },
    onError: () => {
      toast.error('Failed to apply capacity override');
    },
  });

  const liftMutation = useMutation({
    mutationFn: async (overrideId: string) => {
      return api.liftOverride(overrideId, 'Manual lift', user?.email || 'unknown');
    },
    onSuccess: () => {
      toast.success('Override lifted');
      queryClient.invalidateQueries({ queryKey: ['activeOverrides'] });
    },
    onError: () => {
      toast.error('Failed to lift override');
    },
  });

  const handlePauseAutomation = async () => {
    pauseMutation.mutate();
  };

  const handleCapacityOverride = async () => {
    capacityMutation.mutate();
  };

  const handleLiftOverride = async (overrideId: string) => {
    liftMutation.mutate(overrideId);
  };

  const getTimeRemaining = (expiresAt?: string) => {
    if (!expiresAt) return 'No expiration';
    const remaining = new Date(expiresAt).getTime() - Date.now();
    if (remaining <= 0) return 'Expired';
    const minutes = Math.floor(remaining / 60000);
    const hours = Math.floor(minutes / 60);
    if (hours > 0) return `${hours}h ${minutes % 60}m remaining`;
    return `${minutes}m remaining`;
  };

  const loading = pauseMutation.isPending || capacityMutation.isPending;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white">Emergency Controls</h1>
            <p className="mt-1 text-slate-400">
              Instantly pause automation or lock capacity during incidents
            </p>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <button
            onClick={() => setShowPauseModal(true)}
            className="bg-red-900/30 border border-red-700 rounded-xl p-6 hover:bg-red-900/50 transition-colors text-left group"
          >
            <div className="flex items-center gap-3">
              <div className="p-3 bg-red-600 rounded-lg group-hover:scale-110 transition-transform">
                <StopIcon className="h-6 w-6 text-white" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Pause Automation</h3>
                <p className="text-sm text-red-400">Stop all scaling actions</p>
              </div>
            </div>
          </button>

          <button
            onClick={() => setShowCapacityModal(true)}
            className="bg-amber-900/30 border border-amber-700 rounded-xl p-6 hover:bg-amber-900/50 transition-colors text-left group"
          >
            <div className="flex items-center gap-3">
              <div className="p-3 bg-amber-600 rounded-lg group-hover:scale-110 transition-transform">
                <ShieldExclamationIcon className="h-6 w-6 text-white" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Lock Capacity</h3>
                <p className="text-sm text-amber-400">Fix min/max instances</p>
              </div>
            </div>
          </button>

          <button
            onClick={() => {/* Navigate to audit log */}}
            className="bg-slate-800 border border-slate-700 rounded-xl p-6 hover:bg-slate-700 transition-colors text-left group"
          >
            <div className="flex items-center gap-3">
              <div className="p-3 bg-slate-600 rounded-lg group-hover:scale-110 transition-transform">
                <ClockIcon className="h-6 w-6 text-white" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">View Audit Log</h3>
                <p className="text-sm text-slate-400">See all emergency actions</p>
              </div>
            </div>
          </button>
        </div>

        {/* Active Overrides */}
        <div className="bg-slate-800 border border-slate-700 rounded-xl p-6">
          <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <ExclamationTriangleIcon className="h-5 w-5 text-amber-400" />
            Active Overrides
            {activeOverrides.length > 0 && (
              <span className="ml-2 px-2 py-0.5 bg-red-600 text-white text-xs rounded-full">
                {activeOverrides.length}
              </span>
            )}
          </h2>

          {loadingOverrides ? (
            <div className="flex items-center justify-center py-8">
              <ArrowPathIcon className="h-8 w-8 text-indigo-400 animate-spin" />
            </div>
          ) : activeOverrides.length === 0 ? (
            <div className="text-center py-8">
              <ShieldExclamationIcon className="h-12 w-12 text-slate-600 mx-auto" />
              <p className="mt-2 text-slate-400">No active emergency overrides</p>
              <p className="text-sm text-slate-500">All automation is running normally</p>
            </div>
          ) : (
            <div className="space-y-4">
              {activeOverrides.map((override) => (
                <div
                  key={override.id}
                  className={`border rounded-lg p-4 ${
                    override.type === 'PAUSE_AUTOMATION'
                      ? 'bg-red-900/20 border-red-700'
                      : 'bg-amber-900/20 border-amber-700'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-start gap-3">
                      <div className={`p-2 rounded-lg ${
                        override.type === 'PAUSE_AUTOMATION' ? 'bg-red-600' : 'bg-amber-600'
                      }`}>
                        {override.type === 'PAUSE_AUTOMATION' ? (
                          <StopIcon className="h-5 w-5 text-white" />
                        ) : (
                          <ShieldExclamationIcon className="h-5 w-5 text-white" />
                        )}
                      </div>
                      <div>
                        <h3 className="text-white font-medium">
                          {override.type === 'PAUSE_AUTOMATION' ? 'Automation Paused' : 'Capacity Locked'}
                        </h3>
                        <p className="text-sm text-slate-400 flex items-center gap-2 mt-1">
                          <ServerStackIcon className="h-4 w-4" />
                          {override.targetName || 'Global'}
                        </p>
                        <p className="text-sm text-slate-400 mt-1">{override.reason}</p>
                        
                        {override.minCapacity !== undefined && (
                          <p className="text-sm text-amber-400 mt-2">
                            Locked at: {override.minCapacity} - {override.maxCapacity} instances
                          </p>
                        )}

                        <div className="flex items-center gap-4 mt-3 text-xs text-slate-500">
                          <span className="flex items-center gap-1">
                            <UserIcon className="h-3 w-3" />
                            {override.createdBy}
                          </span>
                          <span className="flex items-center gap-1">
                            <ClockIcon className="h-3 w-3" />
                            {getTimeRemaining(override.expiresAt)}
                          </span>
                        </div>
                      </div>
                    </div>

                    <button
                      onClick={() => handleLiftOverride(override.id)}
                      className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
                    >
                      <PlayIcon className="h-4 w-4" />
                      Resume
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Pause Automation Modal */}
        {showPauseModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70">
            <div className="bg-slate-800 border border-slate-700 rounded-xl w-full max-w-lg">
              <div className="p-6 border-b border-slate-700">
                <h2 className="text-xl font-bold text-white flex items-center gap-2">
                  <StopIcon className="h-6 w-6 text-red-400" />
                  Pause Automation
                </h2>
              </div>

              <div className="p-6 space-y-4">
                {!confirming ? (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">Scope</label>
                      <div className="flex gap-2">
                        {(['service', 'account', 'global'] as const).map((scope) => (
                          <button
                            key={scope}
                            onClick={() => setSelectedScope(scope)}
                            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                              selectedScope === scope
                                ? 'bg-indigo-600 text-white'
                                : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                            }`}
                          >
                            {scope.charAt(0).toUpperCase() + scope.slice(1)}
                          </button>
                        ))}
                      </div>
                    </div>

                    {selectedScope === 'service' && (
                      <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">Service</label>
                        <select
                          value={selectedService}
                          onChange={(e) => setSelectedService(e.target.value)}
                          className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white"
                        >
                          <option value="">Select a service...</option>
                          {services.map((service) => (
                            <option key={service.id} value={service.id}>{service.serviceName}</option>
                          ))}
                        </select>
                      </div>
                    )}

                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">Reason</label>
                      <textarea
                        value={pauseReason}
                        onChange={(e) => setPauseReason(e.target.value)}
                        placeholder="Why are you pausing automation?"
                        rows={3}
                        className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">
                        Duration (minutes)
                      </label>
                      <input
                        type="number"
                        value={pauseDuration}
                        onChange={(e) => setPauseDuration(Number(e.target.value))}
                        min={5}
                        max={1440}
                        className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white"
                      />
                    </div>
                  </>
                ) : (
                  <div className="bg-red-900/30 border border-red-700 rounded-lg p-4">
                    <div className="flex items-center gap-2 text-red-400 font-medium mb-2">
                      <ExclamationTriangleIcon className="h-5 w-5" />
                      Confirm Pause
                    </div>
                    <p className="text-slate-300 text-sm">
                      You are about to pause automation for{' '}
                      <strong>
                        {selectedScope === 'global'
                          ? 'ALL SERVICES'
                          : services.find(s => s.id === selectedService)?.serviceName}
                      </strong>{' '}
                      for <strong>{pauseDuration} minutes</strong>.
                    </p>
                    <p className="text-slate-400 text-sm mt-2">
                      No automatic scaling will occur during this period. Manual actions will still be available.
                    </p>
                  </div>
                )}
              </div>

              <div className="p-6 border-t border-slate-700 flex justify-end gap-3">
                <button
                  onClick={() => {
                    setShowPauseModal(false);
                    setConfirming(false);
                  }}
                  className="px-4 py-2 text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                {!confirming ? (
                  <button
                    onClick={() => setConfirming(true)}
                    disabled={!pauseReason || (selectedScope === 'service' && !selectedService)}
                    className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
                  >
                    Continue
                  </button>
                ) : (
                  <button
                    onClick={handlePauseAutomation}
                    disabled={loading}
                    className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 flex items-center gap-2"
                  >
                    {loading ? (
                      <>
                        <ArrowPathIcon className="h-4 w-4 animate-spin" />
                        Pausing...
                      </>
                    ) : (
                      <>
                        <StopIcon className="h-4 w-4" />
                        Confirm Pause
                      </>
                    )}
                  </button>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Capacity Override Modal */}
        {showCapacityModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70">
            <div className="bg-slate-800 border border-slate-700 rounded-xl w-full max-w-lg">
              <div className="p-6 border-b border-slate-700">
                <h2 className="text-xl font-bold text-white flex items-center gap-2">
                  <ShieldExclamationIcon className="h-6 w-6 text-amber-400" />
                  Lock Capacity
                </h2>
              </div>

              <div className="p-6 space-y-4">
                {!confirming ? (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">Service</label>
                      <select
                        value={selectedService}
                        onChange={(e) => setSelectedService(e.target.value)}
                        className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white"
                      >
                        <option value="">Select a service...</option>
                        {services.map((service) => (
                          <option key={service.id} value={service.id}>{service.serviceName}</option>
                        ))}
                      </select>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                          Min Capacity
                        </label>
                        <input
                          type="number"
                          value={capacityMin}
                          onChange={(e) => setCapacityMin(Number(e.target.value))}
                          min={0}
                          className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                          Max Capacity
                        </label>
                        <input
                          type="number"
                          value={capacityMax}
                          onChange={(e) => setCapacityMax(Number(e.target.value))}
                          min={capacityMin}
                          className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white"
                        />
                      </div>
                    </div>

                    <div className="bg-slate-700/50 rounded-lg p-4">
                      <p className="text-sm text-slate-400">
                        This will lock the Auto Scaling Group to maintain exactly {capacityMin}-{capacityMax} instances,
                        overriding any automatic scaling rules.
                      </p>
                    </div>
                  </>
                ) : (
                  <div className="bg-amber-900/30 border border-amber-700 rounded-lg p-4">
                    <div className="flex items-center gap-2 text-amber-400 font-medium mb-2">
                      <ExclamationTriangleIcon className="h-5 w-5" />
                      Confirm Capacity Lock
                    </div>
                    <p className="text-slate-300 text-sm">
                      The service <strong>{services.find(s => s.id === selectedService)?.serviceName}</strong> will be locked 
                      to <strong>{capacityMin}-{capacityMax} instances</strong>.
                    </p>
                    <p className="text-slate-400 text-sm mt-2">
                      Automatic scaling will be disabled until you remove this override.
                    </p>
                  </div>
                )}
              </div>

              <div className="p-6 border-t border-slate-700 flex justify-end gap-3">
                <button
                  onClick={() => {
                    setShowCapacityModal(false);
                    setConfirming(false);
                  }}
                  className="px-4 py-2 text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                {!confirming ? (
                  <button
                    onClick={() => setConfirming(true)}
                    disabled={!selectedService}
                    className="px-4 py-2 bg-amber-600 text-white rounded-lg hover:bg-amber-700 disabled:opacity-50"
                  >
                    Continue
                  </button>
                ) : (
                  <button
                    onClick={handleCapacityOverride}
                    disabled={loading}
                    className="px-4 py-2 bg-amber-600 text-white rounded-lg hover:bg-amber-700 disabled:opacity-50 flex items-center gap-2"
                  >
                    {loading ? (
                      <>
                        <ArrowPathIcon className="h-4 w-4 animate-spin" />
                        Applying...
                      </>
                    ) : (
                      <>
                        <ShieldExclamationIcon className="h-4 w-4" />
                        Confirm Lock
                      </>
                    )}
                  </button>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
  );
}
