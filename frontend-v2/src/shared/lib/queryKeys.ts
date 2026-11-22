export const queryKeys = {
  marketplace: ['marketplace'] as const,
  projects: ['projects'] as const,
  wallet: ['wallet'] as const,
  walletTransactions: ['wallet', 'transactions'] as const,
  paymentDetails: ['payment-details'] as const,
  withdrawals: ['withdrawals'] as const,
  payouts: ['payouts'] as const,
  dashboard: {
    ev: ['dashboard', 'ev'] as const,
    company: ['dashboard', 'company'] as const,
    cva: ['dashboard', 'cva'] as const,
    admin: ['dashboard', 'admin'] as const,
  },
} as const
