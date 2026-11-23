import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Download, FileSpreadsheet, Search, Send } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import { errorMessage } from '@/shared/api/phase3b'
import {
  downloadPayoutExcel,
  usePayoutPreview,
} from '@/features/payouts/usePayouts'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatNumber, formatUsd } from '@/shared/lib/format'
import { PageIntro } from './components/PageIntro'
import { QueryError } from './components/QueryState'

function parseId(value: string): number | null {
  const parsed = Number(value.trim())
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

export function PayoutsPage() {
  const [reportInput, setReportInput] = useState('')
  const [distributionInput, setDistributionInput] = useState('')
  const [reportId, setReportId] = useState<number | null>(null)
  const preview = usePayoutPreview(reportId)

  const shareMutation = useMutation({
    mutationFn: (emissionReportId: number) =>
      api.post<unknown>('/profit-sharing/share', {
        data: { emissionReportId },
      }),
    onSuccess: () => {
      toast.success('Đã kích hoạt chia sẻ doanh thu về ví các chủ xe điện')
    },
    onError: (error) => {
      toast.error('Không thể chia sẻ doanh thu', { description: errorMessage(error) })
    },
  })

  const triggerShare = () => {
    const id = parseId(reportInput)
    if (!id) {
      toast.error('Mã báo cáo không hợp lệ', { description: 'Nhập một số nguyên dương.' })
      return
    }
    shareMutation.mutate(id)
  }

  const submitSearch = () => {
    const id = parseId(reportInput)
    if (!id) {
      toast.error('Mã báo cáo không hợp lệ', { description: 'Nhập một số nguyên dương.' })
      return
    }
    setReportId(id)
  }

  const exportExcel = async () => {
    const id = parseId(distributionInput)
    if (!id) {
      toast.error('Mã phân phối không hợp lệ')
      return
    }
    await downloadPayoutExcel(id)
  }

  return (
    <>
      <PageIntro
        eyebrow="Thanh toán"
        title="Xem trước & xuất bảng thanh toán"
        description="Tra cứu phân bổ tiền cho chủ xe điện theo báo cáo phát thải, sau đó xuất file Excel đối soát."
      />

      <Card className="mb-6 p-5">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-[1fr_1fr_auto_auto_auto] lg:items-end">
          <Input
            label="Mã báo cáo phát thải"
            inputMode="numeric"
            placeholder="Ví dụ: 2"
            value={reportInput}
            onChange={(event) => setReportInput(event.target.value)}
          />
          <Input
            label="Mã phân phối (để xuất Excel)"
            inputMode="numeric"
            placeholder="Ví dụ: 1"
            value={distributionInput}
            onChange={(event) => setDistributionInput(event.target.value)}
          />
          <Button leftIcon={<Search aria-hidden className="size-4" />} onClick={submitSearch}>
            Xem trước
          </Button>
          <Button
            variant="secondary"
            loading={shareMutation.isPending}
            leftIcon={<Send aria-hidden className="size-4" />}
            onClick={triggerShare}
          >
            Chia sẻ cho chủ xe
          </Button>
          <Button
            variant="outline"
            leftIcon={<Download aria-hidden className="size-4" />}
            onClick={() => void exportExcel()}
          >
            Xuất Excel .xlsx
          </Button>
        </div>
      </Card>

      {reportId === null ? (
        <Card>
          <EmptyState
            icon={FileSpreadsheet}
            title="Nhập mã báo cáo để xem trước"
            description="Hệ thống sẽ tính toán phân bổ cho từng chủ xe điện theo chính sách thanh toán hiện hành."
          />
        </Card>
      ) : preview.isPending ? (
        <div className="h-64 animate-pulse rounded-card bg-ink-200/60" />
      ) : preview.isError || !preview.data ? (
        <QueryError
          title="Không xem được bảng thanh toán"
          description="Kiểm tra mã báo cáo và quyền COMPANY của tài khoản."
          onRetry={() => void preview.refetch()}
        />
      ) : preview.data.rows.length === 0 ? (
        <Card>
          <EmptyState title="Báo cáo này chưa có dữ liệu phân bổ" />
        </Card>
      ) : (
        <Card>
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ink-100 p-5">
            <div>
              <h3 className="text-base font-semibold text-ink-900">
                Báo cáo #{preview.data.reportId}
              </h3>
              <p className="mt-1 text-sm text-ink-500">
                {formatNumber(preview.data.rows.length)} chủ xe điện được phân bổ
              </p>
            </div>
            <div className="text-right">
              <p className="text-xs text-ink-500">Tổng tiền thanh toán</p>
              <p className="text-xl font-bold text-brand-700">
                {formatUsd(preview.data.totalAmount)}
              </p>
            </div>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] text-sm">
              <thead className="bg-ink-50 text-left text-xs uppercase tracking-wider text-ink-500">
                <tr>
                  <th scope="col" className="px-5 py-3 font-semibold">Chủ xe điện</th>
                  <th scope="col" className="px-5 py-3 font-semibold">Email</th>
                  <th scope="col" className="px-5 py-3 text-right font-semibold">Tín chỉ</th>
                  <th scope="col" className="px-5 py-3 text-right font-semibold">Thành tiền</th>
                  <th scope="col" className="px-5 py-3 font-semibold">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100">
                {preview.data.rows.map((row, index) => (
                  <tr key={`${row.email || 'row'}-${index}`} className="hover:bg-ink-50/60">
                    <td className="px-5 py-3 font-medium text-ink-900">{row.ownerName}</td>
                    <td className="px-5 py-3 text-ink-500">{row.email || '—'}</td>
                    <td className="px-5 py-3 text-right">{formatNumber(row.credits)}</td>
                    <td className="px-5 py-3 text-right font-semibold text-ink-900">
                      {formatUsd(row.amount)}
                    </td>
                    <td className="px-5 py-3">
                      <StatusBadge status={row.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </>
  )
}
