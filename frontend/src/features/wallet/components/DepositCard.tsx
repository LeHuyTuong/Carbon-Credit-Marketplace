import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { CreditCard, ExternalLink } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import { errorMessage, isRecord } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { Input } from '@/shared/components/Input'

type Gateway = 'VNPAY' | 'PAYPAL' | 'STRIPE'

const GATEWAYS: { value: Gateway; label: string; hint: string }[] = [
  { value: 'VNPAY', label: 'VNPay', hint: 'Chuyển khoản ngân hàng nội địa (VND)' },
  { value: 'STRIPE', label: 'Stripe', hint: 'Thẻ thanh toán quốc tế Visa / Mastercard' },
  { value: 'PAYPAL', label: 'PayPal', hint: 'Cổng thanh toán PayPal quốc tế' },
]

export function DepositCard() {
  const [amount, setAmount] = useState('100')
  const [method, setMethod] = useState<Gateway>('VNPAY')
  const [paymentUrl, setPaymentUrl] = useState<string | null>(null)
  const [error, setError] = useState('')

  const depositMutation = useMutation({
    mutationFn: async () => {
      const value = Number(amount)
      const data = await api.post<unknown>('/payment', {
        data: { amount: value, paymentMethod: method },
      })
      return isRecord(data) ? data : {}
    },
    onSuccess: (data) => {
      const url = typeof data.payment_url === 'string'
        ? data.payment_url
        : typeof data.paymentUrl === 'string'
          ? data.paymentUrl
          : null
      setPaymentUrl(url)
      toast.success('Đã khởi tạo lệnh nạp tiền qua cổng thanh toán')
    },
    onError: (err) => {
      toast.error('Chưa khởi tạo được lệnh nạp tiền', { description: errorMessage(err) })
    },
  })

  return (
    <div className="p-5">
      <form
        noValidate
        className="space-y-4"
        onSubmit={(event) => {
          event.preventDefault()
          const value = Number(amount)
          if (!Number.isFinite(value) || value < 2) {
            setError('Số tiền nạp tối thiểu 2 USD (tương đương 50.000 VND theo BR-33)')
            return
          }
          setError('')
          depositMutation.mutate()
        }}
      >
        <div className="grid gap-3 sm:grid-cols-3">
          {GATEWAYS.map((item) => (
            <button
              key={item.value}
              type="button"
              onClick={() => setMethod(item.value)}
              className={`rounded-xl border p-3 text-left transition ${
                method === item.value
                  ? 'border-brand-600 bg-brand-50/70 text-brand-900'
                  : 'border-ink-200 bg-white text-ink-700 hover:border-ink-300'
              }`}
            >
              <p className="text-sm font-bold">{item.label}</p>
              <p className="mt-0.5 text-xs text-ink-500">{item.hint}</p>
            </button>
          ))}
        </div>

        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <Input
            label="Số tiền muốn nạp (USD)"
            type="number"
            min="2"
            step="1"
            value={amount}
            onChange={(event) => {
              setAmount(event.target.value)
              setError('')
            }}
            hint="Không thu phí nạp/rút tiền (BR-03). Nạp tối thiểu 2 USD."
            error={error}
            wrapperClassName="sm:flex-1"
          />
          <Button
            type="submit"
            loading={depositMutation.isPending}
            leftIcon={<CreditCard aria-hidden className="size-4" />}
          >
            Nạp tiền vào ví
          </Button>
        </div>
      </form>

      {paymentUrl && (
        <div className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-xl border border-brand-200 bg-brand-50 p-3 text-xs text-brand-900">
          <span>Liên kết thanh toán đã sẵn sàng (hết hạn sau 10 phút theo BR-35).</span>
          <a
            href={paymentUrl}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1 font-semibold text-brand-700 underline"
          >
            Mở cổng thanh toán
            <ExternalLink className="size-3.5" />
          </a>
        </div>
      )}
    </div>
  )
}
