import { isAxiosError } from 'axios'

interface ErrorBody {
  message?: string
  responseStatus?: { responseMessage?: string }
}

export function apiMessage(error: unknown, fallback = 'Yêu cầu không thành công'): string {
  if (!isAxiosError<ErrorBody>(error)) return fallback
  return (
    error.response?.data?.responseStatus?.responseMessage ||
    error.response?.data?.message ||
    fallback
  )
}
