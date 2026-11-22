import { Link } from 'react-router-dom'
import {
  ArrowRight,
  BarChart3,
  Building2,
  Leaf,
  ShieldCheck,
  Sparkles,
} from 'lucide-react'
import { ListingCard } from '@/features/marketplace/components/ListingCard'
import { useMarketplace } from '@/features/marketplace/useMarketplace'
import { useProjects } from '@/features/projects/useProjects'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { formatNumber, formatUsd } from '@/shared/lib/format'
import { QueryError, QueryLoading } from './components/QueryState'

export function PublicHomePage() {
  const marketplace = useMarketplace()
  const projects = useProjects()
  const listings = marketplace.data ?? []
  const projectList = projects.data ?? []
  const averagePrice = listings.length
    ? listings.reduce((sum, item) => sum + item.pricePerCredit, 0) / listings.length
    : null
  const totalCredits = listings.reduce((sum, item) => sum + item.availableQuantity, 0)

  return (
    <>
      <section className="relative overflow-hidden bg-ink-950">
        <div aria-hidden className="absolute -right-24 -top-32 size-96 rounded-full bg-brand-500/25 blur-3xl" />
        <div aria-hidden className="absolute -bottom-40 left-1/4 size-80 rounded-full bg-brand-700/25 blur-3xl" />
        <div className="relative mx-auto grid max-w-7xl gap-10 px-4 py-16 sm:px-6 sm:py-24 lg:grid-cols-2 lg:items-center lg:px-8">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-brand-400/30 bg-brand-400/10 px-3 py-1.5 text-xs font-semibold text-brand-300">
              <Sparkles aria-hidden className="size-3.5" />
              Sàn giao dịch tín chỉ carbon thế hệ mới
            </span>
            <h1 className="mt-6 text-4xl font-extrabold leading-[1.15] tracking-tight text-white sm:text-5xl lg:text-6xl">
              Biến giảm phát thải
              <span className="block text-brand-400">thành giá trị kinh tế.</span>
            </h1>
            <p className="mt-6 max-w-xl text-base leading-relaxed text-slate-300 sm:text-lg">
              CarbonX kết nối chủ xe điện, doanh nghiệp và đơn vị thẩm định trên một thị trường tín chỉ
              carbon minh bạch, thanh khoản và đo lường được theo thời gian thực.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                to="/marketplace"
                className="inline-flex h-12 items-center gap-2 rounded-xl bg-brand-500 px-6 text-sm font-semibold text-white shadow-lg shadow-brand-900/40 transition hover:bg-brand-400"
              >
                Khám phá marketplace
                <ArrowRight aria-hidden className="size-4" />
              </Link>
              <Link
                to="/register"
                className="inline-flex h-12 items-center gap-2 rounded-xl border border-white/20 bg-white/5 px-6 text-sm font-semibold text-white transition hover:bg-white/10"
              >
                Tham gia CarbonX
              </Link>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:gap-4">
            {[
              { icon: BarChart3, label: 'Tin đăng hoạt động', value: marketplace.isPending ? '—' : formatNumber(listings.length) },
              { icon: Building2, label: 'Dự án carbon', value: projects.isPending ? '—' : formatNumber(projectList.length) },
              { icon: Leaf, label: 'Tín chỉ khả dụng', value: marketplace.isPending ? '—' : formatNumber(totalCredits) },
              { icon: ShieldCheck, label: 'Giá TB / tín chỉ', value: averagePrice === null ? '—' : formatUsd(averagePrice) },
            ].map((item) => (
              <div
                key={item.label}
                className="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur-sm sm:p-5"
              >
                <item.icon aria-hidden className="size-5 text-brand-400" />
                <p className="mt-4 text-2xl font-bold tracking-tight text-white">{item.value}</p>
                <p className="mt-1 text-xs text-slate-400">{item.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
          <div>
            <p className="text-xs font-bold uppercase tracking-widest text-brand-600">Đang diễn ra</p>
            <h2 className="mt-1 text-2xl font-bold tracking-tight text-ink-900">Tin đăng mới nhất</h2>
          </div>
          <Link
            to="/marketplace"
            className="inline-flex items-center gap-1.5 text-sm font-semibold text-brand-600 hover:text-brand-700"
          >
            Xem tất cả <ArrowRight aria-hidden className="size-4" />
          </Link>
        </div>

        {marketplace.isPending ? (
          <QueryLoading />
        ) : marketplace.isError ? (
          <QueryError
            title="Chưa tải được marketplace"
            description="Dữ liệu tin đăng sẽ xuất hiện khi kết nối máy chủ sẵn sàng."
            onRetry={() => void marketplace.refetch()}
          />
        ) : listings.length === 0 ? (
          <Card>
            <EmptyState
              title="Chưa có tin đăng nào"
              description="Các doanh nghiệp sẽ sớm niêm yết tín chỉ trên sàn CarbonX."
            />
          </Card>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {listings.slice(0, 3).map((listing) => (
              <ListingCard key={listing.listingId} listing={listing} />
            ))}
          </div>
        )}
      </section>

      <section id="du-an" className="scroll-mt-20 border-t border-ink-100 bg-white">
        <div className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
          <p className="text-xs font-bold uppercase tracking-widest text-brand-600">Danh mục</p>
          <h2 className="mt-1 text-2xl font-bold tracking-tight text-ink-900">Dự án carbon nổi bật</h2>

          {projects.isPending ? (
            <div className="mt-6 grid gap-4 sm:grid-cols-3">
              {[0, 1, 2].map((key) => (
                <div key={key} className="h-40 animate-pulse rounded-card bg-ink-200/60" />
              ))}
            </div>
          ) : projects.isError ? (
            <div className="mt-6">
              <QueryError
                title="Chưa tải được dự án"
                onRetry={() => void projects.refetch()}
              />
            </div>
          ) : projectList.length === 0 ? (
            <Card className="mt-6">
              <EmptyState title="Chưa có dự án được công bố" />
            </Card>
          ) : (
            <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {projectList.slice(0, 6).map((project) => (
                <Card key={project.id} className="p-5">
                  <span className="inline-flex rounded-full bg-brand-50 px-2.5 py-1 text-xs font-semibold text-brand-700">
                    {project.status || 'Đang triển khai'}
                  </span>
                  <h3 className="mt-3 text-base font-semibold text-ink-900">{project.title}</h3>
                  <p className="mt-2 line-clamp-3 text-sm text-ink-500">
                    {project.description || 'Dự án giảm phát thải đang được thẩm định trên CarbonX.'}
                  </p>
                  {project.location && (
                    <p className="mt-4 text-xs font-medium text-ink-400">{project.location}</p>
                  )}
                </Card>
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 pb-16 sm:px-6 lg:px-8">
        <div className="flex flex-col items-start justify-between gap-4 rounded-3xl bg-brand-600 p-8 text-white sm:flex-row sm:items-center sm:p-10">
          <div>
            <h2 className="text-2xl font-bold tracking-tight">Sẵn sàng tham gia thị trường carbon?</h2>
            <p className="mt-2 text-sm text-brand-100">
              Tạo tài khoản trong 2 phút và bắt đầu theo dõi tín chỉ của bạn.
            </p>
          </div>
          <Link
            to="/register"
            className="inline-flex h-12 shrink-0 items-center justify-center rounded-xl bg-white px-6 text-sm font-semibold text-brand-700 transition hover:bg-brand-50"
          >
            Đăng ký miễn phí
          </Link>
        </div>
      </section>
    </>
  )
}
