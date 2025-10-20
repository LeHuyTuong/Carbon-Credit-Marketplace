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
import LoginAdmin from "./pages/loginAdmin/Login.jsx";
import KYCAdmin from "./pages/KYCAdmin/KYC.jsx";
import Register from "./pages/Register/Register.jsx";
import ForgotPassword from "./pages/ForgotPassword/ForgotPassword.jsx";
import ChangePassword from "./pages/ChangePassword/ChangePassword.jsx";
import ChangePasswordForm from "./pages/ChangePasswordForm/ChangePasswordForm.jsx";
import Marketplace from "./pages/Dashboard/Company/Marketplace/Marketplace.jsx";
import OTP from "./pages/OTP/OTP.jsx";
import Privacy from "./pages/Term&Privacy/Privacy.jsx";
import TermsOfUse from "./pages/Term&Privacy/TermsOfUse.jsx";
import ManageVehicle from "./pages/Dashboard/EVOwner/ManageVehicle/ManageVehicle.jsx";
import Wallet from "./pages/Wallet/Wallet.jsx";
import Deposit from "./pages/Wallet/Deposit/Deposit.jsx";
import Withdraw from "./pages/Wallet/Withdraw/Withdraw.jsx";
import WalletHistory from "./pages/Wallet/WalletHistory/WalletHistory.jsx";
import KYC from "./pages/KYC/KYC.jsx";
import Profile from "./pages/Profile/Profile.jsx";
import Order from "./pages/Dashboard/Company/Order/Order.jsx";
import PurchaseHistory from "./pages/Dashboard/Company/PurchaseHistory/PurchaseHistory.jsx";
import PaymentDetail from "./pages/PaymentDetail/PaymentDetail.jsx";
import KYCCompany from "./pages/Dashboard/Company/KYCCompany/KYCCompany.jsx";
import ProfileCompany from "./pages/Dashboard/Company/ProfileCompany/ProfileCompany.jsx";
import RoleRoute from "./components/RoleRoute.jsx";
import RegisterProject from "./pages/Dashboard/Company/Projects/RegisterProject.jsx";
import ListProjects from "./pages/Dashboard/Company/Projects/ListProjects.jsx";
import DetailProject from "./pages/Dashboard/Company/Projects/DetailProject.jsx";
import ViewRegisteredProject from "./pages/Dashboard/Company/Projects/ViewRegisteredProject.jsx";
import ManageCredits from "./pages/Dashboard/Company/ManageCredits/ManageCredits.jsx";
import UploadReport from "./pages/Dashboard/Company/Report/UploadReport.jsx";
import DetailReport from "./pages/Dashboard/Company/Report/DetailReport.jsx";
import CreditDetail from "./pages/Dashboard/Company/ManageCredits/CreditDetail.jsx";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

//  import AdminApp
import AdminApp from "./AdminApp.jsx";
//  import AdminApp
import CVAApp from "./CVAApp.jsx";

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
          <Route path="/change-form" element={<ChangePasswordForm />} />
          <Route path="/upload-report" element={<UploadReport />} />

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
        <Route path="/purchase-history" element={<PurchaseHistory />} />
        <Route path="/payment-detail" element={<PaymentDetail />} />
        <Route path="/deposit" element={<Deposit />} />
        <Route path="/withdraw" element={<Withdraw />} />
        <Route path="/transaction-history" element={<WalletHistory />} />
        <Route path="/register-project" element={<RegisterProject />} />
        <Route path="/list-projects" element={<ListProjects />} />
        <Route path="/detail-project/:id" element={<DetailProject />} />
        <Route path="/manage-credits" element={<ManageCredits />} />
        <Route path="/detail-report/:id" element={<DetailReport />} />
        <Route path="/detail-credit/:id" element={<CreditDetail />} />
        <Route
          path="/view-registered-project/:id"
          element={<ViewRegisteredProject />}
        />
        {/*Route riêng cho admin, không Navbar */}

        <Route path="/admin/login" element={<LoginAdmin/>} />
        <Route path="/admin/kyc" element={<KYCAdmin/>}/>
        {/*Route riêng cho admin và cva, không Navbar */}

        <Route path="/admin/*" element={<AdminApp />} />
        <Route path="/cva/*" element={<CVAApp />} />
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
