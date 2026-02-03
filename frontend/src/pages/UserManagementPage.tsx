import { useState, useEffect } from 'react';
import { Layout } from '../components/Layout';
import { 
  UserGroupIcon, 
  ShieldCheckIcon, 
  PlusIcon,
  MagnifyingGlassIcon,
  CheckCircleIcon,
  XCircleIcon,
  KeyIcon,
  CloudIcon,
  ExclamationTriangleIcon
} from '@heroicons/react/24/outline';
import { useLoadBalancingStore } from '../store/loadBalancingStore';
import { api } from '../services/api';
import clsx from 'clsx';

interface ManagedUser {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'READONLY' | 'OPERATOR' | 'ADMIN';
  status: 'ACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';
  accessibleAccounts: string[];
  lastLoginAt?: string;
  createdAt: string;
}

const roleColors = {
  ADMIN: 'bg-purple-500/20 text-purple-400 border-purple-500/30',
  OPERATOR: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
  READONLY: 'bg-slate-500/20 text-slate-400 border-slate-500/30',
};

const statusColors = {
  ACTIVE: 'bg-green-500/20 text-green-400',
  SUSPENDED: 'bg-red-500/20 text-red-400',
  PENDING_VERIFICATION: 'bg-amber-500/20 text-amber-400',
};

export function UserManagementPage() {
  const { user: currentUser, accounts } = useLoadBalancingStore();
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedUser, setSelectedUser] = useState<ManagedUser | null>(null);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showAccountModal, setShowAccountModal] = useState(false);
  const [editForm, setEditForm] = useState({
    role: 'READONLY' as 'READONLY' | 'OPERATOR' | 'ADMIN',
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const data = await api.getAllUsers();
      setUsers(data);
    } catch (err) {
      console.error('Failed to fetch users:', err);
      setError('Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateRole = async () => {
    if (!selectedUser) return;
    
    try {
      setSaving(true);
      setError(null);
      await api.updateUserRole(selectedUser.id, editForm.role);
      setSuccess(`Role updated successfully for ${selectedUser.username}`);
      setShowEditModal(false);
      fetchUsers();
    } catch (err) {
      setError('Failed to update user role');
    } finally {
      setSaving(false);
    }
  };

  const handleSuspendUser = async (userId: string) => {
    if (!confirm('Are you sure you want to suspend this user?')) return;
    
    try {
      await api.suspendUser(userId);
      setSuccess('User suspended successfully');
      fetchUsers();
    } catch (err) {
      setError('Failed to suspend user');
    }
  };

  const handleActivateUser = async (userId: string) => {
    try {
      await api.activateUser(userId);
      setSuccess('User activated successfully');
      fetchUsers();
    } catch (err) {
      setError('Failed to activate user');
    }
  };

  const handleGrantAccountAccess = async (accountId: string) => {
    if (!selectedUser) return;
    
    try {
      await api.grantAccountAccess(selectedUser.id, accountId);
      setSuccess('Account access granted');
      fetchUsers();
      setShowAccountModal(false);
    } catch (err) {
      setError('Failed to grant account access');
    }
  };

  const handleRevokeAccountAccess = async (userId: string, accountId: string) => {
    if (!confirm('Are you sure you want to revoke access to this account?')) return;
    
    try {
      await api.revokeAccountAccess(userId, accountId);
      setSuccess('Account access revoked');
      fetchUsers();
    } catch (err) {
      setError('Failed to revoke account access');
    }
  };

  const filteredUsers = users.filter(u => 
    u.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
    u.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
    u.firstName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    u.lastName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const getAccountName = (accountId: string) => {
    const account = accounts.find(a => a.id === accountId);
    return account?.accountName || accountId.substring(0, 8) + '...';
  };

  if (currentUser?.role !== 'ADMIN') {
    return (
      <Layout>
        <div className="p-6">
          <div className="flex items-center justify-center h-96">
            <div className="text-center">
              <ShieldCheckIcon className="h-16 w-16 text-slate-500 mx-auto mb-4" />
              <h2 className="text-xl font-semibold text-slate-300 mb-2">Access Denied</h2>
              <p className="text-slate-500">You need administrator privileges to access user management.</p>
            </div>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="p-6 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white flex items-center gap-3">
              <UserGroupIcon className="h-8 w-8 text-indigo-400" />
              User Management
            </h1>
            <p className="text-slate-400 mt-1">Manage users, roles, and account access permissions</p>
          </div>
        </div>

        {/* Alerts */}
        {error && (
          <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4 flex items-center gap-3">
            <ExclamationTriangleIcon className="h-5 w-5 text-red-400" />
            <span className="text-red-400">{error}</span>
            <button onClick={() => setError(null)} className="ml-auto text-red-400 hover:text-red-300">×</button>
          </div>
        )}
        
        {success && (
          <div className="bg-green-500/10 border border-green-500/30 rounded-lg p-4 flex items-center gap-3">
            <CheckCircleIcon className="h-5 w-5 text-green-400" />
            <span className="text-green-400">{success}</span>
            <button onClick={() => setSuccess(null)} className="ml-auto text-green-400 hover:text-green-300">×</button>
          </div>
        )}

        {/* Search and Stats */}
        <div className="grid grid-cols-4 gap-4">
          <div className="col-span-2">
            <div className="relative">
              <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400" />
              <input
                type="text"
                placeholder="Search users by name, username, or email..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 placeholder-slate-500 focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              />
            </div>
          </div>
          <div className="bg-slate-800/50 border border-slate-700 rounded-lg p-4">
            <div className="text-2xl font-bold text-white">{users.length}</div>
            <div className="text-sm text-slate-400">Total Users</div>
          </div>
          <div className="bg-slate-800/50 border border-slate-700 rounded-lg p-4">
            <div className="text-2xl font-bold text-green-400">{users.filter(u => u.status === 'ACTIVE').length}</div>
            <div className="text-sm text-slate-400">Active Users</div>
          </div>
        </div>

        {/* Users Table */}
        <div className="bg-slate-800/50 border border-slate-700 rounded-xl overflow-hidden">
          <table className="w-full">
            <thead className="bg-slate-800">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider">User</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider">Role</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider">Status</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider">Account Access</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider">Last Login</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-400 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-500 mx-auto"></div>
                    <p className="text-slate-400 mt-2">Loading users...</p>
                  </td>
                </tr>
              ) : filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-slate-400">
                    No users found
                  </td>
                </tr>
              ) : (
                filteredUsers.map((user) => (
                  <tr key={user.id} className="hover:bg-slate-700/30 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white font-semibold">
                          {user.firstName.charAt(0)}{user.lastName.charAt(0)}
                        </div>
                        <div>
                          <div className="font-medium text-white">{user.firstName} {user.lastName}</div>
                          <div className="text-sm text-slate-400">@{user.username} • {user.email}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={clsx(
                        "px-2.5 py-1 rounded-full text-xs font-medium border",
                        roleColors[user.role]
                      )}>
                        {user.role}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className={clsx(
                        "px-2.5 py-1 rounded-full text-xs font-medium",
                        statusColors[user.status]
                      )}>
                        {user.status.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex flex-wrap gap-1">
                        {user.accessibleAccounts.length === 0 ? (
                          <span className="text-slate-500 text-sm">No accounts</span>
                        ) : (
                          <>
                            {user.accessibleAccounts.slice(0, 2).map((accountId) => (
                              <span key={accountId} className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-700 rounded text-xs text-slate-300">
                                <CloudIcon className="h-3 w-3" />
                                {getAccountName(accountId)}
                                {user.id !== currentUser?.id && (
                                  <button
                                    onClick={() => handleRevokeAccountAccess(user.id, accountId)}
                                    className="ml-1 text-slate-500 hover:text-red-400"
                                  >
                                    ×
                                  </button>
                                )}
                              </span>
                            ))}
                            {user.accessibleAccounts.length > 2 && (
                              <span className="px-2 py-0.5 bg-slate-700 rounded text-xs text-slate-400">
                                +{user.accessibleAccounts.length - 2} more
                              </span>
                            )}
                          </>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-400">
                      {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleDateString() : 'Never'}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => {
                            setSelectedUser(user);
                            setEditForm({ role: user.role });
                            setShowEditModal(true);
                          }}
                          className="p-2 text-slate-400 hover:text-indigo-400 hover:bg-slate-700 rounded-lg transition-colors"
                          title="Edit Role"
                        >
                          <KeyIcon className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => {
                            setSelectedUser(user);
                            setShowAccountModal(true);
                          }}
                          className="p-2 text-slate-400 hover:text-blue-400 hover:bg-slate-700 rounded-lg transition-colors"
                          title="Manage Account Access"
                        >
                          <CloudIcon className="h-4 w-4" />
                        </button>
                        {user.id !== currentUser?.id && (
                          user.status === 'ACTIVE' ? (
                            <button
                              onClick={() => handleSuspendUser(user.id)}
                              className="p-2 text-slate-400 hover:text-red-400 hover:bg-slate-700 rounded-lg transition-colors"
                              title="Suspend User"
                            >
                              <XCircleIcon className="h-4 w-4" />
                            </button>
                          ) : (
                            <button
                              onClick={() => handleActivateUser(user.id)}
                              className="p-2 text-slate-400 hover:text-green-400 hover:bg-slate-700 rounded-lg transition-colors"
                              title="Activate User"
                            >
                              <CheckCircleIcon className="h-4 w-4" />
                            </button>
                          )
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Role Legend */}
        <div className="bg-slate-800/30 border border-slate-700 rounded-xl p-4">
          <h3 className="text-sm font-semibold text-slate-300 mb-3">Role Permissions</h3>
          <div className="grid grid-cols-3 gap-4">
            <div className="flex items-start gap-3">
              <span className={clsx("px-2.5 py-1 rounded-full text-xs font-medium border", roleColors.ADMIN)}>ADMIN</span>
              <p className="text-sm text-slate-400">Full access: manage users, accounts, services, policies, and all configurations</p>
            </div>
            <div className="flex items-start gap-3">
              <span className={clsx("px-2.5 py-1 rounded-full text-xs font-medium border", roleColors.OPERATOR)}>OPERATOR</span>
              <p className="text-sm text-slate-400">Operations access: manage services, deployments, scaling, and emergency controls</p>
            </div>
            <div className="flex items-start gap-3">
              <span className={clsx("px-2.5 py-1 rounded-full text-xs font-medium border", roleColors.READONLY)}>READONLY</span>
              <p className="text-sm text-slate-400">View only: can see dashboards, metrics, and logs but cannot make changes</p>
            </div>
          </div>
        </div>
      </div>

      {/* Edit Role Modal */}
      {showEditModal && selectedUser && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-slate-800 border border-slate-700 rounded-xl w-full max-w-md p-6 shadow-2xl">
            <h3 className="text-xl font-semibold text-white mb-4">Edit User Role</h3>
            <div className="mb-4">
              <p className="text-slate-400 text-sm mb-4">
                Editing role for <span className="text-white font-medium">{selectedUser.firstName} {selectedUser.lastName}</span>
              </p>
              <label className="block text-sm font-medium text-slate-300 mb-2">Role</label>
              <select
                value={editForm.role}
                onChange={(e) => setEditForm({ role: e.target.value as any })}
                className="w-full px-4 py-2.5 bg-slate-900 border border-slate-600 rounded-lg text-white focus:ring-2 focus:ring-indigo-500"
              >
                <option value="READONLY">Read Only</option>
                <option value="OPERATOR">Operator</option>
                <option value="ADMIN">Administrator</option>
              </select>
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowEditModal(false)}
                className="px-4 py-2 text-slate-400 hover:text-white transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleUpdateRole}
                disabled={saving}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg font-medium transition-colors disabled:opacity-50"
              >
                {saving ? 'Saving...' : 'Update Role'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Account Access Modal */}
      {showAccountModal && selectedUser && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-slate-800 border border-slate-700 rounded-xl w-full max-w-lg p-6 shadow-2xl">
            <h3 className="text-xl font-semibold text-white mb-4">Manage Account Access</h3>
            <p className="text-slate-400 text-sm mb-4">
              Grant or revoke AWS account access for <span className="text-white font-medium">{selectedUser.firstName} {selectedUser.lastName}</span>
            </p>
            
            <div className="mb-4">
              <label className="block text-sm font-medium text-slate-300 mb-2">Current Access</label>
              <div className="bg-slate-900 rounded-lg p-3 min-h-[60px]">
                {selectedUser.accessibleAccounts.length === 0 ? (
                  <p className="text-slate-500 text-sm">No accounts assigned</p>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {selectedUser.accessibleAccounts.map((accountId) => (
                      <span key={accountId} className="inline-flex items-center gap-2 px-3 py-1.5 bg-slate-700 rounded-lg text-sm text-slate-300">
                        <CloudIcon className="h-4 w-4 text-slate-400" />
                        {getAccountName(accountId)}
                        <button
                          onClick={() => handleRevokeAccountAccess(selectedUser.id, accountId)}
                          className="text-slate-500 hover:text-red-400 transition-colors"
                        >
                          ×
                        </button>
                      </span>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="mb-6">
              <label className="block text-sm font-medium text-slate-300 mb-2">Grant Access To</label>
              <div className="space-y-2 max-h-48 overflow-y-auto">
                {accounts.filter(a => !selectedUser.accessibleAccounts.includes(a.id)).map((account) => (
                  <button
                    key={account.id}
                    onClick={() => handleGrantAccountAccess(account.id)}
                    className="w-full flex items-center justify-between px-4 py-3 bg-slate-900 hover:bg-slate-700 rounded-lg transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      <CloudIcon className="h-5 w-5 text-slate-400" />
                      <div className="text-left">
                        <div className="text-sm font-medium text-white">{account.accountName}</div>
                        <div className="text-xs text-slate-500">{account.accountId}</div>
                      </div>
                    </div>
                    <PlusIcon className="h-4 w-4 text-indigo-400" />
                  </button>
                ))}
                {accounts.filter(a => !selectedUser.accessibleAccounts.includes(a.id)).length === 0 && (
                  <p className="text-slate-500 text-sm text-center py-4">All accounts already assigned</p>
                )}
              </div>
            </div>

            <div className="flex justify-end">
              <button
                onClick={() => setShowAccountModal(false)}
                className="px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg font-medium transition-colors"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}

export default UserManagementPage;
