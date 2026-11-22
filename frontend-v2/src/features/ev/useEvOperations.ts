import { isAxiosError } from 'axios'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { api } from '@/shared/api/client'
import { asList, mapVehicle, type Vehicle } from '@/shared/api/domain'
import { apiMessage } from '@/shared/lib/errors'
import { queryKeys } from '@/shared/lib/queryKeys'
import type { KycValues, VehicleValues } from './schemas'

type Raw = Record<string, unknown>

function isRecord(value: unknown): value is Raw {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

export function useKyc() {
  return useQuery({
    queryKey: queryKeys.kyc,
    queryFn: async (): Promise<Partial<KycValues> | null> => {
      try {
        const payload = await api.get<unknown>('/kyc/user')
        if (!isRecord(payload)) return null
        const documentType = String(
          isRecord(payload.documentType) ? payload.documentType.name ?? '' : payload.documentType ?? '',
        ).toUpperCase()
        const gender = String(
          isRecord(payload.gender) ? payload.gender.name ?? '' : payload.gender ?? '',
        ).toUpperCase()
        return {
          name: String(payload.name ?? ''),
          phone: String(payload.phone ?? ''),
          country: String(payload.country ?? ''),
          address: String(payload.address ?? ''),
          birthDate: String(payload.birthDate ?? '').slice(0, 10),
          documentType: (['CCCD', 'CMND', 'DRIVE_CARD'].includes(documentType)
            ? documentType
            : 'CCCD') as KycValues['documentType'],
          documentNumber: String(payload.documentNumber ?? ''),
          gender: (['MALE', 'FEMALE', 'OTHER'].includes(gender) ? gender : 'MALE') as KycValues['gender'],
        }
      } catch (error) {
        if (isAxiosError(error) && error.response?.status === 404) return null
        throw error
      }
    },
  })
}

export function useSubmitKyc() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (values: KycValues) => api.post<unknown>('/kyc/kycEv', values),
    onSuccess: () => {
      toast.success('Hồ sơ KYC đã được gửi')
      void queryClient.invalidateQueries({ queryKey: queryKeys.kyc })
    },
    onError: (error) => toast.error('Không gửi được hồ sơ KYC', { description: apiMessage(error) }),
  })
}

export function useVehicles() {
  return useQuery({
    queryKey: queryKeys.vehicles,
    queryFn: async (): Promise<Vehicle[]> => {
      try {
        const payload = await api.get<unknown>('/vehicles')
        return asList(payload).map((item) => (isRecord(item) ? mapVehicle(item) : null)).filter((item): item is Vehicle => item !== null)
      } catch (error) {
        if (isAxiosError(error) && error.response?.status === 404) return []
        throw error
      }
    },
  })
}

export interface SaveVehicleInput {
  id?: number
  values: VehicleValues
  file: File | null
}

export function useSaveVehicle() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, values, file }: SaveVehicleInput) => {
      const form = new FormData()
      form.append('plateNumber', values.plateNumber)
      form.append('brand', values.brand)
      form.append('model', values.model)
      form.append('companyId', values.companyId)
      if (file) form.append('documentFile', file)
      return id
        ? api.put<unknown>(`/vehicles/${id}`, form)
        : api.post<unknown>('/vehicles', form)
    },
    onSuccess: (_, { id }) => {
      toast.success(id ? 'Đã cập nhật xe' : 'Đã thêm xe điện')
      void queryClient.invalidateQueries({ queryKey: queryKeys.vehicles })
    },
    onError: (error) => toast.error('Không lưu được thông tin xe', { description: apiMessage(error) }),
  })
}

export function useDeleteVehicle() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.delete<unknown>(`/vehicles/${id}`),
    onSuccess: () => {
      toast.success('Đã xoá xe điện')
      void queryClient.invalidateQueries({ queryKey: queryKeys.vehicles })
    },
    onError: (error) => toast.error('Không xoá được xe', { description: apiMessage(error) }),
  })
}
