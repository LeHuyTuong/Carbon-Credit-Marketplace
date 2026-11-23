import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { CvaApplicationsPage } from '@/pages/cva/CvaApplicationsPage'
import { api } from '@/shared/api/client'

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <CvaApplicationsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('CvaApplicationsPage — CVA Thẩm định hồ sơ', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('phân tách chính xác hồ sơ chờ duyệt (UNDER_REVIEW) và lịch sử đã duyệt', async () => {
    vi.spyOn(api, 'get').mockResolvedValue([
      {
        id: 4,
        projectId: 4,
        projectTitle: 'Trạm sạc Năng lượng Mặt trời Cần Thơ',
        companyName: 'VinFast Green Mobility JSC',
        status: 'UNDER_REVIEW',
        submittedAt: '2026-11-20T10:00:00Z',
      },
      {
        id: 3,
        projectId: 3,
        projectTitle: 'Hành lang Taxi Điện Xanh Miền Trung',
        companyName: 'VinFast Green Mobility JSC',
        status: 'CVA_APPROVED',
        submittedAt: '2026-11-18T10:00:00Z',
      },
    ])

    renderPage()

    // 1 hồ sơ chờ duyệt
    expect(await screen.findByText('1 hồ sơ')).toBeInTheDocument()
    expect(screen.getByText('Trạm sạc Năng lượng Mặt trời Cần Thơ')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Duyệt \/ bổ sung \/ từ chối/ })).toBeInTheDocument()

    // 1 hồ sơ trong bảng lịch sử
    expect(screen.getByText('Lịch sử thẩm định hồ sơ')).toBeInTheDocument()
    expect(screen.getByText('1 hồ sơ đã qua thẩm định')).toBeInTheDocument()
    expect(screen.getByText('Hành lang Taxi Điện Xanh Miền Trung')).toBeInTheDocument()
  })

  it('hiển thị empty state khi không có hồ sơ nào đang chờ duyệt', async () => {
    vi.spyOn(api, 'get').mockResolvedValue([
      {
        id: 3,
        projectId: 3,
        projectTitle: 'Hành lang Taxi Điện Xanh Miền Trung',
        companyName: 'VinFast Green Mobility JSC',
        status: 'CVA_APPROVED',
        submittedAt: '2026-11-18T10:00:00Z',
      },
    ])

    renderPage()

    expect(await screen.findByText('Không có hồ sơ chờ duyệt')).toBeInTheDocument()
    expect(screen.getByText('0 hồ sơ')).toBeInTheDocument()
    expect(screen.getByText('Lịch sử thẩm định hồ sơ')).toBeInTheDocument()
  })
})
