// API Types for Load Balancing Control Plane

// ==================== Account Types ====================

export interface AwsAccount {
  id: string;
  accountId: string;
  accountName: string;
  roleArn: string;
  externalId: string;
  status: AccountStatus;
  enabledRegions: string[];
  lastValidatedAt?: string;
  validationError?: string;
  createdAt: string;
  updatedAt: string;
}

export type AccountStatus = 
  | 'PENDING_VALIDATION' 
  | 'ACTIVE' 
  | 'VALIDATION_FAILED' 
  | 'SUSPENDED';

export interface CreateAccountRequest {
  accountId: string;
  accountName: string;
  roleArn: string;
  enabledRegions?: string[];
}

export interface OnboardingResponse {
  id: string;
  accountId: string;
  accountName: string;
  externalId: string;
  trustPolicyTemplate: string;
  iamRoleSetupInstructions: string;
}

export interface ValidationResult {
  valid: boolean;
  accountId: string;
  assumedRoleArn?: string;
  validatedAt: string;
  errorMessage?: string;
}

// ==================== Blueprint Types ====================

export interface Blueprint {
  id: string;
  name: string;
  description?: string;
  awsAccountId: string;
  awsAccountName: string;
  amiId: string;
  instanceType: string;
  securityGroupIds?: string[];
  subnetIds?: string[];
  iamInstanceProfile?: string;
  healthCheckPath: string;
  healthCheckIntervalSeconds: number;
  drainTimeSeconds: number;
  startupTimeSeconds: number;
  version: number;
  status: BlueprintStatus;
  createdBy?: string;
  approvedBy?: string;
  approvedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export type BlueprintStatus = 'DRAFT' | 'ACTIVE' | 'DEPRECATED';

export interface CreateBlueprintRequest {
  name: string;
  description?: string;
  awsAccountId: string;
  amiId: string;
  instanceType: string;
  securityGroupIds?: string[];
  subnetIds?: string[];
  iamInstanceProfile?: string;
  userData?: string;
  healthCheckPath?: string;
  healthCheckIntervalSeconds?: number;
  drainTimeSeconds?: number;
  startupTimeSeconds?: number;
  tags?: Record<string, string>;
}

// ==================== Policy Types ====================

export interface Policy {
  id: string;
  name: string;
  description?: string;
  awsAccountId: string;
  awsAccountName: string;
  sloConfig?: SloConfiguration;
  scalingRules?: ScalingConfiguration;
  costConfig?: CostConfiguration;
  deploymentConfig?: DeploymentConfiguration;
  version: number;
  status: PolicyStatus;
  createdBy?: string;
  approvedBy?: string;
  approvedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export type PolicyStatus = 'DRAFT' | 'ACTIVE' | 'DEPRECATED';

export interface SloConfiguration {
  targetAvailability?: number;
  latencyP99TargetMs?: number;
  latencyP95TargetMs?: number;
  maxErrorRate?: number;
  minHealthyInstances?: number;
}

export interface ScalingConfiguration {
  minInstances?: number;
  maxInstances?: number;
  cpuScaleOutThreshold?: number;
  cpuScaleInThreshold?: number;
  scaleOutCooldownSeconds?: number;
  scaleInCooldownSeconds?: number;
  maxScaleOutStep?: number;
  maxScaleInStep?: number;
  predictiveScalingEnabled?: boolean;
  predictiveScalingMode?: string;
}

export interface CostConfiguration {
  monthlyBudgetLimit?: number;
  dailySpendLimit?: number;
  costAlertThreshold?: number;
  spotInstancePercentage?: number;
  reservedInstanceCoverage?: number;
  enableRightsizing?: boolean;
}

export interface DeploymentConfiguration {
  strategy?: 'BLUE_GREEN' | 'CANARY' | 'ROLLING';
  canaryPercentage?: number;
  canaryDurationMinutes?: number;
  bakeTimeMinutes?: number;
  rollbackOnAlarm?: boolean;
  autoRollbackThreshold?: number;
  requireApproval?: boolean;
  approvalTimeoutMinutes?: number;
}

export interface CreatePolicyRequest {
  name: string;
  description?: string;
  awsAccountId: string;
  sloConfig?: SloConfiguration;
  scalingRules?: ScalingConfiguration;
  costConfig?: CostConfiguration;
  deploymentConfig?: DeploymentConfiguration;
}

// ==================== Service Types ====================

export interface Service {
  id: string;
  serviceName: string;
  displayName?: string;
  description?: string;
  awsAccountId: string;
  blueprintId: string;
  policyId: string;
  primaryRegion: string;
  status: ServiceStatus;
  automationEnabled: boolean;
  scalingEnabled: boolean;
  deploymentEnabled: boolean;
  healthStatus?: Record<string, unknown>;
  lastHealthCheckAt?: string;
  regions: ServiceRegion[];
  createdAt: string;
  updatedAt: string;
}

export type ServiceStatus = 
  | 'PROVISIONING' 
  | 'ACTIVE' 
  | 'DEGRADED' 
  | 'FAILED' 
  | 'SUSPENDED';

export interface ServiceRegion {
  id?: string;
  region: string;
  status: RegionStatus;
  asgName?: string;
  albArn?: string;
  targetGroupArn?: string;
  desiredCapacity?: number;
  runningInstances?: number;
  trafficWeight: number;
  lastMetricsAt?: string;
}

export type RegionStatus = 
  | 'PROVISIONING' 
  | 'ACTIVE' 
  | 'DEGRADED' 
  | 'FAILING_OVER' 
  | 'FAILED';

export interface CreateServiceRequest {
  serviceName: string;
  displayName?: string;
  description?: string;
  awsAccountId: string;
  blueprintId: string;
  policyId: string;
  primaryRegion: string;
  regions: string[];
}

export interface HealthSummary {
  serviceId: string;
  serviceName: string;
  overallStatus: string;
  sloViolations: string[];
  totalInstances: number;
  healthyInstances: number;
  regionHealth: ServiceRegion[];
  lastCheckedAt?: string;
}

// ==================== Metrics Types ====================

export interface ServiceMetrics {
  serviceId: string;
  serviceName: string;
  avgCpu: number;
  latencyP99: number;
  errorRate: number;
  requestsPerMinute: number;
  healthyInstances: number;
  unhealthyInstances: number;
  healthStatus: string;
  sloViolations: string[];
  regionMetrics: RegionMetrics[];
  collectedAt: string;
}

export interface RegionMetrics {
  region: string;
  avgCpu: number;
  avgMemory?: number;
  networkIn?: number;
  networkOut?: number;
  requestCount: number;
  latencyP50: number;
  latencyP95: number;
  latencyP99: number;
  errorRate: number;
  healthyHostCount: number;
  unhealthyHostCount: number;
  activeConnections: number;
  error?: string;
  collectedAt: string;
}

export interface DashboardData {
  serviceId: string;
  serviceName: string;
  serviceStatus: string;
  healthStatus: string;
  automationEnabled: boolean;
  scalingEnabled: boolean;
  metrics: MetricsSummary;
  capacity: CapacitySummary;
  regions: RegionSummary[];
  sloViolations: string[];
  refreshedAt: string;
}

export interface MetricsSummary {
  avgCpu: number;
  latencyP99: number;
  errorRate: number;
  requestsPerMinute: number;
}

export interface CapacitySummary {
  healthyInstances: number;
  unhealthyInstances: number;
  regionCount: number;
}

export interface RegionSummary {
  region: string;
  status: string;
  trafficWeight: number;
  desiredCapacity: number;
  totalInstances: number;
  healthyInstances: number;
  avgCpu: number;
  latencyP99: number;
  errorRate: number;
}

// ==================== Control Loop Types ====================

export interface ControlLoopStatus {
  isLeader: boolean;
  instanceId: string;
  currentLeader?: string;
  activeServicesCount: number;
  lastCheckedAt: string;
}

export interface AuditLogEntry {
  id: string;
  actionType: string;
  category: string;
  description: string;
  status: string;
  isManualOverride: boolean;
  triggeredBy?: string;
  durationMs?: number;
  createdAt: string;
}

// ==================== WebSocket Types ====================

export interface WsMessage {
  type: 'connected' | 'subscribed' | 'unsubscribed' | 'metrics' | 'error' | 'dashboard_update' | 'dashboard_subscribed' | 'dashboard_unsubscribed';
  data: unknown;
}

export interface WsMetricsData {
  serviceId: string;
  serviceName: string;
  status: string;
  healthStatus: string;
  avgCpu: number;
  latencyP99: number;
  errorRate: number;
  requestsPerMinute: number;
  healthyInstances: number;
  unhealthyInstances: number;
  sloViolations: string[];
  automationEnabled: boolean;
  scalingEnabled: boolean;
  timestamp: string;
  regions: WsRegionMetrics[];
}

export interface WsRegionMetrics {
  region: string;
  avgCpu: number;
  latencyP99: number;
  errorRate: number;
  healthyHosts: number;
  unhealthyHosts: number;
  requestCount: number;
}

export interface DashboardData {
  services: Service[];
  stats: {
    totalServices: number;
    healthyServices: number;
    activeRegions: number;
    totalRequests: number;
  };
  controlLoop?: {
    isLeader: boolean;
    status: string;
  };
  recentActions?: AuditLog[];
  timestamp: string;
}

// ==================== User & Auth Types ====================

export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  role: UserRole;
  status: UserStatus;
  selectedModule?: string;
  accessibleAccounts: string[];
  lastLoginAt?: string;
  createdAt: string;
  canRead: boolean;
  canOperate: boolean;
  canAdmin: boolean;
}

export type UserRole = 'READONLY' | 'OPERATOR' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  user: User;
  token: string;
}

// ==================== Discovered Resource Types ====================

export interface DiscoveredResource {
  id: string;
  awsAccountId: string;
  resourceType: ResourceType;
  resourceId: string;
  resourceArn: string;
  resourceName: string;
  region: string;
  adoptionStatus: AdoptionStatus;
  configurationJson?: string;
  tagsJson?: string;
  currentCapacity?: number;
  minCapacity?: number;
  maxCapacity?: number;
  cpuUtilization?: number;
  latencyP99?: number;
  errorRate?: number;
  adoptedServiceId?: string;
  lastSyncedAt?: string;
  createdAt: string;
}

export type ResourceType = 
  | 'AUTO_SCALING_GROUP' 
  | 'LAUNCH_TEMPLATE' 
  | 'LAUNCH_CONFIGURATION' 
  | 'APPLICATION_LOAD_BALANCER' 
  | 'TARGET_GROUP' 
  | 'EC2_INSTANCE';

export type AdoptionStatus = 'DISCOVERED' | 'OBSERVED' | 'ADOPTED' | 'IGNORED';

// ==================== Control Loop Execution Types ====================

export interface ControlLoopExecution {
  id: string;
  serviceId: string;
  serviceName: string;
  phase: ExecutionPhase;
  status: ExecutionStatus;
  region: string;
  cpuUtilization?: number;
  memoryUtilization?: number;
  latencyP99?: number;
  errorRate?: number;
  requestsPerSecond?: number;
  currentCapacity?: number;
  analysisReason?: string;
  scalingDecision?: ScalingDecision;
  targetCapacity?: number;
  aiAdvisory?: string;
  aiConfidenceScore?: number;
  executionDetails?: string;
  startedAt?: string;
  completedAt?: string;
  durationMs?: number;
  createdAt: string;
}

export type ExecutionPhase = 
  | 'OBSERVING' 
  | 'ANALYZING' 
  | 'EXECUTING' 
  | 'COMPLETED' 
  | 'SKIPPED';

export type ExecutionStatus = 'IN_PROGRESS' | 'SUCCESS' | 'FAILED' | 'SKIPPED';

export type ScalingDecision = 
  | 'SCALE_OUT' 
  | 'SCALE_IN' 
  | 'NO_ACTION' 
  | 'BLOCKED_BY_COOLDOWN' 
  | 'BLOCKED_BY_SLO' 
  | 'BLOCKED_BY_GUARDRAIL';

// ==================== Scaling Event Types ====================

export interface ScalingEvent {
  id: string;
  serviceId: string;
  serviceName: string;
  region: string;
  eventType: ScalingEventType;
  eventSource: ScalingEventSource;
  status: ScalingEventStatus;
  previousCapacity: number;
  targetCapacity: number;
  actualCapacity?: number;
  reason: string;
  triggerMetrics?: string;
  triggeredBy?: string;
  isManualOverride?: boolean;
  overrideReason?: string;
  startedAt?: string;
  completedAt?: string;
  durationMs?: number;
  errorMessage?: string;
  createdAt: string;
}

export type ScalingEventType = 
  | 'SCALE_OUT' 
  | 'SCALE_IN' 
  | 'MANUAL_OVERRIDE' 
  | 'EMERGENCY_SCALE' 
  | 'INITIAL_CAPACITY' 
  | 'CAPACITY_RESTORE';

export type ScalingEventSource = 
  | 'CONTROL_LOOP' 
  | 'MANUAL' 
  | 'DEPLOYMENT' 
  | 'FAILOVER' 
  | 'EMERGENCY' 
  | 'SCHEDULE';

export type ScalingEventStatus = 
  | 'PENDING' 
  | 'IN_PROGRESS' 
  | 'COMPLETED' 
  | 'FAILED' 
  | 'ROLLED_BACK';

// ==================== Deployment Workflow Types ====================

export interface DeploymentWorkflow {
  id: string;
  serviceId: string;
  serviceName: string;
  deploymentId: string;
  strategy: DeploymentStrategy;
  status: WorkflowStatus;
  currentPhase: WorkflowPhase;
  blueTrafficPercent: number;
  greenTrafficPercent: number;
  canaryTrafficPercent: number;
  healthyThreshold: number;
  errorRateThreshold: number;
  latencyThresholdMs: number;
  canarySteps: number;
  currentCanaryStep: number;
  stepDurationMinutes: number;
  autoRollbackEnabled: boolean;
  rollbackTriggeredAt?: string;
  rollbackReason?: string;
  progressLog?: string;
  initiatedBy?: string;
  startedAt?: string;
  completedAt?: string;
  durationMs?: number;
  createdAt: string;
}

export type DeploymentStrategy = 'BLUE_GREEN' | 'CANARY' | 'ROLLING';

export type WorkflowStatus = 
  | 'PENDING' 
  | 'IN_PROGRESS' 
  | 'PAUSED' 
  | 'COMPLETED' 
  | 'FAILED' 
  | 'ROLLED_BACK';

export type WorkflowPhase = 
  | 'INITIALIZING' 
  | 'PROVISIONING' 
  | 'HEALTH_CHECK' 
  | 'TRAFFIC_SHIFTING' 
  | 'MONITORING' 
  | 'FINALIZING' 
  | 'ROLLING_BACK' 
  | 'COMPLETED';

// ==================== Emergency Override Types ====================

export interface EmergencyOverride {
  id: string;
  serviceId?: string;
  serviceName?: string;
  type: OverrideType; // Added for backward compat
  overrideType?: OverrideType;
  scope: OverrideScope;
  status: OverrideStatus;
  reason: string;
  overrideCapacity?: number;
  previousCapacity?: number;
  previousAutomationState?: boolean;
  initiatedBy?: string;
  createdBy?: string; // Alias for initiatedBy
  approvedBy?: string;
  expiresAt?: string;
  liftedAt?: string;
  liftedBy?: string;
  liftReason?: string;
  isActive?: boolean;
  createdAt: string;
  targetId?: string;
  targetName?: string;
  minCapacity?: number;
  maxCapacity?: number;
}

export type OverrideType = 
  | 'PAUSE_AUTOMATION' 
  | 'CAPACITY_OVERRIDE' 
  | 'FORCED_ROLLBACK' 
  | 'TRAFFIC_SHIFT' 
  | 'EMERGENCY_SCALE';

export type OverrideScope = 'GLOBAL' | 'SERVICE' | 'REGION' | 'ACCOUNT';
export type OverrideStatus = 'ACTIVE' | 'EXPIRED' | 'LIFTED' | 'SUPERSEDED';
