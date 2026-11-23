import { Link } from 'react-router-dom'
import { ArrowRight, ArrowUpRight, CheckCircle2, FileCheck2, ShieldCheck } from 'lucide-react'
import { ListingCard } from '@/features/marketplace/components/ListingCard'
import { useMarketplace } from '@/features/marketplace/useMarketplace'
import { useProjects } from '@/features/projects/useProjects'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { formatNumber, formatUsd } from '@/shared/lib/format'
import { QueryError, QueryLoading } from './components/QueryState'

const FALLBACK_PROJECT_IMAGES = [
  '/images/project1.jpg',
  '/images/project2.jpg',
  '/images/project3.jpg',
]

const REGISTRY_STEPS = [
  {
    code: '01',
    title: 'Dữ liệu sạc xe điện (kWh)',
    detail: 'Doanh nghiệp tải báo cáo CSV theo biển số xe, tự động băm SHA-256 lưu dấu vết kiểm toán.',
  },
  {
    code: '02',
    title: 'Thẩm định 2 cấp CVA & Admin',
    detail: '8 luật phát hiện bất thường dữ liệu kết hợp gợi ý AI giúp CVA thẩm định trước khi Admin chốt.',
  },
  {
    code: '03',
    title: 'Phát hành Serial & Giao dịch',
    detail: 'Cấp mã serial duy nhất theo năm–công ty–dự án, khấu trừ buffer/leakage và giao dịch nguyên tử.',
  },
  {
    code: '04',
    title: 'Triệt tiêu PDF & Chia lợi nhuận',
    detail: 'Khóa vĩnh viễn tín chỉ chống tính trùng, xuất chứng chỉ PDF và chi trả doanh thu về ví chủ xe.',
  },
]

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
    <div className="bg-ink-50">
      {/* Editorial Climate-Exchange Hero */}
      <section className="border-b border-ink-200 bg-ink-50">
        <div className="mx-auto grid min-w-0 w-full max-w-[1680px] gap-10 px-4 py-12 sm:px-6 sm:py-16 lg:grid-cols-12 lg:items-center lg:gap-12 lg:px-8">
          <div className="min-w-0 lg:col-span-7">
            <p className="font-mono text-xs font-semibold uppercase tracking-[0.16em] text-brand-600">
              EV Carbon Registry &amp; Exchange · Vietnam
            </p>
            <h1 className="mt-4 break-words font-extrabold text-4xl leading-[1.14] tracking-tight text-ink-900 sm:text-5xl lg:text-6xl">
              Thị trường tín chỉ carbon xe điện{' '}
              <span className="text-brand-600">đo lường &amp; kiểm chứng.</span>
            </h1>
            <p className="mt-5 max-w-xl text-base leading-relaxed text-ink-600 sm:text-lg">
              Nền tảng đăng ký, thẩm định độc lập hai cấp (CVA &amp; Admin), giao dịch và triệt tiêu tín
              chỉ carbon gắn trực tiếp với dữ liệu sạc xe điện thực tế.
            </p>

            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Link
                to="/marketplace"
                className="inline-flex h-12 items-center gap-2 rounded-lg bg-brand-600 px-6 text-sm font-semibold text-white transition hover:bg-brand-700"
              >
                Khám phá sàn giao dịch
                <ArrowRight aria-hidden className="size-4" />
              </Link>
              <Link
                to="/register"
                className="inline-flex h-12 items-center gap-2 rounded-lg border border-ink-300 bg-white px-6 text-sm font-semibold text-ink-900 transition hover:border-ink-900 hover:bg-ink-100"
              >
                Mở tài khoản doanh nghiệp
              </Link>
            </div>

            {/* Market Ticker Bar */}
            <dl className="mt-10 grid grid-cols-2 gap-x-6 gap-y-5 border-t border-ink-200 pt-6 sm:grid-cols-4">
              {[
                { label: 'Tin đang niêm yết', value: marketplace.isPending ? '—' : formatNumber(listings.length) },
                { label: 'Dự án mở', value: projects.isPending ? '—' : formatNumber(projectList.length) },
                { label: 'Khối lượng (tCO2e)', value: marketplace.isPending ? '—' : formatNumber(totalCredits) },
                { label: 'Giá TB / tín chỉ', value: averagePrice === null ? '—' : formatUsd(averagePrice) },
              ].map((stat) => (
                <div key={stat.label} className="min-w-0">
                  <dt className="text-xs font-medium text-ink-500">{stat.label}</dt>
                  <dd className="mt-1 break-words font-mono text-xl font-semibold tabular-nums text-ink-900 sm:text-2xl">
                    {stat.value}
                  </dd>
                </div>
              ))}
            </dl>
          </div>

          {/* Grounded Real Photography Visual */}
          <div className="min-w-0 lg:col-span-5">
            <div className="overflow-hidden rounded-xl border border-ink-200 bg-white shadow-card">
              <div className="relative aspect-[4/3] overflow-hidden bg-ink-100">
                <img
                  src="/images/project1.jpg"
                  alt="Hạ tầng trạm sạc và dự án giảm phát thải xe điện CarbonX"
                  className="size-full object-cover"
                />
                <div className="absolute left-3 top-3 inline-flex items-center gap-1.5 rounded-md bg-white/95 px-2.5 py-1 text-xs font-semibold text-brand-700 shadow-sm">
                  <ShieldCheck aria-hidden className="size-3.5" />
                  1 Credit = 1 tCO2e · CVA Verified
                </div>
              </div>
              <div className="grid grid-cols-2 divide-x divide-ink-200 border-t border-ink-200 bg-white">
                <div className="flex items-center gap-3 p-4">
                  <img
                    src="/images/vinfast-vfe34.png"
                    alt="Xe điện tham gia hệ sinh thái CarbonX"
                    className="h-10 w-14 shrink-0 rounded object-contain bg-ink-50 p-1"
                  />
                  <div className="min-w-0">
                    <p className="font-mono text-xs font-semibold text-brand-600">EV OWNER</p>
                    <p className="truncate text-xs font-medium text-ink-700">Nhận chia sẻ doanh thu</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 p-4">
                  <img
                    src="/images/charging-station.png"
                    alt="Trạm sạc điện đo lường kWh"
                    className="size-10 shrink-0 rounded object-contain bg-ink-50 p-1"
                  />
                  <div className="min-w-0">
                    <p className="font-mono text-xs font-semibold text-brand-600">AUDIT TRAIL</p>
                    <p className="truncate text-xs font-medium text-ink-700">SHA-256 &amp; PDF Serial</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 4-Step Registry Pipeline */}
      <section className="border-b border-ink-200 bg-white">
        <div className="mx-auto min-w-0 w-full max-w-[1680px] px-4 py-12 sm:px-6 lg:px-8">
          <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
            <div>
              <p className="font-mono text-xs font-semibold uppercase tracking-widest text-brand-600">
                Quy trình chuẩn sổ đăng ký
              </p>
              <h2 className="mt-1 text-2xl font-bold tracking-tight text-ink-900">
                Từ dữ liệu sạc xe điện đến chứng chỉ bù trừ phát thải
              </h2>
            </div>
            <span className="inline-flex items-center gap-1.5 font-mono text-xs text-ink-500">
              <FileCheck2 aria-hidden className="size-4 text-brand-600" />
              Chống tính trùng (Double Issuance &amp; Double Use)
            </span>
          </div>

          <div className="grid gap-px overflow-hidden rounded-xl border border-ink-200 bg-ink-200 sm:grid-cols-2 lg:grid-cols-4">
            {REGISTRY_STEPS.map((step) => (
              <div key={step.code} className="bg-white p-5">
                <p className="font-mono text-xs font-semibold text-brand-600">BƯỚC {step.code}</p>
                <h3 className="mt-2 text-base font-bold text-ink-900">{step.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-ink-600">{step.detail}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Live Marketplace Listings */}
      <section className="mx-auto min-w-0 w-full max-w-[1680px] px-4 py-14 sm:px-6 lg:px-8">
        <div className="mb-6 flex min-w-0 flex-wrap items-end justify-between gap-3">
          <div className="min-w-0">
            <p className="font-mono text-xs font-semibold uppercase tracking-widest text-brand-600">
              Đang giao dịch
            </p>
            <h2 className="mt-1 text-2xl font-bold tracking-tight text-ink-900">Tin đăng mới nhất</h2>
          </div>
          <Link
            to="/marketplace"
            className="inline-flex min-h-10 items-center gap-1.5 px-2 text-sm font-semibold text-brand-600 hover:text-brand-700"
          >
            Xem tất cả <ArrowRight aria-hidden className="size-4" />
          </Link>
        </div>

        {marketplace.isPending ? (
          <QueryLoading />
        ) : marketplace.isError ? (
          <QueryError
            title="Chưa kết nối được máy chủ dữ liệu"
            description="Khởi động Backend tại cổng 8082 để tải danh sách tin đăng bán trực tiếp từ cơ sở dữ liệu."
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

      {/* Live Carbon Projects */}
      <section id="du-an" className="scroll-mt-20 border-t border-ink-200 bg-white">
        <div className="mx-auto min-w-0 w-full max-w-[1680px] px-4 py-14 sm:px-6 lg:px-8">
          <div className="mb-6 min-w-0">
            <p className="font-mono text-xs font-semibold uppercase tracking-widest text-brand-600">
              Danh mục chương trình
            </p>
            <h2 className="mt-1 text-2xl font-bold tracking-tight text-ink-900">Dự án giảm phát thải</h2>
          </div>

          {projects.isPending ? (
            <QueryLoading />
          ) : projects.isError ? (
            <QueryError
              title="Chưa tải được danh sách dự án"
              description="Khởi động Backend tại cổng 8082 để hiển thị các dự án giảm phát thải đang mở."
              onRetry={() => void projects.refetch()}
            />
          ) : projectList.length === 0 ? (
            <Card>
              <EmptyState title="Chưa có dự án công khai" description="Dự án mới sẽ xuất hiện tại đây." />
            </Card>
          ) : (
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {projectList.slice(0, 6).map((project, idx) => (
                <Card key={project.id} className="overflow-hidden transition hover:border-ink-300">
                  <div className="aspect-[16/9] overflow-hidden bg-ink-100">
                    <img
                      src={FALLBACK_PROJECT_IMAGES[idx % FALLBACK_PROJECT_IMAGES.length]}
                      alt={project.title}
                      className="size-full object-cover"
                    />
                  </div>
                  <div className="p-5">
                    <div className="flex items-center justify-between gap-2">
                      <span className="inline-flex items-center gap-1 font-mono text-xs font-semibold text-brand-600">
                        <CheckCircle2 aria-hidden className="size-3.5" />
                        {project.status}
                      </span>
                      <span className="font-mono text-xs text-ink-500">PRJ-{project.id}</span>
                    </div>
                    <h3 className="mt-2 text-base font-bold leading-snug text-ink-900">{project.title}</h3>
                    <p className="mt-1.5 text-sm leading-relaxed text-ink-600">
                      {project.description || 'Dự án giảm phát thải xe điện đã được kiểm chứng trên CarbonX.'}
                    </p>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Editorial Footer CTA */}
      <section className="mx-auto min-w-0 w-full max-w-[1680px] px-4 py-14 sm:px-6 lg:px-8">
        <div className="flex flex-col items-start justify-between gap-6 rounded-xl border border-brand-800 bg-brand-700 p-8 text-white sm:flex-row sm:items-center sm:p-10">
          <div className="max-w-xl">
            <p className="font-mono text-xs font-semibold uppercase tracking-widest text-brand-100">
              CarbonX Registry
            </p>
            <h2 className="mt-2 font-bold text-3xl tracking-tight text-white">
              Sẵn sàng tham gia thị trường tín chỉ carbon minh bạch?
            </h2>
            <p className="mt-2 text-sm text-white/90">
              Đăng ký tài khoản chủ xe điện hoặc doanh nghiệp để theo dõi phát thải, giao dịch và nhận
              chứng chỉ triệt tiêu PDF.
            </p>
          </div>
          <Link
            to="/register"
            className="inline-flex h-12 shrink-0 items-center gap-2 rounded-lg bg-white px-6 text-sm font-semibold text-brand-800 transition hover:bg-ink-50"
          >
            Bắt đầu ngay
            <ArrowUpRight aria-hidden className="size-4" />
          </Link>
        </div>
      </section>
    </div>
  )
}
