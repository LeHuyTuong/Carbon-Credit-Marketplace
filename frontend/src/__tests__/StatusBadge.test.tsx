import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatusBadge, type StatusKind } from '@/shared/components/StatusBadge'

interface StatusCase {
  status: string
  kind: StatusKind
  label: string
  tone: string
}

const cases: StatusCase[] = [
  { status: 'AVAILABLE', kind: 'credit', label: 'Khả dụng', tone: 'bg-emerald-50' },
  { status: 'RETIRED', kind: 'credit', label: 'Đã thu hồi', tone: 'bg-ink-100' },
  { status: 'NEEDS_REVISION', kind: 'application', label: 'Cần chỉnh sửa', tone: 'bg-amber-50' },
  { status: 'FAILED', kind: 'withdrawal', label: 'Thất bại', tone: 'bg-rose-50' },
  { status: 'PAID_OUT', kind: 'emission', label: 'Đã chi trả', tone: 'bg-emerald-50' },
  { status: 'ADMIN_APPROVED', kind: 'emission', label: 'Admin duyệt', tone: 'bg-emerald-50' },
]

describe('StatusBadge', () => {
  it.each(cases)('render $status ($kind) thành "$label"', ({ status, kind, label, tone }) => {
    render(<StatusBadge status={status} kind={kind} />)

    const badge = screen.getByText(label)
    expect(badge).toBeInTheDocument()
    expect(badge.className).toContain(tone)
  })

  it('không phân biệt hoa/thường khi map status', () => {
    render(<StatusBadge status="paid_out" kind="emission" />)
    expect(screen.getByText('Đã chi trả')).toBeInTheDocument()
  })

  it('ưu tiên label truyền vào và có fallback cho status lạ', () => {
    const { rerender } = render(<StatusBadge status="AVAILABLE" kind="credit" label="Sẵn sàng bán" />)
    expect(screen.getByText('Sẵn sàng bán')).toBeInTheDocument()

    rerender(<StatusBadge status="STATUS_MỚI" kind="credit" />)
    expect(screen.getByText('STATUS_MỚI')).toBeInTheDocument()
  })
})
