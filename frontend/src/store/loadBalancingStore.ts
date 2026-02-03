import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AwsAccount, Service, DashboardData, User } from '../types';
import { api } from '../services/api';

interface LoadBalancingStore {
  // User & Auth
  user: User | null;
  selectedModule: string | null;
  
  // Accounts
  accounts: AwsAccount[];
  selectedAccountId: string | null;
  selectedAccount: AwsAccount | null;
  loadingAccounts: boolean;
  
  // Services
  services: Service[];
  selectedServiceId: string | null;
  loadingServices: boolean;
  
  // Dashboard
  dashboardData: DashboardData | null;
  loadingDashboard: boolean;
  
  // Actions
  setUser: (user: User | null) => void;
  setSelectedModule: (module: string | null) => void;
  logout: () => void;
  fetchAccounts: () => Promise<void>;
  selectAccount: (accountId: string | null) => void;
  fetchServices: (accountId: string) => Promise<void>;
  selectService: (serviceId: string | null) => void;
  fetchDashboard: (serviceId: string) => Promise<void>;
  refreshDashboard: () => Promise<void>;
}

export const useLoadBalancingStore = create<LoadBalancingStore>()(
  persist(
    (set, get) => ({
      // Initial state
      user: null,
      selectedModule: null,
      
      accounts: [],
      selectedAccountId: null,
      selectedAccount: null,
      loadingAccounts: false,
      
      services: [],
      selectedServiceId: null,
      loadingServices: false,
      
      dashboardData: null,
      loadingDashboard: false,

      // Actions
      setUser: (user) => set({ user }),
      
      setSelectedModule: (module) => set({ selectedModule: module }),
      
      logout: () => set({ 
        user: null, 
        selectedModule: null, 
        accounts: [], 
        selectedAccountId: null, 
        selectedAccount: null 
      }),

      fetchAccounts: async () => {
        set({ loadingAccounts: true });
        try {
          const accounts = await api.getActiveAccounts();
          set({ accounts, loadingAccounts: false });
        } catch (error) {
          console.error('Failed to fetch accounts:', error);
          set({ loadingAccounts: false });
        }
      },

      selectAccount: (accountId) => {
        const accounts = get().accounts;
        const selectedAccount = accountId ? accounts.find(a => a.id === accountId) || null : null;
        set({ selectedAccountId: accountId, selectedAccount, services: [], selectedServiceId: null });
        if (accountId) {
          get().fetchServices(accountId);
        }
      },

      fetchServices: async (accountId) => {
        set({ loadingServices: true });
        try {
          const services = await api.getServicesByAccount(accountId);
          set({ services, loadingServices: false });
        } catch (error) {
          console.error('Failed to fetch services:', error);
          set({ loadingServices: false });
        }
      },

      selectService: (serviceId) => {
        set({ selectedServiceId: serviceId, dashboardData: null });
        if (serviceId) {
          get().fetchDashboard(serviceId);
        }
      },

      fetchDashboard: async (serviceId) => {
        set({ loadingDashboard: true });
        try {
          const dashboardData = await api.getDashboardData(serviceId);
          set({ dashboardData, loadingDashboard: false });
        } catch (error) {
          console.error('Failed to fetch dashboard:', error);
          set({ loadingDashboard: false });
        }
      },

      refreshDashboard: async () => {
        const { selectedServiceId } = get();
        if (selectedServiceId) {
          await get().fetchDashboard(selectedServiceId);
        }
      },
    }),
    {
      name: 'load-balancing-storage',
      partialize: (state) => ({ user: state.user, selectedModule: state.selectedModule }),
    }
  )
);
