import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router-dom'
import { Lock, Mail } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { api } from '@/shared/api/client'
import { Button } from '@/shared/components/Button'
import { Input } from '@/shared/components/Input'
import { AuthShell } from './components/AuthShell'

const emailSchema = z.object({
  email: z.string().trim().min(1, 'Email không được để trống').email('Email không đúng định dạng'),
})
type EmailValues = z.infer<typeof emailSchema>

const passwordSchema = z
  .object({
    password: z
      .string()
      .min(8, 'Mật khẩu phải có ít nhất 8 ký tự')
      .regex(/[0-9]/, 'Mật khẩu phải có ít nhất một chữ số')
      .regex(/[a-z]/, 'Mật khẩu phải có ít nhất một chữ thường')
      .regex(/[A-Z]/, 'Mật khẩu phải có ít nhất một chữ hoa')
      .regex(/[@$!%*?&]/, 'Mật khẩu phải có một ký tự đặc biệt (@$!%*?&)'),
    confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu'),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })
type PasswordValues = z.infer<typeof passwordSchema>

interface ResetState {
  email?: string
  resetToken?: string
}

export function ForgotPasswordPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const resetState = location.state as ResetState | null
  const resetToken = resetState?.resetToken
  const [sentEmail, setSentEmail] = useState(resetState?.email ?? '')

  const emailForm = useForm<EmailValues>({
    resolver: zodResolver(emailSchema),
    defaultValues: { email: resetState?.email ?? '' },
  })
  const passwordForm = useForm<PasswordValues>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { password: '', confirmPassword: '' },
  })

  const sendOtpMutation = useMutation({
    mutationFn: (values: EmailValues) =>
      api.post<unknown>('/send-otp-forgot', { email: values.email.trim() }),
    onSuccess: (_data, values) => {
      const email = values.email.trim()
      setSentEmail(email)
      toast.success('Mã OTP đã được gửi, vui lòng kiểm tra email')
      // Truyền cả query lẫn state để F5 không làm mất màn OTP.
      navigate(`/otp?email=${encodeURIComponent(email)}&from=forgot`, {
        state: { email, from: 'forgot' },
        replace: true,
      })
    },
  })

  const resetMutation = useMutation({
    mutationFn: (values: PasswordValues) =>
      api.post<unknown>(
        '/reset-password',
        { password: values.password, confirmPassword: values.confirmPassword },
        // Backend yêu cầu Bearer token lấy được từ bước verify-otp.
        { headers: { Authorization: `Bearer ${resetToken}` } },
      ),
    onSuccess: () => {
      toast.success('Đặt lại mật khẩu thành công, vui lòng đăng nhập')
      navigate('/login', { replace: true, state: null })
    },
  })

  if (resetToken) {
    return (
      <AuthShell
        title="Đặt lại mật khẩu"
        subtitle={`Tạo mật khẩu mới cho ${sentEmail || 'tài khoản của bạn'}.`}
        footer="Mã đặt lại chỉ dùng được một lần."
      >
        <form
          noValidate
          onSubmit={passwordForm.handleSubmit((values) => resetMutation.mutate(values))}
          className="mt-7 space-y-4"
        >
          <Input
            label="Mật khẩu mới"
            type="password"
            autoComplete="new-password"
            placeholder="Tối thiểu 8 ký tự, có hoa/thường/số/ký tự đặc biệt"
            icon={<Lock aria-hidden className="size-4" />}
            error={passwordForm.formState.errors.password?.message}
            {...passwordForm.register('password')}
          />
          <Input
            label="Xác nhận mật khẩu mới"
            type="password"
            autoComplete="new-password"
            placeholder="Nhập lại mật khẩu mới"
            icon={<Lock aria-hidden className="size-4" />}
            error={passwordForm.formState.errors.confirmPassword?.message}
            {...passwordForm.register('confirmPassword')}
          />
          <Button type="submit" fullWidth size="lg" loading={resetMutation.isPending}>
            Xác nhận đặt lại
          </Button>
        </form>
      </AuthShell>
    )
  }

  return (
    <AuthShell
      title="Quên mật khẩu"
      subtitle="Nhập email đăng ký để nhận mã OTP đặt lại mật khẩu."
      footer={
        <>
          Nhớ lại mật khẩu?{' '}
          <button
            type="button"
            onClick={() => navigate('/login')}
            className="font-semibold text-brand-600 hover:text-brand-700"
          >
            Đăng nhập
          </button>
        </>
      }
    >
      <form
        noValidate
        onSubmit={emailForm.handleSubmit((values) => sendOtpMutation.mutate(values))}
        className="mt-7 space-y-5"
      >
        <Input
          label="Email"
          type="email"
          autoComplete="email"
          placeholder="ban@congty.vn"
          icon={<Mail aria-hidden className="size-4" />}
          error={emailForm.formState.errors.email?.message}
          {...emailForm.register('email')}
        />
        <Button type="submit" fullWidth size="lg" loading={sendOtpMutation.isPending}>
          Gửi mã OTP
        </Button>
      </form>
    </AuthShell>
  )
}
