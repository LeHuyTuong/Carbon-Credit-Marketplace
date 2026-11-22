import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import { downloadFile } from '@/shared/api/download'
import {
  asList,
  asPage,
  mapAi,
  mapAnalysis,
  mapApplication,
  mapReport,
  type AiEvaluation,
  type AnalysisResult,
  type EmissionReport,
  type ProjectApplication,
} from '@/shared/api/domain'
import { apiMessage } from '@/shared/lib/errors'
import { queryKeys } from '@/shared/lib/queryKeys'

type Raw = Record<string, unknown>

function isRecord(value: unknown): value is Raw {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function mapItems<T>(payload: unknown, map: (raw: Raw) => T | null): T[] {
  return asList(payload).map((item) => (isRecord(item) ? map(item) : null)).filter((item): item is T => item !== null)
}

export function useCvaApplications() {
  return useQuery({
    queryKey: queryKeys.cvaApplications,
    queryFn: async (): Promise<ProjectApplication[]> =>
      mapItems(await api.get<unknown>('/project-applications/pending-cva'), mapApplication),
  })
}

export function useCvaDecision() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, approved, note }: { id: number; approved: boolean; note: string }) =>
      api.put<unknown>(`/project-applications/${id}/cva-decision`, undefined, {
        params: { approved, note },
      }),
    onSuccess: (_, { approved }) => {
      toast.success(approved ? 'Đã duyệt hồ sơ dự án' : 'Đã ghi nhận ý kiến từ chối/bổ sung')
      void queryClient.invalidateQueries({ queryKey: queryKeys.cvaApplications })
    },
    onError: (error) => toast.error('Không cập nhật được hồ sơ', { description: apiMessage(error) }),
  })
}

export function useCvaReports(page: number, size: number) {
  return useQuery({
    queryKey: [...queryKeys.cvaReports, page, size] as const,
    queryFn: async () => asPage(await api.get<unknown>('/reports/list-cva-check', { page, size }), mapReport, size),
  })
}

export interface RunAnalysisResult {
  analysis: AnalysisResult
  ai: AiEvaluation
}

export function useRunAnalysis() {
  return useMutation({
    mutationFn: async (reportId: number): Promise<RunAnalysisResult> => {
      const [analysisPayload, aiPayload] = await Promise.all([
        api.post<unknown>(`/reports/${reportId}/analyze`, undefined, { params: { persist: false } }),
        api.post<unknown>(`/reports/${reportId}/ai-score`),
      ])
      return { analysis: mapAnalysis(analysisPayload), ai: mapAi(aiPayload) }
    },
    onError: (error) => toast.error('Không chạy được phân tích báo cáo', { description: apiMessage(error) }),
  })
}

export function useVerifyReport() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, approved, comment }: { id: number; approved: boolean; comment: string }) =>
      api.put<unknown>(`/reports/${id}/verify`, undefined, { params: { approved, comment } }),
    onSuccess: (_, { approved }) => {
      toast.success(approved ? 'Đã phê duyệt báo cáo' : 'Đã từ chối báo cáo')
      void queryClient.invalidateQueries({ queryKey: queryKeys.cvaReports })
    },
    onError: (error) => toast.error('Không cập nhật được báo cáo', { description: apiMessage(error) }),
  })
}

export function downloadCvaReportCsv(reportId: number): Promise<void> {
  return downloadFile(`/reports/${reportId}/export`, `emission_report_${reportId}.csv`)
}

export type { AnalysisResult, AiEvaluation, EmissionReport }
