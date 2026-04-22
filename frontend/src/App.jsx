import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppStateProvider } from './hooks/useAppState';
import Layout from './components/Layout';
import LandingPage from './pages/LandingPage';
import CreateEventPage from './pages/CreateEventPage';
import PublicEventPage from './pages/PublicEventPage';
import PublicResultsPage from './pages/PublicResultsPage';
import HostWorkspacePage from './pages/HostWorkspacePage';
import DashboardPage from './pages/DashboardPage';
import SettingsPage from './pages/SettingsPage';
import PrivacyPage from './pages/PrivacyPage';
import TermsPage from './pages/TermsPage';
import NotFoundPage from './pages/NotFoundPage';

export default function App() {
  return (
    <BrowserRouter>
      <AppStateProvider>
        <Layout>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/create" element={<CreateEventPage />} />
            <Route path="/e/:publicId" element={<PublicEventPage />} />
            <Route path="/e/:publicId/results" element={<PublicResultsPage />} />
            <Route path="/host/:hostToken" element={<HostWorkspacePage />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/privacy" element={<PrivacyPage />} />
            <Route path="/terms" element={<TermsPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Layout>
      </AppStateProvider>
    </BrowserRouter>
  );
}
