import { toNumber } from '@/shared/lib/format'

type Raw = Record<string, unknown>

function isRecord(value: unknown): value is Raw {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function first(...values: unknown[]): unknown {
  return values.find((value) => value !== undefined && value !== null && value !== '')
}

function text(value: unknown, fallback = ''): string {
  return typeof value === 'string' && value.trim() ? value : fallback
}

function nullableText(value: unknown): string | null {
  return text(value) || null
}

function num(value: unknown, fallback = 0): number {
  return toNumber(value, fallback)
}

/** API trả về mảng hoặc Page.content; api client đã bóc envelope. */
export function asList(payload: unknown): unknown[] {
  if (Array.isArray(payload)) return payload
  if (!isRecord(payload)) return []
  for (const key of ['content', 'rows', 'items', 'list', 'data', 'response', 'responseData']) {
    const nested = payload[key]
    if (Array.isArray(nested)) return nested
  }
  return []
}

export interface PageResult<T> {
  rows: T[]
  total: number
  page: number
  size: number
}

export function asPage<T>(payload: unknown, map: (raw: Raw) => T | null, fallbackSize = 20): PageResult<T> {
  const source = isRecord(payload) && isRecord(payload.content) ? payload.content : isRecord(payload) ? payload : {}
  return {
    rows: asList(source.content ?? source).map((item) => (isRecord(item) ? map(item) : null)).filter((item): item is T => item !== null),
    total: num(first(source.totalElements, source.total, source.totalCount)),
    page: num(first(source.number, source.page, 0)),
    size: num(first(source.size, source.pageSize, fallbackSize)),
  }
}

export interface Vehicle {
  id: number
  plateNumber: string
  brand: string
  model: string
  companyId: number | null
  documentUrl: string | null
}

export function mapVehicle(raw: Raw): Vehicle | null {
  const id = num(raw.id)
  if (!id) return null
  return {
    id,
    plateNumber: text(raw.plateNumber),
    brand: text(raw.brand),
    model: text(raw.model),
    companyId: raw.companyId === null || raw.companyId === undefined ? null : num(raw.companyId) || null,
    documentUrl: nullableText(raw.documentUrl),
  }
}

export interface ProjectApplication {
  id: number
  projectId: number
  projectTitle: string
  companyId: number
  companyName: string
  status: string
  reviewNote: string | null
  finalReviewNote: string | null
  applicationDocsUrl: string | null
  submittedAt: string | null
  waitingFor: string | null
  cvaReviewerName: string | null
  adminReviewerName: string | null
}

export function mapApplication(raw: Raw): ProjectApplication | null {
  const id = num(raw.id)
  if (!id) return null
  return {
    id,
    projectId: num(raw.projectId),
    projectTitle: text(first(raw.projectTitle, raw.projectName), 'Dự án chưa cập nhật'),
    companyId: num(raw.companyId),
    companyName: text(raw.companyName, 'Doanh nghiệp chưa cập nhật'),
    status: text(raw.status, 'SUBMITTED'),
    reviewNote: nullableText(raw.reviewNote),
    finalReviewNote: nullableText(raw.finalReviewNote),
    applicationDocsUrl: nullableText(raw.applicationDocsUrl),
    submittedAt: nullableText(raw.submittedAt),
    waitingFor: nullableText(raw.waitingFor),
    cvaReviewerName: nullableText(raw.cvaReviewerName),
    adminReviewerName: nullableText(raw.adminReviewerName),
  }
}

export interface EmissionReport {
  id: number
  sellerName: string
  projectName: string
  projectId: number | null
  period: string | null
  totalEnergy: number
  totalCo2: number
  vehicleCount: number
  status: string
  submittedAt: string | null
  fileName: string | null
  storageUrl: string | null
  aiPreScore: number | null
  verificationScore: number | null
  waitingFor: string | null
  verifiedByCvaName: string | null
  adminComment: string | null
  zeroEnergyRows: number | null
  co2Coverage: number | null
}

export function mapReport(raw: Raw): EmissionReport | null {
  const id = num(first(raw.id, raw.reportId))
  if (!id) return null
  return {
    id,
    sellerName: text(raw.sellerName, '—'),
    projectName: text(raw.projectName, '—'),
    projectId: raw.projectId === null || raw.projectId === undefined ? null : num(raw.projectId) || null,
    period: nullableText(raw.period),
    totalEnergy: num(raw.totalEnergy),
    totalCo2: num(raw.totalCo2),
    vehicleCount: num(raw.vehicleCount),
    status: text(raw.status, 'SUBMITTED'),
    submittedAt: nullableText(raw.submittedAt),
    fileName: nullableText(raw.uploadOriginalFilename),
    storageUrl: nullableText(raw.uploadStorageUrl),
    aiPreScore: raw.aiPreScore === null || raw.aiPreScore === undefined ? null : num(raw.aiPreScore),
    verificationScore: raw.verificationScore === null || raw.verificationScore === undefined ? null : num(raw.verificationScore),
    waitingFor: nullableText(raw.waitingFor),
    verifiedByCvaName: nullableText(raw.verifiedByCvaName),
    adminComment: nullableText(raw.adminComment),
    zeroEnergyRows: raw.zeroEnergyRows === undefined ? null : num(raw.zeroEnergyRows),
    co2Coverage: raw.co2Coverage === undefined ? null : num(raw.co2Coverage),
  }
}

export interface RuleResult {
  ruleId: string
  name: string
  score: number
  maxScore: number
  message: string
  evidence: string
}

export interface AnalysisResult {
  reportId: number
  version: string
  dataQualityScore: number
  dataQualityMax: number
  fraudRiskScore: number
  fraudRiskMax: number
  rules: RuleResult[]
  fraudReasons: string[]
}

export function mapAnalysis(payload: unknown): AnalysisResult {
  const raw = isRecord(payload) ? payload : {}
  const rules = asList(first(raw.details, raw.rules)).map((item) => mapRule(item)).filter((rule): rule is RuleResult => rule !== null)
  return {
    reportId: num(raw.reportId),
    version: text(raw.version),
    dataQualityScore: num(raw.dataQualityScore),
    dataQualityMax: num(raw.dataQualityMax, 70),
    fraudRiskScore: num(raw.fraudRiskScore),
    fraudRiskMax: num(raw.fraudRiskMax, 30),
    rules,
    fraudReasons: asList(raw.fraudReasons).map((reason) => text(reason)).filter(Boolean),
  }
}

function mapRule(value: unknown): RuleResult | null {
  if (!isRecord(value)) return null
  return {
    ruleId: text(value.ruleId),
    name: text(value.name, text(value.ruleId, 'Quy tắc')),
    score: num(value.score),
    maxScore: num(value.maxScore),
    message: text(value.message),
    evidence: text(value.evidence),
  }
}

export interface AiEvaluation {
  aiPreScore: number | null
  aiVersion: string | null
  aiPreNotes: string | null
}

export function mapAi(payload: unknown): AiEvaluation {
  const raw = isRecord(payload) ? payload : {}
  return {
    aiPreScore: raw.aiPreScore === undefined || raw.aiPreScore === null ? null : num(raw.aiPreScore),
    aiVersion: nullableText(raw.aiVersion),
    aiPreNotes: nullableText(raw.aiPreNotes),
  }
}

export interface CreditBatch {
  id: number
  batchCode: string
  vintageYear: number | null
  projectId: number | null
  projectTitle: string
  creditsCount: number
  totalTco2e: number
  residualTco2e: number
  status: string
  issuedAt: string | null
  expiresAt: string | null
  serialFrom: string | null
  serialTo: string | null
  certificateUrl: string | null
  reportId: number | null
  reportPeriod: string | null
  reportFileName: string | null
}

export function mapCreditBatch(raw: Raw, allowMissingId = false): CreditBatch | null {
  const id = num(raw.id)
  if (!id && !allowMissingId) return null
  return {
    id,
    batchCode: text(raw.batchCode, `Lô ${id}`),
    vintageYear: raw.vintageYear === undefined || raw.vintageYear === null ? null : num(raw.vintageYear),
    projectId: raw.projectId === undefined || raw.projectId === null ? null : num(raw.projectId),
    projectTitle: text(first(raw.projectTitle, raw.projectName), '—'),
    creditsCount: num(raw.creditsCount),
    totalTco2e: num(raw.totalTco2e),
    residualTco2e: num(raw.residualTco2e),
    status: text(raw.status),
    issuedAt: nullableText(raw.issuedAt),
    expiresAt: nullableText(raw.expiresAt),
    serialFrom: nullableText(raw.serialFrom),
    serialTo: nullableText(raw.serialTo),
    certificateUrl: nullableText(raw.certificateUrl),
    reportId: raw.reportId === undefined || raw.reportId === null ? null : num(raw.reportId),
    reportPeriod: nullableText(raw.reportPeriod),
    reportFileName: nullableText(raw.reportFileName),
  }
}

export interface CreditOrder {
  id: number
  status: string
  totalAmount: number
  creditAmount: number
  createAt: string | null
}

export function mapOrder(raw: Raw): CreditOrder | null {
  const id = num(raw.id)
  if (!id) return null
  return {
    id,
    status: text(raw.status, 'PENDING'),
    totalAmount: num(raw.totalAmount),
    creditAmount: num(raw.creditAmount),
    createAt: nullableText(first(raw.createAt, raw.createdAt)),
  }
}

export interface AdminProject {
  id: number
  title: string
  description: string
  status: string
  emissionFactorKgPerKwh: number | null
  bufferReservePct: number | null
  uncertaintyPct: number | null
  leakagePct: number | null
  measurementMethod: string
  commitments: string
  technicalIndicators: string
  startedDate: string | null
  endDate: string | null
  logo: string | null
  legalDocsFile: string | null
}

export function mapAdminProject(raw: Raw): AdminProject | null {
  const id = num(raw.id)
  if (!id) return null
  const optionalNumber = (value: unknown) => (value === undefined || value === null || value === '' ? null : num(value))
  return {
    id,
    title: text(first(raw.title, raw.projectName), 'Dự án chưa cập nhật'),
    description: text(raw.description),
    status: text(raw.status, 'OPEN'),
    emissionFactorKgPerKwh: optionalNumber(raw.emissionFactorKgPerKwh),
    bufferReservePct: optionalNumber(raw.bufferReservePct),
    uncertaintyPct: optionalNumber(raw.uncertaintyPct),
    leakagePct: optionalNumber(raw.leakagePct),
    measurementMethod: text(raw.measurementMethod),
    commitments: text(raw.commitments),
    technicalIndicators: text(raw.technicalIndicators),
    startedDate: nullableText(raw.startedDate),
    endDate: nullableText(raw.endDate),
    logo: nullableText(raw.logo),
    legalDocsFile: nullableText(raw.legalDocsFile),
  }
}

export interface AdminWithdrawal {
  id: number
  amount: number
  status: string
  requestedAt: string | null
  processedAt: string | null
  bankName: string
  accountNumber: string
  accountHolder: string
  branch: string
  walletBalance: number | null
  userEmail: string
}

export function mapWithdrawalAdmin(raw: Raw): AdminWithdrawal | null {
  const id = num(raw.id)
  if (!id) return null
  const details = parsePaymentDetails(raw.paymentDetails)
  const user = isRecord(raw.user) ? raw.user : {}
  const wallet = isRecord(user.wallet) ? user.wallet : null
  return {
    id,
    amount: num(raw.amount),
    status: text(raw.status, 'PENDING'),
    requestedAt: nullableText(first(raw.requestedAt, raw.createdAt)),
    processedAt: nullableText(raw.processedAt),
    bankName: text(details.bankName),
    accountNumber: text(details.accountNumber),
    accountHolder: text(details.accountHolder),
    branch: text(details.branch),
    walletBalance: wallet ? num(first(wallet.balance, wallet.amount)) : null,
    userEmail: text(user.email),
  }
}

function parsePaymentDetails(value: unknown): Raw {
  if (isRecord(value)) return value
  if (typeof value !== 'string' || !value.trim()) return {}
  try {
    const parsed: unknown = JSON.parse(value)
    return isRecord(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

export interface AdminUser {
  id: number
  email: string
  status: string
  roles: string[]
  walletBalance: number | null
}

export function mapAdminUser(raw: Raw): AdminUser | null {
  const id = num(raw.id)
  if (!id) return null
  const wallet = isRecord(raw.wallet) ? raw.wallet : null
  const roles = Array.isArray(raw.roles)
    ? raw.roles
        .map((role) => {
          if (typeof role === 'string') return role
          if (isRecord(role)) return text(first(role.roleName, role.name, role.authority))
          return ''
        })
        .filter(Boolean)
    : []
  return {
    id,
    email: text(raw.email),
    status: text(raw.status, 'PENDING'),
    roles,
    walletBalance: wallet ? num(first(wallet.balance, wallet.amount)) : null,
  }
}
