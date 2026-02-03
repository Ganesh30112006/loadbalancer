import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { 
  ShieldCheckIcon, 
  ServerStackIcon,
  ExclamationCircleIcon,
  LockClosedIcon,
  CloudIcon,
  CpuChipIcon,
  GlobeAltIcon,
  ChartBarIcon,
  KeyIcon
} from '@heroicons/react/24/outline';
import { useLoadBalancingStore } from '../store/loadBalancingStore';
import { api } from '../services/api';

export function LoginPage() {
  const navigate = useNavigate();
  const { setUser } = useLoadBalancingStore();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await api.login(username, password);
      setUser(response.user);
      localStorage.setItem('userId', response.user.id);
      navigate('/module-selection');
    } catch (err: any) {
      setError(err.message || 'Invalid credentials. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const features = [
    { icon: CloudIcon, title: 'Multi-Account Management', desc: 'Manage infrastructure across multiple AWS accounts' },
    { icon: CpuChipIcon, title: 'Intelligent Auto-Scaling', desc: 'ML-powered scaling decisions with guardrails' },
    { icon: GlobeAltIcon, title: 'Global Traffic Management', desc: 'Route 53 integration for multi-region failover' },
    { icon: ChartBarIcon, title: 'Real-time Observability', desc: 'CloudWatch metrics and custom dashboards' },
  ];

  return (
    <div className="min-h-screen bg-slate-950 flex">
      {/* Left Panel - Branding & Context */}
      <div className="hidden lg:flex lg:w-1/2 relative overflow-hidden">
        {/* Gradient Background */}
        <div className="absolute inset-0 bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900" />
        
        {/* Subtle Grid Pattern */}
        <div className="absolute inset-0 opacity-10">
          <div className="absolute inset-0" style={{
            backgroundImage: `linear-gradient(rgba(99, 102, 241, 0.3) 1px, transparent 1px),
                             linear-gradient(90deg, rgba(99, 102, 241, 0.3) 1px, transparent 1px)`,
            backgroundSize: '50px 50px'
          }} />
        </div>

        {/* Floating Cloud Elements */}
        <div className="absolute top-20 left-20 w-72 h-72 bg-indigo-500/10 rounded-full blur-3xl" />
        <div className="absolute bottom-20 right-20 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl" />
        <div className="absolute top-1/2 left-1/3 w-64 h-64 bg-blue-500/10 rounded-full blur-3xl" />

        {/* Content */}
        <div className="relative z-10 flex flex-col justify-center px-12 xl:px-20">
          {/* Logo & Title */}
          <div className="flex items-center gap-4 mb-8">
            <div className="p-3 bg-indigo-600 rounded-xl shadow-lg shadow-indigo-500/30">
              <ServerStackIcon className="h-10 w-10 text-white" />
            </div>
            <div>
              <h1 className="text-3xl font-bold text-white">Cloud Control Plane</h1>
              <p className="text-indigo-300 text-sm">Infrastructure Automation Platform</p>
            </div>
          </div>

          {/* Tagline */}
          <h2 className="text-4xl xl:text-5xl font-bold text-white leading-tight mb-6">
            Operate AWS Infrastructure<br />
            <span className="text-indigo-400">at Scale</span>
          </h2>
          <p className="text-slate-400 text-lg mb-12 max-w-md">
            Enterprise-grade control plane for managing ALB, Auto Scaling Groups, 
            Route 53, and CloudWatch across your entire organization.
          </p>

          {/* Feature List */}
          <div className="space-y-4">
            {features.map((feature, index) => (
              <div key={index} className="flex items-start gap-4 group">
                <div className="p-2 bg-slate-800/50 rounded-lg border border-slate-700/50 group-hover:border-indigo-500/50 transition-colors">
                  <feature.icon className="h-5 w-5 text-indigo-400" />
                </div>
                <div>
                  <h3 className="text-white font-medium">{feature.title}</h3>
                  <p className="text-slate-500 text-sm">{feature.desc}</p>
                </div>
              </div>
            ))}
          </div>

          {/* Security Badge */}
          <div className="mt-12 flex items-center gap-3 px-4 py-3 bg-slate-800/30 rounded-lg border border-slate-700/50 max-w-md">
            <ShieldCheckIcon className="h-8 w-8 text-green-400 flex-shrink-0" />
            <div>
              <p className="text-green-400 text-sm font-medium">Security-First Architecture</p>
              <p className="text-slate-500 text-xs">No AWS access keys stored. IAM Role + STS AssumeRole only.</p>
            </div>
          </div>
        </div>
      </div>

      {/* Right Panel - Login Form */}
      <div className="flex-1 flex flex-col justify-center px-4 sm:px-6 lg:px-20 xl:px-24">
        <div className="mx-auto w-full max-w-sm lg:max-w-md">
          {/* Mobile Logo */}
          <div className="lg:hidden flex justify-center mb-8">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-indigo-600 rounded-lg">
                <ServerStackIcon className="h-8 w-8 text-white" />
              </div>
              <span className="text-xl font-bold text-white">Cloud Control Plane</span>
            </div>
          </div>

          {/* Form Header */}
          <div className="mb-8">
            <h2 className="text-2xl font-bold text-white">Sign in to your account</h2>
            <p className="mt-2 text-slate-400">
              Access your infrastructure control dashboard
            </p>
          </div>

          {/* Login Form */}
          <form className="space-y-5" onSubmit={handleLogin}>
            {error && (
              <div className="bg-red-950/50 border border-red-800 rounded-lg p-4 flex items-center gap-3">
                <ExclamationCircleIcon className="h-5 w-5 text-red-400 flex-shrink-0" />
                <span className="text-sm text-red-400">{error}</span>
              </div>
            )}

            <div>
              <label htmlFor="username" className="block text-sm font-medium text-slate-300 mb-2">
                Username
              </label>
              <input
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full px-4 py-3 bg-slate-900 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
                placeholder="admin"
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-2">
                <label htmlFor="password" className="block text-sm font-medium text-slate-300">
                  Password
                </label>
                <button type="button" className="text-sm text-indigo-400 hover:text-indigo-300 transition-colors">
                  Forgot password?
                </button>
              </div>
              <div className="relative">
                <input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full px-4 py-3 bg-slate-900 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
                  placeholder="••••••••••••"
                />
                <LockClosedIcon className="absolute right-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-500" />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex justify-center items-center gap-2 py-3 px-4 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-lg shadow-lg shadow-indigo-500/25 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-slate-950 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
            >
              {loading ? (
                <>
                  <div className="h-5 w-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Signing in...
                </>
              ) : (
                <>
                  <ShieldCheckIcon className="h-5 w-5" />
                  Sign in
                </>
              )}
            </button>

            {/* Divider */}
            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-slate-700" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-4 bg-slate-950 text-slate-500">Or continue with</span>
              </div>
            </div>

            {/* SSO Button */}
            <button
              type="button"
              className="w-full flex justify-center items-center gap-3 py-3 px-4 bg-slate-800 hover:bg-slate-700 text-white font-medium rounded-lg border border-slate-700 focus:outline-none focus:ring-2 focus:ring-slate-500 focus:ring-offset-2 focus:ring-offset-slate-950 transition-all"
            >
              <KeyIcon className="h-5 w-5 text-slate-400" />
              Sign in with SSO
            </button>
          </form>

          {/* Sign Up Link */}
          <p className="mt-8 text-center text-sm text-slate-400">
            Don't have an account?{' '}
            <Link to="/signup" className="text-indigo-400 hover:text-indigo-300 font-medium transition-colors">
              Request access
            </Link>
          </p>

          {/* Security Notice */}
          <div className="mt-8 flex items-center justify-center gap-2 text-xs text-slate-500">
            <ShieldCheckIcon className="h-4 w-4" />
            <span>Secured with enterprise-grade encryption</span>
          </div>
        </div>
      </div>
    </div>
  );
}
