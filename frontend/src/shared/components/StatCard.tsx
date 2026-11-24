import type { LucideIcon } from 'lucide-react'
import { cn } from '@/shared/lib/cn'
import { formatNumber, formatUsd } from '@/shared/lib/format'
import { Card } from './Card'

type Tone = 'brand' | 'slate' | 'amber' | 'rose'

const tones: Record<Tone, string> = {
  brand: 'bg-brand-50 text-brand-600 ring-brand-100',
  slate: 'bg-slate-100 text-slate-600 ring-slate-200',
  amber: 'bg-amber-50 text-amber-600 ring-amber-100',
  rose: 'bg-rose-50 text-rose-600 ring-rose-100',
}

interface StatCardProps {
  title: string
  value: string | number | null
  unit?: string
  hint?: string
  delta?: number | null
  icon: LucideIcon
  tone?: Tone
  className?: string
}

function formatMetricValue(value: string | number | null, unit?: string): string {
  if (value === null) return '—'
  if (typeof value !== 'number') return value
  if (unit === 'USD') return formatUsd(value)
  const formatted = formatNumber(value)
  return unit ? `${formatted} ${unit}` : formatted
}

export function StatCard({
  title,
  value,
  unit,
  hint,
  delta,
  icon: Icon,
  tone = 'brand',
  className,
}: StatCardProps) {
  return (
    <Card className={cn('p-5', className)}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium text-ink-500">{title}</p>
          <p className="mt-2 text-2xl font-bold tracking-tight text-ink-900">
            {formatMetricValue(value, unit)}
          </p>
        </div>
        <span className={cn('grid size-10 shrink-0 place-items-center rounded-xl ring-1', tones[tone])}>
          <Icon aria-hidden className="size-5" />
        </span>
      </div>
      {(hint || delta !== undefined) && (
        <div className="mt-3 flex items-center gap-2 text-xs">
          {delta !== undefined && delta !== null && (
            <span
              className={cn(
                'font-semibold',
                delta >= 0 ? 'text-emerald-600' : 'text-rose-600',
              )}
            >
              {delta >= 0 ? '+' : ''}
              {delta.toFixed(1)}%
            </span>
          )}
          {hint && <span className="truncate text-ink-400">{hint}</span>}
        </div>
      )}
    </Card>
  )
}
