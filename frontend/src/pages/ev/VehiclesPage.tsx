import { useEffect, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { AxiosError } from 'axios'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Car, FileText, Pencil, Plus, Trash2, X } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { api } from '@/shared/api/client'
import {
  errorMessage,
  mapList,
  mapVehicle,
  toFormData,
  type Vehicle,
} from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { Input } from '@/shared/components/Input'
import { queryKeys } from '@/shared/lib/queryKeys'
import { EmptyState } from '@/shared/components/EmptyState'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

const vehicleSchema = z.object({
  plateNumber: z.string().trim().min(3, 'Biển số xe phải có ít nhất 3 ký tự'),
  brand: z.string().trim().min(1, 'Vui lòng nhập hãng xe'),
  model: z.string().trim().min(1, 'Vui lòng nhập mẫu xe'),
  companyId: z
    .string()
    .trim()
    .min(1, 'Vui lòng nhập mã doanh nghiệp')
    .regex(/^[0-9]+$/, 'Mã doanh nghiệp phải là số'),
})

type VehicleFormValues = z.infer<typeof vehicleSchema>

type DialogState = { mode: 'create' } | { mode: 'edit'; vehicle: Vehicle } | null

const multipart = { headers: { 'Content-Type': 'multipart/form-data' } } as const

function VehicleDialog({
  state,
  saving,
  onClose,
  onSubmit,
}: {
  state: NonNullable<DialogState>
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
    defaultValues: { plateNumber: '', brand: '', model: '', companyId: '' },
  })

  useEffect(() => {
    if (state.mode === 'edit') {
      const { vehicle } = state
      reset({
        plateNumber: vehicle.plateNumber,
        brand: vehicle.brand,
        model: vehicle.model,
        companyId: vehicle.companyId === null ? '' : String(vehicle.companyId),
      })
    } else {
      reset({ plateNumber: '', brand: '', model: '', companyId: '' })
      setDocumentFile(null)
      setFileError('')
    }
  }, [state, reset])

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-ink-950/50 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={isEdit ? 'Cập nhật xe điện' : 'Thêm xe điện'}
        className="w-full max-w-lg overflow-hidden rounded-card bg-white shadow-float"
      >
        <div className="flex items-center justify-between border-b border-ink-100 px-5 py-4">
          <h3 className="text-base font-semibold text-ink-900">
            {isEdit ? 'Cập nhật xe điện' : 'Thêm xe điện'}
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
              setFileError('Vui lòng chọn ảnh giấy đăng ký xe')
              return
            }
            setFileError('')
            onSubmit(values, documentFile)
          })}
        >
          <Input
            label="Biển số xe"
            placeholder="30A-12345"
            error={errors.plateNumber?.message}
            {...register('plateNumber')}
          />
          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Hãng xe" placeholder="VinFast" error={errors.brand?.message} {...register('brand')} />
            <Input label="Mẫu xe" placeholder="VF e34" error={errors.model?.message} {...register('model')} />
          </div>
          <Input
            label="Mã doanh nghiệp"
            placeholder="Nhập mã công ty chủ quản xe"
            error={errors.companyId?.message}
            {...register('companyId')}
          />
          <Input
            label="Ảnh / file giấy đăng ký xe"
            type="file"
            accept="image/*,application/pdf"
            hint={isEdit ? 'Để trống nếu không thay đổi giấy tờ.' : 'Bắt buộc khi đăng ký xe mới.'}
            error={fileError}
            onChange={(event) => {
              setDocumentFile(event.target.files?.[0] ?? null)
              if (event.target.files?.length) setFileError('')
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
  const vehiclesQuery = useQuery({
    queryKey: queryKeys.phase3b.vehicles,
    queryFn: async () => {
      try {
        return mapList(await api.get<unknown>('/vehicles'), mapVehicle)
      } catch (error) {
        // Backend trả 404 khi chủ xe chưa có xe nào — đây là empty state hợp lệ.
        if (error instanceof AxiosError && error.response?.status === 404) return []
        throw error
      }
    },
  })

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
        description="Đăng ký, cập nhật và xoá xe điện kèm ảnh giấy đăng ký xe."
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
            description="Thêm xe điện và uploads ảnh giấy đăng ký để bắt đầu."
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
          description={`${vehicles.length} xe đã đăng ký`}
        >
          <div className="grid gap-4 p-5 sm:grid-cols-2 xl:grid-cols-3">
            {vehicles.map((vehicle) => (
              <article key={vehicle.id} className="rounded-xl border border-ink-200 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-base font-semibold text-ink-900">{vehicle.plateNumber || 'Chưa có biển số'}</p>
                    <p className="mt-0.5 text-sm text-ink-500">
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
                      onClick={() => {
                        if (window.confirm(`Xoá xe ${vehicle.plateNumber}?`)) deleteMutation.mutate(vehicle.id)
                      }}
                      disabled={deleteMutation.isPending}
                      className="grid size-8 place-items-center rounded-lg text-ink-500 hover:bg-rose-50 hover:text-rose-600"
                    >
                      <Trash2 className="size-4" />
                    </button>
                  </div>
                </div>

                <div className="mt-4 space-y-1.5 text-xs text-ink-500">
                  <p>Mã xe: #{vehicle.id}</p>
                  {vehicle.companyId !== null && <p>Company ID: {vehicle.companyId}</p>}
                </div>

                {vehicle.documentUrl ? (
                  <a
                    href={vehicle.documentUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="mt-4 inline-flex items-center gap-1.5 text-sm font-semibold text-brand-600 hover:text-brand-700"
                  >
                    <FileText className="size-4" /> Xem giấy đăng ký xe
                  </a>
                ) : (
                  <p className="mt-4 text-xs text-amber-600">Chưa có giấy đăng ký xe</p>
                )}
              </article>
            ))}
          </div>
        </SectionCard>
      )}

      {dialog && (
        <VehicleDialog
          state={dialog}
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
    </>
  )
}
