import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Download, FileSpreadsheet, RotateCcw, Send } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import {
  errorMessage,
  mapEmissionReport,
  mapList,
} from '@/shared/api/phase3b'
import {
  downloadPayoutExcel,
  usePayoutPreview,
} from '@/features/payouts/usePayouts'
import { Button } from '@/shared/components/Button'
import { Card } from '@/shared/components/Card'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate, formatNumber, formatUsd } from '@/shared/lib/format'
import { queryKeys } from '@/shared/lib/queryKeys'
import { PageIntro } from './components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from './components/QueryState'

interface PayoutFormula {
  pricingMode?: string
  unitPrice?: number
  unitPricePerCredit?: number
  unitPricePerKwh?: number
  minPayout?: number
}

interface ProfitDistributionItem {
  id: number
  status: string
  emissionReportId?: number | null
  reportPeriod?: string | null
  totalMoneyDistributed: number
  unsettledOwners: number
  createdAt?: string
  retryable: boolean
}

export function PayoutsPage() {
  const queryClient = useQueryClient()
  const [searchTerm, setSearchTerm] = useState('')
  const [reportId, setReportId] = useState<number | null>(null)

  const reportsQuery = useQuery({
    queryKey: queryKeys.phase3b.myReports,
    queryFn: async () => mapList(await api.get<unknown>('/reports/my-reports'), mapEmissionReport),
  })

  const formulaQuery = useQuery({
    queryKey: ['company', 'payout-formula'],
    queryFn: () => api.get<PayoutFormula>('/companies/payout-formula'),
    retry: false,
  })

  const distributionsQuery = useQuery({
    queryKey: ['company', 'profit-distributions'],
    queryFn: async () => {
      const data = await api.get<ProfitDistributionItem[]>('/profit-sharing/distributions')
      return Array.isArray(data) ? data : []
    },
    retry: false,
  })

  const eligibleReports = useMemo(() => {
    const all = reportsQuery.data ?? []
    return all.filter((r) => ['CREDIT_ISSUED', 'ADMIN_APPROVED', 'PAID_OUT'].includes(r.status.toUpperCase()))
  }, [reportsQuery.data])

  useEffect(() => {
    if (reportId === null && eligibleReports.length > 0) {
      const issued = eligibleReports.find((r) => r.status.toUpperCase() === 'CREDIT_ISSUED')
      setReportId((issued ?? eligibleReports[0]).id)
    }
  }, [eligibleReports, reportId])

  const preview = usePayoutPreview(reportId)

  const filteredRows = (preview.data?.rows ?? []).filter((row) => {
    if (!searchTerm.trim()) return true
    const term = searchTerm.toLowerCase()
    return (
      row.ownerName.toLowerCase().includes(term) ||
      row.email.toLowerCase().includes(term) ||
      row.phone.toLowerCase().includes(term)
    )
  })

  const shareMutation = useMutation({
    mutationFn: (emissionReportId: number) =>
      api.post<unknown>('/profit-sharing/share', {
        data: { emissionReportId },
      }),
    onSuccess: async () => {
      toast.success('Đã kích hoạt chia sẻ doanh thu về ví các chủ xe điện')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['company', 'profit-distributions'] }),
        queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.myReports }),
        queryClient.invalidateQueries({ queryKey: queryKeys.wallet }),
      ])
    },
    onError: (error) => {
      toast.error('Không thể chia sẻ doanh thu', { description: errorMessage(error) })
    },
  })

  const retryMutation = useMutation({
    mutationFn: (distributionId: number) =>
      api.post<unknown>(`/profit-sharing/distributions/${distributionId}/retry`),
    onSuccess: async () => {
      toast.success('Đã gửi lại lệnh chi trả cho các chủ xe chưa nhận')
      await queryClient.invalidateQueries({ queryKey: ['company', 'profit-distributions'] })
    },
    onError: (error) => {
      toast.error('Không gửi lại được lệnh chi trả', { description: errorMessage(error) })
    },
  })

  const selectedReport = eligibleReports.find((r) => r.id === reportId)
  const formula = formulaQuery.data
  const distributions = distributionsQuery.data ?? []

  return (
    <>
      <PageIntro
        eyebrow="Thanh toán"
        title="Chia sẻ doanh thu cho chủ xe điện"
        description="Chọn báo cáo phát thải đã cấp tín chỉ để xem bảng phân bổ từng chủ xe, thực hiện chi trả và xuất Excel đối soát."
      />

      {formula && (
        <div className="mb-6 grid gap-4 sm:grid-cols-3">
          <Card className="p-4">
            <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Chế độ tính chi trả</p>
            <p className="mt-1.5 text-lg font-bold text-ink-900">
              {formula.pricingMode === 'CREDIT' ? 'Theo tín chỉ quy đổi' : formula.pricingMode || 'Theo tín chỉ'}
            </p>
            <p className="mt-0.5 text-xs text-ink-500">Áp dụng thống nhất theo chính sách doanh nghiệp</p>
          </Card>
          <Card className="p-4">
            <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Đơn giá mỗi tín chỉ</p>
            <p className="mt-1.5 text-lg font-bold text-brand-700">
              {formatUsd(formula.unitPricePerCredit ?? formula.unitPrice ?? 0)} / tCO₂e
            </p>
            <p className="mt-0.5 text-xs text-ink-500">
              {formula.unitPricePerKwh && formula.unitPricePerKwh > 0
                ? `Tương đương ${formula.unitPricePerKwh < 0.01 ? `$${formula.unitPricePerKwh.toFixed(4)}` : formatUsd(formula.unitPricePerKwh)} / kWh`
                : 'Quy đổi theo phương pháp luận VM0038'}
            </p>
          </Card>
          <Card className="p-4">
            <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Báo cáo đủ điều kiện</p>
            <p className="mt-1.5 text-lg font-bold text-ink-900">{eligibleReports.length} báo cáo</p>
            <p className="mt-0.5 text-xs text-ink-500">Đã được Admin duyệt & phát hành tín chỉ</p>
          </Card>
        </div>
      )}

      <Card className="mb-6 p-5">
        <div className="grid gap-4 lg:grid-cols-[1fr_auto] lg:items-end">
          <div>
            <label htmlFor="payout-report-select" className="mb-1.5 block text-sm font-medium text-ink-700">
              Chọn báo cáo phát thải cần xem phân bổ / chi trả
            </label>
            <select
              id="payout-report-select"
              value={reportId ?? ''}
              onChange={(e) => setReportId(e.target.value ? Number(e.target.value) : null)}
              className="h-11 w-full rounded-xl border border-ink-200 bg-white px-3.5 text-sm text-ink-900 outline-none focus:border-brand-500 focus:ring-3 focus:ring-brand-500/15"
            >
              {eligibleReports.length === 0 ? (
                <option value="">Chưa có báo cáo nào đủ điều kiện chia doanh thu</option>
              ) : (
                eligibleReports.map((r) => (
                  <option key={r.id} value={r.id}>
                    Kỳ {r.period || '—'} · {r.projectName || `Dự án #${r.projectId}`} ({r.status === 'PAID_OUT' ? 'Đã chi trả' : 'Sẵn sàng chia'})
                  </option>
                ))
              )}
            </select>
          </div>

          <Button
            variant="primary"
            disabled={!reportId || selectedReport?.status.toUpperCase() === 'PAID_OUT'}
            loading={shareMutation.isPending}
            leftIcon={<Send aria-hidden className="size-4" />}
            onClick={() => {
              if (reportId) shareMutation.mutate(reportId)
            }}
          >
            {selectedReport?.status.toUpperCase() === 'PAID_OUT' ? 'Đã chi trả kỳ này' : 'Chia sẻ cho chủ xe'}
          </Button>
        </div>
      </Card>

      {reportId === null ? (
        <Card className="mb-6">
          <EmptyState
            icon={FileSpreadsheet}
            title="Chưa có báo cáo phát thải đủ điều kiện"
            description="Báo cáo cần được CVA thẩm định và Admin phát hành tín chỉ trước khi chia sẻ doanh thu cho chủ xe điện."
          />
        </Card>
      ) : preview.isPending ? (
        <div className="mb-6 h-64 animate-pulse rounded-card bg-ink-200/60" />
      ) : preview.isError || !preview.data ? (
        <div className="mb-6">
          <QueryError
            title="Không xem được bảng thanh toán"
            description="Kiểm tra trạng thái báo cáo và quyền COMPANY của tài khoản."
            onRetry={() => void preview.refetch()}
          />
        </div>
      ) : preview.data.rows.length === 0 ? (
        <Card className="mb-6">
          <EmptyState title="Báo cáo này chưa có dữ liệu phân bổ chủ xe" />
        </Card>
      ) : (
        <Card className="mb-6">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ink-100 p-5">
            <div>
              <h3 className="text-base font-semibold text-ink-900">
                Bảng phân bổ · Kỳ {selectedReport?.period || `#${preview.data.reportId}`}
              </h3>
              <p className="mt-1 text-sm text-ink-500">
                {formatNumber(preview.data.rows.length)} chủ xe điện được phân bổ
              </p>
            </div>
            <div className="text-right">
              <p className="text-xs text-ink-500">Tổng ngân sách chi trả</p>
              <p className="text-xl font-bold text-brand-700">
                {formatUsd(preview.data.totalAmount)}
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 border-b border-ink-100 bg-ink-50/50 p-4 sm:grid-cols-4">
            <div>
              <span className="text-xs text-ink-500">Tổng chủ xe:</span>
              <p className="font-semibold text-ink-900">{formatNumber(preview.data.summary?.ownersCount ?? preview.data.rows.length)}</p>
            </div>
            <div>
              <span className="text-xs text-ink-500">Tổng điện năng sạc:</span>
              <p className="font-semibold text-ink-900">{formatNumber(preview.data.summary?.totalEnergyKwh ?? 0)} kWh</p>
            </div>
            <div>
              <span className="text-xs text-ink-500">Tổng tín chỉ phân bổ:</span>
              <p className="font-semibold text-ink-900">{formatNumber(preview.data.summary?.totalCredits ?? 0)} tCO₂e</p>
            </div>
            <div>
              <span className="text-xs text-ink-500">Trạng thái báo cáo:</span>
              <div className="mt-0.5">
                <StatusBadge status={selectedReport?.status ?? 'CREDIT_ISSUED'} kind="emission" />
              </div>
            </div>
          </div>

          <div className="border-b border-ink-100 p-4">
            <Input
              placeholder="Tìm kiếm theo tên chủ xe, email hoặc số điện thoại..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-sm">
              <thead className="bg-ink-50 text-left text-xs uppercase tracking-wider text-ink-500">
                <tr>
                  <th scope="col" className="px-5 py-3 font-semibold">Chủ xe điện</th>
                  <th scope="col" className="px-5 py-3 font-semibold">Liên hệ</th>
                  <th scope="col" className="px-5 py-3 text-center font-semibold">Số xe</th>
                  <th scope="col" className="px-5 py-3 text-right font-semibold">Điện sạc (kWh)</th>
                  <th scope="col" className="px-5 py-3 text-right font-semibold">Tín chỉ (tCO₂e)</th>
                  <th scope="col" className="px-5 py-3 text-right font-semibold">Thành tiền</th>
                  <th scope="col" className="px-5 py-3 font-semibold">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100">
                {filteredRows.map((row, index) => (
                  <tr key={`${row.email || 'row'}-${index}`} className="hover:bg-ink-50/60">
                    <td className="px-5 py-3">
                      <p className="font-medium text-ink-900">{row.ownerName}</p>
                    </td>
                    <td className="px-5 py-3 text-ink-600">
                      <div>{row.email || '—'}</div>
                      <div className="text-xs text-ink-400">{row.phone}</div>
                    </td>
                    <td className="px-5 py-3 text-center font-medium text-ink-800">
                      {row.vehiclesCount}
                    </td>
                    <td className="px-5 py-3 text-right text-ink-700">
                      {formatNumber(row.energyKwh)}
                    </td>
                    <td className="px-5 py-3 text-right font-medium text-ink-900">
                      {formatNumber(row.credits)}
                    </td>
                    <td className="px-5 py-3 text-right font-bold text-brand-700">
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

      <SectionCard
        title="Lịch sử các đợt phân phối doanh thu"
        description={`${distributions.length} đợt chi trả đã thực hiện · Bấm Xuất Excel để đối soát`}
      >
        {distributionsQuery.isPending ? (
          <div className="p-5"><QueryLoading /></div>
        ) : distributions.length === 0 ? (
          <EmptyState
            icon={FileSpreadsheet}
            title="Chưa có đợt phân phối nào"
            description="Sau khi bấm Chia sẻ cho chủ xe, lịch sử và nút tải Excel đối soát sẽ hiển thị tại đây."
          />
        ) : (
          <div className="scrollbar-thin overflow-x-auto p-5">
            <table className="w-full min-w-[740px] text-left text-sm">
              <thead>
                <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                  <th className="pb-3 font-semibold">Đợt chi trả</th>
                  <th className="pb-3 font-semibold">Kỳ báo cáo</th>
                  <th className="pb-3 font-semibold">Tổng tiền đã chia</th>
                  <th className="pb-3 font-semibold">Chủ xe chưa nhận</th>
                  <th className="pb-3 font-semibold">Trạng thái</th>
                  <th className="pb-3 font-semibold">Ngày tạo</th>
                  <th className="pb-3 text-right font-semibold">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100">
                {distributions.map((dist) => (
                  <tr key={dist.id} className="text-ink-700">
                    <td className="py-3 font-mono font-semibold text-ink-900">#{dist.id}</td>
                    <td className="py-3">{dist.reportPeriod || `Báo cáo #${dist.emissionReportId ?? '—'}`}</td>
                    <td className="py-3 font-mono font-semibold text-brand-700">
                      {formatUsd(dist.totalMoneyDistributed)}
                    </td>
                    <td className="py-3">{dist.unsettledOwners}</td>
                    <td className="py-3">
                      <StatusBadge status={dist.status} />
                    </td>
                    <td className="py-3 text-xs text-ink-500">{formatDate(dist.createdAt)}</td>
                    <td className="py-3">
                      <div className="flex justify-end gap-2">
                        {dist.retryable && (
                          <Button
                            size="sm"
                            variant="outline"
                            loading={retryMutation.isPending && retryMutation.variables === dist.id}
                            leftIcon={<RotateCcw className="size-3.5" />}
                            onClick={() => retryMutation.mutate(dist.id)}
                          >
                            Gửi lại
                          </Button>
                        )}
                        <Button
                          size="sm"
                          variant="outline"
                          leftIcon={<Download className="size-3.5" />}
                          onClick={() => void downloadPayoutExcel(dist.id)}
                        >
                          Xuất Excel .xlsx
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </SectionCard>
    </>
  )
}
