import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Building2, Eye, RefreshCw, Search, ShieldCheck, UserRound } from 'lucide-react'
import { api } from '@/shared/api/client'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

type DirectoryTab = 'company' | 'cva' | 'ev'

interface CompanyItem {
  id: number
  companyName: string
  businessLicense: string
  taxCode: string
  address: string
}

interface CvaItem {
  id: number
  name: string
  email: string
  organization: string
  positionTitle: string
  status: string
}

interface EvKycItem {
  userId: number
  name: string
  gender?: string
  email: string
  phone?: string
  country?: string
  address?: string
  documentType?: string
  documentNumber?: string
}

export function AdminDirectoryPage() {
  const [tab, setTab] = useState<DirectoryTab>('company')
  const [search, setSearch] = useState('')
  const [detailTitle, setDetailTitle] = useState('')
  const [detailRows, setDetailRows] = useState<[string, string][] | null>(null)

  const companiesQuery = useQuery({
    queryKey: ['admin', 'directory', 'companies'],
    queryFn: async () => {
      const data = await api.get<CompanyItem[]>('/kyc/company/listKYCCompany')
      return Array.isArray(data) ? data : []
    },
  })

  const cvasQuery = useQuery({
    queryKey: ['admin', 'directory', 'cvas'],
    queryFn: async () => {
      const data = await api.get<CvaItem[]>('/kyc/cva/list')
      return Array.isArray(data) ? data : []
    },
  })

  const evsQuery = useQuery({
    queryKey: ['admin', 'directory', 'evs'],
    queryFn: async () => {
      const data = await api.get<EvKycItem[]>('/kyc/user/listKYC')
      return Array.isArray(data) ? data : []
    },
  })

  const q = search.trim().toLowerCase()
  const companies = useMemo(
    () =>
      (companiesQuery.data ?? []).filter(
        (c) =>
          !q ||
          c.companyName?.toLowerCase().includes(q) ||
          c.taxCode?.toLowerCase().includes(q) ||
          c.businessLicense?.toLowerCase().includes(q),
      ),
    [companiesQuery.data, q],
  )
  const cvas = useMemo(
    () =>
      (cvasQuery.data ?? []).filter(
        (c) =>
          !q ||
          c.name?.toLowerCase().includes(q) ||
          c.email?.toLowerCase().includes(q) ||
          c.organization?.toLowerCase().includes(q),
      ),
    [cvasQuery.data, q],
  )
  const evs = useMemo(
    () =>
      (evsQuery.data ?? []).filter(
        (e) =>
          !q ||
          e.name?.toLowerCase().includes(q) ||
          e.email?.toLowerCase().includes(q) ||
          (e.phone ?? '').toLowerCase().includes(q) ||
          (e.documentNumber ?? '').toLowerCase().includes(q),
      ),
    [evsQuery.data, q],
  )

  const activeQuery = tab === 'company' ? companiesQuery : tab === 'cva' ? cvasQuery : evsQuery

  return (
    <>
      <PageIntro
        eyebrow="Admin · Danh bạ"
        title="Danh bạ KYC theo vai trò"
        description="Quản lý chi tiết hồ sơ định danh Doanh nghiệp, Đơn vị thẩm định CVA và Chủ xe điện."
        actions={
          <Button
            variant="outline"
            leftIcon={<RefreshCw className="size-4" />}
            onClick={() => {
              void companiesQuery.refetch()
              void cvasQuery.refetch()
              void evsQuery.refetch()
            }}
          >
            Làm mới
          </Button>
        }
      />

      <div className="mb-6 flex flex-wrap gap-2">
        <Button
          variant={tab === 'company' ? 'primary' : 'outline'}
          leftIcon={<Building2 className="size-4" />}
          onClick={() => setTab('company')}
        >
          Doanh nghiệp ({companiesQuery.data?.length ?? 0})
        </Button>
        <Button
          variant={tab === 'cva' ? 'primary' : 'outline'}
          leftIcon={<ShieldCheck className="size-4" />}
          onClick={() => setTab('cva')}
        >
          Đơn vị CVA ({cvasQuery.data?.length ?? 0})
        </Button>
        <Button
          variant={tab === 'ev' ? 'primary' : 'outline'}
          leftIcon={<UserRound className="size-4" />}
          onClick={() => setTab('ev')}
        >
          Chủ xe điện KYC ({evsQuery.data?.length ?? 0})
        </Button>
      </div>

      {activeQuery.isPending ? (
        <QueryLoading />
      ) : activeQuery.isError ? (
        <QueryError onRetry={() => void activeQuery.refetch()} />
      ) : (
        <SectionCard
          title={
            tab === 'company'
              ? 'Hồ sơ Doanh nghiệp'
              : tab === 'cva'
                ? 'Hồ sơ Thẩm định viên CVA'
                : 'Hồ sơ KYC Chủ xe điện'
          }
          description="Tra cứu thông tin pháp lý và chứng từ định danh đã nộp"
        >
          <div className="border-b border-ink-100 p-5">
            <Input
              label="Tìm kiếm trong danh bạ"
              placeholder="Tên, email, mã số thuế hoặc số giấy tờ"
              value={search}
              icon={<Search className="size-4" />}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {tab === 'company' &&
            (companies.length === 0 ? (
              <EmptyState icon={Building2} title="Không có doanh nghiệp" description="Thử từ khoá khác." />
            ) : (
              <div className="scrollbar-thin overflow-x-auto p-5">
                <table className="w-full min-w-[720px] text-left text-sm">
                  <thead>
                    <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                      <th className="pb-3 font-semibold">Doanh nghiệp</th>
                      <th className="pb-3 font-semibold">Mã số thuế</th>
                      <th className="pb-3 font-semibold">GPKD</th>
                      <th className="pb-3 font-semibold">Địa chỉ</th>
                      <th className="pb-3 text-right font-semibold">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {companies.map((c) => (
                      <tr key={c.id} className="text-ink-700">
                        <td className="py-3 font-semibold text-ink-900">{c.companyName}</td>
                        <td className="py-3 font-mono text-xs">{c.taxCode}</td>
                        <td className="py-3 font-mono text-xs text-brand-700">{c.businessLicense}</td>
                        <td className="max-w-xs truncate py-3">{c.address || '—'}</td>
                        <td className="py-3 text-right">
                          <Button
                            variant="outline"
                            size="sm"
                            leftIcon={<Eye className="size-3.5" />}
                            onClick={() => {
                              setDetailTitle(c.companyName)
                              setDetailRows([
                                ['ID Doanh nghiệp', `#${c.id}`],
                                ['Mã số thuế', c.taxCode],
                                ['Giấy phép kinh doanh', c.businessLicense],
                                ['Địa chỉ', c.address || '—'],
                              ])
                            }}
                          >
                            Chi tiết
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ))}

          {tab === 'cva' &&
            (cvas.length === 0 ? (
              <EmptyState icon={ShieldCheck} title="Không có CVA" description="Thử từ khoá khác." />
            ) : (
              <div className="scrollbar-thin overflow-x-auto p-5">
                <table className="w-full min-w-[720px] text-left text-sm">
                  <thead>
                    <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                      <th className="pb-3 font-semibold">Thẩm định viên</th>
                      <th className="pb-3 font-semibold">Tổ chức</th>
                      <th className="pb-3 font-semibold">Chức danh</th>
                      <th className="pb-3 font-semibold">Trạng thái</th>
                      <th className="pb-3 text-right font-semibold">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {cvas.map((c) => (
                      <tr key={c.id} className="text-ink-700">
                        <td className="py-3">
                          <p className="font-semibold text-ink-900">{c.name}</p>
                          <p className="text-xs text-ink-500">{c.email}</p>
                        </td>
                        <td className="py-3">{c.organization || '—'}</td>
                        <td className="py-3 text-ink-600">{c.positionTitle || '—'}</td>
                        <td className="py-3">
                          <StatusBadge status={c.status || 'ACTIVE'} />
                        </td>
                        <td className="py-3 text-right">
                          <Button
                            variant="outline"
                            size="sm"
                            leftIcon={<Eye className="size-3.5" />}
                            onClick={() => {
                              setDetailTitle(c.name)
                              setDetailRows([
                                ['Mã CVA', `#${c.id}`],
                                ['Email', c.email],
                                ['Tổ chức', c.organization || '—'],
                                ['Chức danh', c.positionTitle || '—'],
                                ['Trạng thái', c.status || 'ACTIVE'],
                              ])
                            }}
                          >
                            Chi tiết
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ))}

          {tab === 'ev' &&
            (evs.length === 0 ? (
              <EmptyState icon={UserRound} title="Không có hồ sơ chủ xe" description="Thử từ khoá khác." />
            ) : (
              <div className="scrollbar-thin overflow-x-auto p-5">
                <table className="w-full min-w-[720px] text-left text-sm">
                  <thead>
                    <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                      <th className="pb-3 font-semibold">Chủ xe điện</th>
                      <th className="pb-3 font-semibold">Điện thoại</th>
                      <th className="pb-3 font-semibold">Giấy tờ KYC</th>
                      <th className="pb-3 font-semibold">Địa chỉ</th>
                      <th className="pb-3 text-right font-semibold">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {evs.map((e) => (
                      <tr key={e.userId} className="text-ink-700">
                        <td className="py-3">
                          <p className="font-semibold text-ink-900">{e.name}</p>
                          <p className="text-xs text-ink-500">{e.email}</p>
                        </td>
                        <td className="py-3 font-mono text-xs">{e.phone || '—'}</td>
                        <td className="py-3 font-mono text-xs">
                          {e.documentType ? `${e.documentType}: ${e.documentNumber || '—'}` : '—'}
                        </td>
                        <td className="max-w-xs truncate py-3">{e.address || '—'}</td>
                        <td className="py-3 text-right">
                          <Button
                            variant="outline"
                            size="sm"
                            leftIcon={<Eye className="size-3.5" />}
                            onClick={() => {
                              setDetailTitle(e.name)
                              setDetailRows([
                                ['User ID', `#${e.userId}`],
                                ['Email', e.email],
                                ['Điện thoại', e.phone || '—'],
                                ['Giới tính', e.gender || '—'],
                                ['Giấy tờ', `${e.documentType || ''} ${e.documentNumber || ''}`.trim() || '—'],
                                ['Địa chỉ', e.address || '—'],
                              ])
                            }}
                          >
                            Chi tiết
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ))}
        </SectionCard>
      )}

      {detailRows && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/40 p-4 backdrop-blur-xs"
          onClick={() => setDetailRows(null)}
        >
          <div
            className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="border-b border-ink-100 pb-3 text-lg font-bold text-ink-900">{detailTitle}</h3>
            <dl className="mt-4 space-y-2.5 text-sm">
              {detailRows.map(([k, v]) => (
                <div key={k} className="flex justify-between gap-4">
                  <dt className="text-ink-500">{k}</dt>
                  <dd className="text-right font-medium text-ink-900">{v}</dd>
                </div>
              ))}
            </dl>
            <div className="mt-6 flex justify-end">
              <Button variant="outline" onClick={() => setDetailRows(null)}>
                Đóng
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
