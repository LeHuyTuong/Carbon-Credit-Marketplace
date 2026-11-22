import type { ReactNode } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { ShieldX } from 'lucide-react'
import { cn } from '@/shared/lib/cn'
import { roleHome } from '@/shared/lib/roles'
import { useAuthStore, type Role } from '@/shared/stores/auth.store'
import { EmptyState } from '@/shared/components/EmptyState'

interface RoleGuardProps {
  allow: readonly Role[]
  children: ReactNode
  className?: string
}

export function RoleGuard({ allow, children, className }: RoleGuardProps) {
  const token = useAuthStore((state) => state.token)
  const role = useAuthStore((state) => state.role)
  const location = useLocation()

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (!role || !allow.includes(role)) {
    return (
      <div className={cn('mx-auto max-w-xl py-16', className)}>
        <EmptyState
          icon={ShieldX}
          title="Không có quyền truy cập"
          description="Vai trò của bạn không được phép mở trang này."
          action={
            role ? (
              <Link
                to={roleHome(role)}
                className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-brand-700"
              >
                Về trang tổng quan
              </Link>
            ) : undefined
          }
        />
      </div>
    )
  }

  return <>{children}</>
}
