import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { Bot, CheckCircle2, FileCheck2, Play, ShieldAlert, XCircle } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import {
  errorMessage,
  isRecord,
  mapAiEvaluation,
  mapEmissionReport,
  mapList,
  mapRule,
  pageOf,
  type AiEvaluation,
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

export function CvaReportsPage() {
  const queryClient = useQueryClient()
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [aiScore, setAiScore] = useState<AiEvaluation | null>(null)
  const [verificationScore, setVerificationScore] = useState('7')
  const [comment, setComment] = useState('')
  const [commentError, setCommentError] = useState('')

  const reportsQuery = useQuery({
    queryKey: queryKeys.phase3b.cvaReports,
    queryFn: async () => pageOf(await api.get<unknown>('/reports/list-cva-check')).items.map(mapEmissionReport),
  })
  const reports = reportsQuery.data ?? []

  useEffect(() => {
    if (selectedId === null && reports.length > 0) setSelectedId(reports[0].id)
  }, [reports, selectedId])

  useEffect(() => {
    setAiScore(null)
    setComment('')
  }, [selectedId])

  const detailQuery = useQuery({
    queryKey: selectedId === null ? ['reports', 'detail', 'none'] : queryKeys.phase3b.report(selectedId),
    queryFn: async () => {
      if (selectedId === null) return null
      const data = await api.get<unknown>(`/reports/${selectedId}`)
      return isRecord(data) && (data.id !== undefined || data.reportId !== undefined) ? mapEmissionReport(data) : null
    },
    enabled: selectedId !== null,
    retry: false,
  })
  const rulesQuery = useQuery({
    queryKey: selectedId === null ? ['reports', 'rules', 'none'] : queryKeys.phase3b.analysisRules(selectedId),
    queryFn: async () => {
      if (selectedId === null) return []
      return mapList(await api.get<unknown>(`/reports/${selectedId}/analysis/rules`), mapRule)
    },
    enabled: selectedId !== null,
    retry: false,
  })
  const evDetailsQuery = useQuery({
    queryKey: ['reports', 'ev-details', selectedId],
    queryFn: async () => {
      if (selectedId === null) return []
      const data = await api.get<unknown>(`/reports/${selectedId}/details`, { page: 0, size: 50 })
      const items = pageOf(data).items
      return items.map((raw) => {
        const r = isRecord(raw) ? raw : {}
        return {
          id: Number(r.id ?? 0),
          plate: String(r.vehiclePlate ?? r.vehicle_plate ?? '—'),
          period: String(r.period ?? '—'),
          energy: Number(r.totalEnergy ?? r.total_energy ?? 0),
          co2Kg: Number(r.co2Kg ?? r.co2_kg ?? 0),
        }
      })
    },
    enabled: selectedId !== null,
    retry: false,
  })

  const refreshAnalysis = async () => {
    await queryClient.invalidateQueries({
      queryKey: selectedId === null ? queryKeys.phase3b.cvaReports : queryKeys.phase3b.analysisRules(selectedId),
    })
  }

  const analyzeMutation = useMutation({
    mutationFn: (reportId: number) => api.post<unknown>(`/reports/${reportId}/analyze`, null, { params: { persist: true } }),
    onSuccess: async () => {
      toast.success('Đã chạy phân tích dữ liệu báo cáo')
      await refreshAnalysis()
    },
    onError: (error) => toast.error('Không chạy được phân tích', { description: errorMessage(error) }),
  })

  const aiMutation = useMutation({
    mutationFn: async (reportId: number) => {
      const data = await api.post<unknown>(`/reports/${reportId}/ai-score`)
      return isRecord(data) ? mapAiEvaluation(data) : null
    },
    onSuccess: (result) => {
      if (result?.aiPreScore !== null && result?.aiPreScore !== undefined) {
        setVerificationScore(String(result.aiPreScore))
      }
      setAiScore(result)
      toast.success('Đã có điểm AI gợi ý')
    },
    onError: (error) => toast.error('Không chấm điểm AI được', { description: errorMessage(error) }),
  })

  const verifyMutation = useMutation({
    mutationFn: async ({ reportId, approved, note, score }: { reportId: number; approved: boolean; note: string; score: number }) => {
      // Endpoint có body lưu được verificationScore; nếu gateway cũ chỉ có PUT thì dùng params.
      try {
        return await api.post<unknown>(`/reports/${reportId}/verify`, {
          verificationScore: score,
          comment: note,
          approved,
        })
      } catch (error) {
        if (error instanceof AxiosError && [404, 405].includes(error.response?.status ?? 0)) {
          return api.put<unknown>(`/reports/${reportId}/verify`, null, { params: { approved, comment: note } })
        }
        throw error
      }
    },
    onSuccess: async (_data, variables) => {
      toast.success(variables.approved ? 'Đã duyệt báo cáo' : 'Đã từ chối báo cáo')
      await queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.cvaReports })
    },
    onError: (error) => toast.error('Không lưu được quyết định', { description: errorMessage(error) }),
  })

  const report: EmissionReport | undefined = detailQuery.data ?? reports.find((item) => item.id === selectedId)
  const rules = rulesQuery.data ?? []
  const violations = rules.filter((rule) => rule.score < rule.maxScore).length
  const submitDecision = (approved: boolean) => {
    if (!selectedId) return
    if (!comment.trim()) {
      setCommentError('Vui lòng nhập nhận xét thẩm định')
      return
    }
    const score = Number(verificationScore)
    if (!Number.isFinite(score) || score < 0 || score > 10) {
      toast.error('Điểm thẩm định phải nằm trong khoảng 0–10')
      return
    }
    setCommentError('')
    verifyMutation.mutate({ reportId: selectedId, approved, note: comment.trim(), score })
  }

  return (
    <>
      <PageIntro
        eyebrow="CVA"
        title="Thẩm định báo cáo phát thải"
        description="Chạy phân tích, đối chiếu 8 luật gian lận và điểm AI gợi ý trước khi duyệt hoặc từ chối báo cáo."
      />

      <div className="grid gap-6 xl:grid-cols-[360px_1fr]">
        <SectionCard title="Báo cáo chờ thẩm định" description={`${reports.length} báo cáo`}>
          {reportsQuery.isPending ? (
            <div className="p-5"><QueryLoading /></div>
          ) : reportsQuery.isError ? (
            <QueryError onRetry={() => void reportsQuery.refetch()} />
          ) : reports.length === 0 ? (
            <EmptyState icon={FileCheck2} title="Không có báo cáo" description="Báo cáo doanh nghiệp nộp sẽ hiển thị tại đây." />
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
                      {item.sellerName || 'Doanh nghiệp chưa cập nhật'} · {formatDate(item.submittedAt)}
                    </p>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </SectionCard>

        <SectionCard
          title="Bối cảnh quyết định"
          description="Số liệu phát thải, kết quả luật gian lận và điểm AI."
          action={
            selectedId !== null ? (
              <div className="flex flex-wrap gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  leftIcon={<Play className="size-4" />}
                  loading={analyzeMutation.isPending}
                  onClick={() => analyzeMutation.mutate(selectedId)}
                >
                  Chạy phân tích
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  leftIcon={<Bot className="size-4" />}
                  loading={aiMutation.isPending}
                  onClick={() => aiMutation.mutate(selectedId)}
                >
                  Chấm điểm AI
                </Button>
              </div>
            ) : undefined
          }
        >
          {selectedId === null ? (
            <EmptyState icon={FileCheck2} title="Chưa chọn báo cáo" description="Chọn một báo cáo để xem bối cảnh thẩm định." />
          ) : (
            <div className="space-y-5 p-5">
              {detailQuery.isError ? (
                <QueryError title="Không tải được chi tiết báo cáo" onRetry={() => void detailQuery.refetch()} />
              ) : detailQuery.isPending ? (
                <div className="h-24 animate-pulse rounded-xl bg-ink-100" />
              ) : null}

              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                <Metric label="Tổng năng lượng sạc" value={`${formatNumber(report?.totalEnergy)} kWh`} />
                <Metric label="Tổng CO₂" value={`${formatNumber(report?.totalCo2)} kg`} />
                <Metric label="Số xe trong báo cáo" value={formatNumber(report?.vehicleCount)} />
                <Metric label="Kỳ báo cáo" value={report?.period || '—'} />
                <Metric label="Dự án" value={report?.projectName || '—'} />
                <Metric label="Doanh nghiệp" value={report?.sellerName || '—'} />
              </div>

              <div className="rounded-xl border border-brand-200 bg-brand-50 p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-brand-900">Điểm AI gợi ý</p>
                    <p className="mt-0.5 text-xs text-brand-700">
                      {aiScore ? `Phiên bản ${aiScore.aiVersion || '—'}` : 'Chưa chấm điểm — bấm “Chấm điểm AI”.'}
                    </p>
                  </div>
                  <span className="text-2xl font-bold text-brand-800">
                    {aiScore?.aiPreScore === null || aiScore?.aiPreScore === undefined
                      ? '—'
                      : formatNumber(aiScore.aiPreScore)}
                  </span>
                </div>
                {aiScore?.aiPreNotes && (
                  <p className="mt-3 whitespace-pre-wrap text-xs text-brand-800">{aiScore.aiPreNotes}</p>
                )}
              </div>

              <div>
                <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                  <h4 className="flex items-center gap-2 text-sm font-semibold text-ink-900">
                    <ShieldAlert className="size-4 text-rose-500" /> Luật gian lận
                  </h4>
                  <p className="text-xs text-ink-500">
                    {rules.length} luật · <span className="font-semibold text-rose-600">{violations} dòng vi phạm</span>
                  </p>
                </div>
                {rulesQuery.isPending ? (
                  <div className="h-32 animate-pulse rounded-xl bg-ink-100" />
                ) : rulesQuery.isError || rules.length === 0 ? (
                  <EmptyState
                    icon={ShieldAlert}
                    title="Chưa có kết quả luật"
                    description="Bấm “Chạy phân tích” để tạo kết quả chấm điểm chi tiết."
                  />
                ) : (
                  <div className="space-y-2">
                    {rules.map((rule, index) => {
                      const violated = rule.score < rule.maxScore
                      return (
                        <article
                          key={`${rule.ruleId}-${index}`}
                          className={`rounded-xl border p-3 ${violated ? 'border-rose-200 bg-rose-50/40' : 'border-ink-200'}`}
                        >
                          <div className="flex flex-wrap items-start justify-between gap-2">
                            <div className="min-w-0">
                              <p className="text-sm font-semibold text-ink-900">
                                {rule.ruleId ? `${rule.ruleId} · ` : ''}{rule.name || `Luật ${index + 1}`}
                              </p>
                              <p className="mt-0.5 text-xs text-ink-600">{rule.message || 'Không có mô tả'}</p>
                            </div>
                            <span className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ${
                              violated ? 'bg-rose-100 text-rose-700' : 'bg-emerald-100 text-emerald-700'
                            }`}>
                              {formatNumber(rule.score)}/{formatNumber(rule.maxScore)}
                            </span>
                          </div>
                          {rule.evidence && (
                            <p className="mt-2 break-words rounded-lg bg-white p-2 text-xs text-ink-500">
                              Bằng chứng: {rule.evidence}
                            </p>
                          )}
                        </article>
                      )
                    })}
                  </div>
                )}
              </div>

              {/* Danh sách xe điện (List EV — SRS #45) */}
              <div className="space-y-3 border-t border-ink-100 pt-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h4 className="text-sm font-semibold text-ink-900">Danh sách xe điện trong báo cáo</h4>
                  <p className="text-xs text-ink-500">
                    {(evDetailsQuery.data ?? []).length} bản ghi xe điện
                  </p>
                </div>
                {evDetailsQuery.isPending ? (
                  <div className="h-24 animate-pulse rounded-xl bg-ink-100" />
                ) : (evDetailsQuery.data ?? []).length === 0 ? (
                  <p className="rounded-xl border border-ink-200 bg-ink-50 p-3 text-xs text-ink-500">
                    Chưa có dòng chi tiết từng xe điện cho báo cáo này.
                  </p>
                ) : (
                  <div className="scrollbar-thin overflow-x-auto rounded-xl border border-ink-200">
                    <table className="w-full min-w-[460px] text-left text-xs">
                      <thead className="bg-ink-50 text-ink-500">
                        <tr>
                          <th className="px-3 py-2 font-semibold">Biển số xe</th>
                          <th className="px-3 py-2 font-semibold">Kỳ</th>
                          <th className="px-3 py-2 text-right font-semibold">Điện năng (kWh)</th>
                          <th className="px-3 py-2 text-right font-semibold">CO₂ giảm (kg)</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-ink-100">
                        {(evDetailsQuery.data ?? []).map((row, idx) => (
                          <tr key={`${row.id}-${idx}`} className="text-ink-700">
                            <td className="px-3 py-2 font-mono font-semibold text-ink-900">{row.plate}</td>
                            <td className="px-3 py-2">{row.period}</td>
                            <td className="px-3 py-2 text-right font-mono">{formatNumber(row.energy)}</td>
                            <td className="px-3 py-2 text-right font-mono font-semibold text-brand-700">
                              {formatNumber(row.co2Kg)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              <div className="space-y-4 border-t border-ink-100 pt-4">
                <h4 className="text-sm font-semibold text-ink-900">Quyết định thẩm định</h4>
                <div className="grid gap-4 sm:grid-cols-[180px_1fr]">
                  <Input
                    label="Điểm thẩm định (0–10)"
                    type="number"
                    min={0}
                    max={10}
                    step={0.1}
                    value={verificationScore}
                    onChange={(event) => setVerificationScore(event.target.value)}
                  />
                  <Input
                    label="Nhận xét"
                    placeholder="Bắt buộc nhập để duyệt hoặc từ chối"
                    value={comment}
                    error={commentError}
                    onChange={(event) => {
                      setComment(event.target.value)
                      if (event.target.value.trim()) setCommentError('')
                    }}
                  />
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button
                    leftIcon={<CheckCircle2 className="size-4" />}
                    loading={verifyMutation.isPending && verifyMutation.variables?.approved === true}
                    onClick={() => submitDecision(true)}
                  >
                    Duyệt báo cáo
                  </Button>
                  <Button
                    variant="danger"
                    leftIcon={<XCircle className="size-4" />}
                    loading={verifyMutation.isPending && verifyMutation.variables?.approved === false}
                    onClick={() => submitDecision(false)}
                  >
                    Từ chối báo cáo
                  </Button>
                </div>
              </div>
            </div>
          )}
        </SectionCard>
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
