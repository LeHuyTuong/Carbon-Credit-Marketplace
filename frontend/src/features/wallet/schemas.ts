import { z } from 'zod'

export const paymentDetailsSchema = z.object({
  bankName: z.string().trim().min(2, 'Vui lòng nhập tên ngân hàng'),
  accountNumber: z.string().trim().min(6, 'Số tài khoản phải có ít nhất 6 ký tự'),
  accountHolder: z.string().trim().min(2, 'Vui lòng nhập tên chủ tài khoản'),
  branch: z.string().trim().min(2, 'Vui lòng nhập chi nhánh'),
})

export type PaymentDetailsValues = z.infer<typeof paymentDetailsSchema>

export function withdrawalSchema(balance: number) {
  return z.object({
    amount: z
      .string()
      .min(1, 'Vui lòng nhập số tiền')
      .refine((value) => Number(value) > 0, 'Số tiền phải lớn hơn 0')
      .refine((value) => Number(value) <= balance, 'Số tiền vượt quá số dư ví'),
  })
}

export type WithdrawalValues = { amount: string }
