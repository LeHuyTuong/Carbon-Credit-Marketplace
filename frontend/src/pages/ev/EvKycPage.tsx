import { useEffect, useId, type SelectHTMLAttributes } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Save } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { api } from '@/shared/api/client'
import { errorMessage, isRecord, mapKyc, type KycProfile } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { queryKeys } from '@/shared/lib/queryKeys'
import { PageIntro } from '@/pages/components/PageIntro'
import { SectionCard } from '@/pages/components/QueryState'

const kycSchema = z.object({
  name: z.string().trim().min(2, 'Họ tên phải có ít nhất 2 ký tự'),
  phone: z
    .string()
    .trim()
    .regex(/^(0[0-9]{9}|\+84[0-9]{9})$/, 'Số điện thoại phải có dạng 09xxxxxxxx hoặc +849xxxxxxxx'),
  country: z.string().trim().min(2, 'Vui lòng nhập quốc gia'),
  address: z.string().trim().min(5, 'Địa chỉ phải có ít nhất 5 ký tự'),
  birthDate: z
    .string()
    .min(1, 'Vui lòng chọn ngày sinh')
    .refine((value) => {
      const date = new Date(value)
      return !Number.isNaN(date.getTime()) && date.getTime() <= Date.now()
    }, 'Ngày sinh không hợp lệ'),
  documentType: z.enum(['CCCD', 'CMND', 'DRIVE_CARD']),
  documentNumber: z.string().trim().min(5, 'Số giấy tờ phải có ít nhất 5 ký tự'),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER']),
})

type KycFormValues = z.infer<typeof kycSchema>

const EMPTY_FORM: KycFormValues = {
  name: '',
  phone: '',
  country: 'Việt Nam',
  address: '',
  birthDate: '',
  documentType: 'CCCD',
  documentNumber: '',
  gender: 'MALE',
}

function SelectField({
  label,
  error,
  children,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement> & { label: string; error?: string }) {
  const id = useId()
  return (
    <div className="w-full">
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-ink-700">
        {label}
      </label>
      <select
        id={id}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        className={`h-11 w-full rounded-xl border bg-white px-3.5 text-sm text-ink-900 shadow-sm outline-none transition focus:ring-3 ${
          error
            ? 'border-rose-400 focus:border-rose-500 focus:ring-rose-500/15'
            : 'border-ink-200 focus:border-brand-500 focus:ring-brand-500/15'
        }`}
        {...props}
      >
        {children}
      </select>
      {error && (
        <p id={`${id}-error`} role="alert" className="mt-1.5 text-xs font-medium text-rose-600">
          {error}
        </p>
      )}
    </div>
  )
}

function toFormValues(profile: KycProfile): KycFormValues {
  const documentTypes: KycFormValues['documentType'][] = ['CCCD', 'CMND', 'DRIVE_CARD']
  const genders: KycFormValues['gender'][] = ['MALE', 'FEMALE', 'OTHER']
  return {
    name: profile.name,
    phone: profile.phone,
    country: profile.country,
    address: profile.address,
    birthDate: profile.birthDate.slice(0, 10),
    documentType: documentTypes.includes(profile.documentType as KycFormValues['documentType'])
      ? (profile.documentType as KycFormValues['documentType'])
      : 'CCCD',
    documentNumber: profile.documentNumber,
    gender: genders.includes(profile.gender as KycFormValues['gender'])
      ? (profile.gender as KycFormValues['gender'])
      : 'MALE',
  }
}

export function EvKycPage() {
  const queryClient = useQueryClient()
  const kycQuery = useQuery({
    queryKey: queryKeys.phase3b.kyc,
    queryFn: async () => {
      const data = await api.get<unknown>('/kyc/user')
      if (!isRecord(data)) return null
      // envelope có response null → chưa có hồ sơ
      if (data.response === null || data.responseData === null) return null
      // tránh nhầm envelope (trace, responseStatus) với hồ sơ KYC
      if (!data.name && !data.email && !data.userId && !data.id) return null
      return mapKyc(data)
    },
    retry: false,
  })
  const existing = kycQuery.data
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<KycFormValues>({
    resolver: zodResolver(kycSchema),
    mode: 'onBlur',
    defaultValues: EMPTY_FORM,
  })

  useEffect(() => {
    if (existing) reset(toFormValues(existing))
  }, [existing, reset])

  const saveMutation = useMutation({
    mutationFn: (values: KycFormValues) => {
      const body = { data: values }
      return existing
        ? api.put<unknown>('/kyc/user', body)
        : api.post<unknown>('/kyc/user', body)
    },
    onSuccess: async () => {
      toast.success(existing ? 'Đã cập nhật hồ sơ KYC' : 'Đã nộp hồ sơ KYC')
      await queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.kyc })
    },
    onError: (error) => {
      toast.error('Không lưu được hồ sơ', { description: errorMessage(error) })
    },
  })

  return (
    <>
      <PageIntro
        eyebrow="Chủ xe điện"
        title="Hồ sơ định danh KYC"
        description="Khai báo thông tin cá nhân và giấy tờ định danh. Các ô nhập hợp lệ được giữ nguyên khi có ô khác sai."
      />

      <SectionCard
        title={existing ? 'Cập nhật thông tin định danh' : 'Đăng ký thông tin định danh'}
        description={
          kycQuery.isError
            ? 'Chưa có hồ sơ trên hệ thống. Vui lòng khai báo thông tin bên dưới.'
            : 'Thông tin sẽ được đối chiếu trước khi xe được phê duyệt tham gia thị trường.'
        }
        action={existing ? <StatusBadge status="SUBMITTED" label="Đã có hồ sơ" /> : undefined}
      >
        <form
          className="space-y-5 p-5"
          noValidate
          onSubmit={handleSubmit((values) => saveMutation.mutate(values))}
        >
          <div className="grid gap-5 md:grid-cols-2">
            <Input
              label="Họ và tên"
              placeholder="Nguyễn Văn A"
              autoComplete="name"
              error={errors.name?.message}
              {...register('name')}
            />
            <Input
              label="Số điện thoại"
              placeholder="09xxxxxxxx"
              autoComplete="tel"
              error={errors.phone?.message}
              {...register('phone')}
            />
            <Input label="Quốc gia" error={errors.country?.message} {...register('country')} />
            <Input
              label="Ngày sinh"
              type="date"
              error={errors.birthDate?.message}
              {...register('birthDate')}
            />
            <div className="md:col-span-2">
              <Input
                label="Địa chỉ thường trú"
                placeholder="Số nhà, đường, quận, thành phố"
                autoComplete="street-address"
                error={errors.address?.message}
                {...register('address')}
              />
            </div>
            <SelectField label="Loại giấy tờ" error={errors.documentType?.message} {...register('documentType')}>
              <option value="CCCD">CCCD</option>
              <option value="CMND">CMND</option>
              <option value="DRIVE_CARD">Bằng lái xe</option>
            </SelectField>
            <Input
              label="Số giấy tờ"
              placeholder="Số CCCD/CMND"
              error={errors.documentNumber?.message}
              {...register('documentNumber')}
            />
            <SelectField label="Giới tính" error={errors.gender?.message} {...register('gender')}>
              <option value="MALE">Nam</option>
              <option value="FEMALE">Nữ</option>
              <option value="OTHER">Khác</option>
            </SelectField>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-ink-100 pt-4">
            <p className="text-xs text-ink-500">
              {existing ? 'Hồ sơ đã tồn tại: lưu sẽ cập nhật toàn bộ thông tin.' : 'Chưa có hồ sơ: lưu sẽ tạo mới.'}
              {isDirty && ' Có thay đổi chưa được lưu.'}
            </p>
            <Button type="submit" loading={saveMutation.isPending} leftIcon={<Save className="size-4" />}>
              {existing ? 'Lưu thay đổi' : 'Nộp hồ sơ KYC'}
            </Button>
          </div>
        </form>
      </SectionCard>
    </>
  )
}
