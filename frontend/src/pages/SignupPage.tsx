import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { 
  ShieldCheckIcon, 
  ServerStackIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  UserIcon,
  EnvelopeIcon,
  LockClosedIcon,
  CheckCircleIcon,
  BoltIcon
} from '@heroicons/react/24/outline';
import { api } from '../services/api';
import toast from 'react-hot-toast';

export function SignupPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [acceptedTerms, setAcceptedTerms] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
  };

  const validateForm = () => {
    if (!formData.firstName.trim() || !formData.lastName.trim()) {
      setError('Please enter your full name');
      return false;
    }
    if (!formData.email.includes('@')) {
      setError('Please enter a valid email address');
      return false;
    }
    if (formData.password.length < 8) {
      setError('Password must be at least 8 characters');
      return false;
    }
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return false;
    }
    if (!acceptedTerms) {
      setError('You must acknowledge the production infrastructure warning');
      return false;
    }
    return true;
  };

  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!validateForm()) return;

    setLoading(true);

    try {
      await api.register({
        username: formData.email.split('@')[0],
        email: formData.email,
        password: formData.password,
        firstName: formData.firstName,
        lastName: formData.lastName,
      });
      
      toast.success('Account created successfully! Please sign in.');
      navigate('/login');
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const passwordStrength = () => {
    const { password } = formData;
    if (!password) return { strength: 0, label: '', color: '' };
    
    let strength = 0;
    if (password.length >= 8) strength++;
    if (password.length >= 12) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;

    if (strength <= 2) return { strength: 1, label: 'Weak', color: 'bg-red-500' };
    if (strength <= 3) return { strength: 2, label: 'Fair', color: 'bg-yellow-500' };
    if (strength <= 4) return { strength: 3, label: 'Good', color: 'bg-blue-500' };
    return { strength: 4, label: 'Strong', color: 'bg-green-500' };
  };

  const pwStrength = passwordStrength();

  return (
    <div className="min-h-screen bg-slate-950 flex">
      {/* Left Panel - Warning & Context */}
      <div className="hidden lg:flex lg:w-1/2 relative overflow-hidden">
        {/* Gradient Background - More serious tone */}
        <div className="absolute inset-0 bg-gradient-to-br from-slate-900 via-slate-900 to-red-950/30" />
        
        {/* Subtle Grid Pattern */}
        <div className="absolute inset-0 opacity-5">
          <div className="absolute inset-0" style={{
            backgroundImage: `linear-gradient(rgba(239, 68, 68, 0.5) 1px, transparent 1px),
                             linear-gradient(90deg, rgba(239, 68, 68, 0.5) 1px, transparent 1px)`,
            backgroundSize: '50px 50px'
          }} />
        </div>

        {/* Floating Elements */}
        <div className="absolute top-20 left-20 w-72 h-72 bg-red-500/5 rounded-full blur-3xl" />
        <div className="absolute bottom-20 right-20 w-96 h-96 bg-orange-500/5 rounded-full blur-3xl" />

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

          {/* Warning Box */}
          <div className="bg-amber-950/50 border border-amber-700 rounded-xl p-6 mb-8">
            <div className="flex items-start gap-4">
              <div className="p-2 bg-amber-600 rounded-lg">
                <ExclamationTriangleIcon className="h-6 w-6 text-white" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-amber-400">Production Infrastructure Access</h3>
                <p className="text-amber-200/70 text-sm mt-2 leading-relaxed">
                  This platform controls <strong>live AWS infrastructure</strong> including 
                  Application Load Balancers, Auto Scaling Groups, Route 53 DNS, and CloudWatch alarms.
                </p>
                <p className="text-amber-200/70 text-sm mt-2 leading-relaxed">
                  Actions taken here have <strong>real production impact</strong>. 
                  Scaling decisions, traffic shifts, and configuration changes affect live systems.
                </p>
              </div>
            </div>
          </div>

          {/* What You'll Control */}
          <h3 className="text-lg font-semibold text-white mb-4">What You'll Be Controlling</h3>
          <div className="space-y-3 mb-8">
            {[
              { label: 'Application Load Balancers', desc: 'Traffic distribution across targets' },
              { label: 'Auto Scaling Groups', desc: 'Instance scaling decisions' },
              { label: 'Route 53 Health Checks', desc: 'Global DNS and failover' },
              { label: 'CloudWatch Alarms', desc: 'Metrics and alerting' },
            ].map((item, index) => (
              <div key={index} className="flex items-center gap-3">
                <CheckCircleIcon className="h-5 w-5 text-indigo-400" />
                <div>
                  <span className="text-white">{item.label}</span>
                  <span className="text-slate-500 text-sm ml-2">— {item.desc}</span>
                </div>
              </div>
            ))}
          </div>

          {/* Security Info */}
          <div className="flex items-center gap-3 px-4 py-3 bg-slate-800/30 rounded-lg border border-slate-700/50">
            <ShieldCheckIcon className="h-8 w-8 text-green-400 flex-shrink-0" />
            <div>
              <p className="text-green-400 text-sm font-medium">Secure by Design</p>
              <p className="text-slate-500 text-xs">No AWS access keys stored. IAM Role + STS AssumeRole only.</p>
            </div>
          </div>

          {/* Post-signup info */}
          <div className="mt-8 p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
            <div className="flex items-start gap-3">
              <BoltIcon className="h-5 w-5 text-indigo-400 mt-0.5" />
              <div>
                <p className="text-white text-sm font-medium">After Signup</p>
                <p className="text-slate-500 text-xs mt-1">
                  You'll be guided through AWS account onboarding to connect your infrastructure.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Right Panel - Signup Form */}
      <div className="flex-1 flex flex-col justify-center px-4 sm:px-6 lg:px-20 xl:px-24">
        <div className="mx-auto w-full max-w-sm lg:max-w-md">
          {/* Mobile Logo */}
          <div className="lg:hidden flex justify-center mb-6">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-indigo-600 rounded-lg">
                <ServerStackIcon className="h-8 w-8 text-white" />
              </div>
              <span className="text-xl font-bold text-white">Cloud Control Plane</span>
            </div>
          </div>

          {/* Mobile Warning */}
          <div className="lg:hidden bg-amber-950/50 border border-amber-700 rounded-lg p-4 mb-6">
            <div className="flex items-center gap-2">
              <ExclamationTriangleIcon className="h-5 w-5 text-amber-400" />
              <p className="text-amber-400 text-sm font-medium">Production Infrastructure Access</p>
            </div>
          </div>

          {/* Form Header */}
          <div className="mb-6">
            <h2 className="text-2xl font-bold text-white">Create your account</h2>
            <p className="mt-2 text-slate-400">
              Request access to the infrastructure control plane
            </p>
          </div>

          {/* Signup Form */}
          <form className="space-y-4" onSubmit={handleSignup}>
            {error && (
              <div className="bg-red-950/50 border border-red-800 rounded-lg p-4 flex items-center gap-3">
                <ExclamationCircleIcon className="h-5 w-5 text-red-400 flex-shrink-0" />
                <span className="text-sm text-red-400">{error}</span>
              </div>
            )}

            {/* Name Fields */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="firstName" className="block text-sm font-medium text-slate-300 mb-2">
                  First Name
                </label>
                <div className="relative">
                  <input
                    id="firstName"
                    name="firstName"
                    type="text"
                    required
                    value={formData.firstName}
                    onChange={handleChange}
                    className="w-full px-4 py-3 bg-slate-900 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
                    placeholder="John"
                  />
                </div>
              </div>
              <div>
                <label htmlFor="lastName" className="block text-sm font-medium text-slate-300 mb-2">
                  Last Name
                </label>
                <input
                  id="lastName"
                  name="lastName"
                  type="text"
                  required
                  value={formData.lastName}
                  onChange={handleChange}
                  className="w-full px-4 py-3 bg-slate-900 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
                  placeholder="Doe"
                />
              </div>
            </div>

            {/* Email */}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-slate-300 mb-2">
                Work Email
              </label>
              <div className="relative">
                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  required
                  value={formData.email}
                  onChange={handleChange}
                  className="w-full px-4 py-3 pl-11 bg-slate-900 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
                  placeholder="john.doe@company.com"
                />
                <EnvelopeIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-500" />
              </div>
            </div>

            {/* Password */}
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-slate-300 mb-2">
                Password
              </label>
              <div className="relative">
                <input
                  id="password"
                  name="password"
                  type="password"
                  required
                  value={formData.password}
                  onChange={handleChange}
                  className="w-full px-4 py-3 pl-11 bg-slate-900 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
                  placeholder="At least 8 characters"
                />
                <LockClosedIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-500" />
              </div>
              {/* Password Strength Indicator */}
              {formData.password && (
                <div className="mt-2">
                  <div className="flex items-center gap-2">
                    <div className="flex-1 h-1.5 bg-slate-700 rounded-full overflow-hidden">
                      <div 
                        className={`h-full ${pwStrength.color} transition-all duration-300`}
                        style={{ width: `${pwStrength.strength * 25}%` }}
                      />
                    </div>
                    <span className={`text-xs ${
                      pwStrength.strength <= 1 ? 'text-red-400' :
                      pwStrength.strength <= 2 ? 'text-yellow-400' :
                      pwStrength.strength <= 3 ? 'text-blue-400' : 'text-green-400'
                    }`}>
                      {pwStrength.label}
                    </span>
                  </div>
                </div>
              )}
            </div>

            {/* Confirm Password */}
            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-slate-300 mb-2">
                Confirm Password
              </label>
              <div className="relative">
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type="password"
                  required
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  className="w-full px-4 py-3 pl-11 bg-slate-900 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
                  placeholder="Confirm your password"
                />
                <LockClosedIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-500" />
                {formData.confirmPassword && formData.password === formData.confirmPassword && (
                  <CheckCircleIcon className="absolute right-3 top-1/2 -translate-y-1/2 h-5 w-5 text-green-400" />
                )}
              </div>
            </div>

            {/* Production Warning Checkbox */}
            <div className="bg-slate-900 border border-slate-700 rounded-lg p-4">
              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={acceptedTerms}
                  onChange={(e) => setAcceptedTerms(e.target.checked)}
                  className="mt-1 h-4 w-4 rounded border-slate-600 bg-slate-800 text-indigo-600 focus:ring-indigo-500 focus:ring-offset-slate-900"
                />
                <span className="text-sm text-slate-300">
                  I understand that this platform controls <strong className="text-amber-400">production AWS infrastructure</strong> and 
                  that my actions will have real impact on live systems. I will exercise caution when making changes.
                </span>
              </label>
            </div>

            <button
              type="submit"
              disabled={loading || !acceptedTerms}
              className="w-full flex justify-center items-center gap-2 py-3 px-4 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-lg shadow-lg shadow-indigo-500/25 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-slate-950 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
            >
              {loading ? (
                <>
                  <div className="h-5 w-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Creating account...
                </>
              ) : (
                <>
                  <UserIcon className="h-5 w-5" />
                  Create account
                </>
              )}
            </button>
          </form>

          {/* Sign In Link */}
          <p className="mt-6 text-center text-sm text-slate-400">
            Already have an account?{' '}
            <Link to="/login" className="text-indigo-400 hover:text-indigo-300 font-medium transition-colors">
              Sign in
            </Link>
          </p>

          {/* Security Notice */}
          <div className="mt-6 flex items-center justify-center gap-2 text-xs text-slate-500">
            <ShieldCheckIcon className="h-4 w-4" />
            <span>Your data is encrypted and secured</span>
          </div>
        </div>
      </div>
    </div>
  );
}
