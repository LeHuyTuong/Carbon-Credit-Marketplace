import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { KeyRound } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { api } from '@/shared/api/client'
import { errorMessage } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { Input } from '@/shared/components/Input'
import { PageIntro } from '@/pages/components/PageIntro'
import { SectionCard } from '@/pages/components/QueryState'

const passwordSchema = z
  .object({
    oldPassword: z.string().min(1, 'Vui lòng nhập mật khẩu hiện tại'),
    newPassword: z
      .string()
      .min(8, 'Mật khẩu mới phải có ít nhất 8 ký tự')
      .regex(/[A-Za-z]/, 'Mật khẩu mới phải chứa chữ cái')
      .regex(/[0-9]/, 'Mật khẩu mới phải chứa chữ số'),
    confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu mới'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })

type PasswordValues = z.infer<typeof passwordSchema>

export function ChangePasswordPage() {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PasswordValues>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { oldPassword: '', newPassword: '', confirmPassword: '' },
  })

  const mutation = useMutation({
    mutationFn: (values: PasswordValues) =>
      api.post<unknown>('/change-password', {
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      }),
    onSuccess: () => {
      toast.success('Đã đổi mật khẩu thành công')
      reset()
    },
    onError: (err) => toast.error('Không đổi được mật khẩu', { description: errorMessage(err) }),
  })

  return (
    <>
      <PageIntro
        eyebrow="Bảo mật tài khoản"
        title="Đổi mật khẩu"
        description="Cập nhật mật khẩu định kỳ để bảo vệ ví tiền và kho tín chỉ carbon của bạn."
      />

      <SectionCard
        title="Thiết lập mật khẩu mới"
        description="Mật khẩu cần có tối thiểu 8 ký tự, bao gồm cả chữ và số."
        className="max-w-xl"
      >
        <form
          noValidate
          onSubmit={handleSubmit((values) => mutation.mutate(values))}
          className="space-y-4 p-5"
        >
          <Input
            label="Mật khẩu hiện tại"
            type="password"
            error={errors.oldPassword?.message}
            {...register('oldPassword')}
          />
          <Input
            label="Mật khẩu mới"
            type="password"
            error={errors.newPassword?.message}
            {...register('newPassword')}
          />
          <Input
            label="Xác nhận mật khẩu mới"
            type="password"
            error={errors.confirmPassword?.message}
            {...register('confirmPassword')}
          />

          <div className="flex justify-end border-t border-ink-100 pt-4">
            <Button type="submit" loading={mutation.isPending} leftIcon={<KeyRound className="size-4" />}>
              Cập nhật mật khẩu
            </Button>
          </div>
        </form>
      </SectionCard>
    </>
  )
}
