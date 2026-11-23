import { beforeEach, describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { RoleGuard } from '@/shared/guards/RoleGuard'
import { normalizeRole } from '@/shared/lib/roles'
import { useAuthStore, type Role } from '@/shared/stores/auth.store'

function renderGuard(allow: readonly Role[]) {
  return render(
    <MemoryRouter initialEntries={['/protected']}>
      <Routes>
        <Route
          path="/protected"
          element={
            <RoleGuard allow={allow}>
              <div>Trang bảo mật</div>
            </RoleGuard>
          }
        />
        <Route path="/login" element={<div>Trang đăng nhập</div>} />
        <Route path="/app/ev" element={<div>Trang EV</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  useAuthStore.setState({ token: null, user: null, role: null })
})

describe('RoleGuard', () => {
  it('cho phép role nằm trong allow', () => {
    useAuthStore.setState({ token: 'company-token', role: 'COMPANY' })
    renderGuard(['COMPANY'])

    expect(screen.getByText('Trang bảo mật')).toBeInTheDocument()
    expect(screen.queryByText('Không có quyền truy cập')).not.toBeInTheDocument()
  })

  it('cho phép role COMPANY sau khi normalize từ CC_BUYER', () => {
    useAuthStore.setState({ token: 'company-token', role: normalizeRole('CC_BUYER') })
    renderGuard(['COMPANY'])

    expect(screen.getByText('Trang bảo mật')).toBeInTheDocument()
  })

  it('chặn role không hợp lệ và dẫn về trang tổng quan của role đó', () => {
    useAuthStore.setState({ token: 'ev-token', role: 'EV_OWNER' })
    renderGuard(['COMPANY', 'ADMIN'])

    expect(screen.queryByText('Trang bảo mật')).not.toBeInTheDocument()
    expect(screen.getByText('Không có quyền truy cập')).toBeInTheDocument()
    expect(screen.getByText('Vai trò của bạn không được phép mở trang này.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Về trang tổng quan' })).toHaveAttribute('href', '/app/ev')
  })

  it('chặn khi có token nhưng role null', () => {
    useAuthStore.setState({ token: 'token-không-có-role', role: null })
    renderGuard(['CVA'])

    expect(screen.queryByText('Trang bảo mật')).not.toBeInTheDocument()
    expect(screen.getByText('Không có quyền truy cập')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Về trang tổng quan' })).not.toBeInTheDocument()
  })

  it('chặn vai CVA truy cập vào route ví tiền của hệ thống', () => {
    useAuthStore.setState({ token: 'cva-token', role: 'CVA' })
    renderGuard(['ADMIN', 'COMPANY', 'EV_OWNER'])

    expect(screen.queryByText('Trang bảo mật')).not.toBeInTheDocument()
    expect(screen.getByText('Không có quyền truy cập')).toBeInTheDocument()
  })

  it('điều hướng về /login khi chưa đăng nhập', () => {
    renderGuard(['COMPANY'])

    expect(screen.getByText('Trang đăng nhập')).toBeInTheDocument()
    expect(screen.queryByText('Trang bảo mật')).not.toBeInTheDocument()
  })
})
