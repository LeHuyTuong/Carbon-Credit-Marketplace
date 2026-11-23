import type { ReactNode } from 'react'
import { CircleAlert } from 'lucide-react'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'

export function QueryLoading({ label = 'Đang tải dữ liệu…' }: { label?: string }) {
  return (
    <div className="space-y-4" role="status" aria-label={label}>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[0, 1, 2, 3].map((key) => (
          <div key={key} className="h-32 animate-pulse rounded-card bg-ink-200/60" />
        ))}
      </div>
      <div className="h-72 animate-pulse rounded-card bg-ink-200/60" />
    </div>
  )
}

export function QueryError({
  title = 'Không tải được dữ liệu',
  description = 'Máy chủ có thể đang bảo trì hoặc bạn chưa có quyền truy cập.',
  onRetry,
}: {
  title?: string
  description?: string
  onRetry?: () => void
}) {
  return (
    <Card>
      <div role="alert" aria-live="polite">
        <EmptyState
          icon={CircleAlert}
          title={title}
          description={description}
          action={
            onRetry ? (
              <Button variant="outline" onClick={onRetry}>
                Thử lại
              </Button>
            ) : undefined
          }
        />
      </div>
    </Card>
  )
}

export function SectionCard({
  title,
  description,
  action,
  children,
  className,
}: {
  title: string
  description?: string
  action?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <Card className={className}>
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-ink-100 p-5">
        <div>
          <h3 className="text-base font-semibold text-ink-900">{title}</h3>
          {description && <p className="mt-1 text-sm text-ink-500">{description}</p>}
        </div>
        {action}
      </div>
      {children}
    </Card>
  )
}
