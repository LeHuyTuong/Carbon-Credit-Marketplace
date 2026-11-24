import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { LogIn, Menu, X } from 'lucide-react'
import { cn } from '@/shared/lib/cn'
import { useAuthStore } from '@/shared/stores/auth.store'
import { roleHome } from '@/shared/lib/roles'

const links = [
  { to: '/', label: 'Trang chủ', end: true },
  { to: '/marketplace', label: 'Sàn giao dịch' },
  { to: '/#du-an', label: 'Dự án' },
]

export function PublicLayout() {
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const token = useAuthStore((state) => state.token)
  const role = useAuthStore((state) => state.role)

  useEffect(() => setMenuOpen(false), [location.pathname])

  return (
    <div className="flex min-h-screen flex-col bg-white">
      <header className="sticky top-0 z-40 border-b border-ink-200 bg-white/95 backdrop-blur-md">
        <div className="mx-auto flex h-16 w-full max-w-[1680px] items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
          <Link to="/" className="flex min-h-10 items-center gap-2.5" aria-label="CarbonX trang chủ">
            <img
              src="/images/logo.png"
              alt="CarbonX"
              className="size-8 rounded-lg object-contain"
            />
            <span className="text-lg font-extrabold tracking-tight text-ink-900">
              Carbon<span className="text-brand-600">X</span>
            </span>
          </Link>

          <nav aria-label="Điều hướng chính" className="hidden items-center gap-1 md:flex">
            {links.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                className={({ isActive }) =>
                  cn(
                    'rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                    isActive && link.to !== '/#du-an'
                      ? 'bg-brand-50 text-brand-700'
                      : 'text-ink-600 hover:bg-ink-50 hover:text-ink-900',
                  )
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>

          <div className="hidden items-center gap-3 md:flex">
            {token && role ? (
              <Link
                to={roleHome(role)}
                className="inline-flex h-10 items-center gap-2 rounded-xl bg-ink-900 px-4 text-sm font-semibold text-white transition hover:bg-ink-800"
              >
                Vào dashboard
              </Link>
            ) : (
              <Link
                to="/login"
                className="inline-flex h-10 items-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white transition hover:bg-brand-700"
              >
                <LogIn aria-hidden className="size-4" />
                Đăng nhập
              </Link>
            )}
          </div>

          <button
            type="button"
            onClick={() => setMenuOpen((open) => !open)}
            aria-expanded={menuOpen}
            aria-label={menuOpen ? 'Đóng menu' : 'Mở menu'}
            className="grid size-10 place-items-center rounded-xl border border-ink-200 text-ink-700 md:hidden"
          >
            {menuOpen ? <X aria-hidden className="size-5" /> : <Menu aria-hidden className="size-5" />}
          </button>
        </div>

        {menuOpen && (
          <nav aria-label="Điều hướng di động" className="border-t border-ink-100 bg-white px-4 py-3 md:hidden">
            <div className="flex flex-col gap-1">
              {links.map((link) => (
                <Link
                  key={link.to}
                  to={link.to}
                  className="rounded-lg px-3 py-2.5 text-sm font-medium text-ink-700 hover:bg-ink-50"
                >
                  {link.label}
                </Link>
              ))}
              <Link
                to={token && role ? roleHome(role) : '/login'}
                className="mt-2 inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white"
              >
                <LogIn aria-hidden className="size-4" />
                {token && role ? 'Vào dashboard' : 'Đăng nhập'}
              </Link>
            </div>
          </nav>
        )}
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="border-t border-ink-100 bg-ink-50">
        <div className="mx-auto flex w-full max-w-[1680px] flex-col gap-2 px-4 py-8 text-sm text-ink-500 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8">
          <p>© {new Date().getFullYear()} CarbonX. Sàn giao dịch tín chỉ carbon minh bạch.</p>
          <div className="flex flex-wrap items-center gap-4">
            <Link to="/terms" className="inline-flex min-h-[28px] items-center py-1 hover:text-ink-900">
              Điều khoản sử dụng
            </Link>
            <Link to="/privacy" className="inline-flex min-h-[28px] items-center py-1 hover:text-ink-900">
              Chính sách bảo mật
            </Link>
          </div>
        </div>
      </footer>
    </div>
  )
}
