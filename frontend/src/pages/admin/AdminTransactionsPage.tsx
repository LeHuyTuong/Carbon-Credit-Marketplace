import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Eye, ReceiptText, RefreshCw, Search } from 'lucide-react'
import { api } from '@/shared/api/client'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { formatDate, formatNumber, formatUsd } from '@/shared/lib/format'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

interface LedgerTxn {
  id: number
  orderId?: number | null
  transactionType: string
  description: string
  balanceBefore?: number | null
  balanceAfter?: number | null
  amount: number
  carbonCreditQuantity?: number | null
  unitPrice?: number | null
  createdAt?: string
  batchCode?: string | null
  distributionId?: number | null
}

const TXN_TYPES = [
  'ALL',
  'BUY_CARBON_CREDIT',
  'SELL_CARBON_CREDIT',
  'ADD_MONEY',
  'WITHDRAWAL',
  'EV_OWNER_PAYOUT',
  'EV_OWNER_PAYOUT_RECEIVE',
  'ISSUE_CREDIT',
] as const

const TYPE_LABELS: Record<string, string> = {
  BUY_CARBON_CREDIT: 'Mua tín chỉ',
  SELL_CARBON_CREDIT: 'Bán tín chỉ',
  ADD_MONEY: 'Nạp tiền / Phí sàn',
  WITHDRAWAL: 'Rút tiền',
  WITHDRAWAL_REFUND: 'Hoàn rút tiền',
  EV_OWNER_PAYOUT: 'Chi trả chủ xe',
  EV_OWNER_PAYOUT_RECEIVE: 'Nhận chia doanh thu',
  ISSUE_CREDIT: 'Phát hành tín chỉ',
  RETIRE_CREDIT: 'Triệt tiêu tín chỉ',
}

export function AdminTransactionsPage() {
  const [search, setSearch] = useState('')
  const [typeFilter, setTypeFilter] = useState<string>('ALL')
  const [selected, setSelected] = useState<LedgerTxn | null>(null)

  const txnsQuery = useQuery({
    queryKey: ['admin', 'transactions'],
    queryFn: async () => {
      const data = await api.get<LedgerTxn[]>('/wallet/transactions')
      return Array.isArray(data) ? data : []
    },
  })

  const txns = txnsQuery.data ?? []
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    return txns.filter((t) => {
      const matchType = typeFilter === 'ALL' || t.transactionType === typeFilter
      const matchQ =
        !q ||
        String(t.id).includes(q) ||
        t.description?.toLowerCase().includes(q) ||
        t.transactionType?.toLowerCase().includes(q) ||
        (t.batchCode ?? '').toLowerCase().includes(q)
      return matchType && matchQ
    })
  }, [txns, search, typeFilter])

  const totalVolume = useMemo(
    () => txns.reduce((sum, t) => sum + Math.abs(Number(t.amount) || 0), 0),
    [txns],
  )

  return (
    <>
      <PageIntro
        eyebrow="Admin · Sổ cái"
        title="Quản lý giao dịch & Sổ cái"
        description="Theo dõi toàn bộ dòng tiền bất biến (balanceBefore → balanceAfter), phí sàn 5% và chi trả chia sẻ doanh thu."
        actions={
          <Button
            variant="outline"
            leftIcon={<RefreshCw className="size-4" />}
            onClick={() => void txnsQuery.refetch()}
          >
            Làm mới
          </Button>
        }
      />

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-ink-100 bg-white p-5 shadow-xs">
          <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Tổng dòng sổ cái</p>
          <p className="mt-2 text-2xl font-bold text-ink-900">{formatNumber(txns.length)} giao dịch</p>
          <p className="mt-1 text-xs text-ink-500">Mọi biến động ví đều ghi vết trước/sau</p>
        </div>
        <div className="rounded-2xl border border-ink-100 bg-white p-5 shadow-xs">
          <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Tổng giá trị luân chuyển</p>
          <p className="mt-2 text-2xl font-bold text-brand-700">{formatUsd(totalVolume)}</p>
          <p className="mt-1 text-xs text-ink-500">Gồm mua bán, nạp/rút và chia doanh thu</p>
        </div>
        <div className="rounded-2xl border border-ink-100 bg-white p-5 shadow-xs">
          <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Kết quả đang lọc</p>
          <p className="mt-2 text-2xl font-bold text-ink-900">{formatNumber(filtered.length)} bản ghi</p>
          <p className="mt-1 text-xs text-ink-500">Nhấn Chi tiết để xem mã lô & đơn hàng</p>
        </div>
      </div>

      {txnsQuery.isPending ? (
        <QueryLoading />
      ) : txnsQuery.isError ? (
        <QueryError onRetry={() => void txnsQuery.refetch()} />
      ) : (
        <SectionCard title="Nhật ký giao dịch hệ thống" description={`${txns.length} dòng · ${filtered.length} hiển thị`}>
          <div className="grid gap-4 border-b border-ink-100 p-5 sm:grid-cols-[1fr_240px]">
            <Input
              label="Tìm kiếm giao dịch"
              placeholder="Mã GD, mô tả, mã lô hoặc loại giao dịch"
              value={search}
              icon={<Search className="size-4" />}
              onChange={(e) => setSearch(e.target.value)}
            />
            <div>
              <label htmlFor="txn-type-filter" className="mb-1.5 block text-sm font-medium text-ink-700">
                Loại giao dịch
              </label>
              <select
                id="txn-type-filter"
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
                className="h-11 w-full rounded-xl border border-ink-200 bg-white px-3.5 text-sm"
              >
                {TXN_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t === 'ALL' ? 'Tất cả loại' : TYPE_LABELS[t] ?? t}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {filtered.length === 0 ? (
            <EmptyState
              icon={ReceiptText}
              title="Không có giao dịch phù hợp"
              description="Thử thay đổi bộ lọc loại giao dịch hoặc từ khoá."
            />
          ) : (
            <div className="scrollbar-thin overflow-x-auto p-5">
              <table className="w-full min-w-[820px] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                    <th className="pb-3 font-semibold">Mã</th>
                    <th className="pb-3 font-semibold">Loại</th>
                    <th className="pb-3 font-semibold">Mô tả</th>
                    <th className="pb-3 font-semibold">Số tiền</th>
                    <th className="pb-3 font-semibold">Số dư (Trước → Sau)</th>
                    <th className="pb-3 font-semibold">Thời gian</th>
                    <th className="pb-3 text-right font-semibold">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {filtered.map((t) => (
                    <tr key={t.id} className="text-ink-700">
                      <td className="py-3 font-mono text-xs text-ink-500">#{t.id}</td>
                      <td className="py-3">
                        <span className="inline-flex whitespace-nowrap rounded-full bg-ink-100 px-2.5 py-1 text-xs font-semibold text-ink-700">
                          {TYPE_LABELS[t.transactionType] ?? t.transactionType}
                        </span>
                      </td>
                      <td className="max-w-xs truncate py-3 text-ink-900">{t.description || '—'}</td>
                      <td className="py-3 font-mono font-semibold text-ink-900">{formatUsd(t.amount)}</td>
                      <td className="py-3 font-mono text-xs text-ink-600">
                        {t.balanceBefore != null && t.balanceAfter != null
                          ? `${formatUsd(t.balanceBefore)} → ${formatUsd(t.balanceAfter)}`
                          : '—'}
                      </td>
                      <td className="py-3 text-xs text-ink-500">{t.createdAt ? formatDate(t.createdAt) : '—'}</td>
                      <td className="py-3 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          leftIcon={<Eye className="size-3.5" />}
                          onClick={() => setSelected(t)}
                        >
                          Chi tiết
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </SectionCard>
      )}

      {selected && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/40 p-4 backdrop-blur-xs"
          onClick={() => setSelected(null)}
        >
          <div
            className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="border-b border-ink-100 pb-4">
              <p className="font-mono text-xs font-semibold text-brand-700">Giao dịch #{selected.id}</p>
              <h3 className="mt-1 text-lg font-bold text-ink-900">
                {TYPE_LABELS[selected.transactionType] ?? selected.transactionType}
              </h3>
            </div>
            <dl className="mt-4 space-y-2.5 text-sm">
              <div>
                <dt className="text-ink-500">Diễn giải</dt>
                <dd className="mt-1 font-medium text-ink-900">{selected.description}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-500">Số tiền</dt>
                <dd className="font-mono font-bold text-brand-700">{formatUsd(selected.amount)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-500">Số dư trước → sau</dt>
                <dd className="font-mono text-ink-900">
                  {selected.balanceBefore != null && selected.balanceAfter != null
                    ? `${formatUsd(selected.balanceBefore)} → ${formatUsd(selected.balanceAfter)}`
                    : '—'}
                </dd>
              </div>
              {selected.orderId != null && (
                <div className="flex justify-between">
                  <dt className="text-ink-500">Đơn hàng liên quan</dt>
                  <dd className="font-mono text-ink-900">#{selected.orderId}</dd>
                </div>
              )}
              {selected.batchCode && (
                <div className="flex justify-between">
                  <dt className="text-ink-500">Mã lô tín chỉ</dt>
                  <dd className="font-mono font-semibold text-brand-700">{selected.batchCode}</dd>
                </div>
              )}
              {selected.distributionId != null && (
                <div className="flex justify-between">
                  <dt className="text-ink-500">Đợt chia doanh thu</dt>
                  <dd className="font-mono text-ink-900">#{selected.distributionId}</dd>
                </div>
              )}
            </dl>
            <div className="mt-6 flex justify-end">
              <Button variant="outline" onClick={() => setSelected(null)}>
                Đóng
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
