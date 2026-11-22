import { beforeEach, describe, expect, it } from 'vitest'
import { normalizeRole, toBackendRole } from '@/shared/lib/roles'
import { useAuthStore, type AuthUser } from '@/shared/stores/auth.store'

const AUTH_KEY = 'carbonx-auth'

type PersistedAuth = {
  state: { token: string | null; user: AuthUser | null; role: string | null }
}

function persisted(): PersistedAuth | null {
  const raw = localStorage.getItem(AUTH_KEY)
  return raw ? (JSON.parse(raw) as PersistedAuth) : null
}

const buyer: AuthUser = { id: 1, email: 'buyer@carbonx.vn', fullName: 'Nguyễn Văn B', role: 'COMPANY' }

beforeEach(() => {
  localStorage.clear()
  useAuthStore.setState({ token: null, user: null, role: null })
})

describe('auth store — setAuth / logout', () => {
  it('setAuth lưu token, user, role và persist đúng field', () => {
    useAuthStore.getState().setAuth({ token: 'company-token', user: buyer, role: 'COMPANY' })

    expect(useAuthStore.getState()).toMatchObject({
      token: 'company-token',
      user: buyer,
      role: 'COMPANY',
    })
    expect(persisted()?.state).toMatchObject({ token: 'company-token', role: 'COMPANY' })
  })

  it('setAuth suy ra role từ user khi không truyền role', () => {
    useAuthStore.getState().setAuth({ token: 'ev-token', user: { ...buyer, role: 'EV_OWNER' } })
    expect(useAuthStore.getState().role).toBe('EV_OWNER')
  })

  it('logout xoá sạch token/user/role cả trong persist', () => {
    useAuthStore.getState().setAuth({ token: 'company-token', user: buyer, role: 'COMPANY' })
    useAuthStore.getState().logout()

    expect(useAuthStore.getState()).toMatchObject({ token: null, user: null, role: null })
    expect(persisted()?.state).toMatchObject({ token: null, user: null, role: null })
  })
})

describe('normalizeRole', () => {
  it('đổi alias CC_BUYER của backend thành COMPANY', () => {
    expect(normalizeRole('CC_BUYER')).toBe('COMPANY')
    expect(normalizeRole('BUYER')).toBe('COMPANY')
    expect(toBackendRole('COMPANY')).toBe('CC_BUYER')
  })

  it('đổi alias EV và chấp nhận role viết thường / có khoảng trắng', () => {
    expect(normalizeRole('EV')).toBe('EV_OWNER')
    expect(normalizeRole(' admin ')).toBe('ADMIN')
    expect(normalizeRole('cva')).toBe('CVA')
  })

  it('trả null cho role không hợp lệ hoặc không phải chuỗi', () => {
    expect(normalizeRole('HACKER')).toBeNull()
    expect(normalizeRole(undefined)).toBeNull()
    expect(normalizeRole(null)).toBeNull()
  })
})

describe('TC-GAP-014 — token không bị ghi đè giữa các role', () => {
  it('đổi role bằng setUser giữ nguyên token; chỉ setAuth mới thay token', () => {
    const { setAuth, setUser, logout } = useAuthStore.getState()

    setAuth({ token: 'company-token', user: buyer, role: normalizeRole('CC_BUYER') })
    expect(useAuthStore.getState().token).toBe('company-token')

    // /auth/me trả role mới cho cùng phiên đăng nhập → không được đụng vào token
    setUser({ ...buyer, role: 'CVA' })
    expect(useAuthStore.getState().token).toBe('company-token')
    expect(useAuthStore.getState().role).toBe('CVA')

    // đăng nhập lại với token khác → thay token, không lẫn token của role trước
    setAuth({ token: 'admin-token', user: { ...buyer, role: 'ADMIN' }, role: 'ADMIN' })
    expect(useAuthStore.getState().token).toBe('admin-token')
    expect(useAuthStore.getState().role).toBe('ADMIN')
    expect(persisted()?.state.token).toBe('admin-token')

    logout()
    expect(useAuthStore.getState().token).toBeNull()
    expect(persisted()?.state.token).toBeNull()
  })
})
