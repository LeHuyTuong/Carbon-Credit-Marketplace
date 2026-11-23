import { z } from 'zod'

export const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'Email không được để trống')
    .email('Email không đúng định dạng'),
  password: z.string().min(1, 'Mật khẩu không được để trống'),
})

export type LoginValues = z.infer<typeof loginSchema>

const passwordRule = z
  .string()
  .min(8, 'Mật khẩu phải có ít nhất 8 ký tự')
  .regex(/[0-9]/, 'Mật khẩu phải có ít nhất một chữ số')
  .regex(/[a-z]/, 'Mật khẩu phải có ít nhất một chữ thường')
  .regex(/[A-Z]/, 'Mật khẩu phải có ít nhất một chữ hoa')
  .regex(/[@$!%*?&]/, 'Mật khẩu phải có một ký tự đặc biệt (@$!%*?&)')

export const registerSchema = z
  .object({
    email: z
      .string()
      .min(1, 'Email không được để trống')
      .email('Email không đúng định dạng'),
    password: passwordRule,
    confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu'),
    roleName: z.enum(['EV_OWNER', 'CC_BUYER', 'CVA'], {
      message: 'Vui lòng chọn vai trò',
    }),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })

export type RegisterValues = z.infer<typeof registerSchema>
