import { useQuery } from '@tanstack/react-query'
import { api } from '@/shared/api/client'
import { mapList, mapProject } from '@/shared/api/mappers'
import { queryKeys } from '@/shared/lib/queryKeys'

export function useProjects() {
  return useQuery({
    queryKey: queryKeys.projects,
    queryFn: async () => {
      const raw = await api.get<unknown>('/projects')
      return mapList(raw, mapProject)
    },
  })
}
