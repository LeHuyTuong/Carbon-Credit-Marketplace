import { useQuery } from '@tanstack/react-query'
import { api } from '@/shared/api/client'
import { mapActivity } from '@/shared/api/mappers'
import type { DashboardMetric, DashboardSnapshot, TrendPoint } from '@/shared/api/types'
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
      const rawDate = firstText(item, ['createdAt', 'submittedAt', 'requestDate', 'date', 'issueDate'])
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

async function load(role: Role): Promise<DashboardSnapshot> {
  // Endpoint CVA nằm ngoài prefix /api/v1 nên cần override baseURL cho request này.
  const cvaConfig = { baseURL: '' }

  if (role === 'EV_OWNER') {
    const [summary, balance, reports, projects, listings] = await Promise.all([
      safeGet('/my/credits/summary'),
      safeGet('/my/credits/balance'),
      safeGet('/reports/my-reports'),
      safeGet('/projects'),
      safeGet('/marketplace'),
    ])
    const reportItems = itemsFrom(reports)
    return {
      metrics: [
        metric('credit-balance', 'Số dư tín chỉ', numberFrom(summary, ['balance', 'creditBalance', 'totalBalance']) ?? numberFrom(balance, ['balance', 'creditBalance']), 'Tín chỉ khả dụng', 'tín chỉ'),
        metric('reports', 'Báo cáo phát thải', countOf(reports), 'Báo cáo đã nộp'),
        metric('projects', 'Dự án tham gia', countOf(projects), 'Dự án trên sàn'),
        metric('price', 'Giá tín chỉ TB', averagePrice(itemsFrom(listings)), 'Tham khảo marketplace', 'USD'),
      ],
      trend: buildTrend([reports, listings]),
      recent: reportItems.slice(0, 5).map((item, index) => mapActivity(item, index)),
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
        metric('projects', 'Dự án trên sàn', countOf(projects), 'Cơ hội đầu tư'),
        metric('formula', 'Đơn giá thanh toán', numberFrom(formula, ['pricePerCredit', 'unitPrice', 'rate']), 'Theo chính sách công ty', 'USD'),
      ],
      trend: buildTrend([listings]),
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
    return {
      metrics: [
        metric('pending-reports', 'Báo cáo chờ thẩm định', countOf(reports) ?? numberFrom(cards, ['pendingReports', 'reportsPending']), 'Cần CVA xử lý'),
        metric('pending-applications', 'Đơn chờ duyệt', countOf(applications), 'Đơn đăng ký dự án'),
        metric('projects', 'Dự án cần thẩm định', numberFrom(cards, ['pendingProjects']) ?? countOf(projects), 'Danh mục dự án'),
        metric('issued-credits', 'Tín chỉ đã cấp', numberFrom(cards, ['issuedCredits', 'creditsIssued']), 'Lũy kế hệ thống'),
      ],
      trend: buildTrend([reports, applications]),
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
  return {
    metrics: [
      metric('users', 'Người dùng', countOf(users), 'Tổng tài khoản'),
      metric('withdrawals', 'Rút tiền chờ xử lý', countOf(withdrawals), 'Yêu cầu ADMIN duyệt'),
      metric('projects', 'Dự án', countOf(projects), 'Tổng dự án'),
      metric('listings', 'Tin đăng', countOf(listings), 'Tổng tin trên sàn'),
    ],
    trend: buildTrend([withdrawals, listings]),
    recent: withdrawalItems.slice(0, 5).map((item, index) => mapActivity(item, index)),
  }
}

export function useDashboardSnapshot(role: Role) {
  return useQuery({
    queryKey: queryKeys.dashboard[DASHBOARD_KEY[role]],
    queryFn: () => load(role),
  })
}
