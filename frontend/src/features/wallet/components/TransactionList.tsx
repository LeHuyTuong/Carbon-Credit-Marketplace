import { ArrowDownLeft, ArrowUpRight } from 'lucide-react'
import { EmptyState } from '@/shared/components/EmptyState'
import { StatusBadge } from '@/shared/components/StatusBadge'
import type { WalletTransaction } from '@/shared/api/types'
import { cn } from '@/shared/lib/cn'
import { formatDate, formatUsd } from '@/shared/lib/format'

export function TransactionList({ transactions }: { transactions: WalletTransaction[] }) {
  if (transactions.length === 0) {
    return <EmptyState title="Chưa có giao dịch nào" className="py-10" />
  }

  return (
    <ul className="divide-y divide-ink-100">
      {transactions.map((item) => {
        const isIncoming = /deposit|credit|refund|received/i.test(item.type)
        return (
          <li key={item.id} className="flex items-center gap-3 px-5 py-3.5">
            <span
              className={cn(
                'grid size-9 shrink-0 place-items-center rounded-xl',
                isIncoming ? 'bg-brand-50 text-brand-600' : 'bg-rose-50 text-rose-600',
              )}
            >
              {isIncoming ? (
                <ArrowDownLeft aria-hidden className="size-4" />
              ) : (
                <ArrowUpRight aria-hidden className="size-4" />
              )}
            </span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-ink-900">
                {item.description || item.type || 'Giao dịch ví'}
              </p>
              <p className="text-xs text-ink-400">{formatDate(item.createdAt)}</p>
            </div>
            <div className="text-right">
              <p className={cn('text-sm font-semibold', isIncoming ? 'text-brand-700' : 'text-ink-900')}>
                {isIncoming ? '+' : '-'}
                {formatUsd(item.amount)}
              </p>
              {item.status && <StatusBadge status={item.status} className="mt-1" />}
            </div>
          </li>
        )
      })}
    </ul>
  )
}
