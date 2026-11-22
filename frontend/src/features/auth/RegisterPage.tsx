import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { Lock, Mail, UserRound } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import type { AuthPayload } from '@/shared/api/types'
import { Button } from '@/shared/components/Button'
import { Input } from '@/shared/components/Input'
import { cn } from '@/shared/lib/cn'
import { normalizeRole, roleHome } from '@/shared/lib/roles'
import { AuthShell } from './components/AuthShell'
import { registerSchema, type RegisterValues } from './schemas'

export function RegisterPage() {
  const navigate = useNavigate()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { email: '', password: '', confirmPassword: '', roleName: 'EV_OWNER' },
  })

  const registerMutation = useMutation({
    mutationFn: (values: RegisterValues) => api.post<AuthPayload>('/auth/register', values),
    onSuccess: (payload) => {
      toast.success(
        payload.message ?? 'Đăng ký thành công',
        { description: 'Vui lòng kiểm tra email để xác thực OTP trước khi đăng nhập.' },
      )
      const role = normalizeRole(payload.roles?.[0] ?? payload.role)
      navigate(role ? roleHome(role) : '/login', { replace: true })
    },
  })

  return (
    <AuthShell
      title="Tạo tài khoản CarbonX"
      subtitle="Chọn vai trò của bạn để bắt đầu tham gia thị trường tín chỉ."
      footer={
        <>
          Đã có tài khoản?{' '}
          <Link to="/login" className="font-semibold text-brand-600 hover:text-brand-700">
            Đăng nhập
          </Link>
        </>
      }
    >
      <form
        noValidate
        onSubmit={handleSubmit((values) => registerMutation.mutate(values))}
        className="mt-7 space-y-4"
      >
        <Input
          label="Email"
          type="email"
          autoComplete="email"
          placeholder="ban@congty.vn"
          icon={<Mail aria-hidden className="size-4" />}
          error={errors.email?.message}
          {...register('email')}
        />

        <div>
          <label htmlFor="roleName" className="mb-1.5 block text-sm font-medium text-ink-700">
            Vai trò
          </label>
          <div className="relative">
            <UserRound aria-hidden className="pointer-events-none absolute left-3 top-3 size-4 text-ink-400" />
            <select
              id="roleName"
              aria-invalid={Boolean(errors.roleName)}
              className={cn(
                'h-11 w-full appearance-none rounded-xl border bg-white pl-10 pr-3 text-sm text-ink-900 outline-none transition',
                errors.roleName
                  ? 'border-rose-400 focus:border-rose-500'
                  : 'border-ink-200 focus:border-brand-500',
              )}
              {...register('roleName')}
            >
              <option value="EV_OWNER">Chủ xe điện (EV Owner)</option>
              <option value="CC_BUYER">Doanh nghiệp mua tín chỉ</option>
              <option value="CVA">Đơn vị thẩm định (CVA)</option>
            </select>
          </div>
          {errors.roleName?.message && (
            <p role="alert" className="mt-1.5 text-xs font-medium text-rose-600">
              {errors.roleName.message}
            </p>
          )}
        </div>

        <Input
          label="Mật khẩu"
          type="password"
          autoComplete="new-password"
          placeholder="Tối thiểu 8 ký tự, có hoa/thường/số/ký tự đặc biệt"
          icon={<Lock aria-hidden className="size-4" />}
          error={errors.password?.message}
          {...register('password')}
        />

        <Input
          label="Xác nhận mật khẩu"
          type="password"
          autoComplete="new-password"
          placeholder="Nhập lại mật khẩu"
          icon={<Lock aria-hidden className="size-4" />}
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />

        <Button type="submit" fullWidth size="lg" loading={registerMutation.isPending}>
          Tạo tài khoản
        </Button>
      </form>
    </AuthShell>
  )
}
