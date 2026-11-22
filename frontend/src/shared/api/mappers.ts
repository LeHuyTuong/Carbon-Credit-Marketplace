import { toNumber } from '@/shared/lib/format'
import type {
  ActivityItem,
  MarketplaceListing,
  NotificationEvent,
  PaymentDetails,
  ProjectSummary,
  TrendPoint,
  WalletSummary,
  WalletTransaction,
  Withdrawal,
} from './types'

type Raw = Record<string, unknown>

function isRecord(value: unknown): value is Raw {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function str(value: unknown, fallback = ''): string {
  return typeof value === 'string' && value.trim() ? value : fallback
}

function first(...values: unknown[]): unknown {
  return values.find((value) => value !== undefined && value !== null && value !== '')
}

export function mapList<T>(raw: unknown, mapper: (item: Raw) => T | null): T[] {
  if (!Array.isArray(raw)) return []
  return raw.map((item) => (isRecord(item) ? mapper(item) : null)).filter((item): item is T => item !== null)
}

export function mapListing(raw: Raw): MarketplaceListing | null {
  const id = toNumber(first(raw.listingId, raw.id), 0)
  if (!id) return null
  return {
    listingId: id,
    quantity: toNumber(raw.quantity),
    availableQuantity: toNumber(first(raw.availableQuantity, raw.quantity)),
    originalQuantity: toNumber(first(raw.originalQuantity, raw.quantity)),
    soldQuantity: toNumber(raw.soldQuantity),
    pricePerCredit: toNumber(raw.pricePerCredit),
    sellerCompanyName: str(first(raw.sellerCompanyName, raw.companyName), 'Doanh nghiệp chưa cập nhật'),
    projectId: raw.projectId === null || raw.projectId === undefined ? null : toNumber(raw.projectId, 0) || null,
    projectTitle: str(first(raw.projectTitle, raw.projectName), 'Dự án chưa cập nhật'),
    expiresAt: str(raw.expiresAt) || null,
    batchCode: str(raw.batchCode),
    logo: str(raw.logo) || null,
  }
}

export function mapProject(raw: Raw): ProjectSummary | null {
  const id = toNumber(first(raw.id, raw.projectId), 0)
  const title = str(first(raw.title, raw.projectTitle))
  if (!id && !title) return null
  return {
    id,
    title: title || 'Dự án CarbonX',
    description: str(raw.description),
    status: str(raw.status),
    location: str(first(raw.location, raw.province, raw.address)),
    imageUrl: str(first(raw.imageUrl, raw.image, raw.logo)) || null,
  }
}

export function mapWallet(raw: unknown): WalletSummary {
  if (!isRecord(raw)) return { id: null, balance: 0, carbonCreditBalance: 0 }
  return {
    id: raw.id === null || raw.id === undefined ? null : toNumber(raw.id, 0) || null,
    balance: toNumber(first(raw.balance, raw.amount)),
    carbonCreditBalance: toNumber(raw.carbonCreditBalance),
  }
}

export function mapTransaction(raw: Raw): WalletTransaction | null {
  const id = toNumber(first(raw.id, raw.transactionId), 0)
  if (!id) return null
  return {
    id,
    type: str(first(raw.type, raw.transactionType)),
    amount: toNumber(raw.amount),
    description: str(raw.description),
    createdAt: str(first(raw.createdAt, raw.transactionDate)) || null,
    status: str(raw.status),
  }
}

export function mapPaymentDetails(raw: unknown): PaymentDetails {
  if (!isRecord(raw)) return { id: null, bankName: '', accountNumber: '', accountHolder: '', branch: '' }
  return {
    id: raw.id === null || raw.id === undefined ? null : toNumber(raw.id, 0) || null,
    bankName: str(first(raw.bankName, raw.bank)),
    accountNumber: str(first(raw.accountNumber, raw.accountNo)),
    accountHolder: str(first(raw.accountHolder, raw.accountHolderName, raw.holderName)),
    branch: str(raw.branch),
  }
}

export function mapWithdrawal(raw: Raw): Withdrawal | null {
  const id = toNumber(first(raw.id, raw.withdrawalId), 0)
  if (!id) return null
  return {
    id,
    amount: toNumber(raw.amount),
    status: str(raw.status, 'PENDING'),
    createdAt: str(first(raw.createdAt, raw.requestDate)) || null,
    note: str(first(raw.note, raw.description)),
  }
}

export function mapNotification(raw: Raw): NotificationEvent {
  return {
    id: str(first(raw.id, raw.notificationId), `${Date.now()}-${Math.random()}`),
    title: str(first(raw.title, raw.subject), 'Thông báo mới'),
    message: str(first(raw.message, raw.content, raw.body)),
    createdAt: str(first(raw.createdAt, raw.timestamp)) || null,
    read: raw.read === true,
  }
}

export function mapTrendPoint(raw: Raw, index: number): TrendPoint {
  return {
    label: str(first(raw.label, raw.month, raw.date, raw.name), `Kỳ ${index + 1}`),
    value: toNumber(first(raw.value, raw.total, raw.amount, raw.count)),
  }
}

export function mapActivity(raw: Raw, index: number): ActivityItem {
  return {
    id: str(first(raw.id, raw.reportId, raw.applicationId), `item-${index}`),
    title: str(first(raw.title, raw.projectTitle, raw.companyName, raw.name), 'Hồ sơ mới'),
    subtitle: str(first(raw.subtitle, raw.description, raw.email)),
    status: str(raw.status, 'PENDING'),
    date: str(first(raw.createdAt, raw.submittedAt, raw.date)) || null,
  }
}
