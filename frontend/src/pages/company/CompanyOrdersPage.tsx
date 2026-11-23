import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, ShoppingCart, XCircle } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import { errorMessage, mapCreditOrder } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate, formatNumber, formatUsd } from '@/shared/lib/format'
import { queryKeys } from '@/shared/lib/queryKeys'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

export function CompanyOrdersPage() {
  const queryClient = useQueryClient()
  const ordersQuery = useQuery({
    queryKey: queryKeys.phase3b.orders,
    queryFn: async () => {
      const data = await api.get<unknown>('/orders')
      return Array.isArray(data) ? data.map(mapCreditOrder) : []
    },
  })

  const completeMutation = useMutation({
    mutationFn: (orderId: number) => api.post<unknown>(`/orders/${orderId}/complete`),
    onSuccess: async () => {
      toast.success('Đơn hàng đã hoàn tất')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.orders }),
        queryClient.invalidateQueries({ queryKey: queryKeys.wallet }),
        queryClient.invalidateQueries({ queryKey: queryKeys.walletTransactions }),
      ])
    },
    onError: (error) => toast.error('Không hoàn tất được đơn hàng', { description: errorMessage(error) }),
  })

  const cancelMutation = useMutation({
    mutationFn: (orderId: number) => api.delete<unknown>(`/orders/${orderId}`),
    onSuccess: async () => {
      toast.success('Đã huỷ đơn hàng')
      await queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.orders })
    },
    onError: (error) => toast.error('Không huỷ được đơn hàng', { description: errorMessage(error) }),
  })

  const orders = ordersQuery.data ?? []

  return (
    <>
      <PageIntro
        eyebrow="Doanh nghiệp"
        title="Đơn mua tín chỉ"
        description="Theo dõi đơn hàng, hoàn tất thanh toán hoặc huỷ các đơn đang ở trạng thái Chờ thanh toán. Phí sàn giao dịch cố định 5%."
      />

      {ordersQuery.isPending ? (
        <QueryLoading />
      ) : ordersQuery.isError ? (
        <QueryError onRetry={() => void ordersQuery.refetch()} />
      ) : (
        <SectionCard title="Lịch sử đơn hàng" description={`${orders.length} đơn hàng`}>
          {orders.length === 0 ? (
            <EmptyState icon={ShoppingCart} title="Chưa có đơn hàng" description="Các đơn mua tín chỉ sẽ hiển thị tại đây." />
          ) : (
            <div className="scrollbar-thin overflow-x-auto p-5">
              <table className="w-full min-w-[760px] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                    <th className="pb-3 font-semibold">Đơn hàng</th>
                    <th className="pb-3 font-semibold">Ngày tạo</th>
                    <th className="pb-3 font-semibold">Số tín chỉ</th>
                    <th className="pb-3 font-semibold">Tổng tiền</th>
                    <th className="pb-3 font-semibold">Phí sàn (5%)</th>
                    <th className="pb-3 font-semibold">Trạng thái</th>
                    <th className="pb-3 text-right font-semibold">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {orders.map((order) => {
                    const pending = order.status.toUpperCase() === 'PENDING'
                    return (
                      <tr key={order.id} className="text-ink-700">
                        <td className="py-3 font-semibold text-ink-900">#{order.id}</td>
                        <td className="py-3">{formatDate(order.createAt)}</td>
                        <td className="py-3">{formatNumber(order.creditAmount)}</td>
                        <td className="py-3">{formatUsd(order.totalAmount)}</td>
                        <td className="py-3 font-mono text-xs text-ink-600">{formatUsd(order.totalAmount * 0.05)}</td>
                        <td className="py-3"><StatusBadge status={order.status} kind="order" /></td>
                        <td className="py-3">
                          <div className="flex justify-end gap-2">
                            {pending ? (
                              <>
                                <Button
                                  size="sm"
                                  leftIcon={<CheckCircle2 className="size-4" />}
                                  loading={completeMutation.isPending && completeMutation.variables === order.id}
                                  onClick={() => completeMutation.mutate(order.id)}
                                >
                                  Hoàn tất
                                </Button>
                                <Button
                                  size="sm"
                                  variant="outline"
                                  leftIcon={<XCircle className="size-4" />}
                                  disabled={cancelMutation.isPending}
                                  onClick={() => {
                                    if (window.confirm(`Huỷ đơn hàng #${order.id}?`)) cancelMutation.mutate(order.id)
                                  }}
                                >
                                  Huỷ đơn
                                </Button>
                              </>
                            ) : (
                              <span className="text-xs text-ink-400">Không có thao tác</span>
                            )}
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </SectionCard>
      )}
    </>
  )
}
