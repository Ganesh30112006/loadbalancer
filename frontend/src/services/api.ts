import axios, { AxiosInstance } from 'axios';
import type { 
  AwsAccount, 
  CreateAccountRequest, 
  OnboardingResponse,
  ValidationResult,
  Blueprint,
  CreateBlueprintRequest,
  Policy,
  CreatePolicyRequest,
  Service,
  CreateServiceRequest,
  HealthSummary,
  ServiceMetrics,
  DashboardData,
  ControlLoopStatus,
  AuditLogEntry
} from '../types';

const API_BASE_URL = (import.meta as any).env?.VITE_API_URL || '/api/v1';

class ApiService {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add request interceptor for auth
    this.client.interceptors.request.use((config) => {
      // Add user ID header if available
      const userId = localStorage.getItem('userId');
      if (userId) {
        config.headers['X-User-Id'] = userId;
      }
      return config;
    });
  }

  // ==================== Account APIs ====================

  async getAccounts(): Promise<AwsAccount[]> {
    const { data } = await this.client.get('/accounts');
    return data;
  }

  async getActiveAccounts(): Promise<AwsAccount[]> {
    const { data } = await this.client.get('/accounts/active');
    return data;
  }

  async getAccount(id: string): Promise<AwsAccount> {
    const { data } = await this.client.get(`/accounts/${id}`);
    return data;
  }

  async onboardAccount(request: CreateAccountRequest): Promise<OnboardingResponse> {
    const { data } = await this.client.post('/accounts', request);
    return data;
  }

  async validateAccount(id: string): Promise<ValidationResult> {
    const { data } = await this.client.post(`/accounts/${id}/validate`);
    return data;
  }

  async suspendAccount(id: string): Promise<void> {
    await this.client.post(`/accounts/${id}/suspend`);
  }

  // ==================== Blueprint APIs ====================

  async getBlueprints(): Promise<Blueprint[]> {
    const { data } = await this.client.get('/blueprints');
    return data;
  }

  async getBlueprintsByAccount(accountId: string): Promise<Blueprint[]> {
    const { data } = await this.client.get(`/blueprints/account/${accountId}`);
    return data;
  }

  async getActiveBlueprints(accountId: string): Promise<Blueprint[]> {
    const { data } = await this.client.get(`/blueprints/account/${accountId}/active`);
    return data;
  }

  async getBlueprint(id: string): Promise<Blueprint> {
    const { data } = await this.client.get(`/blueprints/${id}`);
    return data;
  }

  async createBlueprint(request: CreateBlueprintRequest): Promise<Blueprint> {
    const { data } = await this.client.post('/blueprints', request);
    return data;
  }

  async approveBlueprint(id: string): Promise<Blueprint> {
    const { data } = await this.client.post(`/blueprints/${id}/approve`);
    return data;
  }

  async validateAmi(blueprintId: string, region: string): Promise<Record<string, unknown>> {
    const { data } = await this.client.post(`/blueprints/${blueprintId}/validate-ami?region=${region}`);
    return data;
  }

  // ==================== Policy APIs ====================

  async getPolicies(): Promise<Policy[]> {
    const { data } = await this.client.get('/policies');
    return data;
  }

  async getPoliciesByAccount(accountId: string): Promise<Policy[]> {
    const { data } = await this.client.get(`/policies/account/${accountId}`);
    return data;
  }

  async getActivePolicies(accountId: string): Promise<Policy[]> {
    const { data } = await this.client.get(`/policies/account/${accountId}/active`);
    return data;
  }

  async getPolicy(id: string): Promise<Policy> {
    const { data } = await this.client.get(`/policies/${id}`);
    return data;
  }

  async createPolicy(request: CreatePolicyRequest): Promise<Policy> {
    const { data } = await this.client.post('/policies', request);
    return data;
  }

  async updatePolicy(id: string, request: CreatePolicyRequest): Promise<Policy> {
    const { data } = await this.client.put(`/policies/${id}`, request);
    return data;
  }

  async deletePolicy(id: string): Promise<void> {
    await this.client.delete(`/policies/${id}`);
  }

  async approvePolicy(id: string): Promise<Policy> {
    const { data } = await this.client.post(`/policies/${id}/approve`);
    return data;
  }

  // ==================== Service APIs ====================

  async getServices(): Promise<Service[]> {
    const { data } = await this.client.get('/services');
    return data;
  }

  async getServicesByAccount(accountId: string): Promise<Service[]> {
    const { data } = await this.client.get(`/services/account/${accountId}`);
    return data;
  }

  async getService(id: string): Promise<Service> {
    const { data } = await this.client.get(`/services/${id}`);
    return data;
  }

  async createService(request: CreateServiceRequest): Promise<Service> {
    const { data } = await this.client.post('/services', request);
    return data;
  }

  async getServiceHealth(id: string): Promise<HealthSummary> {
    const { data } = await this.client.get(`/services/${id}/health`);
    return data;
  }

  async setAutomation(serviceId: string, enabled: boolean): Promise<Service> {
    const { data } = await this.client.post(`/services/${serviceId}/automation`, { automationEnabled: enabled });
    return data;
  }

  async setScalingEnabled(serviceId: string, enabled: boolean): Promise<Service> {
    const { data } = await this.client.post(`/services/${serviceId}/scaling`, { scalingEnabled: enabled });
    return data;
  }

  async configureAutomation(
    serviceId: string, 
    settings: { automationEnabled: boolean; scalingEnabled: boolean; deploymentEnabled: boolean }
  ): Promise<Service> {
    const { data } = await this.client.post(`/services/${serviceId}/automation`, settings);
    return data;
  }

  async applyOverride(
    serviceId: string,
    override: { 
      disableScaling?: boolean; 
      disableDeployment?: boolean; 
      disableAutomation?: boolean;
      overrideDurationMinutes?: number;
      reason: string;
    }
  ): Promise<Service> {
    const { data } = await this.client.post(`/services/${serviceId}/override`, override);
    return data;
  }

  async updateTrafficWeights(serviceId: string, weights: Record<string, number>): Promise<void> {
    await this.client.put(`/services/${serviceId}/traffic-weights`, weights);
  }

  // ==================== Metrics APIs ====================

  async getServiceMetrics(serviceId: string): Promise<ServiceMetrics> {
    const { data } = await this.client.get(`/metrics/services/${serviceId}`);
    return data;
  }

  async getDashboardData(serviceId: string): Promise<DashboardData> {
    const { data } = await this.client.get(`/metrics/services/${serviceId}/dashboard`);
    return data;
  }

  // ==================== Control Loop APIs ====================

  async getControlLoopStatus(): Promise<ControlLoopStatus> {
    const { data } = await this.client.get('/control-loop/status');
    return data;
  }

  async setControlLoopAutomation(enabled: boolean): Promise<void> {
    await this.client.post('/control-loop/automation', { enabled });
  }

  async getRecentDecisions(serviceId: string, limit = 10): Promise<AuditLogEntry[]> {
    const { data } = await this.client.get(`/control-loop/services/${serviceId}/decisions?limit=${limit}`);
    return data;
  }

  async getAuditLogs(serviceId: string, options: { limit?: number } = {}): Promise<AuditLogEntry[]> {
    const { data } = await this.client.get(`/control-loop/services/${serviceId}/audit?limit=${options.limit || 50}`);
    return data;
  }

  async getAuditLog(serviceId: string, limit = 50): Promise<AuditLogEntry[]> {
    const { data } = await this.client.get(`/control-loop/services/${serviceId}/audit?limit=${limit}`);
    return data;
  }

  // ==================== Auth APIs ====================

  async login(username: string, password: string): Promise<{ user: any; token: string }> {
    const { data } = await this.client.post('/auth/login', { username, password });
    return data;
  }

  async register(request: { username: string; email: string; password: string; firstName: string; lastName: string }): Promise<any> {
    const { data } = await this.client.post('/auth/register', request);
    return data;
  }

  async updateSelectedModule(userId: string, module: string): Promise<any> {
    const { data } = await this.client.put(`/auth/users/${userId}/module`, { module });
    return data;
  }

  // ==================== User Management APIs ====================

  async getAllUsers(): Promise<any[]> {
    const { data } = await this.client.get('/auth/users');
    return data;
  }

  async getUser(userId: string): Promise<any> {
    const { data } = await this.client.get(`/auth/users/${userId}`);
    return data;
  }

  async updateUserRole(userId: string, role: string): Promise<any> {
    const { data } = await this.client.put(`/auth/users/${userId}/role`, { role });
    return data;
  }

  async suspendUser(userId: string): Promise<void> {
    await this.client.post(`/auth/users/${userId}/suspend`);
  }

  async activateUser(userId: string): Promise<void> {
    await this.client.post(`/auth/users/${userId}/activate`);
  }

  async grantAccountAccess(userId: string, accountId: string): Promise<any> {
    const { data } = await this.client.post(`/auth/users/${userId}/accounts/${accountId}/grant`);
    return data;
  }

  async revokeAccountAccess(userId: string, accountId: string): Promise<any> {
    const { data } = await this.client.post(`/auth/users/${userId}/accounts/${accountId}/revoke`);
    return data;
  }

  async updateProfile(userId: string, profile: { firstName: string; lastName: string; email: string }): Promise<any> {
    const { data } = await this.client.put(`/auth/users/${userId}/profile`, profile);
    return data;
  }

  async changePassword(userId: string, currentPassword: string, newPassword: string): Promise<void> {
    await this.client.post(`/auth/users/${userId}/change-password`, { currentPassword, newPassword });
  }

  // ==================== Emergency APIs ====================

  async pauseServiceAutomation(serviceId: string, reason: string, initiatedBy: string): Promise<any> {
    const { data } = await this.client.post(`/emergency/services/${serviceId}/pause`, { reason, initiatedBy });
    return data;
  }

  async pauseGlobalAutomation(reason: string, initiatedBy: string): Promise<any> {
    const { data } = await this.client.post('/emergency/global/pause', { reason, initiatedBy });
    return data;
  }

  async setCapacityOverride(serviceId: string, targetCapacity: number, reason: string, initiatedBy: string): Promise<any> {
    const { data } = await this.client.post(`/emergency/services/${serviceId}/capacity-override`, { 
      targetCapacity, reason, initiatedBy 
    });
    return data;
  }

  async liftOverride(overrideId: string, reason: string, liftedBy: string): Promise<any> {
    const { data } = await this.client.post(`/emergency/overrides/${overrideId}/lift`, { reason, liftedBy });
    return data;
  }

  async resumeServiceAutomation(serviceId: string, resumedBy: string): Promise<void> {
    await this.client.post(`/emergency/services/${serviceId}/resume`, { resumedBy });
  }

  async getActiveOverrides(): Promise<any[]> {
    const { data } = await this.client.get('/emergency/overrides/active');
    return data;
  }

  async getServiceOverrides(serviceId: string): Promise<any[]> {
    const { data } = await this.client.get(`/emergency/services/${serviceId}/overrides`);
    return data;
  }

  async isAutomationBlocked(serviceId: string): Promise<{ blocked: boolean }> {
    const { data } = await this.client.get(`/emergency/services/${serviceId}/blocked`);
    return data;
  }

  async getScalingEvents(serviceId: string, limit = 50): Promise<any[]> {
    const { data } = await this.client.get(`/emergency/services/${serviceId}/scaling-events?limit=${limit}`);
    return data;
  }

  // ==================== Audit APIs ====================

  async getAllAuditLogs(limit = 100, category?: string): Promise<any[]> {
    const params = new URLSearchParams({ limit: limit.toString() });
    if (category) params.append('category', category);
    const { data } = await this.client.get(`/audit/logs?${params}`);
    return data;
  }

  async getServiceAuditLogs(serviceId: string, limit = 50): Promise<any[]> {
    const { data } = await this.client.get(`/audit/services/${serviceId}/logs?limit=${limit}`);
    return data;
  }

  async getAccountAuditLogs(accountId: string, limit = 50): Promise<any[]> {
    const { data } = await this.client.get(`/audit/accounts/${accountId}/logs?limit=${limit}`);
    return data;
  }

  async getRecentAuditLogs(limit = 20): Promise<any[]> {
    const { data } = await this.client.get(`/audit/logs/recent?limit=${limit}`);
    return data;
  }

  // ==================== Deployment Workflow APIs ====================

  async getAllDeploymentWorkflows(limit = 50): Promise<any[]> {
    const { data } = await this.client.get(`/deployment-workflows?limit=${limit}`);
    return data;
  }

  async getActiveDeploymentWorkflows(): Promise<any[]> {
    const { data } = await this.client.get('/deployment-workflows/active');
    return data;
  }

  async getDeploymentWorkflow(workflowId: string): Promise<any> {
    const { data } = await this.client.get(`/deployment-workflows/${workflowId}`);
    return data;
  }

  async getServiceDeploymentWorkflows(serviceId: string): Promise<any[]> {
    const { data } = await this.client.get(`/deployment-workflows/services/${serviceId}`);
    return data;
  }

  async startBlueGreenDeployment(serviceId: string, deploymentId: string, initiatedBy: string): Promise<any> {
    const { data } = await this.client.post('/deployment-workflows/blue-green', { 
      serviceId, deploymentId, initiatedBy 
    });
    return data;
  }

  async startCanaryDeployment(serviceId: string, deploymentId: string, canarySteps: number, stepDurationMinutes: number, initiatedBy: string): Promise<any> {
    const { data } = await this.client.post('/deployment-workflows/canary', { 
      serviceId, deploymentId, canarySteps, stepDurationMinutes, initiatedBy 
    });
    return data;
  }

  async rollbackDeployment(workflowId: string, reason: string, rolledBackBy: string): Promise<any> {
    const { data } = await this.client.post(`/deployment-workflows/${workflowId}/rollback`, { reason, rolledBackBy });
    return data;
  }
}

export const api = new ApiService();
