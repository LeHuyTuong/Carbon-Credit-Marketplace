import { useEffect, useMemo, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { AxiosError } from 'axios'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Building2, Car, FileText, Pencil, Plus, Trash2, X } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { api } from '@/shared/api/client'
import {
  errorMessage,
  isRecord,
  mapApplication,
  mapList,
  mapVehicle,
  toFormData,
  type Vehicle,
} from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { FileDropzone } from '@/shared/components/FileDropzone'
import { Input } from '@/shared/components/Input'
import { queryKeys } from '@/shared/lib/queryKeys'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

interface CompanyOption {
  id: number
  name: string
}

const vehicleSchema = z.object({
  plateNumber: z.string().trim().min(3, 'Biển số xe phải có ít nhất 3 ký tự'),
  brand: z.string().trim().min(1, 'Vui lòng nhập hãng xe'),
  model: z.string().trim().min(1, 'Vui lòng nhập mẫu xe'),
  companyId: z
    .string()
    .trim()
    .min(1, 'Vui lòng chọn doanh nghiệp đồng hành')
    .regex(/^[0-9]+$/, 'Doanh nghiệp không hợp lệ'),
})

type VehicleFormValues = z.infer<typeof vehicleSchema>

type DialogState = { mode: 'create' } | { mode: 'edit'; vehicle: Vehicle } | null

const multipart = { headers: { 'Content-Type': 'multipart/form-data' } } as const

function VehicleDialog({
  state,
  companies,
  saving,
  onClose,
  onSubmit,
}: {
  state: NonNullable<DialogState>
  companies: CompanyOption[]
  saving: boolean
  onClose: () => void
  onSubmit: (values: VehicleFormValues, documentFile: File | null) => void
}) {
  const isEdit = state.mode === 'edit'
  const [documentFile, setDocumentFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState('')
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<VehicleFormValues>({
    resolver: zodResolver(vehicleSchema),
    mode: 'onBlur',
    defaultValues: { plateNumber: '', brand: '', model: '', companyId: companies[0] ? String(companies[0].id) : '' },
  })

  useEffect(() => {
    if (state.mode === 'edit') {
      const { vehicle } = state
      reset({
        plateNumber: vehicle.plateNumber,
        brand: vehicle.brand,
        model: vehicle.model,
        companyId: vehicle.companyId === null ? (companies[0] ? String(companies[0].id) : '') : String(vehicle.companyId),
      })
    } else {
      reset({
        plateNumber: '',
        brand: '',
        model: '',
        companyId: companies[0] ? String(companies[0].id) : '',
      })
      setDocumentFile(null)
      setFileError('')
    }
  }, [state, companies, reset])

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-ink-950/50 p-4 backdrop-blur-xs">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={isEdit ? 'Cập nhật xe điện' : 'Thêm xe điện'}
        className="w-full max-w-lg overflow-hidden rounded-card bg-white shadow-float"
      >
        <div className="flex items-center justify-between border-b border-ink-100 px-5 py-4">
          <h3 className="text-base font-semibold text-ink-900">
            {isEdit ? 'Cập nhật xe điện' : 'Đăng ký xe điện mới'}
          </h3>
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
          className="space-y-4 p-5"
          noValidate
          onSubmit={handleSubmit((values) => {
            if (!isEdit && !documentFile) {
              setFileError('Vui lòng tải lên ảnh hoặc PDF giấy đăng ký xe')
              return
            }
            setFileError('')
            onSubmit(values, documentFile)
          })}
        >
          <Input
            label="Biển số xe"
            placeholder="Ví dụ: 51K-889.21"
            error={errors.plateNumber?.message}
            {...register('plateNumber')}
          />
          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Hãng xe" placeholder="VinFast" error={errors.brand?.message} {...register('brand')} />
            <Input label="Mẫu xe" placeholder="VF e34" error={errors.model?.message} {...register('model')} />
          </div>

          <div>
            <label htmlFor="vehicle-company-select" className="mb-1.5 block text-sm font-medium text-ink-700">
              Doanh nghiệp đồng hành
            </label>
            <select
              id="vehicle-company-select"
              aria-invalid={Boolean(errors.companyId)}
              className="h-11 w-full rounded-xl border border-ink-200 bg-white px-3.5 text-sm text-ink-900 outline-none focus:border-brand-500 focus:ring-3 focus:ring-brand-500/15"
              {...register('companyId')}
            >
              <option value="">Chọn doanh nghiệp gom phát thải</option>
              {companies.map((c) => (
                <option key={c.id} value={String(c.id)}>
                  {c.name}
                </option>
              ))}
            </select>
            {errors.companyId?.message && (
              <p role="alert" className="mt-1.5 text-xs font-medium text-rose-600">
                {errors.companyId.message}
              </p>
            )}
            <p className="mt-1.5 text-xs text-ink-500">
              Doanh nghiệp đã được phê duyệt dự án sẽ tổng hợp kWh sạc từ xe của bạn để chia sẻ doanh thu.
            </p>
          </div>

          <FileDropzone
            label="Giấy đăng ký xe (Cà vẹt)"
            accept="image/*,application/pdf"
            hint={isEdit ? 'Để trống nếu giữ nguyên giấy tờ hiện tại.' : 'Ảnh JPG/PNG hoặc PDF tối đa 10 MB.'}
            error={fileError}
            file={documentFile}
            onFileChange={(f) => {
              setDocumentFile(f)
              if (f) setFileError('')
            }}
          />

          <div className="flex justify-end gap-2 border-t border-ink-100 pt-4">
            <Button variant="outline" onClick={onClose} disabled={saving}>
              Huỷ
            </Button>
            <Button type="submit" loading={saving}>
              {isEdit ? 'Lưu thay đổi' : 'Thêm xe'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}

export function VehiclesPage() {
  const queryClient = useQueryClient()
  const [dialog, setDialog] = useState<DialogState>(null)
  const [deleteTarget, setDeleteTarget] = useState<Vehicle | null>(null)

  const vehiclesQuery = useQuery({
    queryKey: queryKeys.phase3b.vehicles,
    queryFn: async () => {
      try {
        return mapList(await api.get<unknown>('/vehicles'), mapVehicle)
      } catch (error) {
        if (error instanceof AxiosError && error.response?.status === 404) return []
        throw error
      }
    },
  })

  const companiesQuery = useQuery({
    queryKey: ['ev', 'approved-companies'],
    queryFn: async () => {
      const map = new Map<number, string>()
      try {
        const apps = mapList(await api.get<unknown>('/project-applications'), mapApplication)
        for (const a of apps) {
          if (a.status.toUpperCase() === 'ADMIN_APPROVED' && a.companyId && a.companyName) {
            map.set(a.companyId, a.companyName)
          }
        }
      } catch {
        // Fallback sang marketplace nếu danh sách hồ sơ không khả dụng.
      }
      try {
        const listings = await api.get<unknown>('/marketplace')
        if (Array.isArray(listings)) {
          for (const raw of listings) {
            if (!isRecord(raw)) continue
            const cid = Number(raw.sellerCompanyId ?? raw.companyId ?? 0)
            const cname = String(raw.sellerCompanyName ?? raw.companyName ?? '').trim()
            if (cid > 0 && cname && !map.has(cid)) {
              map.set(cid, cname)
            }
          }
        }
      } catch {
        // Bỏ qua nếu marketplace lỗi.
      }
      return Array.from(map.entries()).map(([id, name]) => ({ id, name }))
    },
    staleTime: 60_000,
  })

  const companies = companiesQuery.data ?? []
  const companyNameById = useMemo(
    () => new Map(companies.map((c) => [c.id, c.name])),
    [companies],
  )

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.vehicles })
  }

  const saveMutation = useMutation({
    mutationFn: async ({ values, file, vehicleId }: { values: VehicleFormValues; file: File | null; vehicleId: number | null }) => {
      const body = toFormData({
        plateNumber: values.plateNumber,
        brand: values.brand,
        model: values.model,
        companyId: Number(values.companyId),
        documentFile: file,
      })
      if (vehicleId === null) return api.post<unknown>('/vehicles', body, multipart)
      return api.put<unknown>(`/vehicles/${vehicleId}`, body, multipart)
    },
    onSuccess: async (_data, variables) => {
      toast.success(variables.vehicleId === null ? 'Đã thêm xe điện' : 'Đã cập nhật xe điện')
      setDialog(null)
      await invalidate()
    },
    onError: (error) => toast.error('Không lưu được xe', { description: errorMessage(error) }),
  })

  const deleteMutation = useMutation({
    mutationFn: (vehicleId: number) => api.delete<unknown>(`/vehicles/${vehicleId}`),
    onSuccess: async () => {
      toast.success('Đã xoá xe điện')
      setDeleteTarget(null)
      await invalidate()
    },
    onError: (error) => toast.error('Không xoá được xe', { description: errorMessage(error) }),
  })

  const vehicles = vehiclesQuery.data ?? []

  return (
    <>
      <PageIntro
        eyebrow="Chủ xe điện"
        title="Quản lý xe điện"
        description="Đăng ký xe điện, chọn doanh nghiệp đồng hành và đính kèm giấy đăng ký xe để nhận chia sẻ doanh thu."
        actions={
          <Button leftIcon={<Plus className="size-4" />} onClick={() => setDialog({ mode: 'create' })}>
            Thêm xe
          </Button>
        }
      />

      {vehiclesQuery.isPending ? (
        <QueryLoading />
      ) : vehiclesQuery.isError ? (
        <QueryError onRetry={() => void vehiclesQuery.refetch()} />
      ) : vehicles.length === 0 ? (
        <SectionCard title="Danh sách xe">
          <EmptyState
            icon={Car}
            title="Chưa có xe nào"
            description="Thêm xe điện và tải lên ảnh giấy đăng ký xe để bắt đầu tích lũy tín chỉ."
            action={
              <Button variant="outline" onClick={() => setDialog({ mode: 'create' })}>
                Thêm xe đầu tiên
              </Button>
            }
          />
        </SectionCard>
      ) : (
        <SectionCard
          title="Danh sách xe điện"
          description={`${vehicles.length} phương tiện đã đăng ký`}
        >
          <div className="grid gap-4 p-5 sm:grid-cols-2 xl:grid-cols-3">
            {vehicles.map((vehicle) => {
              const companyName =
                vehicle.companyId !== null
                  ? companyNameById.get(vehicle.companyId) ?? `Doanh nghiệp đối tác #${vehicle.companyId}`
                  : null
              return (
                <article key={vehicle.id} className="flex flex-col justify-between rounded-xl border border-ink-200 bg-white p-4 transition hover:border-brand-300">
                  <div>
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <span className="inline-flex rounded-lg bg-brand-50 px-2.5 py-1 font-mono text-sm font-bold text-brand-700">
                          {vehicle.plateNumber || 'Chưa có biển số'}
                        </span>
                        <p className="mt-2 text-sm font-semibold text-ink-900">
                          {[vehicle.brand, vehicle.model].filter(Boolean).join(' · ') || 'Chưa cập nhật hãng xe'}
                        </p>
                      </div>
                      <div className="flex shrink-0 gap-1">
                        <button
                          type="button"
                          aria-label={`Sửa xe ${vehicle.plateNumber}`}
                          onClick={() => setDialog({ mode: 'edit', vehicle })}
                          className="grid size-8 place-items-center rounded-lg text-ink-500 hover:bg-brand-50 hover:text-brand-700"
                        >
                          <Pencil className="size-4" />
                        </button>
                        <button
                          type="button"
                          aria-label={`Xoá xe ${vehicle.plateNumber}`}
                          onClick={() => setDeleteTarget(vehicle)}
                          disabled={deleteMutation.isPending}
                          className="grid size-8 place-items-center rounded-lg text-ink-500 hover:bg-rose-50 hover:text-rose-600"
                        >
                          <Trash2 className="size-4" />
                        </button>
                      </div>
                    </div>

                    {companyName && (
                      <div className="mt-3 flex items-center gap-1.5 text-xs text-ink-600">
                        <Building2 aria-hidden className="size-3.5 shrink-0 text-ink-400" />
                        <span className="truncate">{companyName}</span>
                      </div>
                    )}
                  </div>

                  <div className="mt-4 border-t border-ink-100 pt-3">
                    {vehicle.documentUrl ? (
                      <a
                        href={vehicle.documentUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center gap-1.5 text-xs font-semibold text-brand-600 hover:text-brand-700"
                      >
                        <FileText className="size-3.5" /> Xem giấy đăng ký xe
                      </a>
                    ) : (
                      <p className="text-xs text-amber-600">Chưa có giấy đăng ký xe</p>
                    )}
                  </div>
                </article>
              )
            })}
          </div>
        </SectionCard>
      )}

      {dialog && (
        <VehicleDialog
          state={dialog}
          companies={companies}
          saving={saveMutation.isPending}
          onClose={() => setDialog(null)}
          onSubmit={(values, file) => {
            saveMutation.mutate({
              values,
              file,
              vehicleId: dialog.mode === 'edit' ? dialog.vehicle.id : null,
            })
          }}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={`Xoá xe ${deleteTarget?.plateNumber ?? ''}?`}
        description="Xe đã xoá sẽ không tiếp tục được tính vào các kỳ báo cáo phát thải mới."
        confirmLabel="Xoá xe"
        variant="danger"
        loading={deleteMutation.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) deleteMutation.mutate(deleteTarget.id)
        }}
      />
    </>
  )
}
