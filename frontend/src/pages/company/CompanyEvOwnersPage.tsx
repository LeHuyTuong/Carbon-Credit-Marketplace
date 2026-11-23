import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Car, Eye, FileText, RefreshCw, Search, ShieldCheck, Users } from 'lucide-react'
import { api } from '@/shared/api/client'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

interface VehicleSummary {
  id: number
  plateNumber: string
  brand: string
  model: string
  documentFile?: string
}

interface CompanyEvOwner {
  ownerId: number
  fullName: string
  email: string
  vehicleCount: number
  vehicles: VehicleSummary[]
}

export function CompanyEvOwnersPage() {
  const [search, setSearch] = useState('')
  const [selectedOwner, setSelectedOwner] = useState<CompanyEvOwner | null>(null)

  const ownersQuery = useQuery({
    queryKey: ['company', 'ev-owners'],
    queryFn: async () => {
      const res = await api.get<CompanyEvOwner[]>('/vehicles/company/summary')
      return Array.isArray(res) ? res : []
    },
  })

  const owners = ownersQuery.data ?? []

  const totalOwners = owners.length
  const totalVehicles = owners.reduce((sum, o) => sum + (o.vehicleCount || o.vehicles?.length || 0), 0)

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return owners
    return owners.filter(
      (o) =>
        o.fullName?.toLowerCase().includes(q) ||
        o.email?.toLowerCase().includes(q) ||
        o.vehicles?.some(
          (v) =>
            v.plateNumber?.toLowerCase().includes(q) ||
            v.brand?.toLowerCase().includes(q) ||
            v.model?.toLowerCase().includes(q),
        ),
    )
  }, [owners, search])

  return (
    <>
      <PageIntro
        eyebrow="Doanh nghiệp"
        title="Danh sách chủ xe điện tham gia"
        description="Theo dõi danh sách chủ phương tiện đã ủy quyền cho doanh nghiệp gộp phát thải, đo lường tín chỉ carbon và phân phối doanh thu."
        actions={
          <Button
            variant="outline"
            leftIcon={<RefreshCw className="size-4" />}
            onClick={() => void ownersQuery.refetch()}
          >
            Làm mới
          </Button>
        }
      />

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-ink-100 bg-white p-5 shadow-xs">
          <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Chủ xe điện liên kết</p>
          <p className="mt-2 text-2xl font-bold text-ink-900">{totalOwners} người dùng</p>
          <p className="mt-1 text-xs text-ink-500">Đã ký hợp đồng ủy quyền dữ liệu phát thải</p>
        </div>
        <div className="rounded-2xl border border-ink-100 bg-white p-5 shadow-xs">
          <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Tổng xe điện gom nhóm</p>
          <p className="mt-2 text-2xl font-bold text-ink-900">{totalVehicles} phương tiện</p>
          <p className="mt-1 text-xs text-ink-500">Tích hợp vào các đợt phát hành tín chỉ</p>
        </div>
        <div className="rounded-2xl border border-ink-100 bg-white p-5 shadow-xs">
          <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Cơ chế chia sẻ doanh thu</p>
          <p className="mt-2 text-2xl font-bold text-brand-700">Tự động (VM0038)</p>
          <p className="mt-1 text-xs text-ink-500">Tính theo tỷ lệ kWh sạc thực tế</p>
        </div>
      </div>

      {ownersQuery.isPending ? (
        <QueryLoading />
      ) : ownersQuery.isError ? (
        <QueryError onRetry={() => void ownersQuery.refetch()} />
      ) : (
        <SectionCard
          title="Chủ xe điện trong mạng lưới"
          description={`${totalOwners} chủ xe · ${filtered.length} kết quả tìm kiếm`}
        >
          <div className="border-b border-ink-100 p-5">
            <Input
              label="Tìm kiếm chủ xe hoặc phương tiện"
              placeholder="Tên, email hoặc biển số xe (51K-..., 30K-...)"
              value={search}
              icon={<Search className="size-4" />}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {filtered.length === 0 ? (
            <EmptyState
              icon={Users}
              title="Không tìm thấy chủ xe điện"
              description="Thử tìm theo tên hoặc biển số xe khác."
            />
          ) : (
            <div className="scrollbar-thin overflow-x-auto p-5">
              <table className="w-full min-w-[700px] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                    <th className="pb-3 font-semibold">Chủ xe điện</th>
                    <th className="pb-3 font-semibold">Số lượng xe</th>
                    <th className="pb-3 font-semibold">Phương tiện đăng ký</th>
                    <th className="pb-3 font-semibold">Định danh KYC</th>
                    <th className="pb-3 text-right font-semibold">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {filtered.map((o) => (
                    <tr key={o.ownerId} className="text-ink-700">
                      <td className="py-3">
                        <p className="font-semibold text-ink-900">{o.fullName}</p>
                        <p className="text-xs text-ink-500">{o.email}</p>
                      </td>
                      <td className="py-3 font-bold text-ink-800">{o.vehicleCount} xe</td>
                      <td className="py-3">
                        <div className="flex flex-wrap gap-1.5">
                          {o.vehicles?.map((v) => (
                            <span
                              key={v.id}
                              className="inline-flex items-center gap-1 rounded-md bg-ink-100 px-2 py-0.5 font-mono text-xs font-semibold text-ink-800"
                            >
                              <Car className="size-3 text-ink-500" />
                              {v.plateNumber}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="py-3">
                        <StatusBadge status="ACTIVE" label="Đã xác thực" />
                      </td>
                      <td className="py-3 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          leftIcon={<Eye className="size-3.5" />}
                          onClick={() => setSelectedOwner(o)}
                        >
                          Xem xe ({o.vehicleCount})
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

      {selectedOwner && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/40 p-4 backdrop-blur-xs"
          onClick={() => setSelectedOwner(null)}
        >
          <div
            className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-ink-100 pb-4">
              <div className="flex items-center gap-2.5">
                <div className="grid size-10 place-items-center rounded-xl bg-brand-50 text-brand-600">
                  <Users className="size-5" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-ink-900">{selectedOwner.fullName}</h3>
                  <p className="text-xs text-ink-500">{selectedOwner.email} · ID #{selectedOwner.ownerId}</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setSelectedOwner(null)}
                className="rounded-lg p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-700"
              >
                ✕
              </button>
            </div>

            <div className="mt-5 space-y-4 text-sm">
              <h4 className="font-semibold text-ink-900">Danh sách xe điện ủy quyền ({selectedOwner.vehicles?.length ?? 0}):</h4>
              <div className="space-y-3">
                {selectedOwner.vehicles?.map((v) => (
                  <div key={v.id} className="rounded-xl border border-ink-200 bg-ink-50/50 p-3.5">
                    <div className="flex items-center justify-between">
                      <span className="font-mono text-base font-bold text-brand-700">{v.plateNumber}</span>
                      <span className="rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-semibold text-emerald-800">
                        Đang hoạt động
                      </span>
                    </div>
                    <p className="mt-1 text-sm font-medium text-ink-800">{v.brand} {v.model}</p>
                    {v.documentFile && (
                      <p className="mt-2 flex items-center gap-1.5 text-xs text-ink-500">
                        <FileText className="size-3.5 text-ink-400" />
                        Hồ sơ đính kèm: <span className="font-mono text-ink-700">{v.documentFile}</span>
                      </p>
                    )}
                  </div>
                ))}
              </div>

              <div className="rounded-xl border border-brand-200 bg-brand-50/50 p-3.5">
                <p className="flex items-center gap-1.5 text-xs font-semibold text-brand-900">
                  <ShieldCheck className="size-4 text-brand-600" />
                  Quyền lợi chia sẻ doanh thu tín chỉ
                </p>
                <p className="mt-1 text-xs text-brand-700">
                  Chủ xe điện này tự động nhận tiền vào ví khi công ty bán các lô tín chỉ carbon phát hành từ dự án.
                </p>
              </div>
            </div>

            <div className="mt-6 flex justify-end">
              <Button variant="outline" onClick={() => setSelectedOwner(null)}>
                Đóng
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
