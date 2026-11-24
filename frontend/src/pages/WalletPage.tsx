import { useMemo, useState } from 'react'
import { BalanceOverview } from '@/features/wallet/components/BalanceOverview'
import { DepositCard } from '@/features/wallet/components/DepositCard'
import { PaymentDetailsCard } from '@/features/wallet/components/PaymentDetailsCard'
import { TransactionList } from '@/features/wallet/components/TransactionList'
import { WithdrawCard } from '@/features/wallet/components/WithdrawCard'
import { useWalletData } from '@/features/wallet/useWallet'
import type { PaymentDetails, WalletSummary } from '@/shared/api/types'
import { PageIntro } from './components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from './components/QueryState'

const EMPTY_PAYMENT: PaymentDetails = {
  id: null,
  bankName: '',
  accountNumber: '',
  accountHolder: '',
  branch: '',
}

type TxnFilter = 'ALL' | 'CASH' | 'CARBON'

export function WalletPage() {
  const { wallet, transactions, paymentDetails, withdrawals } = useWalletData()
  const [txnFilter, setTxnFilter] = useState<TxnFilter>('ALL')

  const filteredTxns = useMemo(() => {
    const items = transactions.data ?? []
    if (txnFilter === 'ALL') return items
    return items.filter((item) => {
      const isCarbon = /credit|issue|retire/i.test(item.type)
      return txnFilter === 'CARBON' ? isCarbon : !isCarbon
    })
  }, [transactions.data, txnFilter])

  return (
    <>
      <PageIntro
        eyebrow="Ví CarbonX"
        title="Ví và thanh toán"
        description="Quản lý đồng thời số dư tiền (USD) và số dư tín chỉ carbon (BR-02), nạp tiền qua VNPay/PayPal/Stripe và rút tiền không phí (BR-03)."
      />

      {wallet.isPending ? (
        <QueryLoading />
      ) : wallet.isError || !wallet.data ? (
        <QueryError onRetry={() => void wallet.refetch()} />
      ) : (
        <div className="min-w-0 space-y-6">
          <BalanceOverview wallet={wallet.data as WalletSummary} />

          <SectionCard
            className="min-w-0"
            title="Nạp tiền vào ví"
            description="Chọn cổng thanh toán VNPay, Stripe hoặc PayPal để nạp tiền mua tín chỉ carbon (BR-33, BR-35)."
          >
            <DepositCard />
          </SectionCard>

          <div className="grid min-w-0 gap-6 xl:grid-cols-2">
            <SectionCard
              className="min-w-0"
              title="Tài khoản nhận tiền"
              description="Thông tin này được dùng khi phê duyệt yêu cầu rút tiền."
            >
              {paymentDetails.isPending ? (
                <div className="h-56 animate-pulse bg-ink-100" />
              ) : paymentDetails.isError ? (
                <QueryError
                  title="Chưa tải được tài khoản"
                  description="Bạn có thể thêm tài khoản mới trong biểu mẫu bên dưới."
                  onRetry={() => void paymentDetails.refetch()}
                />
              ) : (
                <PaymentDetailsCard details={paymentDetails.data ?? EMPTY_PAYMENT} />
              )}
            </SectionCard>

            <SectionCard
              className="min-w-0"
              title="Rút tiền"
              description="Yêu cầu sẽ được Quản trị viên phê duyệt trước khi chi trả."
            >
              <WithdrawCard
                balance={wallet.data.balance}
                withdrawals={withdrawals.data ?? []}
                hasBankAccount={Boolean(paymentDetails.data?.accountNumber)}
              />
            </SectionCard>
          </div>

          <SectionCard
            className="min-w-0"
            title="Lịch sử giao dịch"
            description="Phân tách dòng tiền (CASH) và dòng tín chỉ (CARBON) theo BR-02"
            action={
              <div className="flex gap-1.5">
                {(
                  [
                    ['ALL', 'Tất cả'],
                    ['CASH', 'Tiền (USD)'],
                    ['CARBON', 'Tín chỉ'],
                  ] as const
                ).map(([key, label]) => (
                  <button
                    key={key}
                    type="button"
                    onClick={() => setTxnFilter(key)}
                    className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
                      txnFilter === key
                        ? 'bg-brand-600 text-white'
                        : 'bg-ink-100 text-ink-600 hover:bg-ink-200'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            }
          >
            <div className="min-w-0 overflow-x-auto">
              {transactions.isPending ? (
                <div className="h-48 animate-pulse bg-ink-100" />
              ) : transactions.isError ? (
                <QueryError onRetry={() => void transactions.refetch()} />
              ) : (
                <TransactionList transactions={filteredTxns} />
              )}
            </div>
          </SectionCard>
        </div>
      )}
    </>
  )
}
