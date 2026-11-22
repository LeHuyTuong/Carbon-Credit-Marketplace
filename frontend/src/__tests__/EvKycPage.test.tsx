import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { EvKycPage } from '@/pages/ev/EvKycPage'
import { api } from '@/shared/api/client'

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <EvKycPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  // Chưa có hồ sơ KYC → form rỗng, không gọi máy chủ thật.
  vi.spyOn(api, 'get').mockResolvedValue(null)
  vi.spyOn(api, 'post').mockResolvedValue({})
  vi.spyOn(api, 'put').mockResolvedValue({})
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('EvKycPage — UX-1 / TC-UX-026', () => {
  it('giữ nguyên các ô hợp lệ và hiện lỗi inline khi số giấy tờ sai', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('Họ và tên'), 'Nguyễn Văn A')
    await user.type(screen.getByLabelText('Số điện thoại'), '0912345678')
    await user.type(screen.getByLabelText('Địa chỉ thường trú'), 'Số 12, đường Lê Lợi, Quận 1')
    fireEvent.change(screen.getByLabelText('Ngày sinh'), { target: { value: '1990-05-15' } })
    await user.type(screen.getByLabelText('Số giấy tờ'), 'abc')

    await user.click(screen.getByRole('button', { name: /Nộp hồ sơ KYC/ }))

    expect(await screen.findByText('Số giấy tờ phải có ít nhất 5 ký tự')).toBeInTheDocument()
    expect(screen.getByLabelText('Số giấy tờ')).toHaveAttribute('aria-invalid', 'true')
    expect(api.post).not.toHaveBeenCalled()
    expect(api.put).not.toHaveBeenCalled()

    // Các ô đã nhập đúng không bị reset khi có một ô sai.
    expect(screen.getByLabelText('Họ và tên')).toHaveValue('Nguyễn Văn A')
    expect(screen.getByLabelText('Số điện thoại')).toHaveValue('0912345678')
    expect(screen.getByLabelText('Địa chỉ thường trú')).toHaveValue('Số 12, đường Lê Lợi, Quận 1')
    expect(screen.getByLabelText('Ngày sinh')).toHaveValue('1990-05-15')
    expect(screen.getByLabelText('Quốc gia')).toHaveValue('Việt Nam')
    expect(screen.getByLabelText('Số giấy tờ')).toHaveValue('abc')
  })
})
