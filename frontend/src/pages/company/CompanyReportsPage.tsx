import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Download, FileBarChart2, FileSpreadsheet, RefreshCw, Upload } from 'lucide-react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import {
  downloadFile,
  errorMessage,
  mapApplication,
  mapEmissionReport,
  mapList,
  mapRule,
  toFormData,
  type EmissionReport,
  type RuleResult,
} from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { FileDropzone } from '@/shared/components/FileDropzone'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate, formatNumber } from '@/shared/lib/format'
import { queryKeys } from '@/shared/lib/queryKeys'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

const multipart = { headers: { 'Content-Type': 'multipart/form-data' } } as const

export function CompanyReportsPage() {
  const queryClient = useQueryClient()
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [csvFile, setCsvFile] = useState<File | null>(null)
  const [projectId, setProjectId] = useState('')
  const [uploadError, setUploadError] = useState('')

  const reportsQuery = useQuery({
    queryKey: queryKeys.phase3b.myReports,
    queryFn: async () => mapList(await api.get<unknown>('/reports/my-reports'), mapEmissionReport),
  })
  const myAppsQuery = useQuery({
    queryKey: queryKeys.phase3b.myApplications,
    queryFn: async () => mapList(await api.get<unknown>('/project-applications/my'), mapApplication),
  })
  const approvedProjects = useMemo(() => {
    const map = new Map<number, string>()
    for (const app of myAppsQuery.data ?? []) {
      if (app.status.toUpperCase() === 'ADMIN_APPROVED' && app.projectId) {
        map.set(app.projectId, app.projectTitle || `Dự án #${app.projectId}`)
      }
    }
    return Array.from(map.entries()).map(([id, title]) => ({ id, title }))
  }, [myAppsQuery.data])
  const reports = reportsQuery.data ?? []

  useEffect(() => {
    if (selectedId === null && reports.length > 0) setSelectedId(reports[0].id)
  }, [reports, selectedId])

  const rulesQuery = useQuery({
    queryKey: selectedId === null ? ['reports', 'rules', 'none'] : queryKeys.phase3b.analysisRules(selectedId),
    queryFn: async () => {
      if (selectedId === null) return []
      return mapList(await api.get<unknown>(`/reports/${selectedId}/analysis/rules`), mapRule)
    },
    enabled: selectedId !== null,
    retry: false,
  })

  const uploadMutation = useMutation({
    mutationFn: async ({ file, projectId }: { file: File; projectId: number | null }) =>
      api.post<unknown>('/reports/upload', toFormData({ file, projectId }), multipart),
    onSuccess: async () => {
      toast.success('Đã upload báo cáo phát thải')
      setCsvFile(null)
      setProjectId('')
      await queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.myReports })
    },
    onError: (error) => toast.error('Không upload được báo cáo', { description: errorMessage(error) }),
  })

  const exportReport = async (reportId: number) => {
    try {
      await downloadFile(`/reports/${reportId}/export`, `emission_report_${reportId}.csv`)
      toast.success('Đã tải CSV báo cáo')
    } catch (error) {
      toast.error('Không tải được CSV', { description: errorMessage(error) })
    }
  }

  const selectedReport: EmissionReport | undefined = reports.find((item) => item.id === selectedId)
  const rules: RuleResult[] = rulesQuery.data ?? []
  const violations = rules.filter((rule) => rule.score < rule.maxScore).length

  return (
    <>
      <PageIntro
        eyebrow="Doanh nghiệp"
        title="Báo cáo phát thải"
        description="Upload CSV, xem kết quả phân tích 8 luật gian lận và tải lại báo cáo đã tổng hợp."
      />

      <div className="space-y-6">
        <SectionCard title="Upload báo cáo CSV" description="Chọn dự án đã được Admin phê duyệt và tải lên file CSV dữ liệu sạc xe điện.">
          <form
            className="grid gap-4 p-5 md:grid-cols-[1.5fr_1fr_auto] md:items-end"
            noValidate
            onSubmit={(event) => {
              event.preventDefault()
              if (!csvFile) {
                setUploadError('Vui lòng chọn file CSV')
                return
              }
              setUploadError('')
              uploadMutation.mutate({ file: csvFile, projectId: projectId === '' ? null : Number(projectId) })
            }}
          >
            <FileDropzone
              label="File CSV báo cáo phát thải"
              accept=".csv,text/csv"
              hint="Định dạng CSV gồm các cột: total_energy, license_plate, period (tối đa 10 MB)"
              error={uploadError}
              file={csvFile}
              onFileChange={(file) => {
                setCsvFile(file)
                if (file) setUploadError('')
              }}
            />
            <div>
              <label htmlFor="report-project-select" className="mb-1.5 block text-sm font-medium text-ink-700">
                Dự án đã được duyệt
              </label>
              <select
                id="report-project-select"
                value={projectId}
                onChange={(event) => setProjectId(event.target.value)}
                className="h-11 w-full rounded-xl border border-ink-200 bg-white px-3.5 text-sm text-ink-900 outline-none focus:border-brand-500 focus:ring-3 focus:ring-brand-500/15"
              >
                <option value="">Tự nhận diện từ cột project_id trong CSV</option>
                {approvedProjects.map((p) => (
                  <option key={p.id} value={String(p.id)}>
                    {p.title}
                  </option>
                ))}
              </select>
              {approvedProjects.length === 0 && !myAppsQuery.isPending && (
                <p className="mt-1.5 text-xs text-amber-700">
                  Chưa có dự án được duyệt.{' '}
                  <Link to="/app/company/projects" className="font-semibold underline">
                    Đăng ký dự án ngay
                  </Link>
                </p>
              )}
            </div>
            <Button type="submit" loading={uploadMutation.isPending} leftIcon={<Upload className="size-4" />}>
              Upload CSV
            </Button>
          </form>
        </SectionCard>

        <div className="grid gap-6 xl:grid-cols-[380px_1fr]">
          <SectionCard title="Báo cáo của tôi" description={`${reports.length} báo cáo`}>
            {reportsQuery.isPending ? (
              <div className="p-5"><QueryLoading /></div>
            ) : reportsQuery.isError ? (
              <QueryError onRetry={() => void reportsQuery.refetch()} />
            ) : reports.length === 0 ? (
              <EmptyState icon={FileBarChart2} title="Chưa có báo cáo" description="Upload file CSV để tạo báo cáo đầu tiên." />
            ) : (
              <ul className="scrollbar-thin max-h-[32rem] divide-y divide-ink-100 overflow-y-auto p-2">
                {reports.map((report) => (
                  <li key={report.id}>
                    <button
                      type="button"
                      onClick={() => setSelectedId(report.id)}
                      className={`w-full rounded-lg px-3 py-3 text-left transition ${
                        report.id === selectedId ? 'bg-brand-50' : 'hover:bg-ink-50'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="truncate text-sm font-semibold text-ink-900">
                          {report.projectName || `Báo cáo #${report.id}`}
                        </span>
                        <StatusBadge status={report.status} kind="emission" />
                      </div>
                      <p className="mt-1 text-xs text-ink-500">
                        {report.period || 'Chưa rõ kỳ'} · {formatDate(report.submittedAt)}
                      </p>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </SectionCard>

          <SectionCard
            title="Kết quả phân tích gian lận"
            description="Điểm từng luật và bằng chứng vi phạm do hệ thống trích xuất."
            action={
              selectedReport ? (
                <Button
                  variant="outline"
                  size="sm"
                  leftIcon={<Download className="size-4" />}
                  onClick={() => void exportReport(selectedReport.id)}
                >
                  Tải CSV
                </Button>
              ) : undefined
            }
          >
            {selectedId === null ? (
              <EmptyState icon={FileSpreadsheet} title="Chưa chọn báo cáo" description="Chọn một báo cáo ở danh sách để xem kết quả." />
            ) : rulesQuery.isPending ? (
              <div className="p-5"><QueryLoading /></div>
            ) : rulesQuery.isError ? (
              <QueryError
                title="Chưa tải được kết quả phân tích"
                description="Báo cáo này có thể chưa được phân tích."
                onRetry={() => void rulesQuery.refetch()}
              />
            ) : (
              <div className="space-y-5 p-5">
                {selectedReport && (
                  <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                    <Metric label="Tổng năng lượng" value={`${formatNumber(selectedReport.totalEnergy)} kWh`} />
                    <Metric label="Tổng CO₂" value={`${formatNumber(selectedReport.totalCo2)} kg`} />
                    <Metric label="Số xe" value={formatNumber(selectedReport.vehicleCount)} />
                    <Metric label="Số dòng CSV" value={formatNumber(selectedReport.uploadRows)} />
                  </div>
                )}

                <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-ink-50 p-3">
                  <p className="text-sm text-ink-600">
                    <span className="font-semibold text-ink-900">{rules.length} luật</span> được kiểm tra ·{' '}
                    <span className="font-semibold text-rose-600">{violations} dòng vi phạm</span>
                  </p>
                  <Button
                    variant="ghost"
                    size="sm"
                    leftIcon={<RefreshCw className="size-4" />}
                    onClick={() => void rulesQuery.refetch()}
                  >
                    Làm mới kết quả
                  </Button>
                </div>

                {rules.length === 0 ? (
                  <EmptyState icon={FileSpreadsheet} title="Chưa có dữ liệu phân tích" description="Hãy upload báo cáo hợp lệ để hệ thống chấm điểm." />
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
