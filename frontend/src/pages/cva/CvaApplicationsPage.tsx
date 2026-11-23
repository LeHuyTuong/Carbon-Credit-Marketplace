import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, ClipboardList, ExternalLink, RotateCcw, X } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import { errorMessage, mapApplication, mapList, type ProjectApplication } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate } from '@/shared/lib/format'
import { queryKeys } from '@/shared/lib/queryKeys'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

type Decision = 'APPROVE' | 'REVISION' | 'REJECT'

const DECISION_LABELS: Record<Decision, string> = {
  APPROVE: 'Duyệt hồ sơ',
  REVISION: 'Yêu cầu bổ sung',
  REJECT: 'Từ chối hồ sơ',
}

function DecisionDialog({
  application,
  saving,
  onClose,
  onSubmit,
}: {
  application: ProjectApplication
  saving: boolean
  onClose: () => void
  onSubmit: (decision: Decision, note: string) => void
}) {
  const [decision, setDecision] = useState<Decision>('APPROVE')
  const [note, setNote] = useState('')
  const [noteError, setNoteError] = useState('')

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-ink-950/50 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Quyết định thẩm định hồ sơ"
        className="w-full max-w-lg rounded-card bg-white p-5 shadow-float"
      >
        <div className="flex items-start justify-between gap-3">
          <div>
            <h3 className="text-base font-semibold text-ink-900">Thẩm định cấp 1</h3>
            <p className="mt-1 text-sm text-ink-500">
              {application.projectTitle || `Dự án #${application.projectId}`} · {application.companyName || 'Doanh nghiệp chưa cập nhật'}
            </p>
          </div>
          <button
            type="button"
            aria-label="Đóng"
            onClick={onClose}
            className="grid size-8 place-items-center rounded-lg text-ink-500 hover:bg-ink-100"
          >
            <X className="size-5" />
          </button>
        </div>

        <div className="mt-5 space-y-4">
          <div>
            <p className="mb-2 text-sm font-medium text-ink-700">Quyết định</p>
            <div className="grid gap-2 sm:grid-cols-3">
              {(Object.keys(DECISION_LABELS) as Decision[]).map((option) => (
                <button
                  key={option}
                  type="button"
                  onClick={() => setDecision(option)}
                  className={`rounded-xl border px-3 py-2.5 text-xs font-semibold transition ${
                    decision === option
                      ? 'border-brand-500 bg-brand-50 text-brand-700'
                      : 'border-ink-200 text-ink-600 hover:border-ink-300'
                  }`}
                >
                  {DECISION_LABELS[option]}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label htmlFor="cva-note" className="mb-1.5 block text-sm font-medium text-ink-700">
              Ghi chú thẩm định
            </label>
            <textarea
              id="cva-note"
              rows={4}
              value={note}
              onChange={(event) => {
                setNote(event.target.value)
                if (event.target.value.trim()) setNoteError('')
              }}
              aria-invalid={Boolean(noteError)}
              placeholder="Nhận xét về hồ sơ, lý do bổ sung hoặc từ chối…"
              className={`w-full rounded-xl border bg-white p-3 text-sm outline-none focus:ring-3 ${
                noteError
                  ? 'border-rose-400 focus:ring-rose-500/15'
                  : 'border-ink-200 focus:border-brand-500 focus:ring-brand-500/15'
              }`}
            />
            {noteError && <p role="alert" className="mt-1.5 text-xs font-medium text-rose-600">{noteError}</p>}
          </div>

          <div className="flex justify-end gap-2 border-t border-ink-100 pt-4">
            <Button variant="outline" onClick={onClose} disabled={saving}>
              Huỷ
            </Button>
            <Button
              variant={decision === 'REJECT' ? 'danger' : 'primary'}
              loading={saving}
              onClick={() => {
                if (!note.trim()) {
                  setNoteError('Vui lòng nhập ghi chú thẩm định')
                  return
                }
                onSubmit(decision, note.trim())
              }}
            >
              {DECISION_LABELS[decision]}
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}

export function CvaApplicationsPage() {
  const queryClient = useQueryClient()
  const [target, setTarget] = useState<ProjectApplication | null>(null)
  const applicationsQuery = useQuery({
    queryKey: queryKeys.phase3b.pendingApplications,
    queryFn: async () => mapList(await api.get<unknown>('/project-applications/pending-cva'), mapApplication),
  })

  const decisionMutation = useMutation({
    mutationFn: async ({ id, decision, note }: { id: number; decision: Decision; note: string }) => {
      const finalNote = decision === 'REVISION' ? `YÊU CẦU BỔ SUNG: ${note}` : note
      return api.put<unknown>(`/project-applications/${id}/cva-decision`, null, {
        params: { approved: decision === 'APPROVE', note: finalNote },
      })
    },
    onSuccess: async () => {
      toast.success('Đã lưu quyết định thẩm định')
      setTarget(null)
      await queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.pendingApplications })
    },
    onError: (error) => toast.error('Không lưu được quyết định', { description: errorMessage(error) }),
  })

  const applications = applicationsQuery.data ?? []
  const pendingApplications = applications.filter((app) => app.status === 'UNDER_REVIEW')
  const historyApplications = applications.filter((app) => app.status !== 'UNDER_REVIEW')

  return (
    <>
      <PageIntro
        eyebrow="CVA"
        title="Hồ sơ dự án chờ thẩm định"
        description="Duyệt, yêu cầu bổ sung hoặc từ chối hồ sơ tham gia dự án ở vòng thẩm định cấp 1."
      />

      {applicationsQuery.isPending ? (
        <QueryLoading />
      ) : applicationsQuery.isError ? (
        <QueryError onRetry={() => void applicationsQuery.refetch()} />
      ) : (
        <div className="space-y-6">
          <SectionCard title="Danh sách chờ duyệt" description={`${pendingApplications.length} hồ sơ`}>
            {pendingApplications.length === 0 ? (
              <EmptyState icon={ClipboardList} title="Không có hồ sơ chờ duyệt" description="Hồ sơ mới sẽ xuất hiện tại đây." />
            ) : (
              <div className="grid gap-4 p-5 lg:grid-cols-2">
                {pendingApplications.map((application) => (
                  <article key={application.id} className="flex flex-col rounded-xl border border-ink-200 p-4">
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <h3 className="font-semibold text-ink-900">
                          {application.projectTitle || `Dự án #${application.projectId}`}
                        </h3>
                        <p className="mt-0.5 text-sm text-ink-500">
                          {application.companyName || 'Doanh nghiệp chưa cập nhật'}
                        </p>
                      </div>
                      <StatusBadge status={application.status} kind="application" />
                    </div>

                    <dl className="mt-3 space-y-1 text-xs text-ink-500">
                      <div className="flex justify-between gap-2">
                        <dt>Nộp ngày</dt>
                        <dd className="text-ink-700">{formatDate(application.submittedAt)}</dd>
                      </div>
                      <div className="flex justify-between gap-2">
                        <dt>Mã hồ sơ</dt>
                        <dd className="text-ink-700">#{application.id}</dd>
                      </div>
                    </dl>

                    {application.applicationDocsUrl && (
                      <a
                        href={application.applicationDocsUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="mt-3 inline-flex items-center gap-1.5 text-sm font-semibold text-brand-600 hover:text-brand-700"
                      >
                        <ExternalLink className="size-4" /> Xem hồ sơ đính kèm
                      </a>
                    )}

                    <div className="mt-4 flex flex-wrap gap-2 border-t border-ink-100 pt-4">
                      <Button
                        size="sm"
                        leftIcon={<CheckCircle2 className="size-4" />}
                        onClick={() => setTarget(application)}
                      >
                        Duyệt / bổ sung / từ chối
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        leftIcon={<RotateCcw className="size-4" />}
                        onClick={() => void applicationsQuery.refetch()}
                      >
                        Làm mới
                      </Button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </SectionCard>

          {historyApplications.length > 0 && (
            <SectionCard
              title="Lịch sử thẩm định hồ sơ"
              description={`${historyApplications.length} hồ sơ đã qua thẩm định`}
            >
              <div className="scrollbar-thin overflow-x-auto p-5">
                <table className="w-full min-w-[640px] text-left text-sm">
                  <thead>
                    <tr className="border-b border-ink-100 text-xs font-semibold text-ink-500">
                      <th className="pb-3 pr-4">Mã hồ sơ</th>
                      <th className="pb-3 pr-4">Tên dự án</th>
                      <th className="pb-3 pr-4">Doanh nghiệp</th>
                      <th className="pb-3 pr-4">Ngày nộp</th>
                      <th className="pb-3">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {historyApplications.map((app) => (
                      <tr key={app.id}>
                        <td className="py-3 pr-4 font-mono text-xs text-ink-500">#{app.id}</td>
                        <td className="py-3 pr-4 font-medium text-ink-900">{app.projectTitle || `Dự án #${app.projectId}`}</td>
                        <td className="py-3 pr-4 text-ink-600">{app.companyName || 'Doanh nghiệp'}</td>
                        <td className="py-3 pr-4 text-xs text-ink-500">{formatDate(app.submittedAt)}</td>
                        <td className="py-3"><StatusBadge status={app.status} kind="application" /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </SectionCard>
          )}
        </div>
      )}

      {target && (
        <DecisionDialog
          application={target}
          saving={decisionMutation.isPending}
          onClose={() => setTarget(null)}
          onSubmit={(decision, note) => decisionMutation.mutate({ id: target.id, decision, note })}
        />
      )}
    </>
  )
}
