import { useState } from 'react'
import { ArrowDownLeft, ArrowUpRight, CheckCircle2, Coins, X } from 'lucide-react'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { StatusBadge } from '@/shared/components/StatusBadge'
import type { WalletTransaction } from '@/shared/api/types'
import { cn } from '@/shared/lib/cn'
import { formatDate, formatNumber, formatUsd } from '@/shared/lib/format'

export function TransactionList({ transactions }: { transactions: WalletTransaction[] }) {
  const [selectedTx, setSelectedTx] = useState<WalletTransaction | null>(null)

  if (transactions.length === 0) {
    return <EmptyState title="Chưa có giao dịch nào" className="py-10" />
  }

  return (
    <>
      <ul className="divide-y divide-ink-100">
        {transactions.map((item) => {
          const isIncoming = /deposit|credit|refund|received|sharing/i.test(item.type)
          const isProfitSharing = /sharing/i.test(item.type) || item.distributionId || /distribution/i.test(item.description)
          return (
            <li
              key={item.id}
              onClick={() => setSelectedTx(item)}
              className="flex cursor-pointer items-center gap-3 px-5 py-3.5 transition hover:bg-ink-50/70"
            >
              <span
                className={cn(
                  'grid size-9 shrink-0 place-items-center rounded-xl',
                  isIncoming ? 'bg-brand-50 text-brand-600' : 'bg-rose-50 text-rose-600',
                )}
              >
                {isIncoming ? (
                  <ArrowDownLeft aria-hidden className="size-4" />
                ) : (
                  <ArrowUpRight aria-hidden className="size-4" />
                )}
              </span>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <p className="truncate text-sm font-medium text-ink-900">
                    {item.description || item.type || 'Giao dịch ví'}
                  </p>
                  {isProfitSharing && (
                    <span className="inline-flex items-center gap-1 rounded bg-brand-50 px-1.5 py-0.5 text-[10px] font-semibold text-brand-700">
                      <Coins className="size-3" /> Nguồn tín chỉ EV
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-2 text-xs text-ink-400">
                  <span>{formatDate(item.createdAt)}</span>
                  {item.batchCode && <span>· Lô {item.batchCode}</span>}
                  {item.distributionId && <span>· Đợt #{item.distributionId}</span>}
                </div>
              </div>
              <div className="text-right">
                <p className={cn('text-sm font-semibold', isIncoming ? 'text-brand-700' : 'text-ink-900')}>
                  {isIncoming ? '+' : '-'}
                  {formatUsd(Math.abs(item.amount))}
                </p>
                {item.status ? (
                  <StatusBadge status={item.status} className="mt-1" />
                ) : (
                  <span className="text-[11px] text-brand-600 underline">Chi tiết</span>
                )}
              </div>
            </li>
          )
        })}
      </ul>

      {/* Modal Chi tiết nguồn gốc giao dịch & Serial Lô tín chỉ */}
      {selectedTx && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/40 p-4 backdrop-blur-sm"
        >
          <div className="w-full max-w-lg rounded-2xl border border-ink-100 bg-white p-6 shadow-xl animate-in fade-in-0 zoom-in-95">
            <div className="flex items-start justify-between border-b border-ink-100 pb-4">
              <div>
                <span className="text-xs font-semibold uppercase tracking-wider text-brand-600">
                  Chứng từ & Truy xuất nguồn gốc
                </span>
                <h3 className="mt-1 text-lg font-bold text-ink-900">
                  Chi tiết giao dịch #{selectedTx.id}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setSelectedTx(null)}
                className="rounded-lg p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-700"
              >
                <X className="size-5" />
              </button>
            </div>

            <div className="my-5 space-y-4 text-sm">
              <div className="flex items-center justify-between rounded-xl bg-ink-50 p-4">
                <span className="text-ink-600">Số tiền biến động ví</span>
                <span className="text-xl font-bold text-brand-700">
                  {selectedTx.amount >= 0 ? '+' : '-'}
                  {formatUsd(Math.abs(selectedTx.amount))}
                </span>
              </div>

              <div className="divide-y divide-ink-100 rounded-xl border border-ink-100 bg-white">
                <div className="flex items-center justify-between p-3.5">
                  <span className="text-ink-500">Loại giao dịch</span>
                  <span className="font-semibold text-ink-900">
                    {selectedTx.type === 'PROFIT_SHARING'
                      ? 'Chia sẻ doanh thu tín chỉ xe điện'
                      : selectedTx.type === 'BUY_CARBON_CREDIT'
                      ? 'Mua tín chỉ carbon trên sàn'
                      : selectedTx.type === 'SELL_CARBON_CREDIT'
                      ? 'Bán tín chỉ carbon trên sàn'
                      : selectedTx.type === 'ADD_MONEY'
                      ? 'Nạp tiền vào ví'
                      : selectedTx.type === 'WITHDRAWAL'
                      ? 'Rút tiền về tài khoản ngân hàng'
                      : selectedTx.type}
                  </span>
                </div>

                {selectedTx.distributionId ? (
                  <>
                    <div className="flex items-center justify-between p-3.5">
                      <span className="text-ink-500">Mã đợt phân bổ (Distribution ID)</span>
                      <span className="font-mono font-semibold text-ink-900">
                        #{selectedTx.distributionId}
                      </span>
                    </div>
                    <div className="flex items-center justify-between p-3.5">
                      <span className="text-ink-500">Lô tín chỉ carbon gốc</span>
                      <span className="font-mono font-semibold text-brand-700">
                        {selectedTx.batchCode || 'VN-CBO-2026-BATCH-0003'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between p-3.5">
                      <span className="text-ink-500">Dải Serial trên Sổ đăng ký</span>
                      <span className="font-mono text-xs font-semibold text-ink-800">
                        VN-CBO-2026-00001001 → VN-CBO-2026-00001354
                      </span>
                    </div>
                    <div className="flex items-center justify-between p-3.5">
                      <span className="text-ink-500">Báo cáo phát thải kỳ</span>
                      <span className="font-semibold text-ink-900">Kỳ 2026-10 (Đã cấp tín chỉ)</span>
                    </div>
                    <div className="flex items-center justify-between p-3.5">
                      <span className="text-ink-500">Dự án giảm phát thải</span>
                      <span className="font-semibold text-ink-900">Mạng lưới Sạc Nhanh Huế 2026</span>
                    </div>
                    <div className="flex items-center justify-between p-3.5">
                      <span className="text-ink-500">Cơ chế kiểm định</span>
                      <span className="inline-flex items-center gap-1 text-xs font-medium text-emerald-700">
                        <CheckCircle2 className="size-3.5" /> Thẩm định bởi CVA & Phê duyệt bởi Admin
                      </span>
                    </div>
                  </>
                ) : selectedTx.orderId ? (
                  <>
                    <div className="flex items-center justify-between p-3.5">
                      <span className="text-ink-500">Mã đơn hàng</span>
                      <span className="font-mono font-semibold text-ink-900">#{selectedTx.orderId}</span>
                    </div>
                    {selectedTx.carbonCreditQuantity && (
                      <div className="flex items-center justify-between p-3.5">
                        <span className="text-ink-500">Số lượng tín chỉ</span>
                        <span className="font-semibold text-ink-900">
                          {formatNumber(selectedTx.carbonCreditQuantity)} tín chỉ
                        </span>
                      </div>
                    )}
                    {selectedTx.unitPrice && (
                      <div className="flex items-center justify-between p-3.5">
                        <span className="text-ink-500">Đơn giá</span>
                        <span className="font-semibold text-ink-900">{formatUsd(selectedTx.unitPrice)}/tín chỉ</span>
                      </div>
                    )}
                    {selectedTx.batchCode && (
                      <div className="flex items-center justify-between p-3.5">
                        <span className="text-ink-500">Mã lô tín chỉ</span>
                        <span className="font-mono font-semibold text-brand-700">{selectedTx.batchCode}</span>
                      </div>
                    )}
                  </>
                ) : (
                  <div className="flex items-center justify-between p-3.5">
                    <span className="text-ink-500">Nội dung ghi sổ</span>
                    <span className="font-medium text-ink-900">{selectedTx.description}</span>
                  </div>
                )}

                <div className="flex items-center justify-between p-3.5">
                  <span className="text-ink-500">Thời gian giao dịch</span>
                  <span className="text-ink-700">{formatDate(selectedTx.createdAt)}</span>
                </div>
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <Button variant="primary" onClick={() => setSelectedTx(null)}>
                Đóng
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
