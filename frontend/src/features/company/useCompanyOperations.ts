import { isAxiosError } from 'axios'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import { downloadFile } from '@/shared/api/download'
import {
  asList,
  asPage,
  mapAnalysis,
  mapApplication,
  mapCreditBatch,
  mapOrder,
  mapReport,
  type ProjectApplication,
} from '@/shared/api/domain'
import { apiMessage } from '@/shared/lib/errors'
import { queryKeys } from '@/shared/lib/queryKeys'
import type { EmissionReport } from '@/shared/api/domain'

type Raw = Record<string, unknown>

function isRecord(value: unknown): value is Raw {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function mapItems<T>(payload: unknown, map: (raw: Raw) => T | null): T[] {
  return asList(payload).map((item) => (isRecord(item) ? map(item) : null)).filter((item): item is T => item !== null)
}

function emptyOn404<T>(error: unknown, fallback: T[]): T[] {
  if (isAxiosError(error) && error.response?.status === 404) return fallback
  throw error
}

export function useCompanyApplications(status: string) {
  return useQuery({
    queryKey: [...queryKeys.companyApplications, status] as const,
    queryFn: async (): Promise<ProjectApplication[]> => {
      try {
        const payload = await api.get<unknown>('/project-applications/my', status ? { status } : undefined)
        return mapItems(payload, mapApplication)
      } catch (error) {
        return emptyOn404(error, [])
      }
    },
  })
}

export function useSubmitApplication() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ projectId, file }: { projectId: string; file: File }) => {
      const form = new FormData()
      form.append('file', file)
      return api.post<unknown>('/project-applications', form, { params: { projectId: Number(projectId) } })
    },
    onSuccess: () => {
      toast.success('Hồ sơ tham gia dự án đã được nộp')
      void queryClient.invalidateQueries({ queryKey: queryKeys.companyApplications })
    },
    onError: (error) => toast.error('Không nộp được hồ sơ', { description: apiMessage(error) }),
  })
}

export function useCompanyReports(status: string, projectId: string) {
  return useQuery({
    queryKey: [...queryKeys.companyReports, status, projectId] as const,
    queryFn: async (): Promise<EmissionReport[]> => {
      const params: Record<string, unknown> = {}
      if (status) params.status = status
      if (projectId) params.projectId = Number(projectId)
      try {
        return mapItems(await api.get<unknown>('/reports/my-reports', params), mapReport)
      } catch (error) {
        return emptyOn404(error, [])
      }
    },
  })
}

export function useUploadReport() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ file, projectId }: { file: File; projectId: string }) => {
      const form = new FormData()
      form.append('file', file)
      if (projectId) form.append('projectId', projectId)
      return api.post<unknown>('/reports/upload', form)
    },
    onSuccess: () => {
      toast.success('Báo cáo phát thải đã được upload')
      void queryClient.invalidateQueries({ queryKey: queryKeys.companyReports })
    },
    onError: (error) => toast.error('Không upload được báo cáo', { description: apiMessage(error) }),
  })
}

export function useRuleAnalysis(reportId: number | null) {
  return useQuery({
    queryKey: [...queryKeys.reports, reportId, 'rules'] as const,
    enabled: reportId !== null,
    queryFn: async () => mapAnalysis(await api.get<unknown>(`/reports/${reportId}/analysis/rules`)),
  })
}

export function useCompanyBatches(page: number, size: number) {
  return useQuery({
    queryKey: [...queryKeys.companyBatches, page, size] as const,
    queryFn: async () => asPage(await api.get<unknown>('/my/credits/batches', { page, size }), mapCreditBatch, size),
  })
}

export function useListCredits() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { batchId: number; quantity: string; pricePerCredit: string }) =>
      api.post<unknown>('/marketplace', {
        data: {
          batchId: input.batchId,
          quantity: Number(input.quantity),
          pricePerCredit: Number(input.pricePerCredit),
        },
      }),
    onSuccess: () => {
      toast.success('Tín chỉ đã được niêm yết')
      void queryClient.invalidateQueries({ queryKey: queryKeys.companyBatches })
    },
    onError: (error) => toast.error('Không niêm yết được tín chỉ', { description: apiMessage(error) }),
  })
}

export function useRetireCredits() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { batchCode: string; quantity: string }) =>
      api.post<unknown>('/my/credits/retire', {
        data: { batchCode: input.batchCode, quantity: Number(input.quantity) },
      }),
    onSuccess: () => {
      toast.success('Đã triệt tiêu tín chỉ')
      void queryClient.invalidateQueries({ queryKey: queryKeys.companyBatches })
    },
    onError: (error) => toast.error('Không triet tiêu được tín chỉ', { description: apiMessage(error) }),
  })
}

export function useOrders() {
  return useQuery({
    queryKey: queryKeys.orders,
    queryFn: async () => mapItems(await api.get<unknown>('/orders'), mapOrder),
  })
}

export function useCompleteOrder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.post<unknown>(`/orders/${id}/complete`),
    onSuccess: () => {
      toast.success('Đơn hàng đã hoàn tất')
      void queryClient.invalidateQueries({ queryKey: queryKeys.orders })
    },
    onError: (error) => toast.error('Không hoàn tất được đơn hàng', { description: apiMessage(error) }),
  })
}

export function useCancelOrder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.delete<unknown>(`/orders/${id}`),
    onSuccess: () => {
      toast.success('Đơn hàng đã bị huỷ')
      void queryClient.invalidateQueries({ queryKey: queryKeys.orders })
    },
    onError: (error) => toast.error('Không huỷ được đơn hàng', { description: apiMessage(error) }),
  })
}

export function downloadReportCsv(reportId: number): Promise<void> {
  return downloadFile(`/reports/${reportId}/export`, `emission_report_${reportId}.csv`)
}

export function downloadCertificate(userId: number): Promise<void> {
  return downloadFile(`/reports/download/${userId}`, `certificate-${userId}.pdf`)
}
