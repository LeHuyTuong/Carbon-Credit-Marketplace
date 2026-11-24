import {
  Activity,
  ArrowRight,
  Coins,
  FolderKanban,
  RefreshCw,
  TrendingUp,
  type LucideIcon,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { useDashboardSnapshot } from '@/features/dashboard/useDashboardSnapshot'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { StatCard } from '@/shared/components/StatCard'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate } from '@/shared/lib/format'
import type { Role } from '@/shared/stores/auth.store'
import { BreakdownDonutChart, TrendChart } from './Charts'
import { PageIntro } from './PageIntro'
import { QueryError, QueryLoading, SectionCard } from './QueryState'

const ICONS: LucideIcon[] = [Coins, Activity, FolderKanban, TrendingUp]
const TONES = ['brand', 'slate', 'amber', 'rose'] as const

const QUICK_ACTIONS: Record<Role, { label: string; to: string }[]> = {
  EV_OWNER: [
    { label: 'Hồ sơ KYC', to: '/app/ev/kyc' },
    { label: 'Đăng ký xe điện', to: '/app/ev/vehicles' },
    { label: 'Xem ví & doanh thu', to: '/app/wallet' },
  ],
  COMPANY: [
    { label: 'Nộp báo cáo CSV', to: '/app/company/reports' },
    { label: 'Kho tín chỉ', to: '/app/company/credits' },
    { label: 'Chia sẻ doanh thu', to: '/app/company/payouts' },
  ],
  CVA: [
    { label: 'Thẩm định hồ sơ dự án', to: '/app/cva/applications' },
    { label: 'Thẩm định báo cáo CSV', to: '/app/cva/reports' },
  ],
  ADMIN: [
    { label: 'Duyệt hồ sơ cấp 2', to: '/app/admin/applications' },
    { label: 'Phát hành tín chỉ', to: '/app/admin/issuance' },
    { label: 'Duyệt rút tiền', to: '/app/admin/withdrawals' },
  ],
}

interface DashboardViewProps {
  role: Role
  eyebrow: string
  title: string
  description: string
}

export function DashboardView({ role, eyebrow, title, description }: DashboardViewProps) {
  const { data, isPending, isError, isFetching, refetch } = useDashboardSnapshot(role)
  const actions = QUICK_ACTIONS[role] ?? []

  return (
    <>
      <PageIntro
        eyebrow={eyebrow}
        title={title}
        description={description}
        actions={
          <Button
            variant="outline"
            size="sm"
            loading={isFetching && !isPending}
            leftIcon={<RefreshCw aria-hidden className="size-4" />}
            onClick={() => void refetch()}
          >
            Làm mới
          </Button>
        }
      />

      <div className="mb-6 flex flex-wrap items-center gap-2">
        {actions.map((act) => (
          <Link
            key={act.to}
            to={act.to}
            className="inline-flex items-center gap-1.5 rounded-xl border border-ink-200 bg-white px-3.5 py-2 text-xs font-semibold text-ink-800 shadow-2xs transition hover:border-brand-400 hover:text-brand-700"
          >
            {act.label}
            <ArrowRight aria-hidden className="size-3.5" />
          </Link>
        ))}
      </div>

      {isPending ? (
        <QueryLoading />
      ) : isError || !data ? (
        <QueryError onRetry={() => void refetch()} />
      ) : (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {data.metrics.map((item, index) => (
              <StatCard
                key={item.key}
                title={item.label}
                value={item.value}
                unit={item.unit}
                hint={item.hint}
                icon={ICONS[index % ICONS.length]}
                tone={TONES[index % TONES.length]}
              />
            ))}
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            <SectionCard
              title="Xu hướng hoạt động"
              description="Biến động khối lượng theo thời gian"
              className="lg:col-span-2"
            >
              <TrendChart data={data.trend} />
            </SectionCard>
            <SectionCard
              title={data.breakdownTitle || 'Phân bổ dữ liệu'}
              description={data.breakdownDescription || 'Tổng hợp theo nhóm thực tế'}
            >
              <BreakdownDonutChart data={data.breakdown ?? []} />
            </SectionCard>
          </div>

          <SectionCard title="Hoạt động gần đây" description="5 cập nhật mới nhất từ hệ thống">
            {data.recent.length === 0 ? (
              <EmptyState title="Chưa có hoạt động gần đây" className="py-10" />
            ) : (
              <ul className="divide-y divide-ink-100">
                {data.recent.map((item) => (
                  <li key={item.id} className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-ink-900">{item.title}</p>
                      {item.subtitle && (
                        <p className="truncate text-xs text-ink-500">{item.subtitle}</p>
                      )}
                    </div>
                    <div className="flex shrink-0 items-center gap-3">
                      <StatusBadge status={item.status} />
                      <span className="text-xs text-ink-400">{formatDate(item.date)}</span>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </SectionCard>
        </div>
      )}
    </>
  )
}
