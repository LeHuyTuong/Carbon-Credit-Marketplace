import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Coins, Eye, RefreshCw, Search, ShieldCheck } from 'lucide-react'
import { api } from '@/shared/api/client'
import { mapCreditBatch, pageOf, type CreditBatch } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate, formatNumber } from '@/shared/lib/format'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

export function CvaCreditsPage() {
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState<CreditBatch | null>(null)

  const batchesQuery = useQuery({
    queryKey: ['cva', 'credit-batches'],
    queryFn: async () =>
      pageOf(await api.get<unknown>('/credits/batches', { page: 0, size: 50 })).items.map(mapCreditBatch),
  })

  const batches = batchesQuery.data ?? []
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return batches
    return batches.filter(
      (b) =>
        b.batchCode.toLowerCase().includes(q) ||
        b.projectTitle.toLowerCase().includes(q) ||
        b.companyName.toLowerCase().includes(q),
    )
  }, [batches, search])

  return (
    <>
      <PageIntro
        eyebrow="CVA · Thẩm định"
        title="Lô tín chỉ đã phát hành"
        description="Theo dõi sổ đăng ký lô tín chỉ carbon, dải số serial và trạng thái chứng nhận sau thẩm định."
        actions={
          <Button
            variant="outline"
            leftIcon={<RefreshCw className="size-4" />}
            onClick={() => void batchesQuery.refetch()}
          >
            Làm mới
          </Button>
        }
      />

      {batchesQuery.isPending ? (
        <QueryLoading />
      ) : batchesQuery.isError ? (
        <QueryError onRetry={() => void batchesQuery.refetch()} />
      ) : (
        <SectionCard
          title="Sổ đăng ký lô tín chỉ"
          description={`${batches.length} lô tín chỉ · ${filtered.length} kết quả`}
        >
          <div className="border-b border-ink-100 p-5">
            <Input
              label="Tìm kiếm lô tín chỉ"
              placeholder="Mã lô, tên dự án hoặc doanh nghiệp"
              value={search}
              icon={<Search className="size-4" />}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {filtered.length === 0 ? (
            <EmptyState
              icon={Coins}
              title="Chưa có lô tín chỉ"
              description="Các lô tín chỉ sau khi Admin phát hành sẽ hiển thị tại đây."
            />
          ) : (
            <div className="scrollbar-thin overflow-x-auto p-5">
              <table className="w-full min-w-[780px] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                    <th className="pb-3 font-semibold">Mã lô</th>
                    <th className="pb-3 font-semibold">Dự án / Doanh nghiệp</th>
                    <th className="pb-3 font-semibold">Số tín chỉ</th>
                    <th className="pb-3 font-semibold">Tổng tCO₂e</th>
                    <th className="pb-3 font-semibold">Trạng thái</th>
                    <th className="pb-3 text-right font-semibold">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {filtered.map((batch) => (
                    <tr key={batch.id} className="text-ink-700">
                      <td className="py-3 font-mono text-xs font-semibold text-brand-700">{batch.batchCode}</td>
                      <td className="py-3">
                        <p className="font-semibold text-ink-900">{batch.projectTitle || '—'}</p>
                        <p className="text-xs text-ink-500">{batch.companyName || '—'}</p>
                      </td>
                      <td className="py-3 font-semibold">{formatNumber(batch.creditsCount)}</td>
                      <td className="py-3 font-mono">{formatNumber(batch.totalTco2e)}</td>
                      <td className="py-3">
                        <StatusBadge status={batch.status} />
                      </td>
                      <td className="py-3 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          leftIcon={<Eye className="size-3.5" />}
                          onClick={() => setSelected(batch)}
                        >
                          Chi tiết
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </SectionCard>
      )}

      {selected && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/40 p-4 backdrop-blur-xs"
          onClick={() => setSelected(null)}
        >
          <div
            className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between border-b border-ink-100 pb-4">
              <div>
                <p className="font-mono text-xs font-semibold text-brand-700">{selected.batchCode}</p>
                <h3 className="mt-1 text-lg font-bold text-ink-900">Chi tiết lô tín chỉ #{selected.id}</h3>
              </div>
              <StatusBadge status={selected.status} />
            </div>
            <dl className="mt-4 space-y-2.5 text-sm">
              <div className="flex justify-between">
                <dt className="text-ink-500">Số lượng tín chỉ phát hành</dt>
                <dd className="font-semibold text-ink-900">{formatNumber(selected.creditsCount)} tín chỉ</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-500">Tổng phát thải quy đổi</dt>
                <dd className="font-mono font-semibold text-ink-900">{formatNumber(selected.totalTco2e)} tCO₂e</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-500">Kỳ báo cáo / Năm</dt>
                <dd className="font-mono text-ink-900">{selected.reportPeriod || selected.vintageYear || '—'}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-500">Dải số serial</dt>
                <dd className="font-mono text-ink-900">
                  {selected.serialFrom && selected.serialTo ? `${selected.serialFrom} → ${selected.serialTo}` : '—'}
                </dd>
              </div>
              {selected.issuedAt && (
                <div className="flex justify-between">
                  <dt className="text-ink-500">Ngày phát hành</dt>
                  <dd className="text-ink-900">{formatDate(selected.issuedAt)}</dd>
                </div>
              )}
            </dl>
            <div className="mt-4 flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs text-emerald-900">
              <ShieldCheck className="size-4 shrink-0 text-emerald-600" />
              Lô tín chỉ bất biến: số lượng và dải serial giữ nguyên kể cả khi quy tắc dự án thay đổi về sau.
            </div>
            <div className="mt-6 flex justify-end">
              <Button variant="outline" onClick={() => setSelected(null)}>
                Đóng
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
