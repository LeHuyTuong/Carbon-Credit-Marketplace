import { useQuery } from '@tanstack/react-query'
import { api } from '@/shared/api/client'
import { mapActivity } from '@/shared/api/mappers'
import type { BreakdownPoint, DashboardMetric, DashboardSnapshot, TrendPoint } from '@/shared/api/types'
import { toNumber } from '@/shared/lib/format'
import { queryKeys } from '@/shared/lib/queryKeys'
import type { Role } from '@/shared/stores/auth.store'

type Raw = Record<string, unknown>

const DASHBOARD_KEY = {
  EV_OWNER: 'ev',
  COMPANY: 'company',
  CVA: 'cva',
  ADMIN: 'admin',
} as const

function isRaw(value: unknown): value is Raw {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function numberFrom(raw: unknown, keys: readonly string[], depth = 2): number | null {
  if (!isRaw(raw)) return null
  for (const key of keys) {
    const value = raw[key]
    if (value !== undefined && value !== null) {
      const parsed = toNumber(value, Number.NaN)
      if (Number.isFinite(parsed)) return parsed
    }
  }
  if (depth > 0) {
    for (const value of Object.values(raw)) {
      if (isRaw(value)) {
        const found = numberFrom(value, keys, depth - 1)
        if (found !== null) return found
      }
    }
  }
  return null
}

function itemsFrom(raw: unknown): Raw[] {
  if (Array.isArray(raw)) return raw.filter(isRaw)
  if (!isRaw(raw)) return []
  const preferred = ['items', 'rows', 'content', 'reports', 'list', 'applications', 'data']
  for (const key of preferred) {
    const nested = itemsFrom(raw[key])
    if (nested.length) return nested
  }
  return []
}

function countOf(raw: unknown): number | null {
  if (Array.isArray(raw)) return raw.length
  if (!isRaw(raw)) return null
  return numberFrom(raw, ['totalElements', 'totalCount', 'count', 'total'])
}

function averagePrice(listings: Raw[]): number | null {
  const prices = listings
    .map((item) => numberFrom(item, ['pricePerCredit'], 0))
    .filter((value): value is number => value !== null && value > 0)
  if (!prices.length) return null
  return Math.round((prices.reduce((sum, value) => sum + value, 0) / prices.length) * 100) / 100
}

function firstText(raw: Raw, keys: readonly string[]): string | null {
  for (const key of keys) {
    const value = raw[key]
    if (typeof value === 'string' && value) return value
  }
  return null
}

function buildTrend(sources: unknown[]): TrendPoint[] {
  const grouped = new Map<string, number>()
  for (const source of sources) {
    for (const item of itemsFrom(source)) {
      const rawDate = firstText(item, ['createdAt', 'submittedAt', 'requestedAt', 'requestDate', 'date', 'issueDate', 'issuedAt'])
      if (!rawDate) continue
      const date = new Date(rawDate)
      if (Number.isNaN(date.getTime())) continue
      const label = `${date.getMonth() + 1}/${String(date.getFullYear()).slice(-2)}`
      grouped.set(label, (grouped.get(label) ?? 0) + 1)
    }
  }
  return [...grouped.entries()]
    .map(([label, value]) => ({ label, value }))
    .slice(-8)
}

function groupByKey(items: Raw[], keys: readonly string[], labelMap?: Record<string, string>): BreakdownPoint[] {
  const counts = new Map<string, number>()
  for (const item of items) {
    const rawVal = firstText(item, keys) ?? 'Khác'
    const label = labelMap?.[rawVal.toUpperCase()] ?? rawVal
    counts.set(label, (counts.get(label) ?? 0) + 1)
  }
  return [...counts.entries()]
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 5)
}

function metric(
  key: string,
  label: string,
  value: number | null,
  hint: string,
  unit?: string,
): DashboardMetric {
  return { key, label, value, hint, unit }
}

async function safeGet(url: string, config?: { baseURL?: string }): Promise<unknown> {
  try {
    return await api.get<unknown>(url, undefined, config)
  } catch {
    return null
  }
}

const STATUS_LABELS: Record<string, string> = {
  AVAILABLE: 'Đang mở bán',
  SOLD: 'Đã bán hết',
  CANCELLED: 'Đã huỷ',
  EXPIRED: 'Hết hạn',
  SUBMITTED: 'Chờ thẩm định',
  UNDER_REVIEW: 'Đang thẩm định',
  CVA_APPROVED: 'CVA đã duyệt',
  ADMIN_APPROVED: 'Admin đã duyệt',
  CREDIT_ISSUED: 'Đã cấp tín chỉ',
  PAID_OUT: 'Đã chi trả',
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  SUCCEEDED: 'Đã chi trả',
  REJECTED: 'Từ chối',
}

async function load(role: Role): Promise<DashboardSnapshot> {
  const cvaConfig = { baseURL: '' }

  if (role === 'EV_OWNER') {
    const [vehicles, wallet, projects, listings] = await Promise.all([
      safeGet('/vehicles'),
      safeGet('/wallet'),
      safeGet('/projects'),
      safeGet('/marketplace'),
    ])
    const vehicleItems = itemsFrom(vehicles)
    const listingItems = itemsFrom(listings)
    return {
      metrics: [
        metric('vehicles', 'Xe điện đã đăng ký', vehicleItems.length, 'Phương tiện của tôi', 'xe'),
        metric('wallet', 'Số dư ví', numberFrom(wallet, ['balance']), 'Tiền nhận chia sẻ doanh thu', 'USD'),
        metric('projects', 'Dự án trên sàn', countOf(projects), 'Dự án đang mở'),
        metric('price', 'Giá tín chỉ TB', averagePrice(listingItems), 'Tham khảo marketplace', 'USD'),
      ],
      trend: buildTrend([vehicles, listings]),
      breakdown: groupByKey(vehicleItems, ['brand', 'model']),
      breakdownTitle: 'Phương tiện theo hãng xe',
      breakdownDescription: 'Phân bổ xe điện đã đăng ký của bạn',
      recent: vehicleItems.slice(0, 5).map((item, index) => mapActivity(item, index)),
    }
  }

  if (role === 'COMPANY') {
    const [listings, wallet, projects, formula] = await Promise.all([
      safeGet('/marketplace'),
      safeGet('/wallet'),
      safeGet('/projects'),
      safeGet('/companies/payout-formula'),
    ])
    const listingItems = itemsFrom(listings)
    return {
      metrics: [
        metric('listings', 'Tin đăng hoạt động', countOf(listings), 'Tin đang mở bán'),
        metric('wallet', 'Số dư ví', numberFrom(wallet, ['balance']), 'Khả dụng để mua tín chỉ', 'USD'),
        metric('projects', 'Dự án trên sàn', countOf(projects), 'Cơ hội đăng ký'),
        metric('formula', 'Đơn giá chia chủ xe', numberFrom(formula, ['unitPricePerCredit', 'pricePerCredit', 'unitPrice']), 'Theo chính sách công ty', 'USD'),
      ],
      trend: buildTrend([listings]),
      breakdown: groupByKey(listingItems, ['status'], STATUS_LABELS),
      breakdownTitle: 'Tin đăng theo trạng thái',
      breakdownDescription: 'Tình trạng các lô tín chỉ trên sàn',
      recent: listingItems.slice(0, 5).map((item, index) => mapActivity(item, index)),
    }
  }

  if (role === 'CVA') {
    const [cards, reports, applications, projects] = await Promise.all([
      safeGet('/api/cva/dashboard/cards', cvaConfig),
      safeGet('/reports/list-cva-check'),
      safeGet('/project-applications/pending-cva'),
      safeGet('/projects'),
    ])
    const reportItems = itemsFrom(reports)
    const appItems = itemsFrom(applications)
    return {
      metrics: [
        metric('pending-reports', 'Báo cáo chờ thẩm định', countOf(reports) ?? numberFrom(cards, ['pendingReports', 'reportsPending']), 'Cần CVA xử lý'),
        metric('pending-applications', 'Đơn chờ duyệt', countOf(applications), 'Đơn đăng ký dự án'),
        metric('projects', 'Dự án đang theo dõi', numberFrom(cards, ['pendingProjects']) ?? countOf(projects), 'Danh mục dự án'),
        metric('issued-credits', 'Tín chỉ đã cấp', numberFrom(cards, ['issuedCredits', 'creditsIssued']), 'Lũy kế hệ thống', 'tín chỉ'),
      ],
      trend: buildTrend([reports, applications]),
      breakdown: groupByKey([...reportItems, ...appItems], ['status'], STATUS_LABELS),
      breakdownTitle: 'Hồ sơ & Báo cáo theo trạng thái',
      breakdownDescription: 'Phân loại hàng đợi thẩm định độc lập',
      recent: reportItems.slice(0, 5).map((item, index) => mapActivity(item, index)),
    }
  }

  const [users, withdrawals, projects, listings] = await Promise.all([
    safeGet('/users'),
    safeGet('/withdrawal/admin'),
    safeGet('/projects'),
    safeGet('/marketplace'),
  ])
  const withdrawalItems = itemsFrom(withdrawals)
  const listingItems = itemsFrom(listings)
  return {
    metrics: [
      metric('users', 'Người dùng', countOf(users), 'Tổng tài khoản'),
      metric('withdrawals', 'Rút tiền chờ xử lý', countOf(withdrawals), 'Yêu cầu ADMIN duyệt'),
      metric('projects', 'Dự án', countOf(projects), 'Tổng dự án'),
      metric('listings', 'Tin đăng', countOf(listings), 'Tổng tin trên sàn'),
    ],
    trend: buildTrend([withdrawals, listings]),
    breakdown: groupByKey(withdrawalItems.length ? withdrawalItems : listingItems, ['status'], STATUS_LABELS),
    breakdownTitle: withdrawalItems.length ? 'Lệnh rút tiền theo trạng thái' : 'Tin đăng theo trạng thái',
    breakdownDescription: 'Tổng hợp trạng thái xử lý trên hệ thống',
    recent: withdrawalItems.slice(0, 5).map((item, index) => mapActivity(item, index)),
  }
}

export function useDashboardSnapshot(role: Role) {
  return useQuery({
    queryKey: queryKeys.dashboard[DASHBOARD_KEY[role]],
    queryFn: () => load(role),
  })
}
