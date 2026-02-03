import { useState, useEffect, ReactNode } from 'react';
import { Outlet, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { 
  HomeIcon, 
  CloudIcon, 
  CubeIcon, 
  ShieldCheckIcon, 
  ServerStackIcon,
  CpuChipIcon,
  RocketLaunchIcon,
  ClipboardDocumentListIcon,
  ChevronDownIcon,
  BellIcon,
  MagnifyingGlassIcon,
  SignalIcon,
  ArrowPathIcon,
  ExclamationTriangleIcon as ExclamationTriangleOutline,
  ArrowRightOnRectangleIcon,
  UserGroupIcon
} from '@heroicons/react/24/outline';
import { CheckCircleIcon, ExclamationTriangleIcon } from '@heroicons/react/24/solid';
import clsx from 'clsx';
import { useLoadBalancingStore } from '../store/loadBalancingStore';

const getNavigation = (isAdmin: boolean) => [
  { name: 'Dashboard', href: '/dashboard', icon: HomeIcon },
  { name: 'AWS Accounts', href: '/accounts', icon: CloudIcon },
  { name: 'Services', href: '/services', icon: ServerStackIcon },
  { name: 'Blueprints', href: '/blueprints', icon: CubeIcon },
  { name: 'Policies', href: '/policies', icon: ShieldCheckIcon },
  { name: 'Deployments', href: '/deployments', icon: RocketLaunchIcon },
  { name: 'Control Loop', href: '/control-loop', icon: CpuChipIcon },
  { name: 'Emergency', href: '/emergency', icon: ExclamationTriangleOutline, danger: true },
  { name: 'Audit Logs', href: '/audit-logs', icon: ClipboardDocumentListIcon },
  ...(isAdmin ? [{ name: 'User Management', href: '/users', icon: UserGroupIcon }] : []),
];

const environments = ['Production', 'Staging', 'Development'];
const regions = ['us-east-1', 'us-west-2', 'eu-west-1', 'ap-southeast-1'];

interface LayoutProps {
  children?: ReactNode;
}

export function Layout({ children }: LayoutProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const { accounts, selectedAccount, selectAccount, fetchAccounts, user, logout } = useLoadBalancingStore();
  const [selectedEnv, setSelectedEnv] = useState('Production');
  const [selectedRegion, setSelectedRegion] = useState('us-east-1');
  const [lastSync, setLastSync] = useState(new Date());
  const [showEnvDropdown, setShowEnvDropdown] = useState(false);
  const [showAccountDropdown, setShowAccountDropdown] = useState(false);
  const [showRegionDropdown, setShowRegionDropdown] = useState(false);

  useEffect(() => {
    fetchAccounts();
  }, [fetchAccounts]);

  useEffect(() => {
    const interval = setInterval(() => setLastSync(new Date()), 1000);
    return () => clearInterval(interval);
  }, []);

  const formatLastSync = () => {
    const seconds = Math.floor((new Date().getTime() - lastSync.getTime()) / 1000);
    if (seconds < 5) return 'Just now';
    if (seconds < 60) return `${seconds}s ago`;
    const minutes = Math.floor(seconds / 60);
    return `${minutes}m ago`;
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      {/* Top Navigation Bar */}
      <header className="fixed top-0 left-0 right-0 h-14 bg-slate-900 border-b border-slate-800 z-50">
        <div className="flex items-center justify-between h-full px-4">
          {/* Left side - Logo and selectors */}
          <div className="flex items-center gap-6">
            {/* Logo */}
            <div className="flex items-center gap-2 pr-6 border-r border-slate-700">
              <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600">
                <CpuChipIcon className="h-5 w-5 text-white" />
              </div>
              <span className="text-lg font-semibold text-white">LoadBalancer</span>
            </div>

            {/* Environment Selector */}
            <div className="relative">
              <button
                onClick={() => setShowEnvDropdown(!showEnvDropdown)}
                className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-sm font-medium transition-colors"
              >
                <span className={clsx(
                  "w-2 h-2 rounded-full",
                  selectedEnv === 'Production' ? "bg-green-500" : 
                  selectedEnv === 'Staging' ? "bg-amber-500" : "bg-blue-500"
                )} />
                {selectedEnv}
                <ChevronDownIcon className="h-4 w-4 text-slate-400" />
              </button>
              {showEnvDropdown && (
                <div className="absolute top-full left-0 mt-1 w-40 bg-slate-800 border border-slate-700 rounded-lg shadow-xl py-1 animate-fade-in">
                  {environments.map((env) => (
                    <button
                      key={env}
                      onClick={() => { setSelectedEnv(env); setShowEnvDropdown(false); }}
                      className={clsx(
                        "w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-slate-700 transition-colors",
                        selectedEnv === env ? "text-indigo-400" : "text-slate-300"
                      )}
                    >
                      <span className={clsx(
                        "w-2 h-2 rounded-full",
                        env === 'Production' ? "bg-green-500" : 
                        env === 'Staging' ? "bg-amber-500" : "bg-blue-500"
                      )} />
                      {env}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Account Selector */}
            <div className="relative">
              <button
                onClick={() => setShowAccountDropdown(!showAccountDropdown)}
                className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-sm font-medium transition-colors"
              >
                <CloudIcon className="h-4 w-4 text-slate-400" />
                {selectedAccount?.accountName || 'Select Account'}
                <ChevronDownIcon className="h-4 w-4 text-slate-400" />
              </button>
              {showAccountDropdown && (
                <div className="absolute top-full left-0 mt-1 w-56 bg-slate-800 border border-slate-700 rounded-lg shadow-xl py-1 animate-fade-in max-h-64 overflow-y-auto">
                  {accounts.map((account) => (
                    <button
                      key={account.id}
                      onClick={() => { selectAccount(account.id); setShowAccountDropdown(false); }}
                      className={clsx(
                        "w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-slate-700 transition-colors",
                        selectedAccount?.id === account.id ? "text-indigo-400 bg-slate-700/50" : "text-slate-300"
                      )}
                    >
                      <div className="flex-1 text-left">
                        <div className="font-medium">{account.accountName}</div>
                        <div className="text-xs text-slate-500">{account.accountId}</div>
                      </div>
                      {account.status === 'ACTIVE' ? (
                        <CheckCircleIcon className="h-4 w-4 text-green-500" />
                      ) : (
                        <ExclamationTriangleIcon className="h-4 w-4 text-amber-500" />
                      )}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Region Selector */}
            <div className="relative">
              <button
                onClick={() => setShowRegionDropdown(!showRegionDropdown)}
                className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-sm font-medium transition-colors"
              >
                <SignalIcon className="h-4 w-4 text-slate-400" />
                {selectedRegion}
                <ChevronDownIcon className="h-4 w-4 text-slate-400" />
              </button>
              {showRegionDropdown && (
                <div className="absolute top-full left-0 mt-1 w-40 bg-slate-800 border border-slate-700 rounded-lg shadow-xl py-1 animate-fade-in">
                  {regions.map((region) => (
                    <button
                      key={region}
                      onClick={() => { setSelectedRegion(region); setShowRegionDropdown(false); }}
                      className={clsx(
                        "w-full px-3 py-2 text-sm text-left hover:bg-slate-700 transition-colors",
                        selectedRegion === region ? "text-indigo-400" : "text-slate-300"
                      )}
                    >
                      {region}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Right side - Search, status, notifications, user */}
          <div className="flex items-center gap-4">
            {/* Global Search */}
            <div className="relative">
              <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
              <input
                type="text"
                placeholder="Search services, deployments..."
                className="w-64 pl-9 pr-4 py-1.5 bg-slate-800 border border-slate-700 rounded-lg text-sm placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              />
              <kbd className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-500 bg-slate-700 px-1.5 py-0.5 rounded">⌘K</kbd>
            </div>

            {/* Live Data Status */}
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800/50 border border-slate-700/50">
              <div className="w-2 h-2 rounded-full bg-green-500 animate-live" />
              <span className="text-xs text-slate-400">Live</span>
              <span className="text-xs text-slate-500">•</span>
              <ArrowPathIcon className="h-3 w-3 text-slate-500" />
              <span className="text-xs text-slate-500">{formatLastSync()}</span>
            </div>

            {/* Notifications */}
            <button className="relative p-2 rounded-lg hover:bg-slate-800 transition-colors">
              <BellIcon className="h-5 w-5 text-slate-400" />
              <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full" />
            </button>

            {/* User Profile */}
            <div className="flex items-center gap-3 pl-3 border-l border-slate-700">
              <button
                onClick={() => navigate('/profile')}
                className="flex items-center gap-3 hover:bg-slate-800 rounded-lg px-2 py-1 transition-colors"
              >
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-sm font-semibold">
                  {user?.firstName?.charAt(0) || ''}{user?.lastName?.charAt(0) || ''}
                </div>
                <div className="text-right hidden sm:block">
                  <p className="text-sm font-medium text-white">{user?.fullName || 'User'}</p>
                  <p className="text-xs text-slate-400">{user?.role || 'OPERATOR'}</p>
                </div>
              </button>
              <button 
                onClick={() => { logout(); navigate('/login'); }}
                className="p-2 rounded-lg hover:bg-slate-800 transition-colors"
                title="Logout"
              >
                <ArrowRightOnRectangleIcon className="h-5 w-5 text-slate-400" />
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Left Sidebar */}
      <aside className="fixed top-14 left-0 bottom-0 w-56 bg-slate-900/50 border-r border-slate-800">
        <nav className="p-3 space-y-1">
          {getNavigation(user?.role === 'ADMIN').map((item) => {
            const isActive = location.pathname === item.href || 
              (item.href !== '/dashboard' && location.pathname.startsWith(item.href));
            const isDanger = 'danger' in item && item.danger;
            return (
              <NavLink
                key={item.name}
                to={item.href}
                className={clsx(
                  'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150',
                  isActive
                    ? isDanger 
                      ? 'bg-red-500/10 text-red-400 border border-red-500/20'
                      : 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20'
                    : isDanger
                      ? 'text-red-400 hover:text-red-300 hover:bg-red-900/20'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
                )}
              >
                <item.icon className={clsx(
                  "h-5 w-5", 
                  isActive 
                    ? isDanger ? "text-red-400" : "text-indigo-400" 
                    : isDanger ? "text-red-500" : "text-slate-500"
                )} />
                {item.name}
              </NavLink>
            );
          })}
        </nav>

        {/* Bottom section */}
        <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-slate-800">
          <div className="flex items-center justify-between text-xs text-slate-500">
            <span>Control Plane</span>
            <span className="font-mono">v1.0.0</span>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <main className="pt-14 pl-56">
        <div className="p-6">
          {children || <Outlet />}
        </div>
      </main>

      {/* Click outside handlers */}
      {(showEnvDropdown || showAccountDropdown || showRegionDropdown) && (
        <div 
          className="fixed inset-0 z-40" 
          onClick={() => {
            setShowEnvDropdown(false);
            setShowAccountDropdown(false);
            setShowRegionDropdown(false);
          }}
        />
      )}
    </div>
  );
}

export default Layout;
