import { z } from 'zod'

const required = (label: string) => z.string().trim().min(1, `Vui lòng nhập ${label}`)

export const kycSchema = z.object({
  name: required('họ tên').min(2, 'Họ tên phải có ít nhất 2 ký tự'),
  phone: z
    .string()
    .trim()
    .regex(/^(0|\+84)\d{9,10}$/, 'Số điện thoại không đúng định dạng'),
  country: required('quốc gia'),
  address: required('địa chỉ'),
  birthDate: z
    .string()
    .min(1, 'Vui lòng chọn ngày sinh')
    .refine((value) => !Number.isNaN(Date.parse(value)) && new Date(value) <= new Date(), 'Ngày sinh không hợp lệ'),
  documentType: z.enum(['CCCD', 'CMND', 'DRIVE_CARD'], {
    message: 'Vui lòng chọn loại giấy tờ',
  }),
  documentNumber: required('số giấy tờ').min(5, 'Số giấy tờ phải có ít nhất 5 ký tự'),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER'], {
    message: 'Vui lòng chọn giới tính',
  }),
})

export type KycValues = z.infer<typeof kycSchema>

export const vehicleSchema = z.object({
  plateNumber: required('biển số xe').min(3, 'Biển số xe phải có ít nhất 3 ký tự'),
  brand: required('hãng xe'),
  model: required('mẫu xe'),
  companyId: z
    .string()
    .trim()
    .min(1, 'Vui lòng nhập mã doanh nghiệp')
    .refine((value) => Number.isInteger(Number(value)) && Number(value) > 0, 'Mã doanh nghiệp phải là số nguyên dương'),
})

export type VehicleValues = z.infer<typeof vehicleSchema>
