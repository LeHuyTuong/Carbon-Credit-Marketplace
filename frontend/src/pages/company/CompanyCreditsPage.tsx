import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Coins, Download, Flame, Store, Trash2, X } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import {
  downloadFile,
  errorMessage,
  isRecord,
  mapCreditBatch,
  pageOf,
  type CreditBatch,
} from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate, formatNumber, formatUsd } from '@/shared/lib/format'
import { queryKeys } from '@/shared/lib/queryKeys'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

type CreditsTab = 'batches' | 'listings' | 'retired'
type BatchModal = { mode: 'list' | 'retire'; batch: CreditBatch } | null

export function CompanyCreditsPage() {
  const queryClient = useQueryClient()
  const [tab, setTab] = useState<CreditsTab>('batches')
  const [modal, setModal] = useState<BatchModal>(null)
  const [quantity, setQuantity] = useState('10')
  const [price, setPrice] = useState('20')
  const [formError, setFormError] = useState('')
  const [cancelTargetId, setCancelTargetId] = useState<number | null>(null)

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
      setCancelTargetId(null)
      await invalidate()
    },
    onError: (error) => toast.error('Không huỷ được tin niêm yết', { description: errorMessage(error) }),
  })

  const listMutation = useMutation({
    mutationFn: async ({ batchId, qty, unitPrice }: { batchId: number; qty: number; unitPrice: number }) =>
      api.post<unknown>('/marketplace', {
        data: { batchId, quantity: qty, pricePerCredit: unitPrice },
      }),
    onSuccess: async () => {
      toast.success('Đã niêm yết tín chỉ lên sàn')
      setModal(null)
      await invalidate()
    },
    onError: (error) => toast.error('Không niêm yết được tín chỉ', { description: errorMessage(error) }),
  })

  const retireMutation = useMutation({
    mutationFn: async ({ batchCode, qty }: { batchCode: string; qty: number }) =>
      api.post<unknown>('/my/credits/retire', {
        data: { batchCode, quantity: qty },
      }),
    onSuccess: async () => {
      toast.success('Đã triệt tiêu tín chỉ thành công')
      setModal(null)
      await invalidate()
    },
    onError: (error) => toast.error('Không triệt tiêu được tín chỉ', { description: errorMessage(error) }),
  })

  const openModal = (mode: 'list' | 'retire', batch: CreditBatch) => {
    setFormError('')
    setQuantity(String(Math.min(10, Math.max(1, Math.floor(batch.creditsCount)))))
    setPrice('20')
    setModal({ mode, batch })
  }

  const downloadBatchCertificate = async (batch: CreditBatch) => {
    if (batch.certificateUrl) {
      window.open(batch.certificateUrl, '_blank', 'noreferrer')
      return
    }
    try {
      await downloadFile(`/credits/batches/${batch.id}`, `batch-${batch.batchCode}.json`)
      toast.success(`Đã tải thông tin lô ${batch.batchCode}`)
    } catch (error) {
      toast.error('Lô này chưa có file chứng chỉ PDF đính kèm', { description: errorMessage(error) })
    }
  }

  return (
    <>
      <PageIntro
        eyebrow="Doanh nghiệp"
        title="Kho tín chỉ carbon"
        description="Quản lý các lô tín chỉ đã cấp, niêm yết bán trực tiếp lên sàn, triệt tiêu (retire) và tải chứng chỉ theo từng lô."
      />

      <div className="mb-6 flex flex-wrap gap-2">
        <Button variant={tab === 'batches' ? 'primary' : 'outline'} onClick={() => setTab('batches')}>
          Kho lô tín chỉ ({batches.length})
        </Button>
        <Button variant={tab === 'listings' ? 'primary' : 'outline'} onClick={() => setTab('listings')}>
          Đang niêm yết ({myListings.length})
        </Button>
        <Button variant={tab === 'retired' ? 'primary' : 'outline'} onClick={() => setTab('retired')}>
          Đã triệt tiêu ({retiredCredits.length})
        </Button>
      </div>

      {tab === 'batches' &&
        (batchesQuery.isPending ? (
          <QueryLoading />
        ) : batchesQuery.isError ? (
          <QueryError onRetry={() => void batchesQuery.refetch()} />
        ) : (
          <SectionCard title="Tín chỉ theo lô trong kho" description={`${batches.length} lô tín chỉ đã cấp`}>
            {batches.length === 0 ? (
              <EmptyState icon={Coins} title="Chưa có tín chỉ" description="Tín chỉ được cấp sau khi báo cáo phát thải được phê duyệt." />
            ) : (
              <div className="scrollbar-thin overflow-x-auto p-5">
                <table className="w-full min-w-[820px] text-left text-sm">
                  <thead>
                    <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                      <th className="pb-3 font-semibold">Mã lô</th>
                      <th className="pb-3 font-semibold">Dự án</th>
                      <th className="pb-3 font-semibold">Số tín chỉ</th>
                      <th className="pb-3 font-semibold">Tổng tCO₂e</th>
                      <th className="pb-3 font-semibold">Hạn dùng</th>
                      <th className="pb-3 font-semibold">Trạng thái</th>
                      <th className="pb-3 text-right font-semibold">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {batches.map((batch) => (
                      <tr key={batch.id} className="text-ink-700">
                        <td className="py-3 font-mono text-xs font-semibold text-brand-700">
                          {batch.batchCode || `#${batch.id}`}
                        </td>
                        <td className="py-3 font-medium text-ink-900">{batch.projectTitle || '—'}</td>
                        <td className="py-3 font-semibold">{formatNumber(batch.creditsCount)}</td>
                        <td className="py-3 font-mono">{formatNumber(batch.totalTco2e)}</td>
                        <td className="py-3 text-xs text-ink-500">{formatDate(batch.expiresAt)}</td>
                        <td className="py-3"><StatusBadge status={batch.status} kind="credit" /></td>
                        <td className="py-3 whitespace-nowrap">
                          <div className="flex justify-end gap-1.5">
                            <Button
                              size="sm"
                              variant="outline"
                              className="whitespace-nowrap"
                              leftIcon={<Store className="size-3.5 shrink-0" />}
                              onClick={() => openModal('list', batch)}
                            >
                              Niêm yết
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              className="whitespace-nowrap"
                              leftIcon={<Flame className="size-3.5 shrink-0" />}
                              onClick={() => openModal('retire', batch)}
                            >
                              Triệt tiêu
                            </Button>
                            <Button
                              size="sm"
                              variant="ghost"
                              className="whitespace-nowrap"
                              title="Xem / tải chứng chỉ lô"
                              leftIcon={<Download className="size-3.5 shrink-0" />}
                              onClick={() => void downloadBatchCertificate(batch)}
                            >
                              Chứng chỉ
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </SectionCard>
        ))}

      {tab === 'listings' && (
        <SectionCard title="Tin đang niêm yết trên sàn" description={`${myListings.length} tin bán của doanh nghiệp`}>
          {myListings.length === 0 ? (
            <EmptyState icon={Store} title="Chưa có tin niêm yết nào" description="Chọn nút Niêm yết tại tab Kho lô tín chỉ để đưa tín chỉ lên sàn." />
          ) : (
            <div className="scrollbar-thin overflow-x-auto p-5">
              <table className="w-full min-w-[700px] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                    <th className="pb-3 font-semibold">Dự án</th>
                    <th className="pb-3 font-semibold">Còn lại / Đã bán</th>
                    <th className="pb-3 font-semibold">Đơn giá</th>
                    <th className="pb-3 font-semibold">Hết hạn</th>
                    <th className="pb-3 font-semibold">Trạng thái</th>
                    <th className="pb-3 text-right font-semibold">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {myListings.map((item) => (
                    <tr key={item.id} className="text-ink-700">
                      <td className="py-3 font-semibold text-ink-900">{item.projectTitle}</td>
                      <td className="py-3">
                        {formatNumber(item.quantity)} / {formatNumber(item.soldQuantity)} đã bán
                      </td>
                      <td className="py-3 font-semibold text-brand-700">{formatUsd(item.pricePerCredit)}</td>
                      <td className="py-3 text-xs text-ink-500">{formatDate(item.expiresAt)}</td>
                      <td className="py-3"><StatusBadge status={item.status} kind="credit" /></td>
                      <td className="py-3 text-right">
                        {item.status.toUpperCase() === 'AVAILABLE' ? (
                          <Button
                            size="sm"
                            variant="outline"
                            leftIcon={<Trash2 className="size-3.5" />}
                            loading={cancelListingMutation.isPending && cancelListingMutation.variables === item.id}
                            onClick={() => setCancelTargetId(item.id)}
                          >
                            Huỷ niêm yết
                          </Button>
                        ) : (
                          <span className="text-xs text-ink-400">—</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </SectionCard>
      )}

      {tab === 'retired' && (
        <SectionCard title="Lịch sử tín chỉ đã triệt tiêu (Retire)" description={`${retiredCredits.length} bản ghi triệt tiêu vĩnh viễn`}>
          {retiredCredits.length === 0 ? (
            <EmptyState icon={Flame} title="Chưa có tín chỉ nào được triệt tiêu" description="Chọn nút Triệt tiêu tại tab Kho lô tín chỉ để bù trừ phát thải cho doanh nghiệp." />
          ) : (
            <div className="scrollbar-thin overflow-x-auto p-5">
              <table className="w-full min-w-[640px] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                    <th className="pb-3 font-semibold">Mã tín chỉ</th>
                    <th className="pb-3 font-semibold">Tên</th>
                    <th className="pb-3 font-semibold">Số lượng</th>
                    <th className="pb-3 font-semibold">Ngày cấp</th>
                    <th className="pb-3 font-semibold">Trạng thái</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {retiredCredits.map((c) => (
                    <tr key={c.id} className="text-ink-700">
                      <td className="py-3 font-mono text-xs font-semibold text-ink-900">{c.creditCode || `#${c.id}`}</td>
                      <td className="py-3">{c.name}</td>
                      <td className="py-3 font-semibold">{formatNumber(c.amount)}</td>
                      <td className="py-3 text-xs text-ink-500">{formatDate(c.issuedAt)}</td>
                      <td className="py-3"><StatusBadge status={c.status} kind="credit" /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </SectionCard>
      )}

      {modal && (
        <div
          className="fixed inset-0 z-50 grid place-items-center bg-ink-950/50 p-4 backdrop-blur-xs"
          onClick={() => setModal(null)}
        >
          <div
            role="dialog"
            aria-modal="true"
            className="w-full max-w-md rounded-card bg-white p-6 shadow-float"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-3 border-b border-ink-100 pb-4">
              <div>
                <h3 className="text-base font-bold text-ink-900">
                  {modal.mode === 'list' ? 'Niêm yết tín chỉ lên sàn' : 'Triệt tiêu tín chỉ (Retire)'}
                </h3>
                <p className="mt-1 font-mono text-xs text-brand-700">{modal.batch.batchCode}</p>
              </div>
              <button
                type="button"
                aria-label="Đóng"
                onClick={() => setModal(null)}
                className="grid size-8 place-items-center rounded-lg text-ink-400 hover:bg-ink-100"
              >
                <X className="size-4" />
              </button>
            </div>

            <form
              className="mt-4 space-y-4"
              noValidate
              onSubmit={(e) => {
                e.preventDefault()
                const qty = Number(quantity)
                if (!Number.isInteger(qty) || qty < 1) {
                  setFormError('Số lượng phải là số nguyên lớn hơn hoặc bằng 1')
                  return
                }
                if (modal.mode === 'list') {
                  const unitPrice = Number(price)
                  if (!Number.isFinite(unitPrice) || unitPrice <= 0) {
                    setFormError('Đơn giá phải lớn hơn 0 USD')
                    return
                  }
                  setFormError('')
                  listMutation.mutate({ batchId: modal.batch.id, qty, unitPrice })
                } else {
                  setFormError('')
                  retireMutation.mutate({ batchCode: modal.batch.batchCode, qty })
                }
              }}
            >
              <Input
                label="Số lượng tín chỉ (số nguyên)"
                type="number"
                min={1}
                step={1}
                value={quantity}
                onChange={(e) => {
                  setQuantity(e.target.value)
                  setFormError('')
                }}
                hint={`Lô ${modal.batch.batchCode} · Niên vụ ${modal.batch.vintageYear || '—'}`}
              />
              {modal.mode === 'list' && (
                <Input
                  label="Đơn giá mỗi tín chỉ (USD)"
                  type="number"
                  min={0.01}
                  step={0.01}
                  value={price}
                  onChange={(e) => {
                    setPrice(e.target.value)
                    setFormError('')
                  }}
                />
              )}
              {formError && <p role="alert" className="text-xs font-medium text-rose-600">{formError}</p>}
              <div className="flex justify-end gap-2 border-t border-ink-100 pt-4">
                <Button variant="outline" onClick={() => setModal(null)}>
                  Huỷ
                </Button>
                <Button
                  type="submit"
                  variant={modal.mode === 'retire' ? 'secondary' : 'primary'}
                  loading={listMutation.isPending || retireMutation.isPending}
                >
                  {modal.mode === 'list' ? 'Xác nhận niêm yết' : 'Xác nhận triệt tiêu'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={cancelTargetId !== null}
        title={`Huỷ tin niêm yết #${cancelTargetId ?? ''}?`}
        description="Số tín chỉ chưa bán sẽ được trả về trạng thái khả dụng trong kho của doanh nghiệp."
        confirmLabel="Huỷ niêm yết"
        variant="danger"
        loading={cancelListingMutation.isPending}
        onCancel={() => setCancelTargetId(null)}
        onConfirm={() => {
          if (cancelTargetId !== null) cancelListingMutation.mutate(cancelTargetId)
        }}
      />
    </>
  )
}
