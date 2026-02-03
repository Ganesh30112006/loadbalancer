import { useNavigate } from 'react-router-dom';
import { 
  ServerStackIcon, 
  BeakerIcon,
  ArrowRightIcon,
  ChartBarIcon,
  CpuChipIcon,
  ShieldCheckIcon
} from '@heroicons/react/24/outline';
import { useLoadBalancingStore } from '../store/loadBalancingStore';

const modules = [
  {
    id: 'load-balancing',
    name: 'Load Balancing',
    description: 'Manage AWS Auto Scaling Groups, traffic distribution, and automated scaling policies',
    icon: ServerStackIcon,
    features: [
      'Multi-region load balancing',
      'Auto-scaling with SLO guardrails',
      'Blue/Green & Canary deployments',
      'Real-time metrics & alerts'
    ],
    status: 'active',
    color: 'indigo'
  },
  {
    id: 'load-testing',
    name: 'Load Testing',
    description: 'Performance testing, stress testing, and capacity planning tools',
    icon: BeakerIcon,
    features: [
      'Distributed load generation',
      'Performance benchmarking',
      'Capacity planning',
      'Regression detection'
    ],
    status: 'coming-soon',
    color: 'purple'
  }
];

export function ModuleSelectionPage() {
  const navigate = useNavigate();
  const { user, setSelectedModule } = useLoadBalancingStore();

  const handleModuleSelect = (moduleId: string) => {
    if (moduleId === 'load-balancing') {
      setSelectedModule(moduleId);
      navigate('/');
    }
    // load-testing is coming soon
  };

  return (
    <div className="min-h-screen bg-slate-900">
      {/* Header */}
      <header className="bg-slate-800 border-b border-slate-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-indigo-600 rounded-lg">
                <CpuChipIcon className="h-6 w-6 text-white" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-white">Cloud Control Plane</h1>
                <p className="text-sm text-slate-400">Select a module to continue</p>
              </div>
            </div>
            
            {user && (
              <div className="flex items-center gap-3">
                <div className="text-right">
                  <p className="text-sm font-medium text-white">{user.fullName}</p>
                  <p className="text-xs text-slate-400">
                    {user.role} • {user.canAdmin ? 'Full Access' : user.canOperate ? 'Operator' : 'Read Only'}
                  </p>
                </div>
                <div className="h-10 w-10 bg-indigo-600 rounded-full flex items-center justify-center">
                  <span className="text-sm font-medium text-white">
                    {user.firstName?.charAt(0)}{user.lastName?.charAt(0)}
                  </span>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold text-white">Choose Your Module</h2>
          <p className="mt-4 text-lg text-slate-400">
            Select the infrastructure management module you want to work with
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-4xl mx-auto">
          {modules.map((module) => {
            const Icon = module.icon;
            const isActive = module.status === 'active';
            
            return (
              <div
                key={module.id}
                onClick={() => isActive && handleModuleSelect(module.id)}
                className={`relative bg-slate-800 border rounded-xl p-6 transition-all duration-200 ${
                  isActive 
                    ? 'border-slate-700 hover:border-indigo-500 cursor-pointer hover:shadow-lg hover:shadow-indigo-500/10' 
                    : 'border-slate-700/50 opacity-60 cursor-not-allowed'
                }`}
              >
                {!isActive && (
                  <div className="absolute top-4 right-4">
                    <span className="px-2 py-1 bg-purple-900/50 text-purple-400 text-xs font-medium rounded-full">
                      Coming Soon
                    </span>
                  </div>
                )}

                <div className={`p-3 rounded-lg w-fit ${
                  module.color === 'indigo' ? 'bg-indigo-600' : 'bg-purple-600'
                }`}>
                  <Icon className="h-8 w-8 text-white" />
                </div>

                <h3 className="mt-4 text-xl font-semibold text-white">{module.name}</h3>
                <p className="mt-2 text-slate-400">{module.description}</p>

                <ul className="mt-6 space-y-3">
                  {module.features.map((feature, idx) => (
                    <li key={idx} className="flex items-center gap-2 text-sm text-slate-300">
                      <ShieldCheckIcon className="h-4 w-4 text-green-400 flex-shrink-0" />
                      {feature}
                    </li>
                  ))}
                </ul>

                {isActive && (
                  <div className="mt-6 flex items-center gap-2 text-indigo-400 font-medium">
                    <span>Enter Module</span>
                    <ArrowRightIcon className="h-4 w-4" />
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Quick Stats */}
        <div className="mt-16 grid grid-cols-1 md:grid-cols-3 gap-6 max-w-4xl mx-auto">
          <div className="bg-slate-800/50 border border-slate-700 rounded-lg p-4 text-center">
            <ChartBarIcon className="h-8 w-8 text-indigo-400 mx-auto" />
            <p className="mt-2 text-2xl font-bold text-white">99.99%</p>
            <p className="text-sm text-slate-400">Platform Uptime</p>
          </div>
          <div className="bg-slate-800/50 border border-slate-700 rounded-lg p-4 text-center">
            <ServerStackIcon className="h-8 w-8 text-green-400 mx-auto" />
            <p className="mt-2 text-2xl font-bold text-white">1M+</p>
            <p className="text-sm text-slate-400">Scaling Actions</p>
          </div>
          <div className="bg-slate-800/50 border border-slate-700 rounded-lg p-4 text-center">
            <ShieldCheckIcon className="h-8 w-8 text-amber-400 mx-auto" />
            <p className="mt-2 text-2xl font-bold text-white">Zero</p>
            <p className="text-sm text-slate-400">Hidden Automations</p>
          </div>
        </div>
      </main>
    </div>
  );
}
