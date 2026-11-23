import { useMemo, useState } from 'react'
import { RotateCcw, Search, SlidersHorizontal } from 'lucide-react'
import { ListingCard } from '@/features/marketplace/components/ListingCard'
import { useMarketplace } from '@/features/marketplace/useMarketplace'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { formatNumber } from '@/shared/lib/format'
import { PageIntro } from './components/PageIntro'
import { QueryError } from './components/QueryState'

type SortMode = 'newest' | 'price-asc' | 'price-desc'

export function MarketplacePage() {
  const { data, isPending, isError, refetch, isFetching } = useMarketplace()
  const [search, setSearch] = useState('')
  const [projectId, setProjectId] = useState('all')
  const [sort, setSort] = useState<SortMode>('newest')

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
        eyebrow="Marketplace"
        title="Sàn giao dịch tín chỉ carbon"
        description="Tìm kiếm, so sánh và lọc các tin đăng tín chỉ carbon đang hoạt động trên CarbonX."
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
            <div className="relative min-w-0">
              <SlidersHorizontal aria-hidden className="pointer-events-none absolute left-3 top-3 size-4 text-ink-400" />
              <select
                aria-label="Lọc theo dự án"
                value={projectId}
                onChange={(event) => setProjectId(event.target.value)}
                className="h-11 w-full appearance-none rounded-xl border border-ink-200 bg-white pl-9 pr-3 text-sm text-ink-800 outline-none focus:border-brand-500 sm:w-52"
              >
                <option value="all">Tất cả dự án</option>
                {projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.title}
                  </option>
                ))}
              </select>
            </div>
            <select
              aria-label="Sắp xếp"
              value={sort}
              onChange={(event) => setSort(event.target.value as SortMode)}
              className="h-11 w-full appearance-none rounded-xl border border-ink-200 bg-white px-3 text-sm text-ink-800 outline-none focus:border-brand-500 sm:w-44"
            >
              <option value="newest">Mới nhất</option>
              <option value="price-asc">Giá tăng dần</option>
              <option value="price-desc">Giá giảm dần</option>
            </select>
          </div>
        </div>
        <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-xs text-ink-500">
          <span>
            {isPending
              ? 'Đang tải tin đăng…'
              : `Hiển thị ${formatNumber(filtered.length)} / ${formatNumber(listings.length)} tin đăng`}
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
            title={hasFilters ? 'Không khớp tin đăng nào' : 'Marketplace chưa có tin đăng'}
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
            <ListingCard key={listing.listingId} listing={listing} />
          ))}
        </div>
      )}
    </div>
  )
}
