import { useEffect, useRef } from 'react'
import { LoaderCircle, ShieldAlert } from 'lucide-react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { decodeJwt } from '@/shared/lib/jwt'
import { normalizeRole, roleHome } from '@/shared/lib/roles'
import { useAuthStore } from '@/shared/stores/auth.store'

/** Token OAuth là JWT: đúng 3 đoạn base64url, giới hạn độ dài để chặn URL rác. */
function isJwtShape(token: string): boolean {
  const parts = token.split('.')
  return (
    parts.length === 3 &&
    token.length <= 4096 &&
    parts.every((part) => part.length > 0 && /^[A-Za-z0-9_-]+$/.test(part))
  )
}

export function OAuthSuccessPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const setAuth = useAuthStore((state) => state.setAuth)
  const handled = useRef(false)

  useEffect(() => {
    // StrictMode chạy effect hai lần trong dev: chỉ xử lý callback một lần.
    if (handled.current) return
    handled.current = true

    const hashParams = new URLSearchParams(window.location.hash.replace(/^#/, ''))
    const token = hashParams.get('token') ?? searchParams.get('token') ?? ''
    if (!isJwtShape(token)) {
      toast.error('Liên kết đăng nhập Google không hợp lệ')
      navigate('/login', { replace: true })
      return
    }

    const claims = decodeJwt(token)
    const role = normalizeRole(claims.roles?.[0] ?? claims.role)
    if (!claims.email || !role) {
      toast.error('Thông tin tài khoản Google không đầy đủ')
      navigate('/login', { replace: true })
      return
    }

    setAuth({
      token,
      role,
      user: { email: claims.email, role, fullName: claims.fullName ?? claims.name },
    })
    toast.success('Đăng nhập Google thành công')

    // replace: xoá token khỏi thanh địa chỉ và history (tránh lộ qua referrer).
    navigate(roleHome(role), { replace: true })
  }, [navigate, searchParams, setAuth])

  return (
    <div className="grid min-h-[60vh] place-items-center px-4 text-center" role="status" aria-live="polite">
      <div>
        <ShieldAlert aria-hidden className="mx-auto size-10 text-brand-600" />
        <p className="mt-4 flex items-center justify-center gap-2 text-sm font-medium text-ink-700">
          <LoaderCircle aria-hidden className="size-4 animate-spin" />
          Đang hoàn tất đăng nhập Google…
        </p>
      </div>
    </div>
  )
}
