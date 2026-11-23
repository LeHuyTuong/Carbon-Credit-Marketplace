import { useQuery } from '@tanstack/react-query'
import { toast } from 'sonner'
import { api, apiClient, API_BASE } from '@/shared/api/client'
import { queryKeys } from '@/shared/lib/queryKeys'

export interface PayoutRow {
  ownerId?: number
  ownerName: string
  email: string
  phone: string
  vehiclesCount: number
  energyKwh: number
  credits: number
  amount: number
  status: string
}

export interface PayoutSummary {
  grandTotalPayout: number
  totalEnergyKwh: number
  totalCredits: number
  ownersCount: number
}

export interface PayoutPreview {
  rows: PayoutRow[]
  totalAmount: number
  reportId: number | null
  summary?: PayoutSummary
}

interface RawRow extends Record<string, unknown> {}

function toNumber(value: unknown): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/[\s,]/g, ''))
    return Number.isFinite(parsed) ? parsed : 0
  }
  return 0
}

function first(...values: unknown[]): unknown {
  return values.find((value) => value !== undefined && value !== null && value !== '')
}

function text(value: unknown, fallback = '—'): string {
  return typeof value === 'string' && value.trim() ? value : fallback
}

function extractRows(payload: unknown): RawRow[] {
  const visit = (value: unknown): RawRow[] => {
    if (Array.isArray(value)) {
      return value.filter((item): item is RawRow => item !== null && typeof item === 'object')
    }
    if (value && typeof value === 'object') {
      const record = value as Record<string, unknown>
      for (const key of ['rows', 'items', 'owners', 'content', 'data', 'evOwners', 'details', 'page', 'summary', 'response']) {
        const nested = visit(record[key])
        if (nested.length) return nested
      }
    }
    return []
  }
  return visit(payload)
}

export function usePayoutPreview(reportId: number | null) {
  return useQuery({
    queryKey: [...queryKeys.payouts, reportId] as const,
    enabled: reportId !== null,
    queryFn: async (): Promise<PayoutPreview> => {
      const payload = await api.get<unknown>(`/companies/reports/${reportId}/owners`, {
        page: 0,
        size: 20,
      })
      const rows = extractRows(payload).map((row) => ({
        ownerId: toNumber(first(row.ownerId, row.id)),
        ownerName: text(first(row.ownerName, row.name, row.fullName)),
        email: text(first(row.email, row.ownerEmail), ''),
        phone: text(first(row.phone, row.ownerPhone, row.phoneNumber), '—'),
        vehiclesCount: toNumber(first(row.vehiclesCount, row.vehicleCount, row.vehicles)) || 1,
        energyKwh: toNumber(first(row.energyKwh, row.totalEnergy, row.energy)),
        credits: toNumber(first(row.credits, row.creditAmount, row.totalCredits, row.quantity)),
        amount: toNumber(first(row.amount, row.amountUsd, row.payoutAmount, row.totalAmount, row.total)),
        status: text(first(row.status, row.paymentStatus), 'PENDING'),
      }))
      const rawSummary = (payload && typeof payload === 'object' && 'response' in payload && payload.response && typeof payload.response === 'object' && 'summary' in payload.response ? payload.response.summary : null) as Record<string, unknown> | null
      const summary: PayoutSummary = {
        grandTotalPayout: rawSummary ? toNumber(rawSummary.grandTotalPayout) : rows.reduce((s, r) => s + r.amount, 0),
        totalEnergyKwh: rawSummary ? toNumber(rawSummary.totalEnergyKwh) : rows.reduce((s, r) => s + r.energyKwh, 0),
        totalCredits: rawSummary ? toNumber(rawSummary.totalCredits) : rows.reduce((s, r) => s + r.credits, 0),
        ownersCount: rawSummary ? toNumber(rawSummary.ownersCount) : rows.length,
      }
      return {
        rows,
        totalAmount: summary.grandTotalPayout || rows.reduce((sum, row) => sum + row.amount, 0),
        reportId,
        summary,
      }
    },
  })
}

export function payoutExportUrl(distributionId: number): string {
  return `${API_BASE}/companies/payouts/${distributionId}/export.xlsx`
}

/** Excel được sinh ở backend; tải qua client có Bearer token rồi push file cho trình duyệt. */
export async function downloadPayoutExcel(distributionId: number): Promise<void> {
  try {
    const response = await apiClient.get<Blob>(payoutExportUrl(distributionId), {
      responseType: 'blob',
      baseURL: '',
    })
    const objectUrl = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')
    anchor.href = objectUrl
    anchor.download = `payout_${distributionId}.xlsx`
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
    URL.revokeObjectURL(objectUrl)
    toast.success('Đã xuất file Excel')
  } catch {
    toast.error('Không xuất được file Excel', {
      description: 'Kiểm tra mã phân phối và quyền truy cập của tài khoản.',
    })
  }
}
