import { Suspense, lazy } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { LoaderCircle } from 'lucide-react'
import { DashboardLayout } from '@/app/layouts/DashboardLayout'
import { PublicLayout } from '@/app/layouts/PublicLayout'
import { RoleGuard } from '@/shared/guards/RoleGuard'
import { roleHome } from '@/shared/lib/roles'
import { ROLES, useAuthStore } from '@/shared/stores/auth.store'

const LoginPage = lazy(() => import('@/features/auth/LoginPage').then((m) => ({ default: m.LoginPage })))
const RegisterPage = lazy(() =>
  import('@/features/auth/RegisterPage').then((m) => ({ default: m.RegisterPage })),
)
const OtpPage = lazy(() => import('@/features/auth/OtpPage').then((m) => ({ default: m.OtpPage })))
const ForgotPasswordPage = lazy(() =>
  import('@/features/auth/ForgotPasswordPage').then((m) => ({ default: m.ForgotPasswordPage })),
)
const OAuthSuccessPage = lazy(() =>
  import('@/features/auth/OAuthSuccessPage').then((m) => ({ default: m.OAuthSuccessPage })),
)
const PublicHomePage = lazy(() =>
  import('@/pages/PublicHomePage').then((m) => ({ default: m.PublicHomePage })),
)
const MarketplacePage = lazy(() =>
  import('@/pages/MarketplacePage').then((m) => ({ default: m.MarketplacePage })),
)
const EvDashboardPage = lazy(() =>
  import('@/pages/EvDashboardPage').then((m) => ({ default: m.EvDashboardPage })),
)
const CompanyDashboardPage = lazy(() =>
  import('@/pages/CompanyDashboardPage').then((m) => ({ default: m.CompanyDashboardPage })),
)
const CvaDashboardPage = lazy(() =>
  import('@/pages/CvaDashboardPage').then((m) => ({ default: m.CvaDashboardPage })),
)
const AdminDashboardPage = lazy(() =>
  import('@/pages/AdminDashboardPage').then((m) => ({ default: m.AdminDashboardPage })),
)
const WalletPage = lazy(() => import('@/pages/WalletPage').then((m) => ({ default: m.WalletPage })))
const PayoutsPage = lazy(() =>
  import('@/pages/PayoutsPage').then((m) => ({ default: m.PayoutsPage })),
)
const EvKycPage = lazy(() => import('@/pages/ev/EvKycPage').then((m) => ({ default: m.EvKycPage })))
const VehiclesPage = lazy(() => import('@/pages/ev/VehiclesPage').then((m) => ({ default: m.VehiclesPage })))
const CompanyProjectsPage = lazy(() =>
  import('@/pages/company/CompanyProjectsPage').then((m) => ({ default: m.CompanyProjectsPage })),
)
const CompanyReportsPage = lazy(() =>
  import('@/pages/company/CompanyReportsPage').then((m) => ({ default: m.CompanyReportsPage })),
)
const CompanyCreditsPage = lazy(() =>
  import('@/pages/company/CompanyCreditsPage').then((m) => ({ default: m.CompanyCreditsPage })),
)
const CompanyOrdersPage = lazy(() =>
  import('@/pages/company/CompanyOrdersPage').then((m) => ({ default: m.CompanyOrdersPage })),
)
const CvaApplicationsPage = lazy(() =>
  import('@/pages/cva/CvaApplicationsPage').then((m) => ({ default: m.CvaApplicationsPage })),
)
const CvaReportsPage = lazy(() =>
  import('@/pages/cva/CvaReportsPage').then((m) => ({ default: m.CvaReportsPage })),
)
const CvaProjectsPage = lazy(() =>
  import('@/pages/cva/CvaProjectsPage').then((m) => ({ default: m.CvaProjectsPage })),
)
const CvaCompaniesPage = lazy(() =>
  import('@/pages/cva/CvaCompaniesPage').then((m) => ({ default: m.CvaCompaniesPage })),
)
const CvaCreditsPage = lazy(() =>
  import('@/pages/cva/CvaCreditsPage').then((m) => ({ default: m.CvaCreditsPage })),
)
const CvaProfilePage = lazy(() =>
  import('@/pages/cva/CvaProfilePage').then((m) => ({ default: m.CvaProfilePage })),
)
const AdminProjectsPage = lazy(() =>
  import('@/pages/admin/AdminProjectsPage').then((m) => ({ default: m.AdminProjectsPage })),
)
const AdminApplicationsPage = lazy(() =>
  import('@/pages/admin/AdminApplicationsPage').then((m) => ({ default: m.AdminApplicationsPage })),
)
const AdminCreditsPage = lazy(() =>
  import('@/pages/admin/AdminCreditsPage').then((m) => ({ default: m.AdminCreditsPage })),
)
const CompanyKycPage = lazy(() =>
  import('@/pages/company/CompanyKycPage').then((m) => ({ default: m.CompanyKycPage })),
)
const ChangePasswordPage = lazy(() =>
  import('@/pages/ChangePasswordPage').then((m) => ({ default: m.ChangePasswordPage })),
)
const AdminIssuancePage = lazy(() =>
  import('@/pages/admin/AdminIssuancePage').then((m) => ({ default: m.AdminIssuancePage })),
)
const AdminWithdrawalsPage = lazy(() =>
  import('@/pages/admin/AdminWithdrawalsPage').then((m) => ({ default: m.AdminWithdrawalsPage })),
)
const AdminUsersPage = lazy(() =>
  import('@/pages/admin/AdminUsersPage').then((m) => ({ default: m.AdminUsersPage })),
)
const AdminVehiclesPage = lazy(() =>
  import('@/pages/admin/AdminVehiclesPage').then((m) => ({ default: m.AdminVehiclesPage })),
)
const AdminTransactionsPage = lazy(() =>
  import('@/pages/admin/AdminTransactionsPage').then((m) => ({ default: m.AdminTransactionsPage })),
)
const AdminDirectoryPage = lazy(() =>
  import('@/pages/admin/AdminDirectoryPage').then((m) => ({ default: m.AdminDirectoryPage })),
)
const AdminProfilePage = lazy(() =>
  import('@/pages/admin/AdminProfilePage').then((m) => ({ default: m.AdminProfilePage })),
)
const TermsPage = lazy(() =>
  import('@/pages/TermsPrivacyPage').then((m) => ({ default: m.TermsPage })),
)
const PrivacyPage = lazy(() =>
  import('@/pages/TermsPrivacyPage').then((m) => ({ default: m.PrivacyPage })),
)
const CompanyEvOwnersPage = lazy(() =>
  import('@/pages/company/CompanyEvOwnersPage').then((m) => ({ default: m.CompanyEvOwnersPage })),
)
const NotFoundPage = lazy(() =>
  import('@/pages/NotFoundPage').then((m) => ({ default: m.NotFoundPage })),
)

function PageFallback() {
  return (
    <div className="grid min-h-[60vh] place-items-center" role="status" aria-label="Đang tải trang">
      <LoaderCircle aria-hidden className="size-8 animate-spin text-brand-600" />
    </div>
  )
}

function AppIndexRedirect() {
  const role = useAuthStore((state) => state.role)
  return <Navigate to={roleHome(role)} replace />
}

export function AppRouter() {
  return (
    <Suspense fallback={<PageFallback />}>
      <Routes>
        <Route element={<PublicLayout />}>
          <Route index element={<PublicHomePage />} />
          <Route path="marketplace" element={<MarketplacePage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="otp" element={<OtpPage />} />
          <Route path="forgot-password" element={<ForgotPasswordPage />} />
          <Route path="oauth-success" element={<OAuthSuccessPage />} />
          <Route path="terms" element={<TermsPage />} />
          <Route path="privacy" element={<PrivacyPage />} />
        </Route>

        <Route path="app" element={<DashboardLayout />}>
          <Route index element={<AppIndexRedirect />} />
          <Route
            path="ev"
            element={
              <RoleGuard allow={['EV_OWNER']}>
                <EvDashboardPage />
              </RoleGuard>
            }
          />
          <Route
            path="ev/kyc"
            element={
              <RoleGuard allow={['EV_OWNER']}>
                <EvKycPage />
              </RoleGuard>
            }
          />
          <Route
            path="ev/vehicles"
            element={
              <RoleGuard allow={['EV_OWNER']}>
                <VehiclesPage />
              </RoleGuard>
            }
          />
          <Route
            path="company"
            element={
              <RoleGuard allow={['COMPANY']}>
                <CompanyDashboardPage />
              </RoleGuard>
            }
          />
          <Route
            path="company/projects"
            element={
              <RoleGuard allow={['COMPANY']}>
                <CompanyProjectsPage />
              </RoleGuard>
            }
          />
          <Route
            path="company/reports"
            element={
              <RoleGuard allow={['COMPANY']}>
                <CompanyReportsPage />
              </RoleGuard>
            }
          />
          <Route
            path="company/credits"
            element={
              <RoleGuard allow={['COMPANY']}>
                <CompanyCreditsPage />
              </RoleGuard>
            }
          />
          <Route
            path="company/orders"
            element={
              <RoleGuard allow={['COMPANY']}>
                <CompanyOrdersPage />
              </RoleGuard>
            }
          />
          <Route
            path="company/ev-owners"
            element={
              <RoleGuard allow={['COMPANY']}>
                <CompanyEvOwnersPage />
              </RoleGuard>
            }
          />
          <Route
            path="company/payouts"
            element={
              <RoleGuard allow={['COMPANY']}>
                <PayoutsPage />
              </RoleGuard>
            }
          />
          <Route
            path="cva"
            element={
              <RoleGuard allow={['CVA']}>
                <CvaDashboardPage />
              </RoleGuard>
            }
          />
          <Route
            path="cva/applications"
            element={
              <RoleGuard allow={['CVA']}>
                <CvaApplicationsPage />
              </RoleGuard>
            }
          />
          <Route
            path="cva/reports"
            element={
              <RoleGuard allow={['CVA']}>
                <CvaReportsPage />
              </RoleGuard>
            }
          />
          <Route
            path="cva/projects"
            element={
              <RoleGuard allow={['CVA']}>
                <CvaProjectsPage />
              </RoleGuard>
            }
          />
          <Route
            path="cva/companies"
            element={
              <RoleGuard allow={['CVA']}>
                <CvaCompaniesPage />
              </RoleGuard>
            }
          />
          <Route
            path="cva/credits"
            element={
              <RoleGuard allow={['CVA']}>
                <CvaCreditsPage />
              </RoleGuard>
            }
          />
          <Route
            path="cva/profile"
            element={
              <RoleGuard allow={['CVA']}>
                <CvaProfilePage />
              </RoleGuard>
            }
          />
          <Route
            path="admin"
            element={
              <RoleGuard allow={['ADMIN']}>
                <AdminDashboardPage />
              </RoleGuard>
            }
          />
          <Route
            path="admin/projects"
            element={
              <RoleGuard allow={['ADMIN']}>
                <AdminProjectsPage />
              </RoleGuard>
            }
          />
          <Route
            path="admin/applications"
            element={
              <RoleGuard allow={['ADMIN']}>
                <AdminApplicationsPage />
              </RoleGuard>
            }
          />
          <Route
            path="admin/issuance"
            element={
              <RoleGuard allow={['ADMIN']}>
                <AdminIssuancePage />
              </RoleGuard>
            }
          />
          <Route
            path="admin/credits"
            element={
              <RoleGuard allow={['ADMIN']}>
                <AdminCreditsPage />
              </RoleGuard>
            }
          />
          <Route
            path="company/kyc"
            element={
              <RoleGuard allow={['COMPANY']}>
                <CompanyKycPage />
              </RoleGuard>
            }
          />
          <Route
            path="password"
            element={
              <RoleGuard allow={ROLES}>
                <ChangePasswordPage />
              </RoleGuard>
            }
          />
          <Route
            path="admin/withdrawals"
            element={
              <RoleGuard allow={['ADMIN']}>
                <AdminWithdrawalsPage />
              </RoleGuard>
            }
          />
          <Route
            path="admin/users"
            element={
              <RoleGuard allow={['ADMIN']}>
                <AdminUsersPage />
              </RoleGuard>
            }
          />
          <Route
            path="admin/vehicles"
            element={
              <RoleGuard allow={['ADMIN']}>
                <AdminVehiclesPage />
              </RoleGuard>
            }
          />
          <Route
            path="admin/transactions"
            element={
              <RoleGuard allow={['ADMIN']}>
                <AdminTransactionsPage />
              </RoleGuard>
            }
          />
          <Route
            path="admin/directory"
            element={
              <RoleGuard allow={['ADMIN']}>
                <AdminDirectoryPage />
              </RoleGuard>
            }
          />
          <Route
            path="admin/profile"
            element={
              <RoleGuard allow={['ADMIN']}>
                <AdminProfilePage />
              </RoleGuard>
            }
          />
          <Route
            path="wallet"
            element={
              <RoleGuard allow={['ADMIN', 'COMPANY', 'EV_OWNER']}>
                <WalletPage />
              </RoleGuard>
            }
          />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  )
}
