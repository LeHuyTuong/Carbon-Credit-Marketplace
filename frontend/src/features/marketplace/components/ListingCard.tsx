import { Building2, CalendarDays, Layers3, ShoppingCart } from 'lucide-react'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { StatusBadge } from '@/shared/components/StatusBadge'
import type { MarketplaceListing } from '@/shared/api/types'
import { formatDate, formatNumber, formatUsd } from '@/shared/lib/format'

export function ListingCard({
  listing,
  onBuy,
}: {
  listing: MarketplaceListing
  onBuy?: (listing: MarketplaceListing) => void
}) {
  return (
    <Card className="group flex h-full flex-col p-5 transition-all duration-300 hover:-translate-y-1 hover:border-brand-300 hover:shadow-float">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-wider text-brand-600">
            {listing.batchCode || 'Tín chỉ carbon'}
          </p>
          <h2 className="mt-1 text-balance text-base font-bold leading-snug text-ink-900">
            {listing.projectTitle}
          </h2>
        </div>
        <StatusBadge status="LISTED" kind="credit" label="Đang bán" />
      </div>

      <div className="mt-4 flex flex-wrap items-baseline justify-between gap-2">
        <div className="flex items-baseline gap-1.5">
          <span className="text-2xl font-bold tracking-tight text-ink-900">
            {formatUsd(listing.pricePerCredit)}
          </span>
          <span className="text-sm text-ink-500">/ tín chỉ</span>
        </div>
        <span className="rounded-md bg-ink-100 px-2 py-0.5 font-mono text-xs font-medium text-ink-600">
          Phí sàn 5% ({formatUsd(listing.pricePerCredit * 0.05)})
        </span>
      </div>

      <dl className="mt-4 flex-1 space-y-2 text-sm text-ink-600">
        <div className="flex items-center gap-2">
          <Layers3 aria-hidden className="size-4 shrink-0 text-ink-400" />
          <dt className="sr-only">Số lượng khả dụng</dt>
          <dd>
            {formatNumber(listing.availableQuantity)} / {formatNumber(listing.quantity)} tín chỉ còn lại
          </dd>
        </div>
        <div className="flex items-center gap-2">
          <Building2 aria-hidden className="size-4 shrink-0 text-ink-400" />
          <dt className="sr-only">Doanh nghiệp bán</dt>
          <dd className="truncate">{listing.sellerCompanyName}</dd>
        </div>
        <div className="flex items-center gap-2">
          <CalendarDays aria-hidden className="size-4 shrink-0 text-ink-400" />
          <dt>Hạn niêm yết</dt>
          <dd className="font-medium text-ink-700">{formatDate(listing.expiresAt)}</dd>
        </div>
      </dl>

      {onBuy && listing.availableQuantity > 0 && (
        <div className="mt-5 border-t border-ink-100 pt-4">
          <Button
            className="w-full"
            size="sm"
            leftIcon={<ShoppingCart aria-hidden className="size-4" />}
            onClick={() => onBuy(listing)}
          >
            Đặt mua tín chỉ
          </Button>
        </div>
      )}
    </Card>
  )
}
