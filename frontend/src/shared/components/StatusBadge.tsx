import { cn } from '@/shared/lib/cn'

export type StatusKind = 'credit' | 'emission' | 'order' | 'application' | 'withdrawal' | 'project'

type Badge = { label: string; className: string }

const success: Badge = {
  label: 'Hoàn tất',
  className: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
}
const info: Badge = {
  label: 'Đang xử lý',
  className: 'bg-sky-50 text-sky-700 ring-sky-600/20',
}
const warning: Badge = {
  label: 'Chờ xử lý',
  className: 'bg-amber-50 text-amber-700 ring-amber-600/20',
}
const danger: Badge = {
  label: 'Từ chối',
  className: 'bg-rose-50 text-rose-700 ring-rose-600/20',
}
const neutral: Badge = {
  label: 'Không xác định',
  className: 'bg-ink-100 text-ink-600 ring-ink-500/20',
}

const CREDIT: Record<string, Badge> = {
  AVAILABLE: { ...success, label: 'Khả dụng' },
  ISSUED: { ...success, label: 'Đã cấp' },
  ISSUE: { ...warning, label: 'Chờ cấp' },
  LISTED: { ...info, label: 'Đang niêm yết' },
  TRADED: { ...info, label: 'Đã giao dịch' },
  SOLD: { ...success, label: 'Đã bán' },
  RETIRED: { ...neutral, label: 'Đã thu hồi' },
  PENDING: { ...warning, label: 'Chờ xử lý' },
  EXPIRED: { ...danger, label: 'Hết hạn' },
}

const EMISSION: Record<string, Badge> = {
  DRAFT: { ...neutral, label: 'Nháp' },
  SUBMITTED: { ...info, label: 'Đã nộp' },
  UNDER_REVIEW: { ...info, label: 'Đang thẩm định' },
  CVA_APPROVED: { ...success, label: 'CVA duyệt' },
  CVA_REJECTED: { ...danger, label: 'CVA từ chối' },
  ADMIN_APPROVED: { ...success, label: 'Admin duyệt' },
  ADMIN_REJECTED: { ...danger, label: 'Admin từ chối' },
  APPROVED: { ...success, label: 'Đã duyệt' },
  REJECTED: { ...danger, label: 'Từ chối' },
  CREDIT_ISSUED: { ...success, label: 'Đã cấp tín chỉ' },
  PAID_OUT: { ...success, label: 'Đã chi trả' },
  NEEDS_REVISION: { ...warning, label: 'Cần chỉnh sửa' },
  PREVIEW: { ...info, label: 'Dự kiến' },
}

const ORDER: Record<string, Badge> = {
  PENDING: { ...warning, label: 'Chờ thanh toán' },
  FINISHED: { ...success, label: 'Hoàn tất' },
  SUCCESS: { ...success, label: 'Thành công' },
  PARTIALLY_FILLED: { ...info, label: 'Khớp một phần' },
  CANCELLED: { ...neutral, label: 'Đã huỷ' },
  ERROR: { ...danger, label: 'Lỗi' },
}

const APPLICATION: Record<string, Badge> = {
  SUBMITTED: { ...info, label: 'Đã nộp' },
  UNDER_REVIEW: { ...info, label: 'Đang thẩm định' },
  CVA_APPROVED: { ...success, label: 'CVA duyệt' },
  CVA_REJECTED: { ...danger, label: 'CVA từ chối' },
  NEEDS_REVISION: { ...warning, label: 'Cần chỉnh sửa' },
  ADMIN_APPROVED: { ...success, label: 'Admin duyệt' },
  ADMIN_REJECTED: { ...danger, label: 'Admin từ chối' },
}

const WITHDRAWAL: Record<string, Badge> = {
  PENDING: { ...warning, label: 'Chờ duyệt' },
  APPROVED: { ...info, label: 'Đã duyệt' },
  COMPLETED: { ...success, label: 'Đã chi trả' },
  SUCCEEDED: { ...success, label: 'Đã chi trả' },
  REJECTED: { ...danger, label: 'Từ chối' },
  FAILED: { ...danger, label: 'Thất bại' },
}

const PROJECT: Record<string, Badge> = {
  OPEN: { ...success, label: 'Đang mở' },
  ACTIVE: { ...success, label: 'Đang hoạt động' },
  CLOSE: { ...neutral, label: 'Đã đóng' },
  COMING_SOON: { ...warning, label: 'Sắp mở' },
}

const MAPS: Record<StatusKind, Record<string, Badge>> = {
  credit: CREDIT,
  emission: EMISSION,
  order: ORDER,
  application: APPLICATION,
  withdrawal: WITHDRAWAL,
  project: PROJECT,
}

export function resolveStatus(status: string, kind?: StatusKind): Badge {
  const key = status.trim().toUpperCase()
  if (kind) return MAPS[kind][key] ?? { ...neutral, label: status || neutral.label }
  for (const map of Object.values(MAPS)) {
    if (map[key]) return map[key]
  }
  return { ...neutral, label: status || neutral.label }
}

interface StatusBadgeProps {
  status: string
  kind?: StatusKind
  label?: string
  className?: string
}

export function StatusBadge({ status, kind, label, className }: StatusBadgeProps) {
  const badge = resolveStatus(status, kind)
  return (
    <span
      className={cn(
        'inline-flex shrink-0 items-center whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset',
        badge.className,
        className,
      )}
    >
      {label ?? badge.label}
    </span>
  )
}
