import { useEffect, useState, type ReactNode } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  Bell,
  Building2,
  CalendarClock,
  Car,
  ClipboardCheck,
  ClipboardList,
  Coins,
  FileText,
  Folder,
  KeyRound,
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
  group: string
}

const NAV_BY_ROLE: Record<Role, NavItem[]> = {
  EV_OWNER: [
    { to: '/app/ev', label: 'Tổng quan', icon: LayoutDashboard, group: 'Cá nhân & Phương tiện' },
    { to: '/app/ev/kyc', label: 'Hồ sơ KYC', icon: UserRound, group: 'Cá nhân & Phương tiện' },
    { to: '/app/ev/vehicles', label: 'Xe điện', icon: Car, group: 'Cá nhân & Phương tiện' },
    { to: '/app/wallet', label: 'Ví của tôi', icon: Wallet, group: 'Tài chính & Bảo mật' },
    { to: '/app/password', label: 'Đổi mật khẩu', icon: KeyRound, group: 'Tài chính & Bảo mật' },
  ],
  COMPANY: [
    { to: '/app/company', label: 'Tổng quan', icon: LayoutDashboard, group: 'Vận hành & MRV' },
    { to: '/app/company/kyc', label: 'Hồ sơ doanh nghiệp', icon: Building2, group: 'Vận hành & MRV' },
    { to: '/app/company/projects', label: 'Dự án & hồ sơ', icon: Folder, group: 'Vận hành & MRV' },
    { to: '/app/company/ev-owners', label: 'Chủ xe tham gia', icon: Car, group: 'Vận hành & MRV' },
    { to: '/app/company/reports', label: 'Báo cáo phát thải', icon: FileText, group: 'Vận hành & MRV' },
    { to: '/app/company/credits', label: 'Kho tín chỉ', icon: Coins, group: 'Tín chỉ & Giao dịch' },
    { to: '/marketplace', label: 'Sàn giao dịch', icon: ShoppingCart, group: 'Tín chỉ & Giao dịch' },
    { to: '/app/company/orders', label: 'Đơn mua', icon: ShoppingCart, group: 'Tín chỉ & Giao dịch' },
    { to: '/app/company/payouts', label: 'Thanh toán', icon: ReceiptText, group: 'Tài chính & Bảo mật' },
    { to: '/app/wallet', label: 'Ví của tôi', icon: Wallet, group: 'Tài chính & Bảo mật' },
    { to: '/app/password', label: 'Đổi mật khẩu', icon: KeyRound, group: 'Tài chính & Bảo mật' },
  ],
  CVA: [
    { to: '/app/cva', label: 'Tổng quan', icon: ClipboardCheck, group: 'Thẩm định độc lập' },
    { to: '/app/cva/applications', label: 'Hồ sơ dự án', icon: ClipboardList, group: 'Thẩm định độc lập' },
    { to: '/app/cva/reports', label: 'Báo cáo phát thải', icon: FileText, group: 'Thẩm định độc lập' },
    { to: '/app/cva/projects', label: 'Dự án', icon: Folder, group: 'Tra cứu sổ đăng ký' },
    { to: '/app/cva/credits', label: 'Lô tín chỉ', icon: Coins, group: 'Tra cứu sổ đăng ký' },
    { to: '/app/cva/companies', label: 'Doanh nghiệp', icon: Building2, group: 'Tra cứu sổ đăng ký' },
    { to: '/app/cva/profile', label: 'Hồ sơ CVA', icon: UserRound, group: 'Tài khoản' },
    { to: '/app/password', label: 'Đổi mật khẩu', icon: KeyRound, group: 'Tài khoản' },
  ],
  ADMIN: [
    { to: '/app/admin', label: 'Tổng quan', icon: ShieldCheck, group: 'Thẩm định & Phát hành' },
    { to: '/app/admin/projects', label: 'Dự án', icon: Folder, group: 'Thẩm định & Phát hành' },
    { to: '/app/admin/applications', label: 'Duyệt hồ sơ cấp 2', icon: ClipboardCheck, group: 'Thẩm định & Phát hành' },
    { to: '/app/admin/issuance', label: 'Phát hành tín chỉ', icon: Coins, group: 'Thẩm định & Phát hành' },
    { to: '/app/admin/credits', label: 'Giám sát sàn & tín chỉ', icon: ReceiptText, group: 'Sàn & Tài chính' },
    { to: '/app/admin/transactions', label: 'Giao dịch & Sổ cái', icon: Landmark, group: 'Sàn & Tài chính' },
    { to: '/app/admin/withdrawals', label: 'Lệnh rút tiền', icon: Landmark, group: 'Sàn & Tài chính' },
    { to: '/app/wallet', label: 'Ví của tôi', icon: Wallet, group: 'Sàn & Tài chính' },
    { to: '/app/admin/users', label: 'Người dùng', icon: Users, group: 'Đối tượng & Hệ thống' },
    { to: '/app/admin/directory', label: 'Danh bạ KYC', icon: Building2, group: 'Đối tượng & Hệ thống' },
    { to: '/app/admin/vehicles', label: 'Quản lý xe điện', icon: Car, group: 'Đối tượng & Hệ thống' },
    { to: '/app/admin/profile', label: 'Hồ sơ Admin', icon: UserRound, group: 'Đối tượng & Hệ thống' },
    { to: '/app/password', label: 'Đổi mật khẩu', icon: KeyRound, group: 'Đối tượng & Hệ thống' },
  ],
}

const TITLES: Record<string, string> = {
  '/app/ev': 'Tổng quan chủ xe điện',
  '/app/ev/kyc': 'Hồ sơ định danh KYC',
  '/app/ev/vehicles': 'Quản lý xe điện',
  '/app/company': 'Tổng quan doanh nghiệp',
  '/app/company/kyc': 'Hồ sơ doanh nghiệp (KYC)',
  '/app/company/projects': 'Dự án & hồ sơ tham gia',
  '/app/company/reports': 'Báo cáo phát thải',
  '/app/company/credits': 'Kho tín chỉ & Triệt tiêu',
  '/app/company/orders': 'Đơn mua tín chỉ',
  '/app/company/ev-owners': 'Chủ xe điện tham gia & gom nhóm',
  '/app/company/payouts': 'Thanh toán & chia sẻ doanh thu',
  '/app/cva': 'Tổng quan thẩm định',
  '/app/cva/projects': 'Danh mục dự án (CVA)',
  '/app/cva/applications': 'Thẩm định hồ sơ dự án',
  '/app/cva/reports': 'Thẩm định báo cáo phát thải',
  '/app/cva/credits': 'Lô tín chỉ đã phát hành',
  '/app/cva/companies': 'Danh sách doanh nghiệp',
  '/app/cva/profile': 'Hồ sơ thẩm định viên CVA',
  '/app/admin': 'Tổng quan hệ thống',
  '/app/admin/projects': 'Quản lý dự án',
  '/app/admin/applications': 'Duyệt hồ sơ dự án (Cấp 2)',
  '/app/admin/issuance': 'Phê duyệt & phát hành tín chỉ',
  '/app/admin/credits': 'Giám sát sàn & tín chỉ',
  '/app/admin/transactions': 'Quản lý giao dịch & Sổ cái',
  '/app/admin/vehicles': 'Quản lý xe điện hệ thống',
  '/app/admin/withdrawals': 'Duyệt lệnh rút tiền',
  '/app/admin/users': 'Quản lý người dùng',
  '/app/admin/directory': 'Danh bạ KYC theo vai trò',
  '/app/admin/profile': 'Hồ sơ Quản trị viên',
  '/app/wallet': 'Ví của tôi',
  '/app/password': 'Đổi mật khẩu',
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

      <nav aria-label="Điều hướng dashboard" className="scrollbar-thin flex-1 space-y-4 overflow-y-auto px-3 pb-4">
        {Array.from(
          items.reduce((acc, item) => {
            const list = acc.get(item.group) ?? []
            list.push(item)
            acc.set(item.group, list)
            return acc
          }, new Map<string, NavItem[]>()),
        ).map(([group, groupItems]) => (
          <div key={group}>
            <p className="px-3 pb-1.5 text-[11px] font-bold uppercase tracking-wider text-ink-400">
              {group}
            </p>
            <div className="flex flex-col gap-0.5">
              {groupItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-brand-600 text-white shadow-sm'
                        : 'text-ink-600 hover:bg-ink-100 hover:text-ink-900',
                    )
                  }
                >
                  <item.icon aria-hidden className="size-4.5 shrink-0" />
                  <span className="truncate">{item.label}</span>
                </NavLink>
              ))}
            </div>
          </div>
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

            <nav aria-label="Breadcrumb" className="flex min-w-0 flex-1 items-center gap-2 text-sm">
              <span className="hidden text-ink-400 sm:inline">{role ? ROLE_LABELS[role] : 'CarbonX'}</span>
              <span aria-hidden className="hidden text-ink-300 sm:inline">/</span>
              <span className="truncate font-semibold text-ink-900">{title}</span>
            </nav>

            <Link
              to="/marketplace"
              className="hidden items-center gap-1.5 rounded-xl border border-ink-200 bg-white px-3 py-2 text-xs font-semibold text-ink-700 transition hover:border-brand-300 hover:text-brand-700 sm:inline-flex"
            >
              <ShoppingCart aria-hidden className="size-3.5" />
              Sàn giao dịch
            </Link>

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
