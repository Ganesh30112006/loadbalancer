-- Load Balancing Schema
-- V1__init_schema.sql

-- Create the schema
CREATE SCHEMA IF NOT EXISTS load_balancing;

-- AWS Accounts table
CREATE TABLE load_balancing.aws_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(12) NOT NULL UNIQUE,
    account_name VARCHAR(255) NOT NULL,
    role_arn VARCHAR(512) NOT NULL,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_VALIDATION',
    enabled_regions TEXT,
    last_validated_at TIMESTAMP WITH TIME ZONE,
    validation_error TEXT,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_aws_accounts_status ON load_balancing.aws_accounts(status);
CREATE INDEX idx_aws_accounts_account_id ON load_balancing.aws_accounts(account_id);

-- Blueprints table
CREATE TABLE load_balancing.blueprints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    aws_account_id UUID NOT NULL REFERENCES load_balancing.aws_accounts(id),
    ami_id VARCHAR(50) NOT NULL,
    instance_type VARCHAR(50) NOT NULL,
    launch_template_config JSONB,
    health_check_path VARCHAR(255) DEFAULT '/health',
    health_check_interval_seconds INTEGER DEFAULT 30,
    drain_time_seconds INTEGER DEFAULT 300,
    startup_time_seconds INTEGER DEFAULT 120,
    version INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by UUID,
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_blueprints_aws_account ON load_balancing.blueprints(aws_account_id);
CREATE INDEX idx_blueprints_status ON load_balancing.blueprints(status);
CREATE INDEX idx_blueprints_name_version ON load_balancing.blueprints(name, version);

-- Policies table
CREATE TABLE load_balancing.policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    aws_account_id UUID NOT NULL REFERENCES load_balancing.aws_accounts(id),
    slo_config JSONB,
    scaling_rules JSONB,
    cost_config JSONB,
    deployment_config JSONB,
    version INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by UUID,
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_policies_aws_account ON load_balancing.policies(aws_account_id);
CREATE INDEX idx_policies_status ON load_balancing.policies(status);
CREATE INDEX idx_policies_name_version ON load_balancing.policies(name, version);

-- Services table
CREATE TABLE load_balancing.services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    aws_account_id UUID NOT NULL REFERENCES load_balancing.aws_accounts(id),
    blueprint_id UUID NOT NULL REFERENCES load_balancing.blueprints(id),
    policy_id UUID NOT NULL REFERENCES load_balancing.policies(id),
    primary_region VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PROVISIONING',
    automation_enabled BOOLEAN DEFAULT FALSE,
    scaling_enabled BOOLEAN DEFAULT FALSE,
    deployment_enabled BOOLEAN DEFAULT FALSE,
    health_status JSONB,
    last_health_check_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(aws_account_id, service_name)
);

CREATE INDEX idx_services_aws_account ON load_balancing.services(aws_account_id);
CREATE INDEX idx_services_status ON load_balancing.services(status);
CREATE INDEX idx_services_automation ON load_balancing.services(automation_enabled, scaling_enabled);

-- Service Regions table
CREATE TABLE load_balancing.service_regions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES load_balancing.services(id) ON DELETE CASCADE,
    region VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PROVISIONING',
    asg_name VARCHAR(255),
    alb_arn VARCHAR(512),
    target_group_arn VARCHAR(512),
    launch_template_id VARCHAR(100),
    desired_capacity INTEGER,
    running_instances INTEGER,
    traffic_weight INTEGER DEFAULT 0,
    last_metrics_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(service_id, region)
);

CREATE INDEX idx_service_regions_service ON load_balancing.service_regions(service_id);
CREATE INDEX idx_service_regions_status ON load_balancing.service_regions(status);

-- Deployments table
CREATE TABLE load_balancing.deployments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES load_balancing.services(id),
    blueprint_id UUID NOT NULL REFERENCES load_balancing.blueprints(id),
    deployment_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    current_phase VARCHAR(50),
    phase_progress INTEGER DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    initiated_by UUID,
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    region_status JSONB,
    canary_metrics JSONB,
    rollback_triggered BOOLEAN DEFAULT FALSE,
    rollback_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_deployments_service ON load_balancing.deployments(service_id);
CREATE INDEX idx_deployments_status ON load_balancing.deployments(status);
CREATE INDEX idx_deployments_started_at ON load_balancing.deployments(started_at);

-- Audit Logs table
CREATE TABLE load_balancing.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID REFERENCES load_balancing.services(id),
    aws_account_id UUID REFERENCES load_balancing.aws_accounts(id),
    cycle_id VARCHAR(100),
    action_type VARCHAR(100) NOT NULL,
    action_category VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    observation_data JSONB,
    analysis_result JSONB,
    decision_details JSONB,
    execution_details JSONB,
    error_message TEXT,
    duration_ms BIGINT,
    triggered_by UUID,
    is_manual_override BOOLEAN DEFAULT FALSE,
    platform_instance_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_service ON load_balancing.audit_logs(service_id);
CREATE INDEX idx_audit_logs_account ON load_balancing.audit_logs(aws_account_id);
CREATE INDEX idx_audit_logs_created_at ON load_balancing.audit_logs(created_at);
CREATE INDEX idx_audit_logs_action_type ON load_balancing.audit_logs(action_type);
CREATE INDEX idx_audit_logs_cycle_id ON load_balancing.audit_logs(cycle_id);

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION load_balancing.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers
CREATE TRIGGER update_aws_accounts_updated_at
    BEFORE UPDATE ON load_balancing.aws_accounts
    FOR EACH ROW EXECUTE FUNCTION load_balancing.update_updated_at_column();

CREATE TRIGGER update_blueprints_updated_at
    BEFORE UPDATE ON load_balancing.blueprints
    FOR EACH ROW EXECUTE FUNCTION load_balancing.update_updated_at_column();

CREATE TRIGGER update_policies_updated_at
    BEFORE UPDATE ON load_balancing.policies
    FOR EACH ROW EXECUTE FUNCTION load_balancing.update_updated_at_column();

CREATE TRIGGER update_services_updated_at
    BEFORE UPDATE ON load_balancing.services
    FOR EACH ROW EXECUTE FUNCTION load_balancing.update_updated_at_column();

CREATE TRIGGER update_service_regions_updated_at
    BEFORE UPDATE ON load_balancing.service_regions
    FOR EACH ROW EXECUTE FUNCTION load_balancing.update_updated_at_column();

CREATE TRIGGER update_deployments_updated_at
    BEFORE UPDATE ON load_balancing.deployments
    FOR EACH ROW EXECUTE FUNCTION load_balancing.update_updated_at_column();
