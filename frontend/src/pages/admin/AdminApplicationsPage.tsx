import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, ClipboardCheck, ExternalLink, RefreshCw, X, XCircle } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import { errorMessage, mapApplication, mapList, type ProjectApplication } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate } from '@/shared/lib/format'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

function AdminDecisionDialog({
  application,
  approved,
  saving,
  onClose,
  onConfirm,
}: {
  application: ProjectApplication
  approved: boolean
  saving: boolean
  onClose: () => void
  onConfirm: (note: string) => void
}) {
  const [note, setNote] = useState(
    approved ? 'Phê duyệt cấp 2 — đủ điều kiện triển khai và nộp báo cáo phát thải.' : '',
  )
  const [error, setError] = useState('')

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-ink-950/50 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={approved ? 'Phê duyệt hồ sơ dự án cấp 2' : 'Từ chối hồ sơ dự án'}
        className="w-full max-w-lg rounded-card bg-white p-5 shadow-float"
      >
        <div className="flex items-start justify-between gap-3">
          <div>
            <h3 className="text-base font-semibold text-ink-900">
              {approved ? 'Phê duyệt hồ sơ dự án (Cấp 2)' : 'Từ chối hồ sơ dự án'}
            </h3>
            <p className="mt-1 text-sm text-ink-500">
              {application.projectTitle || `Dự án #${application.projectId}`} ·{' '}
              {application.companyName || 'Doanh nghiệp'}
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

        <div className="mt-4 space-y-4">
          {application.reviewNote && (
            <div className="rounded-xl border border-ink-200 bg-ink-50 p-3 text-xs text-ink-600">
              <p className="font-semibold text-ink-800">Nhận xét từ CVA (vòng 1):</p>
              <p className="mt-1">{application.reviewNote}</p>
            </div>
          )}

          <div>
            <label htmlFor="admin-app-note" className="mb-1.5 block text-sm font-medium text-ink-700">
              Ghi chú phê duyệt của Quản trị viên
            </label>
            <textarea
              id="admin-app-note"
              rows={3}
              value={note}
              onChange={(event) => {
                setNote(event.target.value)
                if (event.target.value.trim()) setError('')
              }}
              placeholder="Nhập lý do phê duyệt hoặc từ chối hồ sơ…"
              className="w-full rounded-xl border border-ink-200 bg-white p-3 text-sm outline-none focus:border-brand-500 focus:ring-3 focus:ring-brand-500/15"
            />
            {error && <p role="alert" className="mt-1 text-xs font-medium text-rose-600">{error}</p>}
          </div>

          <div className="flex justify-end gap-2 border-t border-ink-100 pt-4">
            <Button variant="outline" onClick={onClose} disabled={saving}>
              Huỷ
            </Button>
            <Button
              variant={approved ? 'primary' : 'danger'}
              loading={saving}
              onClick={() => {
                if (!note.trim()) {
                  setError('Vui lòng nhập ghi chú phê duyệt')
                  return
                }
                onConfirm(note.trim())
              }}
            >
              {approved ? 'Xác nhận phê duyệt cấp 2' : 'Xác nhận từ chối'}
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}

export function AdminApplicationsPage() {
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [target, setTarget] = useState<{ application: ProjectApplication; approved: boolean } | null>(null)

  const appsQuery = useQuery({
    queryKey: ['admin', 'project-applications'],
    queryFn: async () => mapList(await api.get<unknown>('/project-applications'), mapApplication),
  })

  const decisionMutation = useMutation({
    mutationFn: ({ id, approved, note }: { id: number; approved: boolean; note: string }) =>
      api.put<unknown>(`/project-applications/${id}/admin-decision`, null, {
        params: { approved, note },
      }),
    onSuccess: async (_data, variables) => {
      toast.success(variables.approved ? 'Đã phê duyệt hồ sơ dự án (cấp 2)' : 'Đã từ chối hồ sơ dự án')
      setTarget(null)
      await queryClient.invalidateQueries({ queryKey: ['admin', 'project-applications'] })
    },
    onError: (error) => toast.error('Không lưu được quyết định', { description: errorMessage(error) }),
  })

  const applications = appsQuery.data ?? []
  const filtered = useMemo(
    () =>
      statusFilter === 'ALL'
        ? applications
        : applications.filter((item) => item.status.toUpperCase() === statusFilter),
    [applications, statusFilter],
  )
  const pendingAdminCount = applications.filter((item) => item.status.toUpperCase() === 'CVA_APPROVED').length

  return (
    <>
      <PageIntro
        eyebrow="Quản trị viên"
        title="Duyệt hồ sơ dự án (Cấp 2)"
        description="Phê duyệt cuối cùng các hồ sơ tham gia dự án đã vượt qua vòng thẩm định của đơn vị CVA."
        actions={
          <Button
            variant="outline"
            leftIcon={<RefreshCw className="size-4" />}
            onClick={() => void appsQuery.refetch()}
          >
            Làm mới
          </Button>
        }
      />

      {appsQuery.isPending ? (
        <QueryLoading />
      ) : appsQuery.isError ? (
        <QueryError onRetry={() => void appsQuery.refetch()} />
      ) : (
        <SectionCard
          title="Danh sách hồ sơ tham gia dự án"
          description={`${applications.length} hồ sơ · ${pendingAdminCount} chờ Admin duyệt cấp 2`}
          action={
            <select
              aria-label="Lọc theo trạng thái hồ sơ"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
              className="h-9 rounded-xl border border-ink-200 bg-white px-3 text-xs font-medium text-ink-700"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="CVA_APPROVED">CVA đã duyệt (chờ Admin)</option>
              <option value="ADMIN_APPROVED">Admin đã duyệt</option>
              <option value="UNDER_REVIEW">Đang thẩm định CVA</option>
              <option value="ADMIN_REJECTED">Admin từ chối</option>
            </select>
          }
        >
          {filtered.length === 0 ? (
            <EmptyState
              icon={ClipboardCheck}
              title="Không có hồ sơ phù hợp"
              description="Các hồ sơ tham gia dự án của doanh nghiệp sẽ hiển thị tại đây."
            />
          ) : (
            <div className="grid gap-4 p-5 lg:grid-cols-2">
              {filtered.map((app) => {
                const canDecide = app.status.toUpperCase() === 'CVA_APPROVED'
                return (
                  <article key={app.id} className="rounded-xl border border-ink-200 p-4">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div>
                        <h3 className="text-base font-bold text-ink-900">
                          {app.projectTitle || `Dự án #${app.projectId}`}
                        </h3>
                        <p className="mt-0.5 text-sm text-ink-600">{app.companyName || 'Doanh nghiệp'}</p>
                      </div>
                      <StatusBadge status={app.status} kind="application" />
                    </div>

                    <dl className="mt-3 space-y-1.5 rounded-xl bg-ink-50 p-3 text-xs text-ink-600">
                      <div className="flex justify-between gap-3">
                        <dt>Mã hồ sơ</dt>
                        <dd className="font-semibold text-ink-900">#{app.id}</dd>
                      </div>
                      <div className="flex justify-between gap-3">
                        <dt>Ngày nộp</dt>
                        <dd className="font-medium text-ink-800">{formatDate(app.submittedAt)}</dd>
                      </div>
                      {app.reviewNote && (
                        <div className="border-t border-ink-200 pt-1.5">
                          <dt className="font-semibold text-ink-700">Ghi chú CVA:</dt>
                          <dd className="mt-0.5 text-ink-600">{app.reviewNote}</dd>
                        </div>
                      )}
                      {app.finalReviewNote && (
                        <div className="border-t border-ink-200 pt-1.5">
                          <dt className="font-semibold text-ink-700">Ghi chú Admin:</dt>
                          <dd className="mt-0.5 text-ink-600">{app.finalReviewNote}</dd>
                        </div>
                      )}
                    </dl>

                    <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-ink-100 pt-3">
                      {app.applicationDocsUrl && (
                        <a
                          href={app.applicationDocsUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="inline-flex items-center gap-1.5 rounded-lg border border-ink-200 px-3 py-1.5 text-xs font-semibold text-ink-700 hover:bg-ink-50"
                        >
                          <ExternalLink className="size-3.5" />
                          Tài liệu hồ sơ
                        </a>
                      )}
                      {canDecide && (
                        <>
                          <Button
                            size="sm"
                            leftIcon={<CheckCircle2 className="size-4" />}
                            onClick={() => setTarget({ application: app, approved: true })}
                          >
                            Phê duyệt cấp 2
                          </Button>
                          <Button
                            size="sm"
                            variant="danger"
                            leftIcon={<XCircle className="size-4" />}
                            onClick={() => setTarget({ application: app, approved: false })}
                          >
                            Từ chối
                          </Button>
                        </>
                      )}
                    </div>
                  </article>
                )
              })}
            </div>
          )}
        </SectionCard>
      )}

      {target && (
        <AdminDecisionDialog
          application={target.application}
          approved={target.approved}
          saving={decisionMutation.isPending}
          onClose={() => setTarget(null)}
          onConfirm={(note) =>
            decisionMutation.mutate({
              id: target.application.id,
              approved: target.approved,
              note,
            })
          }
        />
      )}
    </>
  )
}
