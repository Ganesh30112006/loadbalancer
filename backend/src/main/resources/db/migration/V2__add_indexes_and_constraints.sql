-- V2__add_indexes_and_constraints.sql

-- Additional indexes for common query patterns

-- For control loop service selection
CREATE INDEX idx_services_control_loop ON load_balancing.services(status, automation_enabled, scaling_enabled)
    WHERE status IN ('ACTIVE', 'DEGRADED') AND automation_enabled = TRUE;

-- For recent audit log queries
CREATE INDEX idx_audit_logs_service_recent ON load_balancing.audit_logs(service_id, created_at DESC);

-- For deployment history
CREATE INDEX idx_deployments_service_recent ON load_balancing.deployments(service_id, created_at DESC);

-- For blueprint/policy versioning
CREATE INDEX idx_blueprints_latest ON load_balancing.blueprints(name, aws_account_id, version DESC)
    WHERE status = 'ACTIVE';
    
CREATE INDEX idx_policies_latest ON load_balancing.policies(name, aws_account_id, version DESC)
    WHERE status = 'ACTIVE';

-- Add comments for documentation
COMMENT ON TABLE load_balancing.aws_accounts IS 'AWS accounts onboarded to the platform with STS AssumeRole configuration';
COMMENT ON TABLE load_balancing.blueprints IS 'Reusable application blueprints defining AMI, instance type, and launch configuration';
COMMENT ON TABLE load_balancing.policies IS 'SLO policies defining scaling rules, cost limits, and deployment strategies';
COMMENT ON TABLE load_balancing.services IS 'Services under platform control with automation settings';
COMMENT ON TABLE load_balancing.service_regions IS 'Per-region state for multi-region services';
COMMENT ON TABLE load_balancing.deployments IS 'Deployment history and active deployment state';
COMMENT ON TABLE load_balancing.audit_logs IS 'Comprehensive audit trail of all control plane decisions and actions';

-- Column comments
COMMENT ON COLUMN load_balancing.aws_accounts.external_id IS 'Platform-generated External ID for STS AssumeRole security';
COMMENT ON COLUMN load_balancing.services.automation_enabled IS 'Master switch for all platform automation on this service';
COMMENT ON COLUMN load_balancing.services.scaling_enabled IS 'Enable automatic scaling decisions';
COMMENT ON COLUMN load_balancing.audit_logs.cycle_id IS 'Control loop cycle identifier for correlating observe/analyze/execute phases';
