import { z } from 'zod';

export const phoneSchema = z.string()
  .min(10, 'Số điện thoại phải có ít nhất 10 số')
  .max(11, 'Số điện thoại tối đa 11 số')
  .regex(/^0[0-9]{9,10}$/, 'Số điện thoại không hợp lệ');

export const emailSchema = z.string()
  .email('Email không hợp lệ')
  .optional()
  .or(z.literal(''));

export const passwordSchema = z.string()
  .min(6, 'Mật khẩu phải có ít nhất 6 ký tự');

export const requiredString = (fieldName) => z.string().min(1, `${fieldName} không được để trống`);

export const numberSchema = z.number().min(0, 'Giá trị phải >= 0');

export const checkoutInfoSchema = z.object({
  soDienThoai: z.string().min(1, 'Vui lòng nhập số điện thoại'),
  hoTen: z.string().min(1, 'Vui lòng nhập họ tên'),
  nguoiNhan: z.string().min(1, 'Vui lòng nhập tên người nhận'),
  sdtNguoiNhan: z.string().min(1, 'Vui lòng nhập SĐT người nhận'),
  diaChiGiaoHangText: z.string().min(1, 'Vui lòng nhập địa chỉ giao hàng'),
});

export const loginSchema = z.object({
  username: requiredString('Tên đăng nhập'),
  password: requiredString('Mật khẩu'),
});

export const registerSchema = z.object({
  hoTen: requiredString('Họ tên'),
  soDienThoai: phoneSchema,
  email: emailSchema,
  username: z.string().min(3, 'Tên đăng nhập phải có ít nhất 3 ký tự'),
  password: passwordSchema,
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Mật khẩu xác nhận không khớp',
  path: ['confirmPassword'],
});
