import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Building2, Eye, RefreshCw, Search } from 'lucide-react'
import { api } from '@/shared/api/client'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { formatDate } from '@/shared/lib/format'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

interface CompanyKycRow {
  id: number
  companyName: string
  businessLicense: string
  taxCode: string
  address: string
  createAt?: string
}

export function CvaCompaniesPage() {
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState<CompanyKycRow | null>(null)

  const companiesQuery = useQuery({
    queryKey: ['cva', 'companies'],
    queryFn: async () => {
      const data = await api.get<CompanyKycRow[]>('/kyc/company/listKYCCompany')
      return Array.isArray(data) ? data : []
    },
  })

  const companies = companiesQuery.data ?? []
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return companies
    return companies.filter(
      (c) =>
        c.companyName?.toLowerCase().includes(q) ||
        c.taxCode?.toLowerCase().includes(q) ||
        c.businessLicense?.toLowerCase().includes(q) ||
        c.address?.toLowerCase().includes(q),
    )
  }, [companies, search])

  return (
    <>
      <PageIntro
        eyebrow="CVA · Thẩm định"
        title="Danh sách doanh nghiệp"
        description="Tra cứu hồ sơ pháp lý, mã số thuế và giấy phép kinh doanh của các doanh nghiệp tham gia thị trường tín chỉ."
        actions={
          <Button
            variant="outline"
            leftIcon={<RefreshCw className="size-4" />}
            onClick={() => void companiesQuery.refetch()}
          >
            Làm mới
          </Button>
        }
      />

      {companiesQuery.isPending ? (
        <QueryLoading />
      ) : companiesQuery.isError ? (
        <QueryError onRetry={() => void companiesQuery.refetch()} />
      ) : (
        <SectionCard
          title="Doanh nghiệp đã định danh KYC"
          description={`${companies.length} doanh nghiệp · ${filtered.length} kết quả`}
        >
          <div className="border-b border-ink-100 p-5">
            <Input
              label="Tìm kiếm doanh nghiệp"
              placeholder="Tên công ty, mã số thuế, GPKD hoặc địa chỉ"
              value={search}
              icon={<Search className="size-4" />}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {filtered.length === 0 ? (
            <EmptyState
              icon={Building2}
              title="Không tìm thấy doanh nghiệp"
              description="Thử tìm với từ khoá khác."
            />
          ) : (
            <div className="scrollbar-thin overflow-x-auto p-5">
              <table className="w-full min-w-[740px] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                    <th className="pb-3 font-semibold">Doanh nghiệp</th>
                    <th className="pb-3 font-semibold">Mã số thuế</th>
                    <th className="pb-3 font-semibold">Giấy phép KD</th>
                    <th className="pb-3 font-semibold">Địa chỉ</th>
                    <th className="pb-3 text-right font-semibold">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {filtered.map((c) => (
                    <tr key={c.id} className="text-ink-700">
                      <td className="py-3 font-semibold text-ink-900">{c.companyName}</td>
                      <td className="py-3 font-mono text-xs">{c.taxCode}</td>
                      <td className="py-3 font-mono text-xs text-brand-700">{c.businessLicense}</td>
                      <td className="max-w-xs truncate py-3 text-ink-600">{c.address || '—'}</td>
                      <td className="py-3 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          leftIcon={<Eye className="size-3.5" />}
                          onClick={() => setSelected(c)}
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

      {selected && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/40 p-4 backdrop-blur-xs"
          onClick={() => setSelected(null)}
        >
          <div
            className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="border-b border-ink-100 pb-4">
              <p className="text-xs font-semibold uppercase text-brand-700">Doanh nghiệp #{selected.id}</p>
              <h3 className="mt-1 text-lg font-bold text-ink-900">{selected.companyName}</h3>
            </div>
            <dl className="mt-4 space-y-3 text-sm">
              <div className="flex justify-between">
                <dt className="text-ink-500">Mã số thuế</dt>
                <dd className="font-mono font-semibold text-ink-900">{selected.taxCode}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-500">Giấy phép kinh doanh</dt>
                <dd className="font-mono font-semibold text-ink-900">{selected.businessLicense}</dd>
              </div>
              <div>
                <dt className="text-ink-500">Địa chỉ trụ sở</dt>
                <dd className="mt-1 text-ink-900">{selected.address || '—'}</dd>
              </div>
              {selected.createAt && (
                <div className="flex justify-between">
                  <dt className="text-ink-500">Ngày định danh</dt>
                  <dd className="text-ink-900">{formatDate(selected.createAt)}</dd>
                </div>
              )}
            </dl>
            <div className="mt-6 flex justify-end">
              <Button variant="outline" onClick={() => setSelected(null)}>
                Đóng
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
