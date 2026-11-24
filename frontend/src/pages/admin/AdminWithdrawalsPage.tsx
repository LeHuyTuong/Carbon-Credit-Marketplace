import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Banknote, CheckCircle2, Landmark, RefreshCw, XCircle } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import { errorMessage, mapAdminWithdrawal, mapList } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'
import { EmptyState } from '@/shared/components/EmptyState'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate, formatUsd } from '@/shared/lib/format'
import { queryKeys } from '@/shared/lib/queryKeys'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

export function AdminWithdrawalsPage() {
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [confirmAction, setConfirmAction] = useState<{ id: number; accept: boolean } | null>(null)

  const withdrawalsQuery = useQuery({
    queryKey: queryKeys.phase3b.adminWithdrawals,
    queryFn: async () => mapList(await api.get<unknown>('/withdrawal/admin'), mapAdminWithdrawal),
  })

  const processMutation = useMutation({
    mutationFn: async ({ id, accept }: { id: number; accept: boolean }) =>
      api.patch<unknown>(`/withdrawal/admin/${id}/process/${accept}`),
    onSuccess: async (_data, variables) => {
      toast.success(variables.accept ? 'Đã duyệt yêu cầu rút tiền' : 'Đã từ chối yêu cầu rút tiền')
      await queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.adminWithdrawals })
    },
    onError: (error) => toast.error('Không xử lý được yêu cầu', { description: errorMessage(error) }),
  })

  const withdrawals = withdrawalsQuery.data ?? []
  const filtered = useMemo(
    () => (statusFilter === 'ALL' ? withdrawals : withdrawals.filter((item) => item.status === statusFilter)),
    [withdrawals, statusFilter],
  )
  const pendingCount = withdrawals.filter((item) => item.status === 'PENDING').length

  return (
    <>
      <PageIntro
        eyebrow="Admin"
        title="Duyệt rút tiền"
        description="Kiểm tra thông tin tài khoản ngân hàng, số dư ví và quyết định duyệt hoặc từ chối từng yêu cầu rút tiền."
        actions={
          <Button
            variant="outline"
            leftIcon={<RefreshCw className="size-4" />}
            onClick={() => void withdrawalsQuery.refetch()}
          >
            Làm mới
          </Button>
        }
      />

      {withdrawalsQuery.isPending ? (
        <QueryLoading />
      ) : withdrawalsQuery.isError ? (
        <QueryError onRetry={() => void withdrawalsQuery.refetch()} />
      ) : (
        <SectionCard
          title="Yêu cầu rút tiền"
          description={`${withdrawals.length} yêu cầu · ${pendingCount} chờ xử lý`}
          action={
            <select
              aria-label="Lọc theo trạng thái"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
              className="h-9 rounded-xl border border-ink-200 bg-white px-3 text-xs font-medium text-ink-700"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="PENDING">Chờ duyệt</option>
              <option value="APPROVED">Đã duyệt</option>
              <option value="SUCCEEDED">Đã chi trả</option>
              <option value="REJECTED">Từ chối</option>
              <option value="FAILED">Thất bại</option>
            </select>
          }
        >
          {filtered.length === 0 ? (
            <EmptyState icon={Banknote} title="Không có yêu cầu" description="Yêu cầu rút tiền sẽ hiển thị tại đây." />
          ) : (
            <div className="grid gap-4 p-5 lg:grid-cols-2">
              {filtered.map((withdrawal) => {
                const pending = withdrawal.status.toUpperCase() === 'PENDING'
                return (
                  <article key={withdrawal.id} className="rounded-xl border border-ink-200 p-4">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <h3 className="text-lg font-bold text-ink-900">{formatUsd(withdrawal.amount)}</h3>
                        <p className="mt-0.5 text-xs text-ink-500">
                          Yêu cầu #{withdrawal.id} · {formatDate(withdrawal.requestedAt)}
                        </p>
                      </div>
                      <StatusBadge status={withdrawal.status} kind="withdrawal" />
                    </div>

                    <dl className="mt-4 space-y-2 rounded-xl bg-ink-50 p-3 text-sm">
                      <div className="flex justify-between gap-3">
                        <dt className="text-ink-500">Người yêu cầu</dt>
                        <dd className="text-right font-medium text-ink-800">
                          {withdrawal.userName || withdrawal.userEmail || '—'}
                        </dd>
                      </div>
                      <div className="flex justify-between gap-3">
                        <dt className="text-ink-500">Email</dt>
                        <dd className="truncate text-right font-medium text-ink-800">{withdrawal.userEmail || '—'}</dd>
                      </div>
                      <div className="flex justify-between gap-3">
                        <dt className="text-ink-500">Ngân hàng</dt>
                        <dd className="text-right font-medium text-ink-800">
                          {withdrawal.bankName ? `${withdrawal.bankName}${withdrawal.branch ? ` · ${withdrawal.branch}` : ''}` : '—'}
                        </dd>
                      </div>
                      <div className="flex justify-between gap-3">
                        <dt className="text-ink-500">Số tài khoản</dt>
                        <dd className="text-right font-medium text-ink-800">{withdrawal.accountNumber || '—'}</dd>
                      </div>
                      <div className="flex justify-between gap-3">
                        <dt className="text-ink-500">Chủ tài khoản</dt>
                        <dd className="text-right font-medium text-ink-800">{withdrawal.accountHolder || '—'}</dd>
                      </div>
                      {withdrawal.walletBalance !== null && (
                        <div className="flex justify-between gap-3 border-t border-ink-200 pt-2">
                          <dt className="flex items-center gap-1.5 text-ink-500">
                            <Landmark className="size-3.5" /> Số dư ví
                          </dt>
                          <dd className="text-right font-semibold text-ink-900">
                            {formatUsd(withdrawal.walletBalance)}
                          </dd>
                        </div>
                      )}
                    </dl>

                    {withdrawal.processedAt && (
                      <p className="mt-2 text-xs text-ink-500">Xử lý ngày {formatDate(withdrawal.processedAt)}</p>
                    )}

                    <div className="mt-4 flex flex-wrap gap-2 border-t border-ink-100 pt-4">
                      {pending ? (
                        <>
                          <Button
                            size="sm"
                            leftIcon={<CheckCircle2 className="size-4" />}
                            loading={processMutation.isPending && processMutation.variables?.id === withdrawal.id && processMutation.variables.accept}
                            onClick={() => setConfirmAction({ id: withdrawal.id, accept: true })}
                          >
                            Duyệt chi trả
                          </Button>
                          <Button
                            size="sm"
                            variant="danger"
                            leftIcon={<XCircle className="size-4" />}
                            loading={processMutation.isPending && processMutation.variables?.id === withdrawal.id && !processMutation.variables.accept}
                            onClick={() => setConfirmAction({ id: withdrawal.id, accept: false })}
                          >
                            Từ chối
                          </Button>
                        </>
                      ) : (
                        <span className="text-xs text-ink-400">Yêu cầu đã được xử lý</span>
                      )}
                    </div>
                  </article>
                )
              })}
            </div>
          )}
        </SectionCard>
      )}

      <ConfirmDialog
        open={confirmAction !== null}
        title={
          confirmAction?.accept
            ? `Duyệt chi trả yêu cầu rút tiền #${confirmAction.id}?`
            : `Từ chối yêu cầu rút tiền #${confirmAction?.id ?? ''}?`
        }
        description={
          confirmAction?.accept
            ? 'Số tiền sẽ được xác nhận chuyển khoản tới tài khoản ngân hàng của người dùng.'
            : 'Số tiền tạm giữ sẽ được hoàn trả lại vào số dư khả dụng trên ví người dùng.'
        }
        confirmLabel={confirmAction?.accept ? 'Duyệt chi trả' : 'Từ chối yêu cầu'}
        variant={confirmAction?.accept ? 'primary' : 'danger'}
        loading={processMutation.isPending}
        onCancel={() => setConfirmAction(null)}
        onConfirm={() => {
          if (confirmAction) {
            processMutation.mutate(confirmAction, {
              onSettled: () => setConfirmAction(null),
            })
          }
        }}
      />
    </>
  )
}
