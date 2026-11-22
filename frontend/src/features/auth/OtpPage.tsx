import { useEffect, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ShieldCheck } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import type { AuthPayload } from '@/shared/api/types'
import { Button } from '@/shared/components/Button'
import { Input } from '@/shared/components/Input'
import { AuthShell } from './components/AuthShell'

/** Luồng xác thực: register = kích hoạt tài khoản, forgot = lấy token đặt lại mật khẩu. */
type Flow = 'register' | 'forgot'

export function OtpPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const email = (searchParams.get('email') ?? '').trim()
  const flow: Flow = searchParams.get('from') === 'forgot' ? 'forgot' : 'register'
  const [otp, setOtp] = useState('')

  // Truy cập thẳng (F5/mở tab mới) vẫn giữ được email/flow nhờ query string.
  useEffect(() => {
    if (!email) {
      toast.error('Thiếu email xác thực')
      navigate('/login', { replace: true })
    }
  }, [email, navigate])

  const verifyMutation = useMutation({
    mutationFn: () => api.post<AuthPayload>('/auth/verify-otp', { email, otpCode: otp }),
    onSuccess: (payload) => {
      if (flow === 'forgot') {
        const resetToken = payload.jwt ?? payload.token
        if (!resetToken) {
          toast.error('Máy chủ không trả về mã đặt lại mật khẩu')
          return
        }
        toast.success('Xác thực thành công, vui lòng đặt mật khẩu mới')
        navigate('/forgot-password', { replace: true, state: { email, resetToken } })
        return
      }
      toast.success('Xác thực tài khoản thành công')
      navigate('/login', { replace: true })
    },
  })

  return (
    <AuthShell
      title="Xác thực mã OTP"
      subtitle="Nhập mã 6 chữ số đã được gửi tới email của bạn."
      footer={
        <>
          Quay lại{' '}
          <button
            type="button"
            onClick={() => navigate('/login', { replace: true })}
            className="font-semibold text-brand-600 hover:text-brand-700"
          >
            trang đăng nhập
          </button>
        </>
      }
    >
      <div className="mt-6 rounded-xl bg-brand-50 px-4 py-3 text-sm text-brand-800">
        Mã OTP được gửi tới <strong className="font-semibold">{email || '—'}</strong>
      </div>

      <form
        noValidate
        onSubmit={(event) => {
          event.preventDefault()
          if (!/^\d{6}$/.test(otp)) {
            toast.warning('Vui lòng nhập đủ 6 chữ số')
            return
          }
          verifyMutation.mutate()
        }}
        className="mt-6 space-y-5"
      >
        <Input
          label="Mã xác thực"
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          placeholder="000000"
          icon={<ShieldCheck aria-hidden className="size-4" />}
          value={otp}
          onChange={(event) => setOtp(event.target.value.replace(/\D/g, '').slice(0, 6))}
          className="tracking-[0.5em]"
        />

        <Button type="submit" fullWidth size="lg" loading={verifyMutation.isPending} disabled={otp.length !== 6}>
          Xác thực
        </Button>
      </form>
    </AuthShell>
  )
}
