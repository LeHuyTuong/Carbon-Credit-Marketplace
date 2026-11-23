import { useQuery } from '@tanstack/react-query'
import { Award, Building2, Mail, RefreshCw, ShieldCheck } from 'lucide-react'
import { api } from '@/shared/api/client'
import { Button } from '@/shared/components/Button'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

interface CvaProfile {
  id: number
  name: string
  email: string
  organization: string
  positionTitle: string
  accreditationNo?: string
  capacityQuota?: number
  status: string
}

export function CvaProfilePage() {
  const profileQuery = useQuery({
    queryKey: ['cva', 'profile'],
    queryFn: () => api.get<CvaProfile>('/kyc/cva'),
  })

  const profile = profileQuery.data

  return (
    <>
      <PageIntro
        eyebrow="CVA · Hồ sơ"
        title="Hồ sơ thẩm định viên CVA"
        description="Thông tin chứng chỉ hành nghề, tổ chức thẩm định độc lập và hạn mức phê duyệt của tài khoản CVA."
        actions={
          <Button
            variant="outline"
            leftIcon={<RefreshCw className="size-4" />}
            onClick={() => void profileQuery.refetch()}
          >
            Làm mới
          </Button>
        }
      />

      {profileQuery.isPending ? (
        <QueryLoading />
      ) : profileQuery.isError || !profile ? (
        <QueryError onRetry={() => void profileQuery.refetch()} />
      ) : (
        <SectionCard title={profile.name} description="Hồ sơ thẩm định viên độc lập (không gắn ví tài chính)">
          <div className="grid gap-6 p-6 md:grid-cols-2">
            <div className="space-y-4 text-sm">
              <div className="flex items-center gap-3 rounded-xl bg-ink-50 p-4">
                <Building2 className="size-5 text-brand-600" />
                <div>
                  <p className="text-xs text-ink-500">Tổ chức thẩm định</p>
                  <p className="font-semibold text-ink-900">{profile.organization || '—'}</p>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-xl bg-ink-50 p-4">
                <Award className="size-5 text-brand-600" />
                <div>
                  <p className="text-xs text-ink-500">Chức danh chuyên môn</p>
                  <p className="font-semibold text-ink-900">{profile.positionTitle || '—'}</p>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-xl bg-ink-50 p-4">
                <Mail className="size-5 text-brand-600" />
                <div>
                  <p className="text-xs text-ink-500">Email công vụ</p>
                  <p className="font-semibold text-ink-900">{profile.email}</p>
                </div>
              </div>
            </div>

            <div className="space-y-4 rounded-2xl border border-ink-200 p-5 text-sm">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-ink-900">Trạng thái chứng nhận</span>
                <StatusBadge status={profile.status || 'ACTIVE'} />
              </div>
              <dl className="space-y-2.5 border-t border-ink-100 pt-3">
                <div className="flex justify-between">
                  <dt className="text-ink-500">Mã hồ sơ CVA</dt>
                  <dd className="font-mono font-semibold text-ink-900">CVA-#{profile.id}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-ink-500">Số chứng chỉ hành nghề</dt>
                  <dd className="font-mono text-ink-900">{profile.accreditationNo || 'ISO-14065-VN'}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-ink-500">Hạn mức thẩm định</dt>
                  <dd className="font-mono text-ink-900">{profile.capacityQuota ?? 'Không giới hạn'}</dd>
                </div>
              </dl>
              <div className="flex items-start gap-2 rounded-xl bg-amber-50 p-3 text-xs text-amber-900">
                <ShieldCheck className="mt-0.5 size-4 shrink-0 text-amber-600" />
                Đơn vị thẩm định độc lập CVA không nắm giữ ví tiền hay tín chỉ nhằm đảm bảo tính khách quan theo chuẩn MRV.
              </div>
            </div>
          </div>
        </SectionCard>
      )}
    </>
  )
}
