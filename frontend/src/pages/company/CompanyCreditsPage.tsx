import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Coins, Download, Flame, Store } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import {
  downloadFile,
  errorMessage,
  mapCreditBatch,
  pageOf,
} from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate, formatNumber } from '@/shared/lib/format'
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
  const batches = batchesQuery.data ?? []

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.myCreditBatches })
  }

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
        </div>
      )}
    </>
  )
}
