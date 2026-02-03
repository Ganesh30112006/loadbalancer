import { Routes, Route, Navigate } from 'react-router-dom';
import { useLoadBalancingStore } from './store/loadBalancingStore';
import DashboardPage from './pages/DashboardPage';
import AccountsPage from './pages/AccountsPage';
import BlueprintsPage from './pages/BlueprintsPage';
import PoliciesPage from './pages/PoliciesPage';
import ServicesPage from './pages/ServicesPage';
import ServiceDetailPage from './pages/ServiceDetailPage';
import ControlLoopPage from './pages/ControlLoopPage';
import DeploymentsPage from './pages/DeploymentsPage';
import AuditLogsPage from './pages/AuditLogsPage';
import { LoginPage } from './pages/LoginPage';
import { SignupPage } from './pages/SignupPage';
import { ModuleSelectionPage } from './pages/ModuleSelectionPage';
import { OnboardingPage } from './pages/OnboardingPage';
import { EmergencyControlsPage } from './pages/EmergencyControlsPage';
import { UserManagementPage } from './pages/UserManagementPage';
import { ProfilePage } from './pages/ProfilePage';

function App() {
  const { user, selectedModule } = useLoadBalancingStore();

  // If no user, show login/signup
  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  // If user but no module selected, show module selection
  if (!selectedModule) {
    return (
      <Routes>
        <Route path="/module-selection" element={<ModuleSelectionPage />} />
        <Route path="*" element={<Navigate to="/module-selection" replace />} />
      </Routes>
    );
  }

  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="/accounts" element={<AccountsPage />} />
      <Route path="/accounts/onboard" element={<OnboardingPage />} />
      <Route path="/blueprints" element={<BlueprintsPage />} />
      <Route path="/policies" element={<PoliciesPage />} />
      <Route path="/services" element={<ServicesPage />} />
      <Route path="/services/:serviceId" element={<ServiceDetailPage />} />
      <Route path="/deployments" element={<DeploymentsPage />} />
      <Route path="/control-loop" element={<ControlLoopPage />} />
      <Route path="/emergency" element={<EmergencyControlsPage />} />
      <Route path="/audit-logs" element={<AuditLogsPage />} />
      <Route path="/users" element={<UserManagementPage />} />
      <Route path="/profile" element={<ProfilePage />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;
