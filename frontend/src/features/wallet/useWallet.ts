import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import { mapList, mapPaymentDetails, mapTransaction, mapWallet, mapWithdrawal } from '@/shared/api/mappers'
import { queryKeys } from '@/shared/lib/queryKeys'

export interface PaymentDetailsInput {
  bankName: string
  accountNumber: string
  accountHolder: string
  branch: string
}

export function useWalletData() {
  const wallet = useQuery({
    queryKey: queryKeys.wallet,
    queryFn: async () => mapWallet(await api.get<unknown>('/wallet')),
    refetchOnMount: 'always',
    staleTime: 0,
  })
  const transactions = useQuery({
    queryKey: queryKeys.walletTransactions,
    queryFn: async () => mapList(await api.get<unknown>('/wallet/transactions'), mapTransaction),
    refetchOnMount: 'always',
    staleTime: 0,
  })
  // Tài khoản ngân hàng có thể chưa được thiết lập -> 404/empty là trạng thái hợp lệ.
  const paymentDetails = useQuery({
    queryKey: queryKeys.paymentDetails,
    queryFn: async () => mapPaymentDetails(await api.get<unknown>('/paymentDetails')),
    retry: false,
  })
  const withdrawals = useQuery({
    queryKey: queryKeys.withdrawals,
    queryFn: async () => mapList(await api.get<unknown>('/withdrawal'), mapWithdrawal),
    retry: false,
  })

  return { wallet, transactions, paymentDetails, withdrawals }
}

export function useSavePaymentDetails() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: PaymentDetailsInput) =>
      api.post<unknown>('/paymentDetails', { data: input }),
    onSuccess: async () => {
      toast.success('Đã lưu tài khoản nhận tiền')
      await queryClient.invalidateQueries({ queryKey: queryKeys.paymentDetails })
    },
  })
}

export function useWithdraw() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (amount: number) => api.post<unknown>(`/withdrawal/${amount}`),
    onSuccess: async () => {
      toast.success('Yêu cầu rút tiền đã được gửi', {
        description: 'Hệ thống sẽ xử lý sau khi tài khoản ngân hàng được đối soát.',
      })
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.wallet }),
        queryClient.invalidateQueries({ queryKey: queryKeys.withdrawals }),
      ])
    },
  })
}
