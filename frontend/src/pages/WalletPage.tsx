import { BalanceOverview } from '@/features/wallet/components/BalanceOverview'
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

export function WalletPage() {
  const { wallet, transactions, paymentDetails, withdrawals } = useWalletData()

  return (
    <>
      <PageIntro
        eyebrow="Ví CarbonX"
        title="Ví và thanh toán"
        description="Quản lý số dư, tài khoản ngân hàng nhận tiền và các yêu cầu rút tiền của bạn."
      />

      {wallet.isPending ? (
        <QueryLoading />
      ) : wallet.isError || !wallet.data ? (
        <QueryError onRetry={() => void wallet.refetch()} />
      ) : (
        <div className="min-w-0 space-y-6">
          <BalanceOverview wallet={wallet.data as WalletSummary} />

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
              description="Yêu cầu sẽ được ADMIN phê duyệt trước khi chi trả."
            >
              <WithdrawCard
                balance={wallet.data.balance}
                withdrawals={withdrawals.data ?? []}
              />
            </SectionCard>
          </div>

          <SectionCard
            className="min-w-0"
            title="Lịch sử giao dịch"
            description="Các khoản tiền vào/ra gần đây của ví"
          >
            <div className="min-w-0 overflow-x-auto">
              {transactions.isPending ? (
                <div className="h-48 animate-pulse bg-ink-100" />
              ) : transactions.isError ? (
                <QueryError onRetry={() => void transactions.refetch()} />
              ) : (
                <TransactionList transactions={transactions.data ?? []} />
              )}
            </div>
          </SectionCard>
        </div>
      )}
    </>
  )
}
