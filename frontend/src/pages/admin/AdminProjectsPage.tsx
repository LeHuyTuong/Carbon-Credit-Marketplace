import { useEffect, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Building2, Info, Pencil, Plus, Trash2, X } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { api } from '@/shared/api/client'
import { errorMessage, mapList, mapProjectItem, toFormData, type ProjectItem } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatNumber } from '@/shared/lib/format'
import { queryKeys } from '@/shared/lib/queryKeys'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

const percent = z
  .string()
  .refine((value) => value === '' || (!Number.isNaN(Number(value)) && Number(value) >= 0 && Number(value) <= 100), {
    message: 'Giá trị phải nằm trong khoảng 0–100',
  })

const projectSchema = z.object({
  title: z.string().trim().min(3, 'Tên dự án phải có ít nhất 3 ký tự'),
  description: z.string().trim().min(10, 'Mô tả phải có ít nhất 10 ký tự'),
  status: z.enum(['OPEN', 'CLOSE', 'COMING_SOON']),
  emissionFactorKgPerKwh: z
    .string()
    .trim()
    .min(1, 'Vui lòng nhập hệ số phát thải')
    .refine((value) => !Number.isNaN(Number(value)) && Number(value) >= 0, 'Hệ số phát thải phải không âm'),
  bufferReservePct: percent,
  uncertaintyPct: percent,
  leakagePct: percent,
  startedDate: z.string(),
  endDate: z.string(),
})

type ProjectFormValues = z.infer<typeof projectSchema>

type DialogState = { mode: 'create' } | { mode: 'edit'; project: ProjectItem } | null

const multipart = { headers: { 'Content-Type': 'multipart/form-data' } } as const

function ProjectDialog({
  state,
  saving,
  onClose,
  onSubmit,
}: {
  state: NonNullable<DialogState>
  saving: boolean
  onClose: () => void
  onSubmit: (values: ProjectFormValues, logo: File | null, legalDocs: File | null) => void
}) {
  const isEdit = state.mode === 'edit'
  const [logo, setLogo] = useState<File | null>(null)
  const [legalDocs, setLegalDocs] = useState<File | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ProjectFormValues>({
    resolver: zodResolver(projectSchema),
    mode: 'onBlur',
    defaultValues: {
      title: '',
      description: '',
      status: 'OPEN',
      emissionFactorKgPerKwh: '',
      bufferReservePct: '',
      uncertaintyPct: '',
      leakagePct: '',
      startedDate: '',
      endDate: '',
    },
  })

  useEffect(() => {
    if (state.mode === 'edit') {
      const { project } = state
      reset({
        title: project.title,
        description: project.description,
        status: (['OPEN', 'CLOSE', 'COMING_SOON'] as const).includes(project.status as ProjectFormValues['status'])
          ? (project.status as ProjectFormValues['status'])
          : 'OPEN',
        emissionFactorKgPerKwh: String(project.emissionFactorKgPerKwh),
        bufferReservePct: String(project.bufferReservePct),
        uncertaintyPct: String(project.uncertaintyPct),
        leakagePct: String(project.leakagePct),
        startedDate: project.startedDate,
        endDate: project.endDate,
      })
    } else {
      reset({
        title: '',
        description: '',
        status: 'OPEN',
        emissionFactorKgPerKwh: '',
        bufferReservePct: '',
        uncertaintyPct: '',
        leakagePct: '',
        startedDate: '',
        endDate: '',
      })
      setLogo(null)
      setLegalDocs(null)
    }
  }, [state, reset])

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-ink-950/50 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={isEdit ? 'Cập nhật dự án' : 'Tạo dự án'}
        className="mx-auto my-4 w-full max-w-2xl rounded-card bg-white shadow-float"
      >
        <div className="flex items-center justify-between border-b border-ink-100 px-5 py-4">
          <h3 className="text-base font-semibold text-ink-900">{isEdit ? 'Cập nhật dự án' : 'Tạo dự án mới'}</h3>
          <button
            type="button"
            aria-label="Đóng"
            onClick={onClose}
            className="grid size-8 place-items-center rounded-lg text-ink-500 hover:bg-ink-100"
          >
            <X className="size-5" />
          </button>
        </div>

        <form className="space-y-4 p-5" noValidate onSubmit={handleSubmit((values) => onSubmit(values, logo, legalDocs))}>
          <Input label="Tên dự án" error={errors.title?.message} {...register('title')} />
          <div>
            <label htmlFor="project-description" className="mb-1.5 block text-sm font-medium text-ink-700">
              Mô tả
            </label>
            <textarea
              id="project-description"
              rows={3}
              aria-invalid={Boolean(errors.description)}
              className={`w-full rounded-xl border bg-white p-3 text-sm outline-none focus:ring-3 ${
                errors.description
                  ? 'border-rose-400 focus:ring-rose-500/15'
                  : 'border-ink-200 focus:border-brand-500 focus:ring-brand-500/15'
              }`}
              {...register('description')}
            />
            {errors.description && (
              <p role="alert" className="mt-1.5 text-xs font-medium text-rose-600">{errors.description.message}</p>
            )}
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="project-status" className="mb-1.5 block text-sm font-medium text-ink-700">
                Trạng thái
              </label>
              <select
                id="project-status"
                className="h-11 w-full rounded-xl border border-ink-200 bg-white px-3.5 text-sm"
                {...register('status')}
              >
                <option value="OPEN">Đang mở</option>
                <option value="CLOSE">Đã đóng</option>
                <option value="COMING_SOON">Sắp mở</option>
              </select>
            </div>
            <Input
              label="Hệ số phát thải (kgCO₂/kWh)"
              type="number"
              min={0}
              step="0.0001"
              error={errors.emissionFactorKgPerKwh?.message}
              {...register('emissionFactorKgPerKwh')}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <Input
              label="Dự phòng rủi ro (%)"
              type="number"
              min={0}
              max={100}
              step="0.0001"
              error={errors.bufferReservePct?.message}
              {...register('bufferReservePct')}
            />
            <Input
              label="Độ bất định (%)"
              type="number"
              min={0}
              max={100}
              step="0.0001"
              error={errors.uncertaintyPct?.message}
              {...register('uncertaintyPct')}
            />
            <Input
              label="Rò rỉ phát thải (%)"
              type="number"
              min={0}
              max={100}
              step="0.0001"
              error={errors.leakagePct?.message}
              {...register('leakagePct')}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Ngày bắt đầu" type="date" {...register('startedDate')} />
            <Input label="Ngày kết thúc" type="date" {...register('endDate')} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label="Logo dự án"
              type="file"
              accept="image/*"
              hint={isEdit ? 'Để trống nếu không đổi.' : undefined}
              onChange={(event) => setLogo(event.target.files?.[0] ?? null)}
            />
            <Input
              label="Tài liệu pháp lý"
              type="file"
              accept=".pdf,.doc,.docx,image/*"
              hint={isEdit ? 'Để trống nếu không đổi.' : undefined}
              onChange={(event) => setLegalDocs(event.target.files?.[0] ?? null)}
            />
          </div>

          <div className="flex justify-end gap-2 border-t border-ink-100 pt-4">
            <Button variant="outline" onClick={onClose} disabled={saving}>
              Huỷ
            </Button>
            <Button type="submit" loading={saving}>
              {isEdit ? 'Lưu thay đổi' : 'Tạo dự án'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}

export function AdminProjectsPage() {
  const queryClient = useQueryClient()
  const [dialog, setDialog] = useState<DialogState>(null)
  const projectsQuery = useQuery({
    queryKey: queryKeys.phase3b.projectsAll,
    queryFn: async () => mapList(await api.get<unknown>('/projects/all'), mapProjectItem),
  })

  const saveMutation = useMutation({
    mutationFn: async ({
      values,
      logo,
      legalDocs,
      projectId,
    }: {
      values: ProjectFormValues
      logo: File | null
      legalDocs: File | null
      projectId: number | null
    }) => {
      const body = toFormData({
        title: values.title,
        description: values.description,
        status: values.status,
        emissionFactorKgPerKwh: values.emissionFactorKgPerKwh,
        bufferReservePct: values.bufferReservePct === '' ? undefined : values.bufferReservePct,
        uncertaintyPct: values.uncertaintyPct === '' ? undefined : values.uncertaintyPct,
        leakagePct: values.leakagePct === '' ? undefined : values.leakagePct,
        startedDate: values.startedDate || undefined,
        endDate: values.endDate || undefined,
        logo,
        legalDocsFile: legalDocs,
      })
      if (projectId === null) return api.post<unknown>('/projects', body, multipart)
      return api.put<unknown>(`/projects/${projectId}`, body, multipart)
    },
    onSuccess: async (_data, variables) => {
      toast.success(variables.projectId === null ? 'Đã tạo dự án' : 'Đã cập nhật dự án')
      setDialog(null)
      await queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.projectsAll })
    },
    onError: (error) => toast.error('Không lưu được dự án', { description: errorMessage(error) }),
  })

  const deleteMutation = useMutation({
    mutationFn: (projectId: number) => api.delete<unknown>(`/projects/${projectId}`),
    onSuccess: async () => {
      toast.success('Đã xoá dự án')
      await queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.projectsAll })
    },
    onError: (error) => toast.error('Không xoá được dự án', { description: errorMessage(error) }),
  })

  const projects = projectsQuery.data ?? []

  return (
    <>
      <PageIntro
        eyebrow="Quản trị viên"
        title="Quản lý dự án"
        description="Tạo, cập nhật, xoá dự án cùng hệ số phát thải và các tỷ lệ dự phòng rủi ro, độ bất định, rò rỉ phát thải."
        actions={
          <Button leftIcon={<Plus className="size-4" />} onClick={() => setDialog({ mode: 'create' })}>
            Tạo dự án
          </Button>
        }
      />

      {projectsQuery.isPending ? (
        <QueryLoading />
      ) : projectsQuery.isError ? (
        <QueryError onRetry={() => void projectsQuery.refetch()} />
      ) : (
        <>
          <div className="mb-6 rounded-2xl border border-brand-200 bg-brand-50/50 p-4 text-xs text-ink-700">
            <div className="flex items-start gap-3">
              <Info className="mt-0.5 size-4 shrink-0 text-brand-700" />
              <div className="space-y-1">
                <p className="font-semibold text-brand-900">
                  Quy chuẩn cấp tín chỉ VM0038 & Tính bất biến của lô phát hành (Audit Rule)
                </p>
                <p className="text-ink-600">
                  Công thức: <code className="rounded bg-brand-100/70 px-1.5 py-0.5 font-mono text-brand-800">Tín chỉ = (Tổng kWh × Hệ số kgCO₂/kWh) / 1000 × (1 - Dự phòng - Bất định - Rò rỉ) + Dư kỳ trước</code>.
                </p>
                <p className="text-ink-500">
                  • <strong>Lô đã phát hành:</strong> Mang số serial và chứng chỉ bất biến, không bị thay đổi hồi tố khi thông số dự án cập nhật.
                  <br />
                  • <strong>Kỳ phát thải mới:</strong> Khi Admin chỉnh sửa hệ số phát thải hoặc tỷ lệ dự phòng, các kỳ nộp về sau sẽ tự động áp dụng công thức mới nhất.
                </p>
              </div>
            </div>
          </div>

          <SectionCard title="Danh sách dự án" description={`${projects.length} dự án`}>
          {projects.length === 0 ? (
            <EmptyState icon={Building2} title="Chưa có dự án" description="Tạo dự án đầu tiên để doanh nghiệp có thể đăng ký." />
          ) : (
            <div className="scrollbar-thin overflow-x-auto p-5">
              <table className="w-full min-w-[900px] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                    <th className="pb-3 font-semibold">Dự án</th>
                    <th className="pb-3 font-semibold">Trạng thái</th>
                    <th className="pb-3 font-semibold">Hệ số kgCO₂/kWh</th>
                    <th className="pb-3 font-semibold">Dự phòng</th>
                    <th className="pb-3 font-semibold">Bất định</th>
                    <th className="pb-3 font-semibold">Rò rỉ</th>
                    <th className="pb-3 text-right font-semibold">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {projects.map((project) => (
                    <tr key={project.id} className="text-ink-700">
                      <td className="py-3">
                        <p className="font-semibold text-ink-900">{project.title || `Dự án #${project.id}`}</p>
                        <p className="mt-0.5 line-clamp-1 max-w-xs text-xs text-ink-500">{project.description}</p>
                      </td>
                      <td className="py-3"><StatusBadge status={project.status} kind="project" /></td>
                      <td className="py-3 font-mono">{Number(project.emissionFactorKgPerKwh ?? 0).toFixed(4)}</td>
                      <td className="py-3">{formatNumber(project.bufferReservePct)}%</td>
                      <td className="py-3">{formatNumber(project.uncertaintyPct)}%</td>
                      <td className="py-3">{formatNumber(project.leakagePct)}%</td>
                      <td className="py-3">
                        <div className="flex justify-end gap-1">
                          <button
                            type="button"
                            aria-label={`Sửa dự án ${project.title}`}
                            onClick={() => setDialog({ mode: 'edit', project })}
                            className="grid size-8 place-items-center rounded-lg text-ink-500 hover:bg-brand-50 hover:text-brand-700"
                          >
                            <Pencil className="size-4" />
                          </button>
                          <button
                            type="button"
                            aria-label={`Xoá dự án ${project.title}`}
                            onClick={() => {
                              if (window.confirm(`Xoá dự án ${project.title}?`)) deleteMutation.mutate(project.id)
                            }}
                            disabled={deleteMutation.isPending}
                            className="grid size-8 place-items-center rounded-lg text-ink-500 hover:bg-rose-50 hover:text-rose-600"
                          >
                            <Trash2 className="size-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </SectionCard>
        </>
      )}

      {dialog && (
        <ProjectDialog
          state={dialog}
          saving={saveMutation.isPending}
          onClose={() => setDialog(null)}
          onSubmit={(values, logo, legalDocs) =>
            saveMutation.mutate({
              values,
              logo,
              legalDocs,
              projectId: dialog.mode === 'edit' ? dialog.project.id : null,
            })
          }
        />
      )}
    </>
  )
}
