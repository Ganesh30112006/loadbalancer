import { useState, useEffect } from 'react';
import { Layout } from '../components/Layout';
import { 
  UserCircleIcon, 
  EnvelopeIcon,
  KeyIcon,
  ShieldCheckIcon,
  ClockIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon,
  EyeIcon,
  EyeSlashIcon,
  CameraIcon,
  CloudIcon,
  ArrowRightOnRectangleIcon
} from '@heroicons/react/24/outline';
import { useLoadBalancingStore } from '../store/loadBalancingStore';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import clsx from 'clsx';

const roleDescriptions = {
  ADMIN: 'Full administrative access to all resources and user management',
  OPERATOR: 'Can manage services, deployments, and emergency controls',
  READONLY: 'Read-only access to dashboards and monitoring',
};

const roleColors = {
  ADMIN: 'bg-purple-500/20 text-purple-400 border-purple-500/30',
  OPERATOR: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
  READONLY: 'bg-slate-500/20 text-slate-400 border-slate-500/30',
};

export function ProfilePage() {
  const { user, accounts, setUser, logout } = useLoadBalancingStore();
  const navigate = useNavigate();
  
  const [activeTab, setActiveTab] = useState<'profile' | 'security' | 'activity'>('profile');
  
  // Profile form
  const [profileForm, setProfileForm] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    email: user?.email || '',
  });
  const [savingProfile, setSavingProfile] = useState(false);
  
  // Password form
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    if (user) {
      setProfileForm({
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
      });
    }
  }, [user]);

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    
    try {
      setSavingProfile(true);
      setError(null);
      const updatedUser = await api.updateProfile(user.id, profileForm);
      setUser(updatedUser);
      setSuccess('Profile updated successfully');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update profile');
    } finally {
      setSavingProfile(false);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setError('New passwords do not match');
      return;
    }
    
    if (passwordForm.newPassword.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }
    
    try {
      setSavingPassword(true);
      setError(null);
      await api.changePassword(user.id, passwordForm.currentPassword, passwordForm.newPassword);
      setSuccess('Password changed successfully');
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to change password');
    } finally {
      setSavingPassword(false);
    }
  };

  const getPasswordStrength = (password: string) => {
    if (!password) return { strength: 0, label: '', color: '' };
    let strength = 0;
    if (password.length >= 8) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;
    
    if (strength <= 2) return { strength, label: 'Weak', color: 'bg-red-500' };
    if (strength <= 3) return { strength, label: 'Fair', color: 'bg-amber-500' };
    if (strength <= 4) return { strength, label: 'Good', color: 'bg-blue-500' };
    return { strength, label: 'Strong', color: 'bg-green-500' };
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!user) {
    return null;
  }

  const passwordStrength = getPasswordStrength(passwordForm.newPassword);

  return (
    <Layout>
      <div className="p-6 max-w-5xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-white">Account Settings</h1>
          <p className="text-slate-400 mt-1">Manage your profile, security, and preferences</p>
        </div>

        {/* Alerts */}
        {error && (
          <div className="mb-6 bg-red-500/10 border border-red-500/30 rounded-lg p-4 flex items-center gap-3">
            <ExclamationTriangleIcon className="h-5 w-5 text-red-400" />
            <span className="text-red-400">{error}</span>
            <button onClick={() => setError(null)} className="ml-auto text-red-400 hover:text-red-300">×</button>
          </div>
        )}
        
        {success && (
          <div className="mb-6 bg-green-500/10 border border-green-500/30 rounded-lg p-4 flex items-center gap-3">
            <CheckCircleIcon className="h-5 w-5 text-green-400" />
            <span className="text-green-400">{success}</span>
            <button onClick={() => setSuccess(null)} className="ml-auto text-green-400 hover:text-green-300">×</button>
          </div>
        )}

        <div className="grid grid-cols-4 gap-6">
          {/* Sidebar */}
          <div className="col-span-1">
            {/* Profile Card */}
            <div className="bg-slate-800/50 border border-slate-700 rounded-xl p-6 mb-6">
              <div className="text-center">
                <div className="relative inline-block">
                  <div className="w-24 h-24 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-3xl font-bold mx-auto">
                    {user.firstName.charAt(0)}{user.lastName.charAt(0)}
                  </div>
                  <button className="absolute bottom-0 right-0 p-2 bg-slate-700 hover:bg-slate-600 rounded-full text-slate-300 transition-colors">
                    <CameraIcon className="h-4 w-4" />
                  </button>
                </div>
                <h2 className="text-lg font-semibold text-white mt-4">{user.firstName} {user.lastName}</h2>
                <p className="text-slate-400 text-sm">@{user.username}</p>
                <span className={clsx(
                  "inline-block mt-3 px-3 py-1 rounded-full text-xs font-medium border",
                  roleColors[user.role]
                )}>
                  {user.role}
                </span>
              </div>
            </div>

            {/* Navigation */}
            <nav className="bg-slate-800/50 border border-slate-700 rounded-xl overflow-hidden">
              <button
                onClick={() => setActiveTab('profile')}
                className={clsx(
                  "w-full flex items-center gap-3 px-4 py-3 text-left transition-colors",
                  activeTab === 'profile' ? "bg-indigo-500/20 text-indigo-400 border-l-2 border-indigo-500" : "text-slate-400 hover:bg-slate-700/50"
                )}
              >
                <UserCircleIcon className="h-5 w-5" />
                Profile
              </button>
              <button
                onClick={() => setActiveTab('security')}
                className={clsx(
                  "w-full flex items-center gap-3 px-4 py-3 text-left transition-colors",
                  activeTab === 'security' ? "bg-indigo-500/20 text-indigo-400 border-l-2 border-indigo-500" : "text-slate-400 hover:bg-slate-700/50"
                )}
              >
                <KeyIcon className="h-5 w-5" />
                Security
              </button>
              <button
                onClick={() => setActiveTab('activity')}
                className={clsx(
                  "w-full flex items-center gap-3 px-4 py-3 text-left transition-colors",
                  activeTab === 'activity' ? "bg-indigo-500/20 text-indigo-400 border-l-2 border-indigo-500" : "text-slate-400 hover:bg-slate-700/50"
                )}
              >
                <ClockIcon className="h-5 w-5" />
                Activity
              </button>
              <button
                onClick={handleLogout}
                className="w-full flex items-center gap-3 px-4 py-3 text-left text-red-400 hover:bg-red-500/10 transition-colors border-t border-slate-700"
              >
                <ArrowRightOnRectangleIcon className="h-5 w-5" />
                Sign Out
              </button>
            </nav>
          </div>

          {/* Main Content */}
          <div className="col-span-3">
            {/* Profile Tab */}
            {activeTab === 'profile' && (
              <div className="space-y-6">
                {/* Profile Form */}
                <div className="bg-slate-800/50 border border-slate-700 rounded-xl p-6">
                  <h3 className="text-lg font-semibold text-white mb-6">Personal Information</h3>
                  <form onSubmit={handleUpdateProfile} className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">First Name</label>
                        <input
                          type="text"
                          value={profileForm.firstName}
                          onChange={(e) => setProfileForm({ ...profileForm, firstName: e.target.value })}
                          className="w-full px-4 py-2.5 bg-slate-900 border border-slate-600 rounded-lg text-white focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">Last Name</label>
                        <input
                          type="text"
                          value={profileForm.lastName}
                          onChange={(e) => setProfileForm({ ...profileForm, lastName: e.target.value })}
                          className="w-full px-4 py-2.5 bg-slate-900 border border-slate-600 rounded-lg text-white focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">Email Address</label>
                      <div className="relative">
                        <EnvelopeIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400" />
                        <input
                          type="email"
                          value={profileForm.email}
                          onChange={(e) => setProfileForm({ ...profileForm, email: e.target.value })}
                          className="w-full pl-10 pr-4 py-2.5 bg-slate-900 border border-slate-600 rounded-lg text-white focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">Username</label>
                      <input
                        type="text"
                        value={user.username}
                        disabled
                        className="w-full px-4 py-2.5 bg-slate-900/50 border border-slate-700 rounded-lg text-slate-500 cursor-not-allowed"
                      />
                      <p className="text-xs text-slate-500 mt-1">Username cannot be changed</p>
                    </div>
                    <div className="flex justify-end pt-4">
                      <button
                        type="submit"
                        disabled={savingProfile}
                        className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg font-medium transition-colors disabled:opacity-50"
                      >
                        {savingProfile ? 'Saving...' : 'Save Changes'}
                      </button>
                    </div>
                  </form>
                </div>

                {/* Role & Permissions */}
                <div className="bg-slate-800/50 border border-slate-700 rounded-xl p-6">
                  <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                    <ShieldCheckIcon className="h-5 w-5 text-indigo-400" />
                    Role & Permissions
                  </h3>
                  <div className="bg-slate-900 rounded-lg p-4">
                    <div className="flex items-center justify-between mb-3">
                      <span className={clsx(
                        "px-3 py-1 rounded-full text-sm font-medium border",
                        roleColors[user.role]
                      )}>
                        {user.role}
                      </span>
                      <span className="text-xs text-slate-500">Contact admin to change</span>
                    </div>
                    <p className="text-sm text-slate-400">{roleDescriptions[user.role]}</p>
                    <div className="mt-4 flex gap-4 text-sm">
                      <span className={clsx("flex items-center gap-1", user.canRead ? "text-green-400" : "text-slate-500")}>
                        {user.canRead ? <CheckCircleIcon className="h-4 w-4" /> : <span className="w-4 h-4" />}
                        Read Access
                      </span>
                      <span className={clsx("flex items-center gap-1", user.canOperate ? "text-green-400" : "text-slate-500")}>
                        {user.canOperate ? <CheckCircleIcon className="h-4 w-4" /> : <span className="w-4 h-4" />}
                        Operate
                      </span>
                      <span className={clsx("flex items-center gap-1", user.canAdmin ? "text-green-400" : "text-slate-500")}>
                        {user.canAdmin ? <CheckCircleIcon className="h-4 w-4" /> : <span className="w-4 h-4" />}
                        Admin
                      </span>
                    </div>
                  </div>
                </div>

                {/* Account Access */}
                <div className="bg-slate-800/50 border border-slate-700 rounded-xl p-6">
                  <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                    <CloudIcon className="h-5 w-5 text-indigo-400" />
                    AWS Account Access
                  </h3>
                  {user.accessibleAccounts.length === 0 ? (
                    <div className="bg-slate-900 rounded-lg p-6 text-center">
                      <CloudIcon className="h-10 w-10 text-slate-600 mx-auto mb-2" />
                      <p className="text-slate-400">No AWS accounts assigned</p>
                      <p className="text-sm text-slate-500 mt-1">Contact an administrator to get access</p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-2 gap-3">
                      {user.accessibleAccounts.map((accountId) => {
                        const account = accounts.find(a => a.id === accountId);
                        return (
                          <div key={accountId} className="bg-slate-900 rounded-lg p-4 flex items-center gap-3">
                            <CloudIcon className="h-8 w-8 text-slate-400" />
                            <div>
                              <div className="font-medium text-white">{account?.accountName || 'Unknown'}</div>
                              <div className="text-xs text-slate-500">{account?.accountId || accountId}</div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Security Tab */}
            {activeTab === 'security' && (
              <div className="space-y-6">
                {/* Change Password */}
                <div className="bg-slate-800/50 border border-slate-700 rounded-xl p-6">
                  <h3 className="text-lg font-semibold text-white mb-6">Change Password</h3>
                  <form onSubmit={handleChangePassword} className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">Current Password</label>
                      <div className="relative">
                        <input
                          type={showCurrentPassword ? 'text' : 'password'}
                          value={passwordForm.currentPassword}
                          onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
                          className="w-full px-4 py-2.5 pr-10 bg-slate-900 border border-slate-600 rounded-lg text-white focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                        />
                        <button
                          type="button"
                          onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-300"
                        >
                          {showCurrentPassword ? <EyeSlashIcon className="h-5 w-5" /> : <EyeIcon className="h-5 w-5" />}
                        </button>
                      </div>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">New Password</label>
                      <div className="relative">
                        <input
                          type={showNewPassword ? 'text' : 'password'}
                          value={passwordForm.newPassword}
                          onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                          className="w-full px-4 py-2.5 pr-10 bg-slate-900 border border-slate-600 rounded-lg text-white focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                        />
                        <button
                          type="button"
                          onClick={() => setShowNewPassword(!showNewPassword)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-300"
                        >
                          {showNewPassword ? <EyeSlashIcon className="h-5 w-5" /> : <EyeIcon className="h-5 w-5" />}
                        </button>
                      </div>
                      {passwordForm.newPassword && (
                        <div className="mt-2">
                          <div className="flex items-center gap-2">
                            <div className="flex-1 h-1.5 bg-slate-700 rounded-full overflow-hidden">
                              <div 
                                className={clsx("h-full transition-all", passwordStrength.color)}
                                style={{ width: `${(passwordStrength.strength / 5) * 100}%` }}
                              />
                            </div>
                            <span className={clsx("text-xs font-medium", 
                              passwordStrength.label === 'Strong' ? 'text-green-400' :
                              passwordStrength.label === 'Good' ? 'text-blue-400' :
                              passwordStrength.label === 'Fair' ? 'text-amber-400' : 'text-red-400'
                            )}>
                              {passwordStrength.label}
                            </span>
                          </div>
                        </div>
                      )}
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">Confirm New Password</label>
                      <input
                        type="password"
                        value={passwordForm.confirmPassword}
                        onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                        className="w-full px-4 py-2.5 bg-slate-900 border border-slate-600 rounded-lg text-white focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                      />
                      {passwordForm.confirmPassword && passwordForm.newPassword !== passwordForm.confirmPassword && (
                        <p className="text-sm text-red-400 mt-1">Passwords do not match</p>
                      )}
                    </div>
                    <div className="flex justify-end pt-4">
                      <button
                        type="submit"
                        disabled={savingPassword || !passwordForm.currentPassword || !passwordForm.newPassword || passwordForm.newPassword !== passwordForm.confirmPassword}
                        className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {savingPassword ? 'Changing...' : 'Change Password'}
                      </button>
                    </div>
                  </form>
                </div>

                {/* Security Info */}
                <div className="bg-slate-800/50 border border-slate-700 rounded-xl p-6">
                  <h3 className="text-lg font-semibold text-white mb-4">Security Information</h3>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between py-3 border-b border-slate-700">
                      <div>
                        <div className="font-medium text-slate-300">Last Login</div>
                        <div className="text-sm text-slate-500">
                          {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : 'Never'}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center justify-between py-3 border-b border-slate-700">
                      <div>
                        <div className="font-medium text-slate-300">Account Created</div>
                        <div className="text-sm text-slate-500">{new Date(user.createdAt).toLocaleString()}</div>
                      </div>
                    </div>
                    <div className="flex items-center justify-between py-3">
                      <div>
                        <div className="font-medium text-slate-300">Account Status</div>
                        <div className="text-sm text-slate-500">{user.status}</div>
                      </div>
                      <span className={clsx(
                        "px-2.5 py-1 rounded-full text-xs font-medium",
                        user.status === 'ACTIVE' ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
                      )}>
                        {user.status}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Activity Tab */}
            {activeTab === 'activity' && (
              <div className="bg-slate-800/50 border border-slate-700 rounded-xl p-6">
                <h3 className="text-lg font-semibold text-white mb-6">Recent Activity</h3>
                <div className="text-center py-12">
                  <ClockIcon className="h-12 w-12 text-slate-600 mx-auto mb-4" />
                  <p className="text-slate-400">Activity log coming soon</p>
                  <p className="text-sm text-slate-500 mt-1">Your recent actions and login history will appear here</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}

export default ProfilePage;
