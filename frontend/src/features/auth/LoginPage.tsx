import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Lock, Mail } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import type { AuthPayload } from '@/shared/api/types'
import { Button } from '@/shared/components/Button'
import { Input } from '@/shared/components/Input'
import { decodeJwt } from '@/shared/lib/jwt'
import { normalizeRole, roleHome } from '@/shared/lib/roles'
import { useAuthStore } from '@/shared/stores/auth.store'
import { AuthShell } from './components/AuthShell'
import { loginSchema, type LoginValues } from './schemas'

interface LocationState {
  from?: string
}

function firstDefined(...values: unknown[]): unknown {
  return values.find((value) => value !== undefined && value !== null && value !== '')
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setAuth = useAuthStore((state) => state.setAuth)
  const [showPassword, setShowPassword] = useState(false)

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const loginMutation = useMutation({
    mutationFn: (values: LoginValues) => api.post<AuthPayload>('/auth/login', values),
    onSuccess: (payload) => {
      const token = payload.jwt ?? payload.token
      if (!token) {
        toast.error('Máy chủ không trả về token đăng nhập')
        return
      }

      const claims = decodeJwt(token)
      const role = normalizeRole(
        firstDefined(payload.roles?.[0], claims.roles?.[0], claims.role, payload.role),
      )
      if (!role) {
        toast.error('Tài khoản chưa có vai trò hợp lệ')
        return
      }

      setAuth({
        token,
        role,
        user: {
          id: firstDefined(claims.sub, claims.userId) as string | number | undefined,
          email: claims.email ?? '',
          fullName: claims.fullName ?? claims.name,
          role,
        },
      })
      toast.success('Đăng nhập thành công')

      const targetHome = roleHome(role)
      const from = (location.state as LocationState | null)?.from
      const canReturnToFrom = typeof from === 'string' && from.startsWith(targetHome)
      navigate(canReturnToFrom ? from : targetHome, { replace: true })
    },
  })

  const handleQuickLogin = (email: string, pass: string) => {
    setValue('email', email, { shouldValidate: true })
    setValue('password', pass, { shouldValidate: true })
    loginMutation.mutate({ email, password: pass })
  }

  const showDemoLogin = import.meta.env.DEV || import.meta.env.VITE_ENABLE_DEMO_LOGIN === 'true'

  const DEMO_ACCOUNTS = [
    { role: 'EV_OWNER', label: 'Chủ xe điện', email: 'evowner@example.com', badge: 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100' },
    { role: 'COMPANY', label: 'Doanh nghiệp', email: 'company@example.com', badge: 'bg-blue-50 text-blue-700 border-blue-200 hover:bg-blue-100' },
    { role: 'CVA', label: 'Đơn vị CVA', email: 'cva@example.com', badge: 'bg-amber-50 text-amber-700 border-amber-200 hover:bg-amber-100' },
    { role: 'ADMIN', label: 'Quản trị viên', email: 'admin@gmail.com', badge: 'bg-rose-50 text-rose-700 border-rose-200 hover:bg-rose-100' },
  ] as const

  return (
    <AuthShell
      title="Chào mừng trở lại"
      subtitle="Đăng nhập để tiếp tục quản lý tín chỉ carbon của bạn."
      footer={
        <>
          Chưa có tài khoản?{' '}
          <Link to="/register" className="font-semibold text-brand-600 hover:text-brand-700">
            Đăng ký ngay
          </Link>
        </>
      }
    >
      <form
        noValidate
        onSubmit={handleSubmit((values) => loginMutation.mutate(values))}
        className="mt-7 space-y-5"
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

        <div className="relative">
          <Input
            label="Mật khẩu"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            placeholder="Nhập mật khẩu"
            icon={<Lock aria-hidden className="size-4" />}
            error={errors.password?.message}
            className="pr-11"
            {...register('password')}
          />
          <button
            type="button"
            onClick={() => setShowPassword((visible) => !visible)}
            aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            className="absolute right-3 top-[38px] grid size-7 place-items-center rounded-md text-ink-400 hover:text-ink-700"
          >
            {showPassword ? <EyeOff aria-hidden className="size-4" /> : <Eye aria-hidden className="size-4" />}
          </button>
        </div>

        <Button type="submit" fullWidth size="lg" loading={loginMutation.isPending}>
          Đăng nhập
        </Button>
      </form>

      {showDemoLogin && (
        <div className="mt-6 rounded-2xl border border-ink-100 bg-ink-50/70 p-4">
          <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">
            Đăng nhập nhanh 1-chạm (Tài khoản mẫu)
          </p>
          <div className="mt-3 grid grid-cols-2 gap-2">
            {DEMO_ACCOUNTS.map((acc) => (
              <button
                key={acc.role}
                type="button"
                onClick={() => handleQuickLogin(acc.email, 'Password@1')}
                disabled={loginMutation.isPending}
                className={`flex flex-col items-start rounded-xl border p-2.5 text-left transition hover:shadow-sm ${acc.badge}`}
              >
                <span className="text-xs font-bold">{acc.label}</span>
                <span className="truncate text-[11px] opacity-80">{acc.email}</span>
              </button>
            ))}
          </div>
        </div>
      )}
    </AuthShell>
  )
}
