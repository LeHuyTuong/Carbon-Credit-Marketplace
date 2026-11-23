import { useMemo, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { RotateCcw, Search, ShoppingCart, SlidersHorizontal, X } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { ListingCard } from '@/features/marketplace/components/ListingCard'
import { useMarketplace } from '@/features/marketplace/useMarketplace'
import { api } from '@/shared/api/client'
import { errorMessage } from '@/shared/api/phase3b'
import type { MarketplaceListing } from '@/shared/api/types'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { formatNumber, formatUsd } from '@/shared/lib/format'
import { queryKeys } from '@/shared/lib/queryKeys'
import { useAuthStore } from '@/shared/stores/auth.store'
import { PageIntro } from './components/PageIntro'
import { QueryError } from './components/QueryState'

type SortMode = 'newest' | 'price-asc' | 'price-desc'

const TRADING_FEE_RATE = 0.05

function BuyCreditDialog({
  listing,
  submitting,
  onClose,
  onSubmit,
}: {
  listing: MarketplaceListing
  submitting: boolean
  onClose: () => void
  onSubmit: (quantity: number) => void
}) {
  const [quantityText, setQuantityText] = useState('10')
  const [error, setError] = useState('')
  const qty = Number(quantityText)
  const validQty = Number.isInteger(qty) && qty >= 1 && qty <= listing.availableQuantity
  const subtotal = validQty ? qty * listing.pricePerCredit : 0
  const fee = subtotal * TRADING_FEE_RATE

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-ink-950/50 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Đặt mua tín chỉ carbon"
        className="w-full max-w-md rounded-card bg-white p-5 shadow-float"
      >
        <div className="flex items-start justify-between gap-3">
          <div>
            <h3 className="text-base font-bold text-ink-900">Đặt mua tín chỉ carbon</h3>
            <p className="mt-1 text-sm text-ink-500">{listing.projectTitle}</p>
          </div>
          <button
            type="button"
            aria-label="Đóng"
            onClick={onClose}
            className="grid size-8 place-items-center rounded-lg text-ink-500 hover:bg-ink-100"
          >
            <X className="size-5" />
          </button>
        </div>

        <form
          className="mt-5 space-y-4"
          noValidate
          onSubmit={(event) => {
            event.preventDefault()
            if (!Number.isInteger(qty) || qty < 1) {
              setError('Số lượng mua phải là số nguyên lớn hơn hoặc bằng 1')
              return
            }
            if (qty > listing.availableQuantity) {
              setError(`Vượt quá số lượng còn lại (${formatNumber(listing.availableQuantity)} tín chỉ)`)
              return
            }
            setError('')
            onSubmit(qty)
          }}
        >
          <Input
            label="Số lượng tín chỉ muốn mua"
            type="number"
            min={1}
            max={listing.availableQuantity}
            step={1}
            value={quantityText}
            onChange={(event) => {
              setQuantityText(event.target.value)
              setError('')
            }}
            hint={`Còn lại: ${formatNumber(listing.availableQuantity)} tín chỉ · Đơn giá: ${formatUsd(listing.pricePerCredit)}`}
            error={error}
          />

          <dl className="space-y-2 rounded-xl border border-ink-200 bg-ink-50 p-3.5 text-sm">
            <div className="flex justify-between">
              <dt className="text-ink-500">Bên bán</dt>
              <dd className="font-medium text-ink-900">{listing.sellerCompanyName}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-ink-500">Giá trị tín chỉ</dt>
              <dd className="font-semibold text-ink-900">{formatUsd(subtotal)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-ink-500">Phí sàn giao dịch (5%)</dt>
              <dd className="font-mono font-medium text-brand-700">{formatUsd(fee)}</dd>
            </div>
          </dl>

          <p className="text-xs text-ink-500">
            Tiền sẽ được trừ trực tiếp từ ví doanh nghiệp và tín chỉ cộng vào kho ngay khi xác nhận.
          </p>

          <div className="flex justify-end gap-2 border-t border-ink-100 pt-4">
            <Button variant="outline" onClick={onClose} disabled={submitting}>
              Huỷ
            </Button>
            <Button
              type="submit"
              loading={submitting}
              leftIcon={<ShoppingCart aria-hidden className="size-4" />}
            >
              Mua & Thanh toán ngay
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}

export function MarketplacePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const role = useAuthStore((state) => state.role)
  const { data, isPending, isError, refetch, isFetching } = useMarketplace()
  const [search, setSearch] = useState('')
  const [projectId, setProjectId] = useState('all')
  const [sort, setSort] = useState<SortMode>('newest')
  const [selectedListing, setSelectedListing] = useState<MarketplaceListing | null>(null)

  const orderMutation = useMutation({
    mutationFn: async ({ listingId, quantity }: { listingId: number; quantity: number }) => {
      const created = await api.post<{ id?: number; orderId?: number }>('/orders', {
        data: { buyerCompanyId: 0, listingId, quantity },
      })
      const orderId = created?.id ?? created?.orderId
      if (!orderId) {
        return { settled: false, orderId: null }
      }
      try {
        await api.post<unknown>(`/orders/${orderId}/complete`)
        return { settled: true, orderId }
      } catch (completeError) {
        return { settled: false, orderId, error: completeError }
      }
    },
    onSuccess: async (result) => {
      setSelectedListing(null)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.marketplace }),
        queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.orders }),
        queryClient.invalidateQueries({ queryKey: queryKeys.wallet }),
        queryClient.invalidateQueries({ queryKey: queryKeys.walletTransactions }),
      ])
      if (result.settled) {
        toast.success('Mua và thanh toán tín chỉ thành công')
      } else {
        toast.warning('Đơn đã tạo nhưng chưa trừ tiền ví', {
          description: result.error
            ? errorMessage(result.error)
            : 'Kiểm tra số dư ví rồi bấm Thanh toán lại trong mục Đơn mua.',
        })
      }
      navigate('/app/company/orders')
    },
    onError: (error) => {
      toast.error('Không thể đặt mua tín chỉ', { description: errorMessage(error) })
    },
  })

  const handleBuyClick = (listing: MarketplaceListing) => {
    if (role !== 'COMPANY') {
      toast.info('Vui lòng đăng nhập bằng tài khoản Doanh nghiệp để đặt mua tín chỉ')
      navigate('/login')
      return
    }
    setSelectedListing(listing)
  }

  const listings = data ?? []
  const projects = useMemo(() => {
    const unique = new Map<number, string>()
    listings.forEach((item) => {
      if (item.projectId !== null) unique.set(item.projectId, item.projectTitle)
    })
    return [...unique.entries()].map(([id, title]) => ({ id, title }))
  }, [listings])

  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase()
    const result = listings.filter((item) => {
      const matchKeyword =
        !keyword ||
        `${item.projectTitle} ${item.sellerCompanyName} ${item.batchCode}`.toLowerCase().includes(keyword)
      const matchProject = projectId === 'all' || String(item.projectId) === projectId
      return matchKeyword && matchProject
    })
    if (sort === 'price-asc') return [...result].sort((a, b) => a.pricePerCredit - b.pricePerCredit)
    if (sort === 'price-desc') return [...result].sort((a, b) => b.pricePerCredit - a.pricePerCredit)
    return result
  }, [listings, search, projectId, sort])

  const hasFilters = search.trim() !== '' || projectId !== 'all' || sort !== 'newest'
  const resetFilters = () => {
    setSearch('')
    setProjectId('all')
    setSort('newest')
  }

  return (
    <div className="mx-auto min-w-0 w-full max-w-[1680px] px-4 py-10 sm:px-6 lg:px-8">
      <PageIntro
        eyebrow="Sàn giao dịch"
        title="Sàn giao dịch tín chỉ carbon"
        description="Tìm kiếm, so sánh và đặt mua tín chỉ carbon đã thẩm định CVA trên CarbonX. Phí sàn giao dịch cố định 5%."
      />

      <Card className="mb-6 p-4 sm:p-5">
        <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:items-center">
          <Input
            aria-label="Tìm kiếm tin đăng"
            placeholder="Tìm dự án, mã lô…"
            icon={<Search aria-hidden className="size-4" />}
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            wrapperClassName="sm:flex-1"
          />
          <div className="flex min-w-0 flex-col gap-3 sm:flex-row">
            <label className="sr-only" htmlFor="filter-project">
              Lọc theo dự án
            </label>
            <select
              id="filter-project"
              value={projectId}
              onChange={(event) => setProjectId(event.target.value)}
              className="h-11 rounded-xl border border-ink-200 bg-white px-3 text-sm text-ink-800 outline-none focus:border-brand-500"
            >
              <option value="all">Tất cả dự án</option>
              {projects.map((project) => (
                <option key={project.id} value={project.id}>
                  {project.title}
                </option>
              ))}
            </select>

            <div className="relative flex items-center">
              <SlidersHorizontal aria-hidden className="pointer-events-none absolute left-3 size-4 text-ink-400" />
              <label className="sr-only" htmlFor="sort-listings">
                Sắp xếp
              </label>
              <select
                id="sort-listings"
                value={sort}
                onChange={(event) => setSort(event.target.value as SortMode)}
                className="h-11 rounded-xl border border-ink-200 bg-white pl-9 pr-3 text-sm text-ink-800 outline-none focus:border-brand-500"
              >
                <option value="newest">Mới nhất</option>
                <option value="price-asc">Giá thấp đến cao</option>
                <option value="price-desc">Giá cao đến thấp</option>
              </select>
            </div>
          </div>
        </div>
        <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-xs text-ink-500">
          <span>
            {isPending
              ? 'Đang tải tin đăng…'
              : `Hiển thị ${formatNumber(filtered.length)} / ${formatNumber(listings.length)} tin đăng · Phí sàn giao dịch 5%`}
          </span>
          {hasFilters && (
            <button
              type="button"
              onClick={resetFilters}
              className="inline-flex items-center gap-1.5 font-semibold text-brand-600 hover:text-brand-700"
            >
              <RotateCcw aria-hidden className="size-3.5" />
              Xoá bộ lọc
            </button>
          )}
        </div>
      </Card>

      {isPending ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2, 3, 4, 5].map((key) => (
            <div key={key} className="h-64 animate-pulse rounded-card bg-ink-200/60" />
          ))}
        </div>
      ) : isError ? (
        <QueryError onRetry={() => void refetch()} />
      ) : filtered.length === 0 ? (
        <Card>
          <EmptyState
            title={hasFilters ? 'Không khớp tin đăng nào' : 'Sàn giao dịch chưa có tin đăng'}
            description={
              hasFilters
                ? 'Thử thay đổi từ khoá hoặc bộ lọc dự án.'
                : 'Các tin đăng mới sẽ xuất hiện tại đây.'
            }
            action={
              hasFilters ? (
                <button
                  type="button"
                  onClick={resetFilters}
                  className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700"
                >
                  Xoá bộ lọc
                </button>
              ) : undefined
            }
          />
        </Card>
      ) : (
        <div className={isFetching ? 'grid gap-4 opacity-60 sm:grid-cols-2 lg:grid-cols-3' : 'grid gap-4 sm:grid-cols-2 lg:grid-cols-3'}>
          {filtered.map((listing) => (
            <ListingCard key={listing.listingId} listing={listing} onBuy={handleBuyClick} />
          ))}
        </div>
      )}

      {selectedListing && (
        <BuyCreditDialog
          listing={selectedListing}
          submitting={orderMutation.isPending}
          onClose={() => setSelectedListing(null)}
          onSubmit={(quantity) =>
            orderMutation.mutate({
              listingId: selectedListing.listingId,
              quantity,
            })
          }
        />
      )}
    </div>
  )
}
