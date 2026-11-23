import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, RefreshCw, Search, ShieldCheck, UserCheck, Users } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import { errorMessage, mapAdminUser, mapList, type AdminUser } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { queryKeys } from '@/shared/lib/queryKeys'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

const USER_STATUSES = ['ACTIVE', 'INACTIVE', 'PENDING', 'BANNED'] as const
type UserStatus = (typeof USER_STATUSES)[number]

export function AdminUsersPage() {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null)

  const usersQuery = useQuery({
    queryKey: queryKeys.phase3b.users,
    queryFn: async () => mapList(await api.get<unknown>('/users'), mapAdminUser),
  })

  const statusMutation = useMutation({
    mutationFn: async ({ id, status }: { id: number; status: UserStatus }) =>
      api.patch<unknown>(`/users/${id}/status`, { status }),
    onSuccess: async (_data, variables) => {
      toast.success(`Đã đổi trạng thái người dùng sang ${variables.status}`)
      await queryClient.invalidateQueries({ queryKey: queryKeys.phase3b.users })
    },
    onError: (error) => toast.error('Không đổi được trạng thái', { description: errorMessage(error) }),
  })

  const users = usersQuery.data ?? []
  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase()
    return users.filter((user: AdminUser) => {
      const matchesKeyword =
        keyword === '' ||
        user.email.toLowerCase().includes(keyword) ||
        user.fullName.toLowerCase().includes(keyword) ||
        user.roles.some((role) => role.toLowerCase().includes(keyword))
      const matchesStatus = statusFilter === 'ALL' || user.status === statusFilter
      return matchesKeyword && matchesStatus
    })
  }, [users, search, statusFilter])

  return (
    <>
      <PageIntro
        eyebrow="Admin"
        title="Quản lý người dùng"
        description="Tra cứu tài khoản theo email, tên hoặc vai trò và thay đổi trạng thái hoạt động của người dùng."
        actions={
          <Button
            variant="outline"
            leftIcon={<RefreshCw className="size-4" />}
            onClick={() => void usersQuery.refetch()}
          >
            Làm mới
          </Button>
        }
      />

      {usersQuery.isPending ? (
        <QueryLoading />
      ) : usersQuery.isError ? (
        <QueryError onRetry={() => void usersQuery.refetch()} />
      ) : (
        <SectionCard
          title="Danh sách người dùng"
          description={`${users.length} tài khoản · ${filtered.length} kết quả`}
        >
          <div className="grid gap-4 border-b border-ink-100 p-5 sm:grid-cols-[1fr_220px]">
            <Input
              label="Tìm kiếm"
              placeholder="Email, họ tên hoặc vai trò"
              value={search}
              icon={<Search className="size-4" />}
              onChange={(event) => setSearch(event.target.value)}
            />
            <div>
              <label htmlFor="user-status-filter" className="mb-1.5 block text-sm font-medium text-ink-700">
                Trạng thái
              </label>
              <select
                id="user-status-filter"
                value={statusFilter}
                onChange={(event) => setStatusFilter(event.target.value)}
                className="h-11 w-full rounded-xl border border-ink-200 bg-white px-3.5 text-sm"
              >
                <option value="ALL">Tất cả</option>
                {USER_STATUSES.map((status) => (
                  <option key={status} value={status}>{status}</option>
                ))}
              </select>
            </div>
          </div>

          {filtered.length === 0 ? (
            <EmptyState
              icon={search || statusFilter !== 'ALL' ? Search : Users}
              title="Không tìm thấy người dùng"
              description="Thử thay đổi từ khóa hoặc bộ lọc trạng thái."
            />
          ) : (
            <div className="scrollbar-thin overflow-x-auto p-5">
              <table className="w-full min-w-[760px] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                    <th className="pb-3 font-semibold">Người dùng</th>
                    <th className="pb-3 font-semibold">Vai trò</th>
                    <th className="pb-3 font-semibold">Trạng thái</th>
                    <th className="pb-3 text-right font-semibold">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {filtered.map((user) => (
                    <tr key={user.id} className="text-ink-700">
                      <td className="py-3">
                        <p className="font-semibold text-ink-900">{user.fullName || 'Chưa cập nhật tên'}</p>
                        <p className="mt-0.5 text-xs text-ink-500">{user.email || `#${user.id}`}</p>
                      </td>
                      <td className="py-3">
                        <div className="flex flex-wrap gap-1">
                          {user.roles.length === 0 ? (
                            <span className="text-xs text-ink-400">—</span>
                          ) : (
                            user.roles.map((role) => (
                              <span
                                key={role}
                                className="inline-flex items-center gap-1 rounded-full bg-ink-100 px-2 py-1 text-xs font-medium text-ink-700"
                              >
                                <ShieldCheck className="size-3" /> {role}
                              </span>
                            ))
                          )}
                        </div>
                        {user.companyName && <p className="mt-1 text-xs text-ink-500">{user.companyName}</p>}
                      </td>
                      <td className="py-3">
                        <StatusBadge
                          status={user.status}
                          label={user.status === 'ACTIVE' ? 'Hoạt động' : user.status === 'BANNED' ? 'Bị khoá' : undefined}
                        />
                      </td>
                      <td className="py-3">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            leftIcon={<Eye className="size-3" />}
                            onClick={() => setSelectedUser(user)}
                          >
                            Chi tiết
                          </Button>
                          <select
                            aria-label={`Đổi trạng thái ${user.email}`}
                            value=""
                            disabled={statusMutation.isPending}
                            onChange={(event) => {
                              const status = event.target.value as UserStatus
                              if (status && status !== user.status) statusMutation.mutate({ id: user.id, status })
                            }}
                            className="h-9 rounded-xl border border-ink-200 bg-white px-2 text-xs font-medium text-ink-700"
                          >
                            <option value="">Đổi trạng thái</option>
                            {USER_STATUSES.filter((status) => status !== user.status).map((status) => (
                              <option key={status} value={status}>{status}</option>
                            ))}
                          </select>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </SectionCard>
      )}

      {selectedUser && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/40 p-4 backdrop-blur-xs"
          onClick={() => setSelectedUser(null)}
        >
          <div
            className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-ink-100 pb-4">
              <div className="flex items-center gap-2.5">
                <div className="grid size-10 place-items-center rounded-xl bg-brand-50 text-brand-600">
                  <UserCheck className="size-5" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-ink-900">{selectedUser.fullName || 'Người dùng'}</h3>
                  <p className="text-xs text-ink-500">{selectedUser.email} · ID #{selectedUser.id}</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setSelectedUser(null)}
                className="rounded-lg p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-700"
              >
                ✕
              </button>
            </div>

            <div className="mt-5 space-y-4 text-sm">
              <div className="grid grid-cols-2 gap-3 rounded-xl bg-ink-50 p-4">
                <div>
                  <span className="text-xs text-ink-500">Mã định danh:</span>
                  <p className="font-mono text-sm font-bold text-ink-900">#{selectedUser.id}</p>
                </div>
                <div>
                  <span className="text-xs text-ink-500">Trạng thái:</span>
                  <div className="mt-0.5">
                    <StatusBadge status={selectedUser.status} />
                  </div>
                </div>
                <div>
                  <span className="text-xs text-ink-500">Họ và tên:</span>
                  <p className="font-semibold text-ink-900">{selectedUser.fullName || 'Chưa cập nhật'}</p>
                </div>
                <div>
                  <span className="text-xs text-ink-500">Email tài khoản:</span>
                  <p className="truncate font-medium text-ink-800">{selectedUser.email}</p>
                </div>
              </div>

              <div className="rounded-xl border border-ink-200 p-4">
                <h4 className="flex items-center gap-1.5 font-semibold text-ink-900">
                  <ShieldCheck className="size-4 text-brand-600" />
                  Vai trò phân quyền hệ thống
                </h4>
                <div className="mt-2.5 flex flex-wrap gap-1.5">
                  {selectedUser.roles.map((r) => (
                    <span
                      key={r}
                      className="inline-flex items-center gap-1 rounded-full bg-brand-50 px-2.5 py-1 text-xs font-semibold text-brand-700"
                    >
                      <ShieldCheck className="size-3" />
                      {r}
                    </span>
                  ))}
                </div>
                {selectedUser.companyName && (
                  <p className="mt-3 text-xs text-ink-600">
                    Thuộc tổ chức: <span className="font-semibold text-ink-900">{selectedUser.companyName}</span>
                  </p>
                )}
              </div>
            </div>

            <div className="mt-6 flex items-center justify-between border-t border-ink-100 pt-4">
              <div className="flex gap-2">
                {selectedUser.status === 'ACTIVE' ? (
                  <Button
                    variant="outline"
                    size="sm"
                    className="text-rose-600 hover:bg-rose-50 hover:text-rose-700"
                    disabled={statusMutation.isPending}
                    onClick={() => {
                      statusMutation.mutate({ id: selectedUser.id, status: 'BANNED' })
                      setSelectedUser((prev) => (prev ? { ...prev, status: 'BANNED' } : null))
                    }}
                  >
                    Khoá tài khoản
                  </Button>
                ) : (
                  <Button
                    variant="outline"
                    size="sm"
                    className="text-emerald-600 hover:bg-emerald-50 hover:text-emerald-700"
                    disabled={statusMutation.isPending}
                    onClick={() => {
                      statusMutation.mutate({ id: selectedUser.id, status: 'ACTIVE' })
                      setSelectedUser((prev) => (prev ? { ...prev, status: 'ACTIVE' } : null))
                    }}
                  >
                    Kích hoạt tài khoản
                  </Button>
                )}
              </div>
              <Button variant="outline" onClick={() => setSelectedUser(null)}>
                Đóng
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
