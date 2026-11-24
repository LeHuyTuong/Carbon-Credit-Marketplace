import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BadgeCheck, Coins, FileBarChart2, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import {
  errorMessage,
  isRecord,
  mapCreditBatch,
  mapEmissionReport,
  mapList,
  pageOf,
  type CreditBatch,
  type EmissionReport,
} from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate, formatNumber } from '@/shared/lib/format'
import { queryKeys } from '@/shared/lib/queryKeys'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

export function AdminIssuancePage() {
  const queryClient = useQueryClient()
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [note, setNote] = useState('')
  const [noteError, setNoteError] = useState('')

  const reportsQuery = useQuery({
    queryKey: queryKeys.phase3b.adminReports,
    queryFn: async () => mapList(await api.get<unknown>('/reports/list'), mapEmissionReport),
  })
  const reports = reportsQuery.data ?? []

  useEffect(() => {
    if (selectedId === null && reports.length > 0) setSelectedId(reports[0].id)
  }, [reports, selectedId])

  useEffect(() => {
    setNote('')
    setNoteError('')
  }, [selectedId])

  const detailQuery = useQuery({
    queryKey: selectedId === null ? ['reports', 'admin', 'none'] : queryKeys.phase3b.report(selectedId),
    queryFn: async () => {
      if (selectedId === null) return null
      const data = await api.get<unknown>(`/reports/${selectedId}`)
      return isRecord(data) && (data.id !== undefined || data.reportId !== undefined) ? mapEmissionReport(data) : null
    },
    enabled: selectedId !== null,
    retry: false,
  })
  const canPreview =
    selectedId !== null &&
    Boolean(
      detailQuery.data &&
        (detailQuery.data.status === 'ADMIN_APPROVED' ||
          detailQuery.data.status === 'APPROVED_BY_ADMIN' ||
          detailQuery.data.status === 'APPROVED'),
    )

  const previewQuery = useQuery({
    queryKey: ['credits', 'preview', selectedId],
    queryFn: async () => {
      if (!canPreview || selectedId === null) return null
      const data = await api.get<unknown>(`/credits/preview/${selectedId}`)
      return isRecord(data) && (data.id !== undefined || data.batchCode !== undefined) ? mapCreditBatch(data) : null
    },
    enabled: canPreview,
    retry: false,
  })
  const batchesQuery = useQuery({
    queryKey: ['credits', 'batches'],
    queryFn: async () => pageOf(await api.get<unknown>('/credits/batches', { page: 0, size: 20 })).items.map(mapCreditBatch),
  })

  const refreshAll = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.adminReports }),
      queryClient.invalidateQueries({ queryKey: ['credits', 'batches'] }),
      queryClient.invalidateQueries({ queryKey: ['credits', 'preview', selectedId] }),
      queryClient.invalidateQueries({ queryKey: selectedId === null ? [] : queryKeys.phase3b.report(selectedId) }),
    ])
  }

  const decisionMutation = useMutation({
    mutationFn: async ({ reportId, approved, note }: { reportId: number; approved: boolean; note: string }) =>
      api.put<unknown>(`/reports/${reportId}/approve`, null, { params: { approved, note } }),
    onSuccess: async (_data, variables) => {
      toast.success(variables.approved ? 'Đã phê duyệt báo cáo' : 'Đã từ chối báo cáo')
      await refreshAll()
    },
    onError: (error) => toast.error('Không lưu được quyết định', { description: errorMessage(error) }),
  })

  const issueMutation = useMutation({
    mutationFn: (reportId: number) => api.post<unknown>('/credits/issue', null, { params: { reportId } }),
    onSuccess: async () => {
      toast.success('Đã cấp tín chỉ cho báo cáo')
      await refreshAll()
    },
    onError: (error) => toast.error('Không cấp được tín chỉ', { description: errorMessage(error) }),
  })

  const submitDecision = (approved: boolean) => {
    if (selectedId === null) return
    if (!note.trim()) {
      setNoteError('Vui lòng nhập ghi chú phê duyệt')
      return
    }
    setNoteError('')
    decisionMutation.mutate({ reportId: selectedId, approved, note: note.trim() })
  }

  const report: EmissionReport | undefined = detailQuery.data ?? reports.find((item) => item.id === selectedId)
  const preview: CreditBatch | null = previewQuery.data ?? null
  const batches = batchesQuery.data ?? []

  return (
    <>
      <PageIntro
        eyebrow="Admin"
        title="Phê duyệt & cấp tín chỉ"
        description="Duyệt cuối báo cáo phát thải, xem trước lượng tín chỉ sẽ cấp và phát hành tín chỉ carbon."
      />

      <div className="grid gap-6 xl:grid-cols-[360px_1fr]">
        <SectionCard title="Báo cáo cần xử lý" description={`${reports.length} báo cáo`}>
          {reportsQuery.isPending ? (
            <div className="p-5"><QueryLoading /></div>
          ) : reportsQuery.isError ? (
            <QueryError onRetry={() => void reportsQuery.refetch()} />
          ) : reports.length === 0 ? (
            <EmptyState icon={FileBarChart2} title="Không có báo cáo" description="Báo cáo sau khi CVA duyệt sẽ hiển thị tại đây." />
          ) : (
            <ul className="scrollbar-thin max-h-[42rem] divide-y divide-ink-100 overflow-y-auto p-2">
              {reports.map((item) => (
                <li key={item.id}>
                  <button
                    type="button"
                    onClick={() => setSelectedId(item.id)}
                    className={`w-full rounded-lg px-3 py-3 text-left transition ${
                      item.id === selectedId ? 'bg-brand-50' : 'hover:bg-ink-50'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="truncate text-sm font-semibold text-ink-900">
                        {item.projectName || `Báo cáo #${item.id}`}
                      </span>
                      <StatusBadge status={item.status} kind="emission" />
                    </div>
                    <p className="mt-1 text-xs text-ink-500">
                      {item.sellerName || 'Doanh nghiệp chưa cập nhật'} · Kỳ {item.period} · {formatDate(item.submittedAt)}
                    </p>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </SectionCard>

        <div className="min-w-0 space-y-6">
          <SectionCard
            title="Xem trước tín chỉ"
            description="Số lượng tín chỉ dự kiến được tính từ phát thải và hệ số dự án."
            action={
              selectedId !== null ? (
                <Button
                  variant="outline"
                  size="sm"
                  leftIcon={<RefreshCw className="size-4" />}
                  onClick={() => void previewQuery.refetch()}
                >
                  Làm mới
                </Button>
              ) : undefined
            }
          >
            {selectedId === null ? (
              <EmptyState icon={Coins} title="Chưa chọn báo cáo" description="Chọn một báo cáo để xem trước tín chỉ." />
            ) : (() => {
              const statusUpper = (report?.status ?? '').toUpperCase()
              const canApproveOrReject = statusUpper === 'CVA_APPROVED'
              const canIssue = statusUpper === 'ADMIN_APPROVED'
              const isAlreadyIssued = statusUpper === 'CREDIT_ISSUED' || statusUpper === 'PAID_OUT'

              return (
              <div className="space-y-5 p-5">
                <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                  <Metric label="Tổng năng lượng" value={`${formatNumber(report?.totalEnergy)} kWh`} />
                  <Metric label="Tổng CO₂" value={`${formatNumber(report?.totalCo2)} kg`} />
                  <Metric label="Kỳ báo cáo" value={report?.period || '—'} />
                  <Metric label="Số xe" value={formatNumber(report?.vehicleCount)} />
                </div>

                {isAlreadyIssued ? (
                  <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
                    Báo cáo này đã hoàn tất phát hành tín chỉ. Xem chi tiết mã lô ở bảng <strong>Các lô đã cấp</strong> bên dưới.
                  </div>
                ) : previewQuery.isPending ? (
                  <div className="h-32 animate-pulse rounded-xl bg-ink-100" />
                ) : previewQuery.isError ? (
                  <QueryError
                    title="Chưa có bản xem trước"
                    description="Báo cáo cần được CVA và Admin phê duyệt trước khi tính toán lượng tín chỉ cấp phát."
                    onRetry={() => void previewQuery.refetch()}
                  />
                ) : preview ? (
                  <div className="rounded-xl border border-brand-200 bg-brand-50 p-4">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-brand-900">Tín chỉ dự kiến</p>
                        <p className="mt-0.5 text-xs text-brand-700">
                          Mã lô {preview.batchCode || '—'} · vintage {preview.vintageYear || '—'}
                        </p>
                      </div>
                      <span className="text-2xl font-bold text-brand-800">{formatNumber(preview.creditsCount)}</span>
                    </div>
                    <div className="mt-3 grid gap-2 text-xs text-brand-800 sm:grid-cols-3">
                      <p>Tổng tCO₂e: {formatNumber(preview.totalTco2e)}</p>
                      <p>Tồn lại: {formatNumber(preview.residualTco2e)}</p>
                      <p>Dự án: {preview.projectTitle || '—'}</p>
                    </div>
                  </div>
                ) : (
                  <EmptyState icon={Coins} title="Không có dữ liệu xem trước" description="Hãy kiểm tra trạng thái báo cáo." />
                )}

                {!isAlreadyIssued && (
                  <div className="space-y-4 border-t border-ink-100 pt-4">
                    <Input
                      label="Ghi chú phê duyệt"
                      placeholder="Bắt buộc nhập khi duyệt hoặc từ chối"
                      value={note}
                      error={noteError}
                      disabled={!canApproveOrReject}
                      onChange={(event) => {
                        setNote(event.target.value)
                        if (event.target.value.trim()) setNoteError('')
                      }}
                    />
                    <div className="flex flex-wrap gap-2">
                      <Button
                        leftIcon={<BadgeCheck className="size-4" />}
                        disabled={!canApproveOrReject}
                        loading={decisionMutation.isPending && decisionMutation.variables?.approved === true}
                        onClick={() => submitDecision(true)}
                      >
                        Phê duyệt báo cáo
                      </Button>
                      <Button
                        variant="danger"
                        disabled={!canApproveOrReject}
                        loading={decisionMutation.isPending && decisionMutation.variables?.approved === false}
                        onClick={() => submitDecision(false)}
                      >
                        Từ chối báo cáo
                      </Button>
                      <Button
                        variant="secondary"
                        leftIcon={<Coins className="size-4" />}
                        disabled={!canIssue}
                        loading={issueMutation.isPending}
                        onClick={() => {
                          if (selectedId !== null) issueMutation.mutate(selectedId)
                        }}
                      >
                        Cấp tín chỉ
                      </Button>
                    </div>
                    {!canApproveOrReject && !canIssue && (
                      <p className="text-xs text-ink-500">
                        Báo cáo đang ở trạng thái chờ CVA thẩm định hoặc đã bị từ chối nên chưa thể phê duyệt/cấp tín chỉ.
                      </p>
                    )}
                  </div>
                )}
              </div>
              )
            })()}
          </SectionCard>

          <SectionCard title="Các lô đã cấp" description={`${batches.length} lô tín chỉ gần đây`}>
            {batchesQuery.isPending ? (
              <div className="p-5"><QueryLoading /></div>
            ) : batchesQuery.isError ? (
              <QueryError onRetry={() => void batchesQuery.refetch()} />
            ) : batches.length === 0 ? (
              <EmptyState icon={Coins} title="Chưa có lô tín chỉ" description="Tín chỉ sau khi phát hành sẽ hiển thị tại đây." />
            ) : (
              <div className="scrollbar-thin overflow-x-auto p-5">
                <table className="w-full min-w-[640px] text-left text-sm">
                  <thead>
                    <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                      <th className="pb-3 font-semibold">Mã lô</th>
                      <th className="pb-3 font-semibold">Dự án</th>
                      <th className="pb-3 font-semibold">Số lượng</th>
                      <th className="pb-3 font-semibold">Tổng tCO₂e</th>
                      <th className="pb-3 font-semibold">Ngày cấp</th>
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
                        <td className="py-3">{formatDate(batch.issuedAt)}</td>
                        <td className="py-3"><StatusBadge status={batch.status} kind="credit" /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </SectionCard>
        </div>
      </div>
    </>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-ink-200 p-3">
      <p className="text-xs text-ink-500">{label}</p>
      <p className="mt-1 text-sm font-semibold text-ink-900">{value}</p>
    </div>
  )
}
