import { useEffect } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Building2, Save } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { api } from '@/shared/api/client'
import { errorMessage, isRecord } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { Input } from '@/shared/components/Input'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryLoading, SectionCard } from '@/pages/components/QueryState'

const companyKycSchema = z.object({
  companyName: z.string().trim().min(2, 'Tên doanh nghiệp phải có ít nhất 2 ký tự'),
  taxCode: z.string().trim().min(5, 'Mã số thuế không hợp lệ'),
  businessLicense: z.string().trim().min(3, 'Vui lòng nhập số giấy phép kinh doanh'),
  address: z.string().trim().min(5, 'Vui lòng nhập địa chỉ trụ sở'),
})

type CompanyKycValues = z.infer<typeof companyKycSchema>

export function CompanyKycPage() {
  const queryClient = useQueryClient()
  const kycQuery = useQuery({
    queryKey: ['kyc', 'company'],
    queryFn: async () => {
      try {
        const data = await api.get<unknown>('/kyc/company')
        if (!isRecord(data)) return null
        return {
          companyName: String(data.companyName ?? data.company_name ?? ''),
          taxCode: String(data.taxCode ?? data.tax_code ?? ''),
          businessLicense: String(data.businessLicense ?? data.business_license ?? ''),
          address: String(data.address ?? ''),
        }
      } catch {
        return null
      }
    },
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CompanyKycValues>({
    resolver: zodResolver(companyKycSchema),
    defaultValues: { companyName: '', taxCode: '', businessLicense: '', address: '' },
  })

  useEffect(() => {
    if (kycQuery.data) reset(kycQuery.data)
  }, [kycQuery.data, reset])

  const hasExisting = Boolean(kycQuery.data?.companyName)

  const saveMutation = useMutation({
    mutationFn: (values: CompanyKycValues) =>
      hasExisting
        ? api.put<unknown>('/kyc/company', { data: values })
        : api.post<unknown>('/kyc/company', { data: values }),
    onSuccess: async () => {
      toast.success(hasExisting ? 'Đã cập nhật hồ sơ KYC doanh nghiệp' : 'Đã gửi hồ sơ KYC doanh nghiệp')
      await queryClient.invalidateQueries({ queryKey: ['kyc', 'company'] })
    },
    onError: (err) => toast.error('Không lưu được hồ sơ doanh nghiệp', { description: errorMessage(err) }),
  })

  return (
    <>
      <PageIntro
        eyebrow="Doanh nghiệp"
        title="Hồ sơ định danh doanh nghiệp (KYC)"
        description="Doanh nghiệp cần hoàn thiện định danh pháp nhân (BR-01, BR-06) trước khi đăng ký dự án và giao dịch tín chỉ carbon."
      />

      {kycQuery.isPending ? (
        <QueryLoading />
      ) : (
        <SectionCard
          title="Thông tin pháp nhân"
          description={hasExisting ? 'Hồ sơ đã được ghi nhận trên hệ thống CarbonX' : 'Điền đầy đủ thông tin pháp nhân bên dưới'}
        >
          <form
            noValidate
            onSubmit={handleSubmit((values) => saveMutation.mutate(values))}
            className="space-y-4 p-5"
          >
            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="Tên doanh nghiệp"
                placeholder="Công ty Cổ phần VinFast Green Mobility"
                icon={<Building2 className="size-4" />}
                error={errors.companyName?.message}
                {...register('companyName')}
              />
              <Input
                label="Mã số thuế"
                placeholder="0108926276"
                error={errors.taxCode?.message}
                {...register('taxCode')}
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="Số giấy phép kinh doanh"
                placeholder="GPKD-0108926276"
                error={errors.businessLicense?.message}
                {...register('businessLicense')}
              />
              <Input
                label="Địa chỉ trụ sở"
                placeholder="Khu kinh tế Đình Vũ - Cát Hải, Hải Phòng"
                error={errors.address?.message}
                {...register('address')}
              />
            </div>

            <div className="flex justify-end border-t border-ink-100 pt-4">
              <Button type="submit" loading={saveMutation.isPending} leftIcon={<Save className="size-4" />}>
                {hasExisting ? 'Cập nhật hồ sơ KYC' : 'Gửi hồ sơ KYC'}
              </Button>
            </div>
          </form>
        </SectionCard>
      )}
    </>
  )
}
