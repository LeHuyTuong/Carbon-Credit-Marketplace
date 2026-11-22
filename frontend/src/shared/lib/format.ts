export function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number') return Number.isFinite(value) ? value : fallback
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/[\s,]/g, ''))
    return Number.isFinite(parsed) ? parsed : fallback
  }
  return fallback
}

const decimal = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 })
const currencyUsd = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 2,
})
const currencyVnd = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

export function formatNumber(value: unknown): string {
  return decimal.format(toNumber(value))
}

export function formatUsd(value: unknown): string {
  return currencyUsd.format(toNumber(value))
}

export function formatVnd(value: unknown): string {
  return currencyVnd.format(toNumber(value))
}

export function formatCompact(value: unknown): string {
  return new Intl.NumberFormat('vi-VN', { notation: 'compact', maximumFractionDigits: 1 }).format(
    toNumber(value),
  )
}

export function formatDate(value: unknown): string {
  if (!value || typeof value !== 'string') return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: 'short', year: 'numeric' }).format(date)
}

export function createTraceId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID()
  return `trace-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}
