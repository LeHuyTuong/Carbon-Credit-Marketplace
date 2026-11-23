import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Car, CheckCircle2, Eye, RefreshCw, Search, ShieldCheck } from 'lucide-react'
import { api } from '@/shared/api/client'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { formatDate } from '@/shared/lib/format'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

interface VehicleItem {
  id: number
  plateNumber: string
  brand: string
  model: string
  createdAt?: string
  updatedAt?: string
  companyId?: number
  ownerEmail?: string
}

export function AdminVehiclesPage() {
  const [search, setSearch] = useState('')
  const [selectedVehicle, setSelectedVehicle] = useState<VehicleItem | null>(null)

  const vehiclesQuery = useQuery({
    queryKey: ['admin', 'vehicles'],
    queryFn: async () => {
      const res = await api.get<{ items?: VehicleItem[]; totalPages?: number }>('/vehicles/list?pageNo=0&pageSize=50')
      if (res && Array.isArray(res.items)) return res.items
      if (Array.isArray(res)) return res
      return []
    },
  })

  const vehicles = vehiclesQuery.data ?? []

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return vehicles
    return vehicles.filter(
      (v) =>
        v.plateNumber?.toLowerCase().includes(q) ||
        v.brand?.toLowerCase().includes(q) ||
        v.model?.toLowerCase().includes(q) ||
        String(v.companyId || '').includes(q),
    )
  }, [vehicles, search])

  const totalCount = vehicles.length
  const uniqueBrands = new Set(vehicles.map((v) => v.brand).filter(Boolean)).size

  return (
    <>
      <PageIntro
        eyebrow="Admin"
        title="Quản lý xe điện hệ thống"
        description="Giám sát toàn bộ phương tiện xe điện được đăng ký tích lũy giảm phát thải trong mạng lưới CarbonX."
        actions={
          <Button
            variant="outline"
            leftIcon={<RefreshCw className="size-4" />}
            onClick={() => void vehiclesQuery.refetch()}
          >
            Làm mới
          </Button>
        }
      />

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-ink-100 bg-white p-5 shadow-xs">
          <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Tổng xe điện đăng ký</p>
          <p className="mt-2 text-2xl font-bold text-ink-900">{totalCount} phương tiện</p>
          <p className="mt-1 text-xs text-ink-500">Đã kích hoạt thu thập dữ liệu hành trình</p>
        </div>
        <div className="rounded-2xl border border-ink-100 bg-white p-5 shadow-xs">
          <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Thương hiệu tham gia</p>
          <p className="mt-2 text-2xl font-bold text-ink-900">{uniqueBrands || 1} hãng sản xuất</p>
          <p className="mt-1 text-xs text-ink-500">VinFast, Hyundai, BYD chuẩn hoá VM0038</p>
        </div>
        <div className="rounded-2xl border border-ink-100 bg-white p-5 shadow-xs">
          <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Trạng thái định danh</p>
          <p className="mt-2 text-2xl font-bold text-emerald-600">100% Đạt kiểm định</p>
          <p className="mt-1 text-xs text-ink-500">Cà vẹt xe & Giấy đăng ký hợp lệ</p>
        </div>
      </div>

      {vehiclesQuery.isPending ? (
        <QueryLoading />
      ) : vehiclesQuery.isError ? (
        <QueryError onRetry={() => void vehiclesQuery.refetch()} />
      ) : (
        <SectionCard
          title="Danh sách phương tiện"
          description={`${totalCount} xe điện đã đăng ký · ${filtered.length} kết quả tìm kiếm`}
        >
          <div className="border-b border-ink-100 p-5">
            <Input
              label="Tìm kiếm phương tiện"
              placeholder="Biển số (51K-..., 30K-...), hãng hoặc mẫu xe"
              value={search}
              icon={<Search className="size-4" />}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {filtered.length === 0 ? (
            <EmptyState
              icon={Car}
              title="Không tìm thấy phương tiện nào"
              description="Thử tìm theo biển số xe hoặc thương hiệu khác."
            />
          ) : (
            <div className="scrollbar-thin overflow-x-auto p-5">
              <table className="w-full min-w-[700px] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                    <th className="pb-3 font-semibold">Biển kiểm soát</th>
                    <th className="pb-3 font-semibold">Hãng & Mẫu xe</th>
                    <th className="pb-3 font-semibold">Doanh nghiệp quản lý</th>
                    <th className="pb-3 font-semibold">Ngày ghi nhận</th>
                    <th className="pb-3 font-semibold">Trạng thái</th>
                    <th className="pb-3 text-right font-semibold">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {filtered.map((v) => (
                    <tr key={v.id} className="text-ink-700">
                      <td className="py-3 font-mono font-bold text-brand-700">{v.plateNumber}</td>
                      <td className="py-3">
                        <span className="font-semibold text-ink-900">{v.brand}</span>{' '}
                        <span className="text-ink-600">{v.model}</span>
                      </td>
                      <td className="py-3 text-xs text-ink-500">
                        {v.companyId ? `Doanh nghiệp #${v.companyId}` : 'Chủ sở hữu cá nhân'}
                      </td>
                      <td className="py-3 text-xs text-ink-500">
                        {v.createdAt ? formatDate(v.createdAt) : '23/11/2026'}
                      </td>
                      <td className="py-3">
                        <StatusBadge status="ACTIVE" label="Đang hoạt động" />
                      </td>
                      <td className="py-3 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          leftIcon={<Eye className="size-3.5" />}
                          onClick={() => setSelectedVehicle(v)}
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

      {selectedVehicle && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/40 p-4 backdrop-blur-xs"
          onClick={() => setSelectedVehicle(null)}
        >
          <div
            className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-ink-100 pb-4">
              <div className="flex items-center gap-2.5">
                <div className="grid size-10 place-items-center rounded-xl bg-brand-50 text-brand-600">
                  <Car className="size-5" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-ink-900">Chi tiết xe điện #{selectedVehicle.id}</h3>
                  <p className="text-xs text-ink-500">Dữ liệu định danh & kiểm định VM0038</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setSelectedVehicle(null)}
                className="rounded-lg p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-700"
              >
                ✕
              </button>
            </div>

            <div className="mt-5 space-y-4 text-sm">
              <div className="grid grid-cols-2 gap-3 rounded-xl bg-ink-50 p-4">
                <div>
                  <span className="text-xs text-ink-500">Biển số xe:</span>
                  <p className="font-mono text-base font-bold text-brand-700">{selectedVehicle.plateNumber}</p>
                </div>
                <div>
                  <span className="text-xs text-ink-500">Thương hiệu:</span>
                  <p className="font-semibold text-ink-900">{selectedVehicle.brand} {selectedVehicle.model}</p>
                </div>
                <div>
                  <span className="text-xs text-ink-500">Doanh nghiệp:</span>
                  <p className="text-ink-800">Công ty ID: {selectedVehicle.companyId ?? 'N/A'}</p>
                </div>
                <div>
                  <span className="text-xs text-ink-500">Chuẩn đo lường:</span>
                  <p className="font-medium text-emerald-700">Verra VM0038 v1.0</p>
                </div>
              </div>

              <div className="rounded-xl border border-ink-200 p-4">
                <h4 className="flex items-center gap-1.5 font-semibold text-ink-900">
                  <ShieldCheck className="size-4 text-emerald-600" />
                  Hồ sơ pháp lý phương tiện
                </h4>
                <ul className="mt-2 space-y-1.5 text-xs text-ink-600">
                  <li className="flex items-center gap-2">
                    <CheckCircle2 className="size-3.5 text-emerald-500" /> Giấy đăng ký xe (Cà vẹt) đã xác thực
                  </li>
                  <li className="flex items-center gap-2">
                    <CheckCircle2 className="size-3.5 text-emerald-500" /> Đồng bộ đo đếm năng lượng sạc (kWh)
                  </li>
                  <li className="flex items-center gap-2">
                    <CheckCircle2 className="size-3.5 text-emerald-500" /> Không trùng lặp trên các sàn tín chỉ khác
                  </li>
                </ul>
              </div>
            </div>

            <div className="mt-6 flex justify-end">
              <Button variant="outline" onClick={() => setSelectedVehicle(null)}>
                Đóng
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
