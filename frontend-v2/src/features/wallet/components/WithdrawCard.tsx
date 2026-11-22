import { useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowUpFromLine } from 'lucide-react'
import type { Withdrawal } from '@/shared/api/types'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate, formatUsd } from '@/shared/lib/format'
import { withdrawalSchema, type WithdrawalValues } from '../schemas'
import { useWithdraw } from '../useWallet'

export function WithdrawCard({ balance, withdrawals }: { balance: number; withdrawals: Withdrawal[] }) {
  const withdraw = useWithdraw()
  const schema = useMemo(() => withdrawalSchema(balance), [balance])

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<WithdrawalValues>({
    resolver: zodResolver(schema),
    defaultValues: { amount: '' },
  })

  return (
    <div className="p-5">
      <form
        noValidate
        onSubmit={handleSubmit(async (values) => {
          await withdraw.mutateAsync(Number(values.amount))
          reset()
        })}
        className="flex flex-col gap-4 sm:flex-row sm:items-end"
      >
        <Input
          label="Số tiền muốn rút (USD)"
          type="number"
          min="1"
          step="0.01"
          placeholder="100"
          hint={`Số dư khả dụng: ${formatUsd(balance)}`}
          error={errors.amount?.message}
          {...register('amount')}
          wrapperClassName="sm:flex-1"
        />
        <Button
          type="submit"
          loading={withdraw.isPending}
          leftIcon={<ArrowUpFromLine aria-hidden className="size-4" />}
        >
          Gửi yêu cầu rút tiền
        </Button>
      </form>

      <div className="mt-6 border-t border-ink-100 pt-4">
        <h4 className="mb-2 text-sm font-semibold text-ink-800">Lịch sử rút tiền</h4>
        {withdrawals.length === 0 ? (
          <EmptyState title="Chưa có yêu cầu rút tiền" className="py-8" />
        ) : (
          <ul className="divide-y divide-ink-100">
            {withdrawals.slice(0, 5).map((item) => (
              <li key={item.id} className="flex flex-wrap items-center justify-between gap-2 py-3">
                <div>
                  <p className="text-sm font-medium text-ink-900">{formatUsd(item.amount)}</p>
                  <p className="text-xs text-ink-400">{formatDate(item.createdAt)}</p>
                </div>
                <StatusBadge status={item.status} kind="withdrawal" />
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
