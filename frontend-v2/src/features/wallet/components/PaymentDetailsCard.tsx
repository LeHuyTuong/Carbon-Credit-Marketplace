import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Building2 } from 'lucide-react'
import type { PaymentDetails } from '@/shared/api/types'
import { Button } from '@/shared/components/Button'
import { Input } from '@/shared/components/Input'
import { paymentDetailsSchema, type PaymentDetailsValues } from '../schemas'
import { useSavePaymentDetails } from '../useWallet'

export function PaymentDetailsCard({ details }: { details: PaymentDetails }) {
  const save = useSavePaymentDetails()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PaymentDetailsValues>({
    resolver: zodResolver(paymentDetailsSchema),
    defaultValues: { bankName: '', accountNumber: '', accountHolder: '', branch: '' },
  })

  useEffect(() => {
    reset({
      bankName: details.bankName,
      accountNumber: details.accountNumber,
      accountHolder: details.accountHolder,
      branch: details.branch,
    })
  }, [details, reset])

  return (
    <div className="p-5">
      <div className="mb-4 flex items-center gap-2 text-sm font-medium text-ink-700">
        <Building2 aria-hidden className="size-4 text-brand-600" />
        {details.id ? 'Tài khoản nhận tiền hiện tại' : 'Thêm tài khoản nhận tiền'}
      </div>

      <form
        noValidate
        onSubmit={handleSubmit((values) => save.mutate(values))}
        className="grid gap-4 sm:grid-cols-2"
      >
        <Input
          label="Ngân hàng"
          placeholder="Vietcombank"
          error={errors.bankName?.message}
          {...register('bankName')}
        />
        <Input
          label="Số tài khoản"
          inputMode="numeric"
          placeholder="1234567890"
          error={errors.accountNumber?.message}
          {...register('accountNumber')}
        />
        <Input
          label="Chủ tài khoản"
          placeholder="NGUYEN VAN A"
          error={errors.accountHolder?.message}
          {...register('accountHolder')}
        />
        <Input
          label="Chi nhánh"
          placeholder="Chi nhánh Hà Nội"
          error={errors.branch?.message}
          {...register('branch')}
        />
        <div className="sm:col-span-2">
          <Button type="submit" loading={save.isPending}>
            {details.id ? 'Cập nhật tài khoản' : 'Lưu tài khoản'}
          </Button>
        </div>
      </form>
    </div>
  )
}
