import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import {
  asList,
  mapAdminProject,
  mapAdminUser,
  mapCreditBatch,
  mapReport,
  mapWithdrawalAdmin,
  type AdminProject,
  type AdminUser,
  type AdminWithdrawal,
  type CreditBatch,
  type EmissionReport,
} from '@/shared/api/domain'
import { apiMessage } from '@/shared/lib/errors'
import { queryKeys } from '@/shared/lib/queryKeys'
import type { AdminProjectValues } from './schemas'

type Raw = Record<string, unknown>

function isRecord(value: unknown): value is Raw {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function mapItems<T>(payload: unknown, map: (raw: Raw) => T | null): T[] {
  return asList(payload).map((item) => (isRecord(item) ? map(item) : null)).filter((item): item is T => item !== null)
}

export function useAdminProjects() {
  return useQuery({
    queryKey: queryKeys.adminProjects,
    queryFn: async (): Promise<AdminProject[]> => mapItems(await api.get<unknown>('/projects'), mapAdminProject),
  })
}

export interface SaveProjectInput {
  id?: number
  values: AdminProjectValues
  logo: File | null
  legalDocsFile: File | null
}

export function useSaveAdminProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, values, logo, legalDocsFile }: SaveProjectInput) => {
      const form = new FormData()
      for (const [key, value] of Object.entries(values)) {
        if (['bufferReservePct', 'uncertaintyPct', 'leakagePct'].includes(key)) continue
        if (value !== '') form.append(key, value)
      }
      // Backend DTO chưa nhận 3 hệ số này; vẫn gửi để chuẩn bị khi API mở field.
      form.append('bufferReservePct', values.bufferReservePct || '0')
      form.append('uncertaintyPct', values.uncertaintyPct || '0')
      form.append('leakagePct', values.leakagePct || '0')
      if (logo) form.append('logo', logo)
      if (legalDocsFile) form.append('legalDocsFile', legalDocsFile)
      return id
        ? api.put<unknown>(`/projects/${id}`, form)
        : api.post<unknown>('/projects', form)
    },
    onSuccess: (_, { id }) => {
      toast.success(id ? 'Đã cập nhật dự án' : 'Đã tạo dự án')
      void queryClient.invalidateQueries({ queryKey: queryKeys.adminProjects })
    },
    onError: (error) => toast.error('Không lưu được dự án', { description: apiMessage(error) }),
  })
}

export function useDeleteAdminProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.delete<unknown>(`/projects/${id}`),
    onSuccess: () => {
      toast.success('Đã xoá dự án')
      void queryClient.invalidateQueries({ queryKey: queryKeys.adminProjects })
    },
    onError: (error) => toast.error('Không xoá được dự án', { description: apiMessage(error) }),
  })
}

export function useAdminReports(page: number, size: number) {
  return useQuery({
    queryKey: [...queryKeys.adminIssuance, page, size] as const,
    queryFn: async (): Promise<EmissionReport[]> =>
      mapItems(await api.get<unknown>('/reports/list', { page, size }), mapReport),
  })
}

export function useApproveReport() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, approved, note }: { id: number; approved: boolean; note: string }) =>
      api.put<unknown>(`/reports/${id}/approve`, undefined, { params: { approved, note } }),
    onSuccess: (_, { approved }) => {
      toast.success(approved ? 'Đã phê duyệt báo cáo' : 'Đã từ chối báo cáo')
      void queryClient.invalidateQueries({ queryKey: queryKeys.adminIssuance })
    },
    onError: (error) => toast.error('Không cập nhật được báo cáo', { description: apiMessage(error) }),
  })
}

export function useCreditPreview(reportId: number | null) {
  return useQuery({
    queryKey: [...queryKeys.adminIssuance, reportId, 'preview'] as const,
    enabled: reportId !== null,
    queryFn: async (): Promise<CreditBatch | null> => {
      const payload = await api.get<unknown>(`/credits/preview/${reportId}`)
      return isRecord(payload) ? mapCreditBatch(payload, true) : null
    },
  })
}

export function useIssueCredits() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (reportId: number) => api.post<unknown>('/credits/issue', undefined, { params: { reportId } }),
    onSuccess: () => {
      toast.success('Đã phát hành lô tín chỉ')
      void queryClient.invalidateQueries({ queryKey: queryKeys.adminIssuance })
      void queryClient.invalidateQueries({ queryKey: queryKeys.companyBatches })
    },
    onError: (error) => toast.error('Không phát hành được tín chỉ', { description: apiMessage(error) }),
  })
}

export function useAdminWithdrawals() {
  return useQuery({
    queryKey: queryKeys.adminWithdrawals,
    queryFn: async (): Promise<AdminWithdrawal[]> =>
      mapItems(await api.get<unknown>('/withdrawal/admin'), mapWithdrawalAdmin),
  })
}

export function useProcessWithdrawal() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, accept }: { id: number; accept: boolean }) =>
      api.patch<unknown>(`/withdrawal/admin/${id}/process/${accept}`),
    onSuccess: (_, { accept }) => {
      toast.success(accept ? 'Đã duyệt lệnh rút tiền' : 'Đã từ chối lệnh rút tiền')
      void queryClient.invalidateQueries({ queryKey: queryKeys.adminWithdrawals })
    },
    onError: (error) => toast.error('Không xử lý được lệnh rút tiền', { description: apiMessage(error) }),
  })
}

export function useAdminUsers() {
  return useQuery({
    queryKey: queryKeys.adminUsers,
    queryFn: async (): Promise<AdminUser[]> => mapItems(await api.get<unknown>('/users'), mapAdminUser),
  })
}

export function useUpdateUserStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      api.patch<unknown>(`/users/${id}/status`, { status }),
    onSuccess: () => {
      toast.success('Đã cập nhật trạng thái người dùng')
      void queryClient.invalidateQueries({ queryKey: queryKeys.adminUsers })
    },
    onError: (error) => toast.error('Không cập nhật được trạng thái', { description: apiMessage(error) }),
  })
}

export type { AdminProject, AdminUser, AdminWithdrawal, CreditBatch }
