import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import ProtectedRoute from './components/ProtectedRoute';

import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import TrackShipment from './pages/TrackShipment';
import Dashboard from './pages/Dashboard';
import Shipments from './pages/Shipments';
import NewShipment from './pages/NewShipment';
import ShipmentDetail from './pages/ShipmentDetail';
import LiveMap from './pages/LiveMap';
import PodQueue from './pages/PodQueue';
import Notifications from './pages/Notifications';
import Analytics from './pages/Analytics';
import Reports from './pages/Reports';
import AdminUsers from './pages/AdminUsers';
import Profile from './pages/Profile';
import NotFound from './pages/NotFound';

const STAFF = ['ADMIN', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT'];

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/track" element={<TrackShipment />} />
      <Route path="/track/:trackingNumber" element={<TrackShipment />} />

      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/shipments" element={<Shipments />} />
        <Route path="/shipments/new" element={<NewShipment />} />
        <Route path="/shipments/:id" element={<ShipmentDetail />} />
        <Route
          path="/live"
          element={
            <ProtectedRoute roles={STAFF}>
              <LiveMap />
            </ProtectedRoute>
          }
        />
        <Route
          path="/pod"
          element={
            <ProtectedRoute roles={STAFF}>
              <PodQueue />
            </ProtectedRoute>
          }
        />
        <Route path="/analytics" element={<Analytics />} />
        <Route path="/reports" element={<Reports />} />
        <Route path="/notifications" element={<Notifications />} />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute roles={['ADMIN', 'SUPPORT_AGENT']}>
              <AdminUsers />
            </ProtectedRoute>
          }
        />
        <Route path="/profile" element={<Profile />} />
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
