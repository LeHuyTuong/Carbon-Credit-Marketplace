import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowRight, ExternalLink, FileUp, FolderOpen, Send, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import {
  errorMessage,
  mapApplication,
  mapList,
  mapProjectItem,
  toFormData,
  type ProjectApplication,
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

function ApplyDialog({
  projectTitle,
  saving,
  onClose,
  onSubmit,
}: {
  projectTitle: string
  saving: boolean
  onClose: () => void
  onSubmit: (file: File) => void
}) {
  const [file, setFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState('')

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-ink-950/50 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Nộp hồ sơ tham gia dự án"
        className="w-full max-w-md rounded-card bg-white p-5 shadow-float"
      >
        <div className="flex items-start justify-between gap-3">
          <div>
            <h3 className="text-base font-semibold text-ink-900">Nộp hồ sơ tham gia dự án</h3>
            <p className="mt-1 text-sm text-ink-500">{projectTitle}</p>
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

        <form
          className="mt-5 space-y-4"
          noValidate
          onSubmit={(event) => {
            event.preventDefault()
            if (!file) {
              setFileError('Vui lòng chọn hồ sơ đính kèm')
              return
            }
            onSubmit(file)
          }}
        >
          <FileDropzone
            label="Hồ sơ tham gia (PDF/DOC/Ảnh)"
            accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
            hint="Hồ sơ sẽ được CVA thẩm định ở vòng 1 (tối đa 10 MB)."
            error={fileError}
            file={file}
            onFileChange={(f) => {
              setFile(f)
              if (f) setFileError('')
            }}
          />
          <div className="flex justify-end gap-2 border-t border-ink-100 pt-4">
            <Button variant="outline" onClick={onClose} disabled={saving}>
              Huỷ
            </Button>
            <Button type="submit" loading={saving} leftIcon={<Send className="size-4" />}>
              Nộp hồ sơ
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}

function ApplicationRow({
  application,
  onResubmit,
}: {
  application: ProjectApplication
  onResubmit: () => void
}) {
  const statusUpper = application.status.toUpperCase()
  const needsAction = ['NEEDS_REVISION', 'CVA_REJECTED', 'ADMIN_REJECTED'].includes(statusUpper)
  const isApproved = statusUpper === 'ADMIN_APPROVED'

  return (
    <article className="rounded-xl border border-ink-200 p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h4 className="font-semibold text-ink-900">{application.projectTitle || `Dự án #${application.projectId}`}</h4>
          <p className="mt-0.5 text-xs text-ink-500">
            Nộp ngày {formatDate(application.submittedAt)} · Hồ sơ #{application.id}
          </p>
        </div>
        <StatusBadge status={application.status} kind="application" />
      </div>

      {(application.reviewNote || application.finalReviewNote) && (
        <div className="mt-3 space-y-1 rounded-lg bg-ink-50 p-3 text-sm text-ink-600">
          {application.reviewNote && <p><span className="font-semibold">CVA:</span> {application.reviewNote}</p>}
          {application.finalReviewNote && <p><span className="font-semibold">Admin:</span> {application.finalReviewNote}</p>}
        </div>
      )}

      <div className="mt-3 flex flex-wrap items-center justify-between gap-3 text-xs text-ink-500">
        <div className="flex flex-wrap items-center gap-4">
          {application.waitingFor && <p>Đang chờ: {application.waitingFor}</p>}
          {application.applicationDocsUrl && (
            <a
              href={application.applicationDocsUrl}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1 font-semibold text-brand-600 hover:text-brand-700"
            >
              <ExternalLink className="size-3.5" /> Xem hồ sơ đã nộp
            </a>
          )}
        </div>
        <div className="flex items-center gap-2">
          {needsAction && (
            <Button size="sm" variant="outline" leftIcon={<FileUp className="size-3.5" />} onClick={onResubmit}>
              Nộp bổ sung hồ sơ
            </Button>
          )}
          {isApproved && (
            <Link
              to="/app/company/reports"
              className="inline-flex items-center gap-1 rounded-lg bg-brand-50 px-2.5 py-1.5 font-semibold text-brand-700 hover:bg-brand-100"
            >
              Nộp báo cáo CSV <ArrowRight className="size-3.5" />
            </Link>
          )}
        </div>
      </div>
    </article>
  )
}

export function CompanyProjectsPage() {
  const queryClient = useQueryClient()
  const [applyingTo, setApplyingTo] = useState<{ id: number; title: string } | null>(null)
  const [statusFilter, setStatusFilter] = useState('ALL')

  const projectsQuery = useQuery({
    queryKey: queryKeys.phase3b.projectsAll,
    queryFn: async () => mapList(await api.get<unknown>('/projects/all'), mapProjectItem),
  })
  const applicationsQuery = useQuery({
    queryKey: queryKeys.phase3b.myApplications,
    queryFn: async () => mapList(await api.get<unknown>('/project-applications/my'), mapApplication),
  })

  const applyMutation = useMutation({
    mutationFn: async ({ projectId, file }: { projectId: number; file: File }) =>
      api.post<unknown>('/project-applications', toFormData({ projectId, file }), multipart),
    onSuccess: async () => {
      toast.success('Đã nộp hồ sơ tham gia dự án')
      setApplyingTo(null)
      await queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.myApplications })
    },
    onError: (error) => toast.error('Không nộp được hồ sơ', { description: errorMessage(error) }),
  })

  const applications = applicationsQuery.data ?? []
  const filteredApplications =
    statusFilter === 'ALL'
      ? applications
      : applications.filter((item) => item.status === statusFilter)
  const appliedProjectIds = new Set(applications.map((item) => item.projectId))

  return (
    <>
      <PageIntro
        eyebrow="Doanh nghiệp"
        title="Dự án & hồ sơ tham gia"
        description="Xem các dự án trên sàn, nộp hồ sơ tham gia và theo dõi trạng thái thẩm định."
      />

      <div className="space-y-6">
        <SectionCard title="Dự án hiện có" description="Chọn dự án để nộp hồ sơ tham gia.">
          {projectsQuery.isPending ? (
            <QueryLoading />
          ) : projectsQuery.isError ? (
            <QueryError onRetry={() => void projectsQuery.refetch()} />
          ) : (projectsQuery.data ?? []).length === 0 ? (
            <EmptyState icon={FolderOpen} title="Chưa có dự án" description="Hệ thống chưa công bố dự án nào." />
          ) : (
            <div className="grid gap-4 p-5 md:grid-cols-2 xl:grid-cols-3">
              {projectsQuery.data?.map((project) => (
                <article key={project.id} className="flex flex-col rounded-xl border border-ink-200 p-4">
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-semibold text-ink-900">{project.title || `Dự án #${project.id}`}</h3>
                    <StatusBadge status={project.status || 'CLOSE'} />
                  </div>
                  <p className="mt-2 line-clamp-3 text-sm text-ink-500">
                    {project.description || 'Chưa có mô tả dự án.'}
                  </p>
                  <dl className="mt-3 space-y-1 text-xs text-ink-500">
                    <div className="flex justify-between gap-2">
                      <dt>Hệ số phát thải</dt>
                      <dd className="font-medium text-ink-700">{formatNumber(project.emissionFactorKgPerKwh)} kgCO₂/kWh</dd>
                    </div>
                    <div className="flex justify-between gap-2">
                      <dt>Thời gian</dt>
                      <dd className="font-medium text-ink-700">
                        {project.startedDate || '—'} → {project.endDate || '—'}
                      </dd>
                    </div>
                  </dl>
                  <Button
                    className="mt-4"
                    variant={appliedProjectIds.has(project.id) ? 'outline' : 'primary'}
                    leftIcon={<FileUp className="size-4" />}
                    onClick={() => setApplyingTo({ id: project.id, title: project.title })}
                  >
                    {appliedProjectIds.has(project.id) ? 'Nộp lại hồ sơ' : 'Nộp hồ sơ tham gia'}
                  </Button>
                </article>
              ))}
            </div>
          )}
        </SectionCard>

        <SectionCard
          title="Hồ sơ của tôi"
          description="Trạng thái: UNDER_REVIEW, CVA_APPROVED, ADMIN_APPROVED, NEEDS_REVISION."
          action={
            <select
              aria-label="Lọc theo trạng thái"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
              className="h-9 rounded-xl border border-ink-200 bg-white px-3 text-xs font-medium text-ink-700"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="SUBMITTED">Đã nộp</option>
              <option value="UNDER_REVIEW">Đang thẩm định</option>
              <option value="NEEDS_REVISION">Cần bổ sung</option>
              <option value="CVA_APPROVED">CVA đã duyệt</option>
              <option value="ADMIN_APPROVED">Admin đã duyệt</option>
              <option value="CVA_REJECTED">CVA từ chối</option>
              <option value="ADMIN_REJECTED">Admin từ chối</option>
            </select>
          }
        >
          {applicationsQuery.isPending ? (
            <QueryLoading />
          ) : applicationsQuery.isError ? (
            <QueryError onRetry={() => void applicationsQuery.refetch()} />
          ) : filteredApplications.length === 0 ? (
            <EmptyState icon={FileUp} title="Chưa có hồ sơ" description="Hồ sơ bạn nộp sẽ hiển thị tại đây." />
          ) : (
            <div className="space-y-3 p-5">
              {filteredApplications.map((application) => (
                <ApplicationRow
                  key={application.id}
                  application={application}
                  onResubmit={() =>
                    setApplyingTo({
                      id: application.projectId,
                      title: application.projectTitle || `Dự án #${application.projectId}`,
                    })
                  }
                />
              ))}
            </div>
          )}
        </SectionCard>
      </div>

      {applyingTo && (
        <ApplyDialog
          projectTitle={applyingTo.title}
          saving={applyMutation.isPending}
          onClose={() => setApplyingTo(null)}
          onSubmit={(file) => applyMutation.mutate({ projectId: applyingTo.id, file })}
        />
      )}
    </>
  )
}
