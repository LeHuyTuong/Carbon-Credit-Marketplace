import { toast } from 'sonner'
import { API_BASE, apiClient } from './client'

/** Tải file cần Bearer token nên không dùng <a href> trực tiếp. */
export async function downloadFile(path: string, filename: string): Promise<void> {
  try {
    const response = await apiClient.get<Blob>(`${API_BASE}${path}`, {
      responseType: 'blob',
      baseURL: '',
    })
    const objectUrl = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')
    anchor.href = objectUrl
    anchor.download = filename
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
    URL.revokeObjectURL(objectUrl)
    toast.success('Đã tải file xuống')
  } catch {
    toast.error('Không tải được file', {
      description: 'Kiểm tra quyền truy cập và trạng thái của bản ghi.',
    })
  }
}
