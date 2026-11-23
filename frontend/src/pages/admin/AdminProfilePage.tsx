import { useQuery } from '@tanstack/react-query'
import { Mail, MapPin, Phone, RefreshCw, ShieldCheck, UserRound } from 'lucide-react'
import { api } from '@/shared/api/client'
import { Button } from '@/shared/components/Button'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

interface AdminProfile {
  id: number
  email: string
  name: string
  phone?: string
  firstName?: string
  lastName?: string
  country?: string
  city?: string
}

export function AdminProfilePage() {
  const profileQuery = useQuery({
    queryKey: ['admin', 'profile'],
    queryFn: () => api.get<AdminProfile>('/kyc/admin'),
  })

  const profile = profileQuery.data

  return (
    <>
      <PageIntro
        eyebrow="Admin · Hồ sơ"
        title="Hồ sơ Quản trị viên"
        description="Thông tin định danh quản trị viên vận hành sàn CarbonX và phân quyền quản trị cấp cao."
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
        <SectionCard title={profile.name || 'Quản trị viên CarbonX'} description={`Admin ID #${profile.id}`}>
          <div className="grid gap-6 p-6 md:grid-cols-2">
            <div className="space-y-4 text-sm">
              <div className="flex items-center gap-3 rounded-xl bg-ink-50 p-4">
                <UserRound className="size-5 text-brand-600" />
                <div>
                  <p className="text-xs text-ink-500">Họ và tên</p>
                  <p className="font-semibold text-ink-900">
                    {profile.name || `${profile.firstName ?? ''} ${profile.lastName ?? ''}`.trim() || '—'}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-xl bg-ink-50 p-4">
                <Mail className="size-5 text-brand-600" />
                <div>
                  <p className="text-xs text-ink-500">Email quản trị</p>
                  <p className="font-semibold text-ink-900">{profile.email}</p>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-xl bg-ink-50 p-4">
                <Phone className="size-5 text-brand-600" />
                <div>
                  <p className="text-xs text-ink-500">Số điện thoại</p>
                  <p className="font-mono font-semibold text-ink-900">{profile.phone || '—'}</p>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-xl bg-ink-50 p-4">
                <MapPin className="size-5 text-brand-600" />
                <div>
                  <p className="text-xs text-ink-500">Khu vực vận hành</p>
                  <p className="font-semibold text-ink-900">
                    {[profile.city, profile.country].filter(Boolean).join(', ') || 'Việt Nam'}
                  </p>
                </div>
              </div>
            </div>

            <div className="space-y-4 rounded-2xl border border-ink-200 p-5 text-sm">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-ink-900">Quyền hạn hệ thống</span>
                <StatusBadge status="ACTIVE" label="Quản trị tối cao" />
              </div>
              <ul className="space-y-2 border-t border-ink-100 pt-3 text-ink-700">
                <li className="flex items-center gap-2">
                  <ShieldCheck className="size-4 text-emerald-600" /> Tạo và quản lý dự án theo chuẩn VM0038
                </li>
                <li className="flex items-center gap-2">
                  <ShieldCheck className="size-4 text-emerald-600" /> Phê duyệt cấp 2 và phát hành lô tín chỉ có serial
                </li>
                <li className="flex items-center gap-2">
                  <ShieldCheck className="size-4 text-emerald-600" /> Giám sát sổ cái, phí sàn 5% và duyệt lệnh rút tiền
                </li>
              </ul>
            </div>
          </div>
        </SectionCard>
      )}
    </>
  )
}
