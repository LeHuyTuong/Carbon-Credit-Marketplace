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
            path="company"
            element={
              <RoleGuard allow={['COMPANY']}>
                <CompanyDashboardPage />
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
            path="admin"
            element={
              <RoleGuard allow={['ADMIN']}>
                <AdminDashboardPage />
              </RoleGuard>
            }
          />
          <Route
            path="wallet"
            element={
              <RoleGuard allow={ROLES}>
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
