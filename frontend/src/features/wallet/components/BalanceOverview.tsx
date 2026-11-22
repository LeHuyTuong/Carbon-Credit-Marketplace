import { Coins, Landmark } from 'lucide-react'
import { StatCard } from '@/shared/components/StatCard'
import type { WalletSummary } from '@/shared/api/types'

export function BalanceOverview({ wallet }: { wallet: WalletSummary }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <StatCard
        title="Số dư ví (USD)"
        value={wallet.balance}
        icon={Landmark}
        hint="Sẵn sàng cho giao dịch và rút tiền"
        tone="brand"
      />
      <StatCard
        title="Số dư tín chỉ"
        value={wallet.carbonCreditBalance}
        icon={Coins}
        hint="Tín chỉ carbon đang nắm giữ"
        tone="slate"
      />
    </div>
  )
}
