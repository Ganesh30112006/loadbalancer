import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  CloudIcon,
  ClipboardDocumentIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon,
  ArrowRightIcon,
  ArrowLeftIcon,
  ShieldCheckIcon,
  ServerStackIcon,
  EyeIcon
} from '@heroicons/react/24/outline';
import { Layout } from '../components/Layout';

interface OnboardingStep {
  id: number;
  title: string;
  description: string;
}

const steps: OnboardingStep[] = [
  { id: 1, title: 'AWS Account', description: 'Enter account details' },
  { id: 2, title: 'IAM Role', description: 'Create cross-account role' },
  { id: 3, title: 'Validate', description: 'Verify connection' },
  { id: 4, title: 'Discovery', description: 'Scan resources' }
];

export function OnboardingPage() {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(1);
  const [accountName, setAccountName] = useState('');
  const [accountId, setAccountId] = useState('');
  const [roleArn, setRoleArn] = useState('');
  const [externalId] = useState(() => `cloud-cp-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`);
  const [validating, setValidating] = useState(false);
  const [validated, setValidated] = useState(false);
  const [scanning, setScanning] = useState(false);
  const [discoveredResources, setDiscoveredResources] = useState<{type: string; count: number}[]>([]);
  const [copied, setCopied] = useState(false);

  const handleCopyExternalId = () => {
    navigator.clipboard.writeText(externalId);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleValidate = async () => {
    setValidating(true);
    // Simulate validation
    await new Promise(resolve => setTimeout(resolve, 2000));
    setValidated(true);
    setValidating(false);
  };

  const handleScan = async () => {
    setScanning(true);
    // Simulate resource discovery
    await new Promise(resolve => setTimeout(resolve, 3000));
    setDiscoveredResources([
      { type: 'Auto Scaling Groups', count: 12 },
      { type: 'Load Balancers', count: 6 },
      { type: 'Target Groups', count: 18 },
      { type: 'Launch Templates', count: 8 },
      { type: 'EC2 Instances', count: 47 }
    ]);
    setScanning(false);
  };

  const handleComplete = () => {
    navigate('/accounts');
  };

  const canProceed = () => {
    switch (currentStep) {
      case 1: return accountName && accountId && accountId.length === 12;
      case 2: return roleArn && roleArn.includes('arn:aws:iam::');
      case 3: return validated;
      case 4: return discoveredResources.length > 0;
      default: return false;
    }
  };

  return (
    <Layout>
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-white">Onboard AWS Account</h1>
          <p className="mt-2 text-slate-400">
            Connect your AWS account to enable infrastructure management
          </p>
        </div>

        {/* Progress Steps */}
        <div className="mb-8">
          <div className="flex items-center">
            {steps.map((step, idx) => (
              <div key={step.id} className="flex items-center">
                <div className={`flex items-center ${idx !== 0 ? 'ml-4' : ''}`}>
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                    step.id < currentStep
                      ? 'bg-green-600 text-white'
                      : step.id === currentStep
                        ? 'bg-indigo-600 text-white'
                        : 'bg-slate-700 text-slate-400'
                  }`}>
                    {step.id < currentStep ? (
                      <CheckCircleIcon className="h-5 w-5" />
                    ) : (
                      step.id
                    )}
                  </div>
                  <div className="ml-2">
                    <p className={`text-sm font-medium ${
                      step.id <= currentStep ? 'text-white' : 'text-slate-500'
                    }`}>
                      {step.title}
                    </p>
                    <p className="text-xs text-slate-500">{step.description}</p>
                  </div>
                </div>
                {idx < steps.length - 1 && (
                  <div className={`h-0.5 w-12 mx-4 ${
                    step.id < currentStep ? 'bg-green-600' : 'bg-slate-700'
                  }`} />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Step Content */}
        <div className="bg-slate-800 border border-slate-700 rounded-xl p-6">
          {currentStep === 1 && (
            <div className="space-y-6">
              <div className="flex items-center gap-3 text-amber-400 bg-amber-900/20 border border-amber-700/50 rounded-lg p-4">
                <EyeIcon className="h-5 w-5 flex-shrink-0" />
                <p className="text-sm">
                  Initial connection is <strong>read-only</strong>. You'll be able to enable automation later after reviewing discovered resources.
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  Account Name
                </label>
                <input
                  type="text"
                  value={accountName}
                  onChange={(e) => setAccountName(e.target.value)}
                  placeholder="e.g., Production, Staging"
                  className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  AWS Account ID
                </label>
                <input
                  type="text"
                  value={accountId}
                  onChange={(e) => setAccountId(e.target.value.replace(/\D/g, '').slice(0, 12))}
                  placeholder="123456789012"
                  className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono"
                />
                <p className="mt-1 text-xs text-slate-500">
                  12-digit AWS account identifier
                </p>
              </div>
            </div>
          )}

          {currentStep === 2 && (
            <div className="space-y-6">
              <div className="bg-slate-700/50 rounded-lg p-4">
                <h3 className="text-sm font-medium text-white mb-3">
                  1. Create IAM Role in your AWS account
                </h3>
                <p className="text-sm text-slate-400 mb-4">
                  Create a cross-account IAM role with the following trust policy and external ID:
                </p>
                
                <div className="bg-slate-900 rounded-lg p-4 font-mono text-sm">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-slate-400">External ID (required)</span>
                    <button
                      onClick={handleCopyExternalId}
                      className="flex items-center gap-1 text-indigo-400 hover:text-indigo-300"
                    >
                      <ClipboardDocumentIcon className="h-4 w-4" />
                      {copied ? 'Copied!' : 'Copy'}
                    </button>
                  </div>
                  <div className="text-green-400 break-all">{externalId}</div>
                </div>
              </div>

              <div className="bg-slate-700/50 rounded-lg p-4">
                <h3 className="text-sm font-medium text-white mb-3">
                  2. Required IAM Permissions
                </h3>
                <ul className="text-sm text-slate-400 space-y-1">
                  <li className="flex items-center gap-2">
                    <CheckCircleIcon className="h-4 w-4 text-green-400" />
                    <code>autoscaling:Describe*</code> - Read ASG configurations
                  </li>
                  <li className="flex items-center gap-2">
                    <CheckCircleIcon className="h-4 w-4 text-green-400" />
                    <code>elasticloadbalancing:Describe*</code> - Read load balancers
                  </li>
                  <li className="flex items-center gap-2">
                    <CheckCircleIcon className="h-4 w-4 text-green-400" />
                    <code>ec2:Describe*</code> - Read EC2 & launch templates
                  </li>
                  <li className="flex items-center gap-2">
                    <CheckCircleIcon className="h-4 w-4 text-green-400" />
                    <code>cloudwatch:GetMetricData</code> - Read metrics
                  </li>
                </ul>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  3. Enter the IAM Role ARN
                </label>
                <input
                  type="text"
                  value={roleArn}
                  onChange={(e) => setRoleArn(e.target.value)}
                  placeholder="arn:aws:iam::123456789012:role/CloudControlPlaneRole"
                  className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono text-sm"
                />
              </div>
            </div>
          )}

          {currentStep === 3 && (
            <div className="space-y-6">
              <div className="text-center py-8">
                {validating ? (
                  <div className="space-y-4">
                    <div className="h-16 w-16 border-4 border-indigo-600/30 border-t-indigo-600 rounded-full animate-spin mx-auto" />
                    <p className="text-slate-400">Validating IAM role and permissions...</p>
                  </div>
                ) : validated ? (
                  <div className="space-y-4">
                    <div className="h-16 w-16 bg-green-600 rounded-full flex items-center justify-center mx-auto">
                      <CheckCircleIcon className="h-10 w-10 text-white" />
                    </div>
                    <p className="text-green-400 font-medium">Connection validated successfully!</p>
                    <p className="text-sm text-slate-400">
                      All required permissions verified for account {accountId}
                    </p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div className="h-16 w-16 bg-slate-700 rounded-full flex items-center justify-center mx-auto">
                      <ShieldCheckIcon className="h-10 w-10 text-slate-400" />
                    </div>
                    <p className="text-slate-400">Ready to validate your IAM role</p>
                    <button
                      onClick={handleValidate}
                      className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
                    >
                      Validate Connection
                    </button>
                  </div>
                )}
              </div>

              {validated && (
                <div className="bg-green-900/20 border border-green-700/50 rounded-lg p-4">
                  <h4 className="text-sm font-medium text-green-400 mb-2">Verified Permissions</h4>
                  <div className="grid grid-cols-2 gap-2 text-sm">
                    <div className="flex items-center gap-2 text-slate-300">
                      <CheckCircleIcon className="h-4 w-4 text-green-400" />
                      Auto Scaling (Read)
                    </div>
                    <div className="flex items-center gap-2 text-slate-300">
                      <CheckCircleIcon className="h-4 w-4 text-green-400" />
                      Load Balancing (Read)
                    </div>
                    <div className="flex items-center gap-2 text-slate-300">
                      <CheckCircleIcon className="h-4 w-4 text-green-400" />
                      EC2 (Read)
                    </div>
                    <div className="flex items-center gap-2 text-slate-300">
                      <CheckCircleIcon className="h-4 w-4 text-green-400" />
                      CloudWatch (Read)
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {currentStep === 4 && (
            <div className="space-y-6">
              {!scanning && discoveredResources.length === 0 && (
                <div className="text-center py-8">
                  <div className="h-16 w-16 bg-slate-700 rounded-full flex items-center justify-center mx-auto">
                    <CloudIcon className="h-10 w-10 text-slate-400" />
                  </div>
                  <p className="mt-4 text-slate-400">Ready to discover resources in {accountName}</p>
                  <button
                    onClick={handleScan}
                    className="mt-4 px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
                  >
                    Start Discovery
                  </button>
                </div>
              )}

              {scanning && (
                <div className="text-center py-8 space-y-4">
                  <div className="h-16 w-16 border-4 border-indigo-600/30 border-t-indigo-600 rounded-full animate-spin mx-auto" />
                  <p className="text-slate-400">Scanning AWS resources...</p>
                  <p className="text-sm text-slate-500">This may take a few moments</p>
                </div>
              )}

              {discoveredResources.length > 0 && (
                <div className="space-y-4">
                  <div className="flex items-center gap-2 text-green-400">
                    <CheckCircleIcon className="h-5 w-5" />
                    <span className="font-medium">Discovery Complete</span>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    {discoveredResources.map((resource) => (
                      <div
                        key={resource.type}
                        className="bg-slate-700/50 rounded-lg p-4 flex items-center gap-3"
                      >
                        <ServerStackIcon className="h-8 w-8 text-indigo-400" />
                        <div>
                          <p className="text-2xl font-bold text-white">{resource.count}</p>
                          <p className="text-sm text-slate-400">{resource.type}</p>
                        </div>
                      </div>
                    ))}
                  </div>

                  <div className="bg-amber-900/20 border border-amber-700/50 rounded-lg p-4">
                    <div className="flex items-center gap-2 text-amber-400 mb-2">
                      <ExclamationTriangleIcon className="h-5 w-5" />
                      <span className="font-medium">Next Steps</span>
                    </div>
                    <p className="text-sm text-slate-400">
                      Resources have been discovered in <strong>read-only mode</strong>. 
                      You can review them in the Services page and explicitly enable 
                      automation for individual resources when ready.
                    </p>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Navigation */}
          <div className="flex items-center justify-between mt-8 pt-6 border-t border-slate-700">
            <button
              onClick={() => setCurrentStep(Math.max(1, currentStep - 1))}
              disabled={currentStep === 1}
              className="flex items-center gap-2 px-4 py-2 text-slate-400 hover:text-white disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ArrowLeftIcon className="h-4 w-4" />
              Back
            </button>

            {currentStep < 4 ? (
              <button
                onClick={() => setCurrentStep(currentStep + 1)}
                disabled={!canProceed()}
                className="flex items-center gap-2 px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Continue
                <ArrowRightIcon className="h-4 w-4" />
              </button>
            ) : (
              <button
                onClick={handleComplete}
                disabled={!canProceed()}
                className="flex items-center gap-2 px-6 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <CheckCircleIcon className="h-5 w-5" />
                Complete Onboarding
              </button>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
