import {
  Activity,
  Coins,
  FolderKanban,
  RefreshCw,
  TrendingUp,
  type LucideIcon,
} from 'lucide-react'
import { useDashboardSnapshot } from '@/features/dashboard/useDashboardSnapshot'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { StatCard } from '@/shared/components/StatCard'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate } from '@/shared/lib/format'
import type { Role } from '@/shared/stores/auth.store'
import { EmissionDistributionChart, MetricBarChart, TrendChart } from './Charts'
import { PageIntro } from './PageIntro'
import { QueryError, QueryLoading, SectionCard } from './QueryState'

const ICONS: LucideIcon[] = [Coins, Activity, FolderKanban, TrendingUp]
const TONES = ['brand', 'slate', 'amber', 'rose'] as const

interface DashboardViewProps {
  role: Role
  eyebrow: string
  title: string
  description: string
}

export function DashboardView({ role, eyebrow, title, description }: DashboardViewProps) {
  const { data, isPending, isError, isFetching, refetch } = useDashboardSnapshot(role)

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
            >
              <TrendChart data={data.trend} />
            </SectionCard>
            <SectionCard
              title="Cơ cấu nguồn phát thải"
              description="Tỷ trọng tín chỉ theo danh mục dự án"
            >
              <EmissionDistributionChart />
            </SectionCard>
            <SectionCard
              title="So sánh chỉ số"
              description="Quy mô các chỉ số vận hành chính"
            >
              <MetricBarChart metrics={data.metrics} />
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
