export interface ApiEnvelope<T> {
  requestTrace?: string
  requestDateTime?: string
  responseDateTime?: string
  responseStatus?: {
    responseCode?: string
    responseMessage?: string
  }
  response?: T | null
  responseData?: T | null
  data?: T | null
  message?: string
}

export interface AuthPayload {
  jwt?: string
  token?: string
  roles?: string[]
  role?: string
  message?: string
}

export interface MarketplaceListing {
  listingId: number
  quantity: number
  availableQuantity: number
  originalQuantity: number
  soldQuantity: number
  pricePerCredit: number
  sellerCompanyName: string
  projectId: number | null
  projectTitle: string
  expiresAt: string | null
  batchCode: string
  logo: string | null
}

export interface ProjectSummary {
  id: number
  title: string
  description: string
  status: string
  location: string
  imageUrl: string | null
}

export interface WalletSummary {
  id: number | null
  balance: number
  carbonCreditBalance: number
}

export interface WalletTransaction {
  id: number
  type: string
  amount: number
  description: string
  createdAt: string | null
  status: string
  batchCode?: string | null
  distributionId?: number | null
  orderId?: number | null
  carbonCreditQuantity?: number | null
  unitPrice?: number | null
}

export interface PaymentDetails {
  id: number | null
  bankName: string
  accountNumber: string
  accountHolder: string
  branch: string
}

export interface Withdrawal {
  id: number
  amount: number
  status: string
  createdAt: string | null
  note: string
}

export interface NotificationEvent {
  id: string
  title: string
  message: string
  createdAt: string | null
  read: boolean
}

export interface DashboardMetric {
  key: string
  label: string
  value: number | null
  unit?: string
  delta?: number | null
  hint?: string
}

export interface TrendPoint {
  label: string
  value: number
}

export interface ActivityItem {
  id: string
  title: string
  subtitle: string
  status: string
  date: string | null
}

export interface BreakdownPoint {
  name: string
  value: number
}

export interface DashboardSnapshot {
  metrics: DashboardMetric[]
  trend: TrendPoint[]
  breakdown?: BreakdownPoint[]
  breakdownTitle?: string
  breakdownDescription?: string
  recent: ActivityItem[]
}
