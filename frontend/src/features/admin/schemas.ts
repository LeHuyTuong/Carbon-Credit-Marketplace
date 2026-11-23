import { z } from 'zod'

const required = (label: string) => z.string().trim().min(1, `Vui lòng nhập ${label}`)
const percent = z
  .string()
  .trim()
  .refine((value) => value === '' || (Number(value) >= 0 && Number(value) <= 100), 'Giá trị phải từ 0 đến 100')

export const adminProjectSchema = z.object({
  title: required('tên dự án'),
  description: required('mô tả'),
  commitments: required('cam kết'),
  technicalIndicators: required('chỉ số kỹ thuật'),
  measurementMethod: required('phương pháp đo'),
  emissionFactorKgPerKwh: z
    .string()
    .trim()
    .refine((value) => value === '' || Number(value) >= 0, 'Hệ số phát thải không được âm'),
  status: z.enum(['OPEN', 'CLOSE', 'COMING_SOON'], {
    message: 'Vui lòng chọn trạng thái',
  }),
  startedDate: z.string(),
  endDate: z.string(),
  bufferReservePct: percent,
  uncertaintyPct: percent,
  leakagePct: percent,
})

export type AdminProjectValues = z.infer<typeof adminProjectSchema>
