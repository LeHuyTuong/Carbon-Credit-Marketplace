import { useQuery } from '@tanstack/react-query'
import { Building2, Coins, Landmark, Layers3, RefreshCw, ShieldCheck } from 'lucide-react'
import { useMarketplace } from '@/features/marketplace/useMarketplace'
import { api } from '@/shared/api/client'
import { mapCreditBatch, pageOf } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { StatCard } from '@/shared/components/StatCard'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate, formatNumber, formatUsd } from '@/shared/lib/format'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

export function AdminCreditsPage() {
  const batchesQuery = useQuery({
    queryKey: ['admin', 'all-credit-batches'],
    queryFn: async () => pageOf(await api.get<unknown>('/credits/batches', { page: 0, size: 50 })).items.map(mapCreditBatch),
  })
  const marketplaceQuery = useMarketplace()
  const companiesQuery = useQuery({
    queryKey: ['admin', 'kyc-companies'],
    queryFn: async () => {
      const data = await api.get<unknown>('/kyc/company/listKYCCompany')
      return Array.isArray(data) ? data : []
    },
    retry: false,
  })
  const txnCountQuery = useQuery({
    queryKey: ['admin', 'txn-count'],
    queryFn: async () => {
      const data = await api.get<unknown>('/wallet/transactions/count')
      return typeof data === 'number' ? data : Number(data ?? 0)
    },
    retry: false,
  })

  const batches = batchesQuery.data ?? []
  const listings = marketplaceQuery.data ?? []
  const totalIssuedCredits = batches.reduce((sum, item) => sum + item.creditsCount, 0)
  const totalListedVolume = listings.reduce((sum, item) => sum + item.availableQuantity, 0)
  const totalMarketValue = listings.reduce((sum, item) => sum + item.availableQuantity * item.pricePerCredit, 0)

  const refreshAll = () => {
    void batchesQuery.refetch()
    void marketplaceQuery.refetch()
    void companiesQuery.refetch()
    void txnCountQuery.refetch()
  }

  return (
    <>
      <PageIntro
        eyebrow="Quản trị viên"
        title="Giám sát tín chỉ & Sàn giao dịch"
        description="Theo dõi tổng lượng tín chỉ phát hành trên sổ đăng ký, tin niêm yết toàn sàn, doanh nghiệp KYC và chính sách phí sàn 5%."
        actions={
          <Button variant="outline" leftIcon={<RefreshCw className="size-4" />} onClick={refreshAll}>
            Làm mới
          </Button>
        }
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          title="Tổng tín chỉ đã phát hành"
          value={totalIssuedCredits}
          icon={Coins}
          hint={`${batches.length} lô trên sổ đăng ký`}
          tone="brand"
        />
        <StatCard
          title="Đang niêm yết trên sàn"
          value={totalListedVolume}
          icon={Layers3}
          hint={`${listings.length} tin · giá trị ${formatUsd(totalMarketValue)}`}
          tone="slate"
        />
        <StatCard
          title="Doanh nghiệp đã định danh"
          value={(companiesQuery.data ?? []).length}
          icon={Building2}
          hint="Hồ sơ doanh nghiệp KYC hợp lệ"
          tone="slate"
        />
        <StatCard
          title="Giao dịch ví toàn hệ thống"
          value={txnCountQuery.data ?? 0}
          icon={Landmark}
          hint="Phí sàn giao dịch cố định: 5% về ví Admin"
          tone="brand"
        />
      </div>

      {batchesQuery.isPending ? (
        <QueryLoading />
      ) : batchesQuery.isError ? (
        <QueryError onRetry={() => void batchesQuery.refetch()} />
      ) : (
        <div className="space-y-6">
          <SectionCard
            title="Sổ đăng ký lô tín chỉ đã phát hành"
            description={`${batches.length} lô tín chỉ có mã serial duy nhất`}
          >
            {batches.length === 0 ? (
              <EmptyState icon={ShieldCheck} title="Chưa có lô tín chỉ nào" description="Lô tín chỉ xuất hiện sau khi Admin cấp phát." />
            ) : (
              <div className="scrollbar-thin overflow-x-auto p-5">
                <table className="w-full min-w-[780px] text-left text-sm">
                  <thead>
                    <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                      <th className="pb-3 font-semibold">Mã lô</th>
                      <th className="pb-3 font-semibold">Dự án</th>
                      <th className="pb-3 font-semibold">Số tín chỉ</th>
                      <th className="pb-3 font-semibold">Tổng tCO₂e</th>
                      <th className="pb-3 font-semibold">Niên vụ</th>
                      <th className="pb-3 font-semibold">Hạn dùng</th>
                      <th className="pb-3 font-semibold">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {batches.map((batch) => (
                      <tr key={batch.id} className="text-ink-700">
                        <td className="py-3 font-mono text-xs font-semibold text-ink-900">
                          {batch.batchCode || `#${batch.id}`}
                        </td>
                        <td className="py-3">{batch.projectTitle || '—'}</td>
                        <td className="py-3 font-semibold text-ink-900">{formatNumber(batch.creditsCount)}</td>
                        <td className="py-3">{formatNumber(batch.totalTco2e)}</td>
                        <td className="py-3">{batch.vintageYear || '—'}</td>
                        <td className="py-3">{formatDate(batch.expiresAt)}</td>
                        <td className="py-3">
                          <StatusBadge status={batch.status} kind="credit" />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </SectionCard>

          <SectionCard
            title="Tin niêm yết đang giao dịch trên sàn"
            description={`${listings.length} tin đăng · Phí sàn 5% hạch toán tự động về ví Admin khi hoàn tất đơn`}
          >
            {listings.length === 0 ? (
              <EmptyState icon={Layers3} title="Chưa có tin niêm yết" description="Doanh nghiệp niêm yết tín chỉ sẽ hiển thị tại đây." />
            ) : (
              <div className="scrollbar-thin overflow-x-auto p-5">
                <table className="w-full min-w-[780px] text-left text-sm">
                  <thead>
                    <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                      <th className="pb-3 font-semibold">Mã tin</th>
                      <th className="pb-3 font-semibold">Dự án</th>
                      <th className="pb-3 font-semibold">Doanh nghiệp bán</th>
                      <th className="pb-3 font-semibold">Còn lại / Tổng</th>
                      <th className="pb-3 font-semibold">Đơn giá</th>
                      <th className="pb-3 font-semibold">Phí sàn (5%)</th>
                      <th className="pb-3 font-semibold">Hạn niêm yết</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {listings.map((item) => (
                      <tr key={item.listingId} className="text-ink-700">
                        <td className="py-3 font-mono text-xs font-semibold text-ink-900">#{item.listingId}</td>
                        <td className="py-3 font-medium text-ink-900">{item.projectTitle}</td>
                        <td className="py-3">{item.sellerCompanyName}</td>
                        <td className="py-3">
                          {formatNumber(item.availableQuantity)} / {formatNumber(item.quantity)}
                        </td>
                        <td className="py-3 font-semibold text-ink-900">{formatUsd(item.pricePerCredit)}</td>
                        <td className="py-3 font-mono text-xs text-brand-700">
                          {formatUsd(item.pricePerCredit * 0.05)}
                        </td>
                        <td className="py-3">{formatDate(item.expiresAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </SectionCard>
        </div>
      )}
    </>
  )
}
