import { AxiosError } from 'axios'
import { toNumber } from '@/shared/lib/format'
import { apiClient } from './client'

export type Raw = Record<string, unknown>

export function isRecord(value: unknown): value is Raw {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function pick(...values: unknown[]): unknown {
  return values.find((value) => value !== undefined && value !== null && value !== '')
}

function text(value: unknown, fallback = ''): string {
  const chosen = pick(value)
  if (typeof chosen === 'string') return chosen
  if (typeof chosen === 'number' || typeof chosen === 'boolean') return String(chosen)
  return fallback
}

function id(value: unknown): number {
  return toNumber(value, 0)
}

/** Backend returns arrays, Spring Page objects, or custom { items } wrappers. */
export function asList(raw: unknown): Raw[] {
  if (Array.isArray(raw)) return raw.filter(isRecord)
  if (isRecord(raw)) {
    for (const candidate of [raw.content, raw.items, raw.results, raw.data]) {
      if (Array.isArray(candidate)) return candidate.filter(isRecord)
    }
  }
  return []
}

export function pageOf(raw: unknown): { items: Raw[]; total: number | null } {
  return {
    items: asList(raw),
    total: isRecord(raw) && raw.totalElements !== undefined ? toNumber(raw.totalElements) : null,
  }
}

export function mapList<T>(raw: unknown, mapper: (item: Raw) => T): T[] {
  return asList(raw).map(mapper)
}

export type FormValue = string | number | boolean | File | null | undefined

export function toFormData(values: Record<string, FormValue>): FormData {
  const formData = new FormData()
  Object.entries(values).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') return
    if (value instanceof File) formData.append(key, value, value.name)
    else formData.append(key, String(value))
  })
  return formData
}

export async function downloadFile(url: string, fileName: string): Promise<void> {
  const response = await apiClient.get<Blob>(url, { responseType: 'blob' })
  const blob = response.data
  if (blob.type.includes('application/json')) {
    const detail = await blob.text()
    throw new Error(detail || 'Không thể tải tệp')
  }
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1_000)
}

export function errorMessage(error: unknown, fallback = 'Thao tác thất bại'): string {
  if (error instanceof AxiosError) {
    const data: unknown = error.response?.data
    if (isRecord(data)) {
      const status = data.responseStatus
      const message = pick(data.message, isRecord(status) ? status.responseMessage : undefined)
      if (typeof message === 'string' && message) return message
    }
  }
  if (error instanceof Error && error.message) return error.message
  return fallback
}

export interface KycProfile {
  userId: number
  name: string
  email: string
  phone: string
  country: string
  address: string
  birthDate: string
  documentType: string
  documentNumber: string
  gender: string
}

export function mapKyc(raw: Raw): KycProfile {
  return {
    userId: id(pick(raw.userId, raw.id)),
    name: text(pick(raw.name, raw.fullName)),
    email: text(raw.email),
    phone: text(raw.phone),
    country: text(raw.country),
    address: text(raw.address),
    birthDate: text(pick(raw.birthDate, raw.birthday)),
    documentType: text(raw.documentType),
    documentNumber: text(raw.documentNumber),
    gender: text(raw.gender),
  }
}

export interface Vehicle {
  id: number
  plateNumber: string
  brand: string
  model: string
  companyId: number | null
  documentUrl: string
}

export function mapVehicle(raw: Raw): Vehicle {
  const company = isRecord(raw.company) ? pick(raw.companyId, raw.company.id) : raw.companyId
  return {
    id: id(pick(raw.id, raw.vehicleId)),
    plateNumber: text(pick(raw.plateNumber, raw.plate)),
    brand: text(raw.brand),
    model: text(raw.model),
    companyId: company === undefined || company === null ? null : id(company),
    documentUrl: text(pick(raw.documentUrl, raw.documentFile)),
  }
}

export interface ProjectItem {
  id: number
  title: string
  description: string
  status: string
  commitments: string
  technicalIndicators: string
  measurementMethod: string
  legalDocsFile: string
  logo: string
  startedDate: string
  endDate: string
  emissionFactorKgPerKwh: number
  bufferReservePct: number
  uncertaintyPct: number
  leakagePct: number
}

export function mapProjectItem(raw: Raw): ProjectItem {
  return {
    id: id(pick(raw.id, raw.projectId)),
    title: text(pick(raw.title, raw.projectTitle)),
    description: text(raw.description),
    status: text(raw.status),
    commitments: text(raw.commitments),
    technicalIndicators: text(raw.technicalIndicators),
    measurementMethod: text(raw.measurementMethod),
    legalDocsFile: text(raw.legalDocsFile),
    logo: text(raw.logo),
    startedDate: text(pick(raw.startedDate, raw.startDate)),
    endDate: text(pick(raw.endDate, raw.endDateAt)),
    emissionFactorKgPerKwh: toNumber(pick(raw.emissionFactorKgPerKwh, raw.emissionFactor)),
    bufferReservePct: toNumber(pick(raw.bufferReservePct, raw.bufferPct)),
    uncertaintyPct: toNumber(pick(raw.uncertaintyPct, raw.uncertainty)),
    leakagePct: toNumber(pick(raw.leakagePct, raw.leakage)),
  }
}

export interface ProjectApplication {
  id: number
  projectId: number
  projectTitle: string
  companyId: number
  companyName: string
  status: string
  reviewNote: string
  finalReviewNote: string
  applicationDocsUrl: string
  submittedAt: string
  waitingFor: string
  cvaReviewerName: string
  adminReviewerName: string
}

export function mapApplication(raw: Raw): ProjectApplication {
  return {
    id: id(pick(raw.id, raw.applicationId)),
    projectId: id(raw.projectId),
    projectTitle: text(pick(raw.projectTitle, raw.projectName)),
    companyId: id(raw.companyId),
    companyName: text(pick(raw.companyName, raw.sellerName)),
    status: text(raw.status),
    reviewNote: text(raw.reviewNote),
    finalReviewNote: text(raw.finalReviewNote),
    applicationDocsUrl: text(pick(raw.applicationDocsUrl, raw.applicationDocsPath)),
    submittedAt: text(pick(raw.submittedAt, raw.createdAt)),
    waitingFor: text(raw.waitingFor),
    cvaReviewerName: text(raw.cvaReviewerName),
    adminReviewerName: text(raw.adminReviewerName),
  }
}

export interface EmissionReport {
  id: number
  sellerId: number
  sellerName: string
  projectId: number
  projectName: string
  period: string
  totalEnergy: number
  totalCo2: number
  vehicleCount: number
  status: string
  source: string
  submittedAt: string
  uploadOriginalFilename: string
  uploadRows: number
}

export function mapEmissionReport(raw: Raw): EmissionReport {
  return {
    id: id(pick(raw.id, raw.reportId)),
    sellerId: id(raw.sellerId),
    sellerName: text(pick(raw.sellerName, raw.companyName)),
    projectId: id(raw.projectId),
    projectName: text(pick(raw.projectName, raw.projectTitle)),
    period: text(raw.period),
    totalEnergy: toNumber(pick(raw.totalEnergy, raw.totalKwh)),
    totalCo2: toNumber(pick(raw.totalCo2, raw.totalCo2Kg)),
    vehicleCount: toNumber(raw.vehicleCount),
    status: text(raw.status),
    source: text(raw.source),
    submittedAt: text(pick(raw.submittedAt, raw.createdAt)),
    uploadOriginalFilename: text(pick(raw.uploadOriginalFilename, raw.fileName)),
    uploadRows: toNumber(raw.uploadRows),
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
  details: RuleResult[]
  fraudReasons: string[]
}

export function mapRule(raw: Raw): RuleResult {
  return {
    ruleId: text(raw.ruleId),
    name: text(raw.name),
    score: toNumber(raw.score),
    maxScore: toNumber(raw.maxScore),
    message: text(raw.message),
    evidence: text(raw.evidence),
  }
}

export function mapAnalysis(raw: Raw): AnalysisResult {
  const fraudReasons = raw.fraudReasons
  return {
    reportId: id(raw.reportId),
    version: text(raw.version),
    dataQualityScore: toNumber(raw.dataQualityScore),
    dataQualityMax: toNumber(raw.dataQualityMax),
    fraudRiskScore: toNumber(raw.fraudRiskScore),
    fraudRiskMax: toNumber(raw.fraudRiskMax),
    details: mapList(raw.details, mapRule),
    fraudReasons: Array.isArray(fraudReasons) ? fraudReasons.map((item) => text(item)) : [],
  }
}

export interface AiEvaluation {
  aiPreScore: number | null
  aiVersion: string
  aiPreNotes: string
}

export function mapAiEvaluation(raw: Raw): AiEvaluation {
  const score = pick(raw.aiPreScore, raw.score)
  return {
    aiPreScore: score === undefined || score === null ? null : toNumber(score),
    aiVersion: text(raw.aiVersion),
    aiPreNotes: text(pick(raw.aiPreNotes, raw.notes)),
  }
}

export interface CreditBatch {
  id: number
  batchCode: string
  vintageYear: number
  creditsCount: number
  totalTco2e: number
  residualTco2e: number
  status: string
  issuedAt: string
  expiresAt: string
  projectTitle: string
  companyName: string
  reportPeriod: string
  reportFileName: string
  certificateUrl: string
  serialFrom: number
  serialTo: number
}

export function mapCreditBatch(raw: Raw): CreditBatch {
  return {
    id: id(pick(raw.id, raw.batchId)),
    batchCode: text(raw.batchCode),
    vintageYear: toNumber(raw.vintageYear),
    creditsCount: toNumber(pick(raw.creditsCount, raw.quantity)),
    totalTco2e: toNumber(raw.totalTco2e),
    residualTco2e: toNumber(raw.residualTco2e),
    status: text(raw.status),
    issuedAt: text(raw.issuedAt),
    expiresAt: text(raw.expiresAt),
    projectTitle: text(raw.projectTitle),
    companyName: text(raw.companyName),
    reportPeriod: text(raw.reportPeriod),
    reportFileName: text(raw.reportFileName),
    certificateUrl: text(raw.certificateUrl),
    serialFrom: toNumber(raw.serialFrom),
    serialTo: toNumber(raw.serialTo),
  }
}

export interface CreditOrder {
  id: number
  companyId: number
  status: string
  totalAmount: number
  creditAmount: number
  createAt: string
}

export function mapCreditOrder(raw: Raw): CreditOrder {
  return {
    id: id(pick(raw.id, raw.orderId)),
    companyId: id(raw.companyId),
    status: text(raw.status),
    totalAmount: toNumber(pick(raw.totalAmount, raw.totalMoney)),
    creditAmount: toNumber(pick(raw.creditAmount, raw.quantity)),
    createAt: text(pick(raw.createAt, raw.createdAt)),
  }
}

export interface AdminWithdrawal {
  id: number
  amount: number
  status: string
  requestedAt: string
  processedAt: string
  userEmail: string
  userName: string
  bankName: string
  accountNumber: string
  accountHolder: string
  branch: string
  walletBalance: number | null
}

export function mapAdminWithdrawal(raw: Raw): AdminWithdrawal {
  const user = isRecord(raw.user) ? raw.user : {}
  const details = isRecord(raw.paymentDetails) ? raw.paymentDetails : {}
  const walletRaw = pick(user.wallet, raw.wallet)
  const wallet = isRecord(walletRaw) ? walletRaw : null
  const balance = wallet ? pick(wallet.balance, wallet.carbonCreditBalance) : undefined
  return {
    id: id(pick(raw.id, raw.withdrawalId)),
    amount: toNumber(raw.amount),
    status: text(raw.status),
    requestedAt: text(pick(raw.requestedAt, raw.createdAt)),
    processedAt: text(raw.processedAt),
    userEmail: text(user.email),
    userName: text(pick(user.fullName, user.name)),
    bankName: text(pick(details.bankName, details.bankCode)),
    accountNumber: text(details.accountNumber),
    accountHolder: text(pick(details.accountHolder, details.accountHolderName, details.customerName)),
    branch: text(details.branch),
    walletBalance: balance === undefined || balance === null ? null : toNumber(balance),
  }
}

export interface AdminUser {
  id: number
  email: string
  fullName: string
  status: string
  roles: string[]
  companyName: string
}

export function mapAdminUser(raw: Raw): AdminUser {
  const roles = raw.roles
  const companyRaw = raw.company
  return {
    id: id(raw.id),
    email: text(raw.email),
    fullName: text(pick(raw.fullName, raw.name)),
    status: text(raw.status, 'PENDING'),
    roles: Array.isArray(roles)
      ? roles.map((role) => (isRecord(role) ? text(pick(role.name, role.authority)) : text(role))).filter(Boolean)
      : [],
    companyName: text(pick(raw.companyName, isRecord(companyRaw) ? companyRaw.companyName : undefined)),
  }
}
