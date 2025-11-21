import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  Outlet,
} from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import Navbar from "../components/Navbar/Navbar.jsx";
import Home from "../pages/Home/Home.jsx";
import Login from "../pages/Login/Login.jsx";
import LoginAdmin from "../pages/Login/LoginAdmin.jsx";
import KYCAdmin from "../pages/Dashboard/Admin/KYCAdmin/KYCAdmin.jsx";
import RegisterCVA from "../pages/Register/RegisterCVA.jsx";
import LoginCVA from "../pages/Login/LoginCVA.jsx";
import OTPCVA from "../pages/OTP/OTP_CVA.jsx";
import KYCCVA from "../pages/Dashboard/CVA/KYCCVA/KYCCVA.jsx";
import Register from "../pages/Register/Register.jsx";
import ForgotPassword from "../pages/ForgotPassword/ForgotPassword.jsx";
import AdminForgotPassword from "../pages/ForgotPassword/ForgotPasswordAdmin.jsx";
import AdminChangePassword from "../pages/ChangePasswordPage/ChangePasswordAdmin.jsx";
import CVAForgotPassword from "../pages/ForgotPassword/ForgotPasswordCVA.jsx";
import CVAChangePassword from "../pages/ChangePasswordPage/ChangePasswordCVA.jsx";
import ChangePassword from "../pages/ChangePassword/ChangePassword.jsx";
import ChangePasswordPage from "../pages/ChangePasswordPage/ChangePasswordPage.jsx";
import Marketplace from "../pages/Dashboard/Company/Marketplace/Marketplace.jsx";
import OTP from "../pages/OTP/OTP.jsx";
import Privacy from "../pages/Term&Privacy/Privacy.jsx";
import TermsOfUse from "../pages/Term&Privacy/TermsOfUse.jsx";
import ManageVehicle from "../pages/Dashboard/EVOwner/ManageVehicle/ManageVehicle.jsx";
import Wallet from "../pages/Wallet/Wallet.jsx";
import Deposit from "../pages/Wallet/Deposit/Deposit.jsx";
import Withdraw from "../pages/Wallet/Withdraw/Withdraw.jsx";
import WalletHistory from "../pages/Wallet/WalletHistory/WalletHistory.jsx";
import KYC from "../pages/Dashboard/EVOwner/KYC/KYC.jsx";
import Profile from "../pages/Dashboard/EVOwner/Profile/Profile.jsx";
import Order from "../pages/Dashboard/Company/Order/Order.jsx";
import PurchaseHistory from "../pages/Dashboard/Company/PurchaseHistory/PurchaseHistory.jsx";
import PaymentDetail from "../pages/PaymentDetail/PaymentDetail.jsx";
import KYCCompany from "../pages/Dashboard/Company/KYCCompany/KYCCompany.jsx";
import ProfileCompany from "../pages/Dashboard/Company/ProfileCompany/ProfileCompany.jsx";
import RoleRoute from "../components/RoleRoute.jsx";
import RegisterProject from "../pages/Dashboard/Company/Projects/RegisterProject.jsx";
import ListProjects from "../pages/Dashboard/Company/Projects/ListProjects.jsx";
import DetailProject from "../pages/Dashboard/Company/Projects/DetailProject.jsx";
import ViewRegisteredProject from "../pages/Dashboard/Company/Projects/ViewRegisteredProject.jsx";
import ListCredits from "../pages/Dashboard/Company/ManageCredits/ListCredits.jsx";
import UploadReport from "../pages/Dashboard/Company/Report/UploadReport.jsx";
import ChooseProjectToUpload from "../pages/Dashboard/Company/Report/ChooseProjectToUpload.jsx";
import DetailReport from "../pages/Dashboard/Company/Report/DetailReport.jsx";
import CreditDetail from "../pages/Dashboard/Company/ManageCredits/CreditDetail.jsx";
import CreditBatchDetail from "../pages/Wallet/components/CreditBatchDetail.jsx";
import RetireCredits from "../pages/Dashboard/Company/RetireCredits/RetireCredits.jsx";
import AIChatWidget from "../components/AI/AIChatWidget.jsx";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

//  import AdminApp
import AdminApp from "./AdminApp.jsx";
//  import AdminApp
import CVAApp from "./CVAApp.jsx";
import EvOwnersList from "../pages/Dashboard/Company/EVOwners/EVOwnersList.jsx";
import PayoutOwnerDetail from "../pages/Dashboard/Company/PayoutOwner/PayoutOwnerDetail.jsx";

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
  const { user } = useAuth();

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
          <Route path="/change-password" element={<ChangePasswordPage />} />
          <Route path="/upload-report" element={<ChooseProjectToUpload />} />
          <Route path="/upload-report/:projectId" element={<UploadReport />} />
          <Route path="/retire" element={<RetireCredits />} />

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
        <Route path="/list-credits" element={<ListCredits />} />
        <Route path="/detail-report/:id" element={<DetailReport />} />
        <Route path="/detail-credit/:id" element={<CreditDetail />} />
        <Route path="/wallet/credits/:id" element={<CreditBatchDetail />} />
        <Route path="/list-EVOwners" element={<EvOwnersList />} />
        <Route
          path="/payout/preview/:reportId"
          element={<PayoutOwnerDetail />}
        />
        <Route
          path="/payout/review/:distributionId"
          element={<PayoutOwnerDetail />}
        />

        <Route
          path="/view-registered-project/:id"
          element={<ViewRegisteredProject />}
        />
        {/*Route riêng cho admin và cva authorization, không Navbar */}

        <Route path="/admin/carbonX/mkp/login" element={<LoginAdmin />} />
        <Route
          path="/admin/kyc"
          element={
            <RoleRoute allowedRoles={["Admin"]}>
              <KYCAdmin />
            </RoleRoute>
          }
        />
        <Route
          path="/admin/forgot-password"
          element={<AdminForgotPassword />}
        />
        <Route
          path="/admin/change-password"
          element={<AdminChangePassword />}
        />
        <Route path="/cva/carbonX/mkp/register" element={<RegisterCVA />} />
        <Route path="/cva/otp" element={<OTPCVA />} />
        <Route path="/cva/carbonX/mkp/login" element={<LoginCVA />} />
        <Route
          path="/cva/kyc"
          element={
            <RoleRoute allowedRoles={["CVA"]}>
              <KYCCVA />
            </RoleRoute>
          }
        />
        <Route path="/cva/forgot-password" element={<CVAForgotPassword />} />
        <Route path="/cva/change-password" element={<CVAChangePassword />} />

        {/*Route riêng cho admin và cva, Navbar riêng */}

        {/* ADMIN ROUTES */}
        <Route
          path="/admin/*"
          element={
            <RoleRoute allowedRoles={["Admin"]}>
              <AdminApp />
            </RoleRoute>
          }
        />

        {/* CVA ROUTES */}
        <Route
          path="/cva/*"
          element={
            <RoleRoute allowedRoles={["CVA"]}>
              <CVAApp />
            </RoleRoute>
          }
        />

        {/*fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>

      {/* hiển thị AIChatWidget chỉ cho COMPANY */}
      {user?.role?.includes("COMPANY") && <AIChatWidget />}

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
