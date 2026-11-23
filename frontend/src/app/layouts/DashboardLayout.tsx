import { useEffect, useState, type ReactNode } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  Bell,
  CalendarClock,
  Car,
  ClipboardCheck,
  ClipboardList,
  Coins,
  FileText,
  Folder,
  Landmark,
  LayoutDashboard,
  Leaf,
  LogOut,
  Menu,
  ReceiptText,
  ShieldCheck,
  ShoppingCart,
  UserRound,
  Users,
  Wallet,
  X,
  type LucideIcon,
} from 'lucide-react'
import { AiChatWidget } from '@/features/ai-assistant/AiChatWidget'
import { useNotificationsSse } from '@/shared/api/sse'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { cn } from '@/shared/lib/cn'
import { formatDate } from '@/shared/lib/format'
import { ROLE_LABELS, roleHome } from '@/shared/lib/roles'
import { useAuthStore, type Role } from '@/shared/stores/auth.store'

interface NavItem {
  to: string
  label: string
  icon: LucideIcon
}

const NAV_BY_ROLE: Record<Role, NavItem[]> = {
  EV_OWNER: [
    { to: '/app/ev', label: 'Tổng quan', icon: LayoutDashboard },
    { to: '/app/ev/kyc', label: 'Hồ sơ KYC', icon: UserRound },
    { to: '/app/ev/vehicles', label: 'Xe điện', icon: Car },
    { to: '/app/wallet', label: 'Ví của tôi', icon: Wallet },
  ],
  COMPANY: [
    { to: '/app/company', label: 'Tổng quan', icon: LayoutDashboard },
    { to: '/app/company/projects', label: 'Dự án & hồ sơ', icon: Folder },
    { to: '/app/company/reports', label: 'Báo cáo phát thải', icon: FileText },
    { to: '/app/company/credits', label: 'Kho tín chỉ', icon: Coins },
    { to: '/app/company/orders', label: 'Đơn mua', icon: ShoppingCart },
    { to: '/app/company/payouts', label: 'Thanh toán', icon: ReceiptText },
    { to: '/app/wallet', label: 'Ví của tôi', icon: Wallet },
  ],
  CVA: [
    { to: '/app/cva', label: 'Tổng quan', icon: ClipboardCheck },
    { to: '/app/cva/applications', label: 'Hồ sơ dự án', icon: ClipboardList },
    { to: '/app/cva/reports', label: 'Báo cáo phát thải', icon: FileText },
    { to: '/app/wallet', label: 'Ví của tôi', icon: Wallet },
  ],
  ADMIN: [
    { to: '/app/admin', label: 'Tổng quan', icon: ShieldCheck },
    { to: '/app/admin/projects', label: 'Dự án', icon: Folder },
    { to: '/app/admin/issuance', label: 'Phát hành tín chỉ', icon: Coins },
    { to: '/app/admin/withdrawals', label: 'Lệnh rút tiền', icon: Landmark },
    { to: '/app/admin/users', label: 'Người dùng', icon: Users },
    { to: '/app/wallet', label: 'Ví của tôi', icon: Wallet },
  ],
}

const TITLES: Record<string, string> = {
  '/app/ev': 'Tổng quan chủ xe điện',
  '/app/ev/kyc': 'Hồ sơ định danh KYC',
  '/app/ev/vehicles': 'Quản lý xe điện',
  '/app/company': 'Tổng quan doanh nghiệp',
  '/app/company/projects': 'Dự án & hồ sơ tham gia',
  '/app/company/reports': 'Báo cáo phát thải',
  '/app/company/credits': 'Kho tín chỉ',
  '/app/company/orders': 'Đơn mua tín chỉ',
  '/app/company/payouts': 'Thanh toán & báo cáo',
  '/app/cva': 'Tổng quan thẩm định',
  '/app/cva/applications': 'Thẩm định hồ sơ dự án',
  '/app/cva/reports': 'Thẩm định báo cáo phát thải',
  '/app/admin': 'Tổng quan hệ thống',
  '/app/admin/projects': 'Quản lý dự án',
  '/app/admin/issuance': 'Phê duyệt & phát hành tín chỉ',
  '/app/admin/withdrawals': 'Duyệt lệnh rút tiền',
  '/app/admin/users': 'Quản lý người dùng',
  '/app/wallet': 'Ví của tôi',
}

function SidebarContent({
  items,
  role,
  email,
  onLogout,
}: {
  items: NavItem[]
  role: Role | null
  email: string
  onLogout: () => void
}) {
  return (
    <div className="flex h-full flex-col">
      <Link to={roleHome(role)} className="flex items-center gap-2.5 px-5 py-5">
        <span className="grid size-9 place-items-center rounded-xl bg-brand-600 text-white">
          <Leaf aria-hidden className="size-5" />
        </span>
        <span className="text-lg font-extrabold tracking-tight text-ink-900">
          Carbon<span className="text-brand-600">X</span>
        </span>
      </Link>

      {role && (
        <div className="mx-4 mb-5 rounded-xl bg-brand-50 px-3 py-2 text-xs font-semibold text-brand-700">
          {ROLE_LABELS[role]}
        </div>
      )}

      <nav aria-label="Điều hướng dashboard" className="flex flex-col gap-1 px-3">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-brand-600 text-white shadow-sm'
                  : 'text-ink-600 hover:bg-ink-100 hover:text-ink-900',
              )
            }
          >
            <item.icon aria-hidden className="size-5 shrink-0" />
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="mt-auto border-t border-ink-100 p-4">
        <div className="mb-3 truncate text-sm font-medium text-ink-800" title={email}>
          {email || 'Người dùng CarbonX'}
        </div>
        <Button variant="outline" size="sm" fullWidth leftIcon={<LogOut className="size-4" />} onClick={onLogout}>
          Đăng xuất
        </Button>
      </div>
    </div>
  )
}

function NotificationPanel({ children }: { children: ReactNode }) {
  return (
    <div className="absolute right-0 top-12 z-50 w-[min(22rem,calc(100vw-2rem))] overflow-hidden rounded-2xl border border-ink-200 bg-white shadow-float">
      {children}
    </div>
  )
}

export function DashboardLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [notifOpen, setNotifOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const role = useAuthStore((state) => state.role)
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const { notifications, connected, unreadCount, markAllRead, clear } = useNotificationsSse()

  useEffect(() => {
    setDrawerOpen(false)
    setNotifOpen(false)
  }, [location.pathname])

  const items = role ? NAV_BY_ROLE[role] : []
  const title = TITLES[location.pathname] ?? 'CarbonX Dashboard'

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen bg-ink-50">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-ink-200 bg-white lg:block">
        <SidebarContent items={items} role={role} email={user?.email ?? ''} onLogout={handleLogout} />
      </aside>

      {drawerOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            aria-label="Đóng menu điều hướng"
            className="absolute inset-0 bg-ink-950/40 backdrop-blur-sm"
            onClick={() => setDrawerOpen(false)}
          />
          <aside className="absolute inset-y-0 left-0 w-72 max-w-[85vw] bg-white shadow-float">
            <button
              type="button"
              onClick={() => setDrawerOpen(false)}
              aria-label="Đóng menu"
              className="absolute right-3 top-4 z-10 grid size-9 place-items-center rounded-lg text-ink-500 hover:bg-ink-100"
            >
              <X aria-hidden className="size-5" />
            </button>
            <SidebarContent items={items} role={role} email={user?.email ?? ''} onLogout={handleLogout} />
          </aside>
        </div>
      )}

      <div className="min-w-0 lg:pl-64">
        <header className="sticky top-0 z-20 border-b border-ink-200 bg-white/90 backdrop-blur-lg">
          <div className="mx-auto flex h-16 max-w-7xl items-center gap-3 px-4 sm:px-6 lg:px-8">
            <button
              type="button"
              onClick={() => setDrawerOpen(true)}
              aria-label="Mở menu điều hướng"
              className="grid size-10 shrink-0 place-items-center rounded-xl border border-ink-200 text-ink-700 lg:hidden"
            >
              <Menu aria-hidden className="size-5" />
            </button>

            <div className="min-w-0 flex-1">
              <h1 className="truncate text-base font-semibold text-ink-900 sm:text-lg">{title}</h1>
              <p className="hidden text-xs text-ink-500 sm:block">CarbonX Workspace</p>
            </div>

            <div className="relative">
              <button
                type="button"
                onClick={() => setNotifOpen((open) => !open)}
                aria-label="Thông báo"
                aria-expanded={notifOpen}
                className="relative grid size-10 place-items-center rounded-xl border border-ink-200 text-ink-600 transition hover:border-brand-300 hover:text-brand-600"
              >
                <Bell aria-hidden className="size-5" />
                {unreadCount > 0 && (
                  <span className="absolute -right-1 -top-1 grid min-w-5 place-items-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white">
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </button>

              {notifOpen && (
                <NotificationPanel>
                  <div className="flex items-center justify-between border-b border-ink-100 px-4 py-3">
                    <div>
                      <p className="text-sm font-semibold text-ink-900">Thông báo</p>
                      <p className="text-xs text-ink-500">
                        {connected ? 'Đang kết nối realtime' : 'Đang kết nối lại…'}
                      </p>
                    </div>
                    <span className={cn('size-2 rounded-full', connected ? 'bg-brand-500' : 'bg-amber-400')} />
                  </div>

                  {notifications.length === 0 ? (
                    <EmptyState
                      icon={CalendarClock}
                      title="Chưa có thông báo"
                      description="Thông báo mới sẽ hiện tại đây theo thời gian thực."
                      className="py-10"
                    />
                  ) : (
                    <ul className="scrollbar-thin max-h-80 divide-y divide-ink-100 overflow-y-auto">
                      {notifications.map((item) => (
                        <li key={item.id} className={cn('px-4 py-3', !item.read && 'bg-brand-50/50')}>
                          <p className="text-sm font-semibold text-ink-900">{item.title}</p>
                          {item.message && <p className="mt-0.5 text-xs text-ink-600">{item.message}</p>}
                          <p className="mt-1 text-[11px] text-ink-400">{formatDate(item.createdAt)}</p>
                        </li>
                      ))}
                    </ul>
                  )}

                  {notifications.length > 0 && (
                    <div className="flex items-center justify-between border-t border-ink-100 px-4 py-2.5">
                      <button
                        type="button"
                        onClick={markAllRead}
                        className="text-xs font-semibold text-brand-600 hover:text-brand-700"
                      >
                        Đánh dấu đã đọc
                      </button>
                      <button
                        type="button"
                        onClick={clear}
                        className="text-xs font-medium text-ink-500 hover:text-ink-800"
                      >
                        Xoá danh sách
                      </button>
                    </div>
                  )}
                </NotificationPanel>
              )}
            </div>

            <Button
              variant="ghost"
              size="sm"
              onClick={handleLogout}
              leftIcon={<LogOut aria-hidden className="size-4" />}
              className="hidden sm:inline-flex"
            >
              Đăng xuất
            </Button>
          </div>
        </header>

        <main className="mx-auto w-full min-w-0 max-w-[1680px] px-4 py-6 sm:px-6 lg:px-10">
          <Outlet />
        </main>
      </div>

      <AiChatWidget />
    </div>
  )
}
