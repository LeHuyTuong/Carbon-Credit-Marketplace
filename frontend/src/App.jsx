import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';

import Navbar from './components/Navbar/Navbar.jsx';
import Home from './pages/Home/Home.jsx';
import Login from './pages/Login/Login.jsx';
import Register from './pages/Register/Register.jsx';
import ForgotPassword from './pages/ForgotPassword/ForgotPassword.jsx';
import ChangePassword from './pages/ChangePassword/ChangePassword.jsx';
import Marketplace from './pages/Market/Marketplace.jsx';
import OTP from './pages/OTP/OTP.jsx';
import Privacy from './pages/Term&Privacy/Privacy.jsx';
import TermsOfUse from './pages/Term&Privacy/TermsOfUse.jsx';
import ManageVehicle from './pages/Dashboard/EVOwner/ManageVehicle/ManageVehicle.jsx';
import KYC from './pages/KYC/KYC.jsx';

function Layout() {
  return (
    <>
      <Navbar />
      <Outlet />
    </>
  );
}

//chặn các trang cần login
function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/home" replace />} />

        {/*routes có navbar */}
        <Route element={<Layout />}>
          <Route path="/home" element={<Home />} />
          <Route path="/marketplace" element={<Marketplace />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/otp" element={<OTP />} />
          <Route path="/forgot" element={<ForgotPassword />} />
          <Route path="/change" element={<ChangePassword />} />
          <Route path="/kyc" element={<KYC />} />
          <Route
            path="/managevehicle"
            element={
              <RequireAuth>
                <ManageVehicle />
              </RequireAuth>
            }
          />
        </Route>

        {/*routes không có navbar */}
        <Route path="/privacy" element={<Privacy />} />
        <Route path="/terms" element={<TermsOfUse />} />

        {/*fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
