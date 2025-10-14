import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  Outlet,
} from "react-router-dom";
import { useAuth } from "./context/AuthContext.jsx";

import Navbar from "./components/Navbar/Navbar.jsx";
import Home from "./pages/Home/Home.jsx";
import Login from "./pages/Login/Login.jsx";
import Register from "./pages/Register/Register.jsx";
import ForgotPassword from "./pages/ForgotPassword/ForgotPassword.jsx";
import ChangePassword from "./pages/ChangePassword/ChangePassword.jsx";
import Marketplace from "./pages/Dashboard/Company/Marketplace/Marketplace.jsx";
import OTP from "./pages/OTP/OTP.jsx";
import Privacy from "./pages/Term&Privacy/Privacy.jsx";
import TermsOfUse from "./pages/Term&Privacy/TermsOfUse.jsx";
import ManageVehicle from "./pages/Dashboard/EVOwner/ManageVehicle/ManageVehicle.jsx";
import Wallet from "./pages/Wallet/Wallet.jsx";
import KYC from "./pages/KYC/KYC.jsx";
import Profile from "./pages/Profile/Profile.jsx";
import Order from "./pages/Dashboard/Company/Order/Order.jsx";
import PaymentDetail from "./pages/PaymentDetail/PaymentDetail.jsx";
import KYCCompany from "./pages/Dashboard/Company/KYCCompany/KYCCompany.jsx";
import ProfileCompany from "./pages/Dashboard/Company/ProfileCompany/ProfileCompany.jsx";
import RoleRoute from "./components/RoleRoute.jsx";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

//  import AdminApp
import AdminApp from "./AdminApp.jsx";

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
          <Route
            path="/kyc"
            element={
              <RoleRoute allowedRoles={["EV_OWNER", "USER"]}>
                <KYC />
              </RoleRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <RoleRoute allowedRoles={["EV_OWNER", "USER"]}>
                <Profile />
              </RoleRoute>
            }
          />

          <Route
            path="/kyc-company"
            element={
              <RoleRoute allowedRoles={["COMPANY"]}>
                <KYCCompany />
              </RoleRoute>
            }
          />

          <Route
            path="/profile-company"
            element={
              <RoleRoute allowedRoles={["COMPANY"]}>
                <ProfileCompany />
              </RoleRoute>
            }
          />
          <Route
            path="/wallet"
            element={
              <RequireAuth>
                <Wallet />
              </RequireAuth>
            }
          />
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
        <Route path="/order" element={<Order />} />
        <Route path="/payment-detail" element={<PaymentDetail />} />
        {/*Route riêng cho admin, không Navbar */}
        <Route path="/admin/*" element={<AdminApp />} />
        {/*fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <ToastContainer
        position="top-center"
        autoClose={2500}
        hideProgressBar={false}
        newestOnTop
        closeOnClick
        pauseOnHover
        theme="colored"
      />
    </BrowserRouter>
  );
}
