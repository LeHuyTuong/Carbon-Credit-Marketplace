import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Coins, Download, Flame, Store, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import {
  downloadFile,
  errorMessage,
  isRecord,
  mapCreditBatch,
  pageOf,
} from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate, formatNumber, formatUsd } from '@/shared/lib/format'
import { queryKeys } from '@/shared/lib/queryKeys'
import { useAuthStore } from '@/shared/stores/auth.store'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

export function CompanyCreditsPage() {
  const queryClient = useQueryClient()
  const userId = useAuthStore((state) => state.user?.id)
  const [listingBatch, setListingBatch] = useState('')
  const [listingQuantity, setListingQuantity] = useState('')
  const [listingPrice, setListingPrice] = useState('')
  const [retireBatch, setRetireBatch] = useState('')
  const [retireQuantity, setRetireQuantity] = useState('')

  const batchesQuery = useQuery({
    queryKey: queryKeys.phase3b.myCreditBatches,
    queryFn: async () => pageOf(await api.get<unknown>('/my/credits/batches')).items.map(mapCreditBatch),
  })
  const myListingsQuery = useQuery({
    queryKey: ['marketplace', 'company-mine'],
    queryFn: async () => {
      const data = await api.get<unknown>('/marketplace/company')
      const arr = Array.isArray(data) ? data : pageOf(data).items
      return arr.map((raw) => {
        const r = isRecord(raw) ? raw : {}
        return {
          id: Number(r.listingId ?? r.id ?? 0),
          projectTitle: String(r.projectTitle ?? r.projectName ?? 'Tín chỉ carbon'),
          quantity: Number(r.quantity ?? 0),
          soldQuantity: Number(r.soldQuantity ?? 0),
          pricePerCredit: Number(r.pricePerCredit ?? 0),
          status: String(r.status ?? 'AVAILABLE'),
          expiresAt: String(r.expiresAt ?? ''),
        }
      })
    },
    retry: false,
  })
  const retiredQuery = useQuery({
    queryKey: ['my-credits', 'retired'],
    queryFn: async () => {
      const data = await api.get<unknown>('/my/credits', { page: 0, size: 50 })
      return pageOf(data)
        .items.map((raw) => {
          const r = isRecord(raw) ? raw : {}
          return {
            id: Number(r.id ?? 0),
            creditCode: String(r.creditCode ?? r.credit_code ?? ''),
            name: String(r.name ?? 'Tín chỉ carbon'),
            amount: Number(r.carbonCredit ?? r.amount ?? 0),
            status: String(r.status ?? ''),
            issuedAt: String(r.issuedAt ?? ''),
          }
        })
        .filter((item) => item.status.toUpperCase() === 'RETIRED')
    },
    retry: false,
  })
  const batches = batchesQuery.data ?? []
  const myListings = myListingsQuery.data ?? []
  const retiredCredits = retiredQuery.data ?? []

  const invalidate = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.myCreditBatches }),
      queryClient.invalidateQueries({ queryKey: ['marketplace', 'company-mine'] }),
      queryClient.invalidateQueries({ queryKey: ['my-credits', 'retired'] }),
    ])
  }

  const cancelListingMutation = useMutation({
    mutationFn: (listingId: number) => api.delete<unknown>(`/marketplace/${listingId}`),
    onSuccess: async () => {
      toast.success('Đã huỷ tin niêm yết trên sàn')
      await invalidate()
    },
    onError: (error) => toast.error('Không huỷ được tin niêm yết', { description: errorMessage(error) }),
  })

  const listMutation = useMutation({
    mutationFn: async () =>
      api.post<unknown>('/marketplace', {
        data: {
          batchId: Number(listingBatch),
          quantity: Number(listingQuantity),
          pricePerCredit: Number(listingPrice),
        },
      }),
    onSuccess: async () => {
      toast.success('Đã niêm yết tín chỉ lên sàn')
      setListingBatch('')
      setListingQuantity('')
      setListingPrice('')
      await invalidate()
    },
    onError: (error) => toast.error('Không niêm yết được tín chỉ', { description: errorMessage(error) }),
  })

  const retireMutation = useMutation({
    mutationFn: async () => {
      const batch = batches.find((item) => String(item.id) === retireBatch)
      return api.post<unknown>('/my/credits/retire', {
        data: { batchCode: batch?.batchCode ?? '', quantity: Number(retireQuantity) },
      })
    },
    onSuccess: async () => {
      toast.success('Đã triệt tiêu tín chỉ')
      setRetireBatch('')
      setRetireQuantity('')
      await invalidate()
    },
    onError: (error) => toast.error('Không triệt tiêu được tín chỉ', { description: errorMessage(error) }),
  })

  const downloadCertificate = async () => {
    if (userId === undefined) {
      toast.error('Không xác định được người dùng để tải chứng chỉ')
      return
    }
    try {
      await downloadFile(`/reports/download/${userId}`, `certificate-${userId}.pdf`)
      toast.success('Đã tải chứng chỉ PDF')
    } catch (error) {
      toast.error('Không tải được chứng chỉ', { description: errorMessage(error) })
    }
  }

  const selectedListingBatch = batches.find((item) => String(item.id) === listingBatch)

  return (
    <>
      <PageIntro
        eyebrow="Doanh nghiệp"
        title="Kho tín chỉ carbon"
        description="Xem tín chỉ theo lô, niêm yết lên sàn, triệt tiêu tín chỉ và tải chứng chỉ PDF."
        actions={
          <Button
            variant="outline"
            leftIcon={<Download className="size-4" />}
            onClick={() => void downloadCertificate()}
          >
            Tải chứng chỉ PDF
          </Button>
        }
      />

      {batchesQuery.isPending ? (
        <QueryLoading />
      ) : batchesQuery.isError ? (
        <QueryError onRetry={() => void batchesQuery.refetch()} />
      ) : (
        <div className="space-y-6">
          <SectionCard title="Tín chỉ theo lô" description={`${batches.length} lô tín chỉ trong kho`}>
            {batches.length === 0 ? (
              <EmptyState icon={Coins} title="Chưa có tín chỉ" description="Tín chỉ được cấp sau khi báo cáo phát thải được phê duyệt." />
            ) : (
              <div className="scrollbar-thin overflow-x-auto p-5">
                <table className="w-full min-w-[720px] text-left text-sm">
                  <thead>
                    <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                      <th className="pb-3 font-semibold">Mã lô</th>
                      <th className="pb-3 font-semibold">Dự án</th>
                      <th className="pb-3 font-semibold">Số lượng</th>
                      <th className="pb-3 font-semibold">Tổng tCO₂e</th>
                      <th className="pb-3 font-semibold">Hạn dùng</th>
                      <th className="pb-3 font-semibold">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {batches.map((batch) => (
                      <tr key={batch.id} className="text-ink-700">
                        <td className="py-3 font-semibold text-ink-900">{batch.batchCode || `#${batch.id}`}</td>
                        <td className="py-3">{batch.projectTitle || '—'}</td>
                        <td className="py-3">{formatNumber(batch.creditsCount)}</td>
                        <td className="py-3">{formatNumber(batch.totalTco2e)}</td>
                        <td className="py-3">{formatDate(batch.expiresAt)}</td>
                        <td className="py-3"><StatusBadge status={batch.status} kind="credit" /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </SectionCard>

          <div className="grid gap-6 lg:grid-cols-2">
            <SectionCard title="Niêm yết lên sàn" description="Chọn lô và số lượng tín chỉ muốn bán.">
              <form
                className="space-y-4 p-5"
                noValidate
                onSubmit={(event) => {
                  event.preventDefault()
                  if (!listingBatch || !listingQuantity || !listingPrice) {
                    toast.error('Vui lòng chọn lô, nhập số lượng và giá bán')
                    return
                  }
                  listMutation.mutate()
                }}
              >
                <div>
                  <label htmlFor="listing-batch" className="mb-1.5 block text-sm font-medium text-ink-700">
                    Lô tín chỉ
                  </label>
                  <select
                    id="listing-batch"
                    value={listingBatch}
                    onChange={(event) => setListingBatch(event.target.value)}
                    className="h-11 w-full rounded-xl border border-ink-200 bg-white px-3.5 text-sm"
                  >
                    <option value="">Chọn lô tín chỉ</option>
                    {batches.map((batch) => (
                      <option key={batch.id} value={batch.id}>
                        {batch.batchCode || `#${batch.id}`} — khả dụng {formatNumber(batch.creditsCount)}
                      </option>
                    ))}
                  </select>
                </div>
                {selectedListingBatch && (
                  <p className="text-xs text-ink-500">
                    Lô {selectedListingBatch.batchCode} · niên vụ {selectedListingBatch.vintageYear || '—'} · tồn {formatNumber(selectedListingBatch.residualTco2e)} tCO₂e
                  </p>
                )}
                <Input
                  label="Số lượng tín chỉ"
                  type="number"
                  min={0.01}
                  step={0.01}
                  value={listingQuantity}
                  onChange={(event) => setListingQuantity(event.target.value)}
                  placeholder="0.00"
                />
                <Input
                  label="Giá mỗi tín chỉ"
                  type="number"
                  min={0.01}
                  step={0.01}
                  value={listingPrice}
                  onChange={(event) => setListingPrice(event.target.value)}
                  placeholder="0.00"
                />
                <Button type="submit" loading={listMutation.isPending} leftIcon={<Store className="size-4" />}>
                  Niêm yết
                </Button>
              </form>
            </SectionCard>

            <SectionCard title="Triệt tiêu tín chỉ" description="Triệt tiêu một phần số lượng trong lô đã chọn.">
              <form
                className="space-y-4 p-5"
                noValidate
                onSubmit={(event) => {
                  event.preventDefault()
                  if (!retireBatch || !retireQuantity) {
                    toast.error('Vui lòng chọn lô và nhập số lượng triệt tiêu')
                    return
                  }
                  retireMutation.mutate()
                }}
              >
                <div>
                  <label htmlFor="retire-batch" className="mb-1.5 block text-sm font-medium text-ink-700">
                    Lô tín chỉ
                  </label>
                  <select
                    id="retire-batch"
                    value={retireBatch}
                    onChange={(event) => setRetireBatch(event.target.value)}
                    className="h-11 w-full rounded-xl border border-ink-200 bg-white px-3.5 text-sm"
                  >
                    <option value="">Chọn lô tín chỉ</option>
                    {batches.map((batch) => (
                      <option key={batch.id} value={batch.id}>
                        {batch.batchCode || `#${batch.id}`}
                      </option>
                    ))}
                  </select>
                </div>
                <Input
                  label="Số lượng triệt tiêu"
                  type="number"
                  min={0.01}
                  step={0.01}
                  value={retireQuantity}
                  onChange={(event) => setRetireQuantity(event.target.value)}
                  placeholder="0.00"
                />
                <Button type="submit" variant="secondary" loading={retireMutation.isPending} leftIcon={<Flame className="size-4" />}>
                  Triệt tiêu
                </Button>
              </form>
            </SectionCard>
          </div>

          {/* Lịch sử tin đã niêm yết (SRS 7a & BR-13) */}
          <SectionCard
            title="Lịch sử tin niêm yết trên sàn"
            description={`${myListings.length} tin niêm yết của doanh nghiệp · Phí sàn giao dịch 5%`}
          >
            {myListings.length === 0 ? (
              <EmptyState icon={Store} title="Chưa có tin niêm yết nào" description="Tin niêm yết bán tín chỉ của bạn sẽ hiển thị tại đây." />
            ) : (
              <div className="scrollbar-thin overflow-x-auto p-5">
                <table className="w-full min-w-[720px] text-left text-sm">
                  <thead>
                    <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                      <th className="pb-3 font-semibold">Mã tin</th>
                      <th className="pb-3 font-semibold">Dự án</th>
                      <th className="pb-3 font-semibold">Còn lại</th>
                      <th className="pb-3 font-semibold">Đã bán</th>
                      <th className="pb-3 font-semibold">Đơn giá</th>
                      <th className="pb-3 font-semibold">Trạng thái</th>
                      <th className="pb-3 text-right font-semibold">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {myListings.map((item) => {
                      const canCancel = ['AVAILABLE', 'LISTED'].includes(item.status.toUpperCase())
                      return (
                        <tr key={item.id} className="text-ink-700">
                          <td className="py-3 font-mono text-xs font-semibold text-ink-900">#{item.id}</td>
                          <td className="py-3 font-medium text-ink-900">{item.projectTitle}</td>
                          <td className="py-3">{formatNumber(item.quantity)}</td>
                          <td className="py-3">{formatNumber(item.soldQuantity)}</td>
                          <td className="py-3 font-semibold">{formatUsd(item.pricePerCredit)}</td>
                          <td className="py-3">
                            <StatusBadge status={item.status} kind="credit" />
                          </td>
                          <td className="py-3 text-right">
                            {canCancel ? (
                              <Button
                                size="sm"
                                variant="outline"
                                leftIcon={<Trash2 className="size-3.5" />}
                                loading={cancelListingMutation.isPending && cancelListingMutation.variables === item.id}
                                onClick={() => cancelListingMutation.mutate(item.id)}
                              >
                                Huỷ niêm yết
                              </Button>
                            ) : (
                              <span className="text-xs text-ink-400">—</span>
                            )}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </SectionCard>

          {/* Lịch sử tín chỉ đã triệt tiêu (SRS 9b) */}
          <SectionCard
            title="Lịch sử tín chỉ đã triệt tiêu (Retire)"
            description={`${retiredCredits.length} bản ghi tín chỉ đã thu hồi vĩnh viễn để bù trừ phát thải`}
          >
            {retiredCredits.length === 0 ? (
              <EmptyState icon={Flame} title="Chưa có tín chỉ đã triệt tiêu" description="Sau khi triệt tiêu tín chỉ, chứng nhận sẽ lưu vĩnh viễn tại đây." />
            ) : (
              <div className="scrollbar-thin overflow-x-auto p-5">
                <table className="w-full min-w-[680px] text-left text-sm">
                  <thead>
                    <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                      <th className="pb-3 font-semibold">Mã tín chỉ</th>
                      <th className="pb-3 font-semibold">Tên tín chỉ</th>
                      <th className="pb-3 font-semibold">Số lượng</th>
                      <th className="pb-3 font-semibold">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {retiredCredits.map((item) => (
                      <tr key={item.id} className="text-ink-700">
                        <td className="py-3 font-mono text-xs font-semibold text-ink-900">{item.creditCode || `#${item.id}`}</td>
                        <td className="py-3">{item.name}</td>
                        <td className="py-3 font-semibold">{formatNumber(item.amount)}</td>
                        <td className="py-3"><StatusBadge status="RETIRED" kind="credit" /></td>
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
