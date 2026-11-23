import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Eye, FolderKanban, RefreshCw, Search } from 'lucide-react'
import { api } from '@/shared/api/client'
import { mapList, mapProjectItem, type ProjectItem } from '@/shared/api/phase3b'
import { Button } from '@/shared/components/Button'
import { EmptyState } from '@/shared/components/EmptyState'
import { Input } from '@/shared/components/Input'
import { StatusBadge } from '@/shared/components/StatusBadge'
import { PageIntro } from '@/pages/components/PageIntro'
import { QueryError, QueryLoading, SectionCard } from '@/pages/components/QueryState'

export function CvaProjectsPage() {
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState<ProjectItem | null>(null)

  const projectsQuery = useQuery({
    queryKey: ['cva', 'projects'],
    queryFn: async () => mapList(await api.get<unknown>('/projects'), mapProjectItem),
  })

  const projects = projectsQuery.data ?? []
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return projects
    return projects.filter(
      (p) =>
        p.title.toLowerCase().includes(q) ||
        p.description.toLowerCase().includes(q) ||
        p.status.toLowerCase().includes(q),
    )
  }, [projects, search])

  return (
    <>
      <PageIntro
        eyebrow="CVA · Thẩm định"
        title="Danh mục dự án carbon"
        description="Tra cứu toàn bộ dự án đang mở và tiêu chuẩn đo lường phát thải (VM0038) trên sàn CarbonX."
        actions={
          <Button
            variant="outline"
            leftIcon={<RefreshCw className="size-4" />}
            onClick={() => void projectsQuery.refetch()}
          >
            Làm mới
          </Button>
        }
      />

      {projectsQuery.isPending ? (
        <QueryLoading />
      ) : projectsQuery.isError ? (
        <QueryError onRetry={() => void projectsQuery.refetch()} />
      ) : (
        <SectionCard
          title="Dự án trên hệ thống"
          description={`${projects.length} dự án · ${filtered.length} kết quả`}
        >
          <div className="border-b border-ink-100 p-5">
            <Input
              label="Tìm kiếm dự án"
              placeholder="Tên dự án, mô tả hoặc trạng thái"
              value={search}
              icon={<Search className="size-4" />}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {filtered.length === 0 ? (
            <EmptyState
              icon={FolderKanban}
              title="Không có dự án phù hợp"
              description="Thử từ khoá khác hoặc làm mới danh sách."
            />
          ) : (
            <div className="scrollbar-thin overflow-x-auto p-5">
              <table className="w-full min-w-[720px] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink-200 text-xs uppercase tracking-wide text-ink-500">
                    <th className="pb-3 font-semibold">Mã</th>
                    <th className="pb-3 font-semibold">Tên dự án</th>
                    <th className="pb-3 font-semibold">Cam kết / Mô tả</th>
                    <th className="pb-3 font-semibold">Trạng thái</th>
                    <th className="pb-3 text-right font-semibold">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {filtered.map((project) => (
                    <tr key={project.id} className="text-ink-700">
                      <td className="py-3 font-mono text-xs text-ink-500">#{project.id}</td>
                      <td className="py-3 font-semibold text-ink-900">{project.title}</td>
                      <td className="max-w-md truncate py-3 text-ink-600">
                        {project.commitments || project.description || '—'}
                      </td>
                      <td className="py-3">
                        <StatusBadge status={project.status} />
                      </td>
                      <td className="py-3 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          leftIcon={<Eye className="size-3.5" />}
                          onClick={() => setSelected(project)}
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
            className="w-full max-w-xl rounded-2xl bg-white p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between border-b border-ink-100 pb-4">
              <div>
                <p className="text-xs font-semibold uppercase text-brand-700">Dự án #{selected.id}</p>
                <h3 className="mt-1 text-lg font-bold text-ink-900">{selected.title}</h3>
              </div>
              <StatusBadge status={selected.status} />
            </div>

            <div className="mt-4 space-y-3 text-sm text-ink-700">
              <div>
                <p className="text-xs font-semibold uppercase text-ink-500">Mô tả dự án</p>
                <p className="mt-1 whitespace-pre-wrap">{selected.description || 'Chưa có mô tả.'}</p>
              </div>
              <div>
                <p className="text-xs font-semibold uppercase text-ink-500">Cam kết giảm phát thải</p>
                <p className="mt-1">{selected.commitments || 'Chuẩn đo lường VM0038'}</p>
              </div>
              <div>
                <p className="text-xs font-semibold uppercase text-ink-500">Yêu cầu kỹ thuật</p>
                <p className="mt-1">{selected.technicalIndicators || 'Đo đếm kWh sạc thực tế theo biển số xe.'}</p>
              </div>
              <div>
                <p className="text-xs font-semibold uppercase text-ink-500">Phương pháp đo lường</p>
                <p className="mt-1">{selected.measurementMethod || 'VM0038 — 0.6767 kg CO₂/kWh'}</p>
              </div>
            </div>

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
