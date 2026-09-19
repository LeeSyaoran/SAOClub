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

export const isValidPhoneNumber = (phone) => {
  if (!phone || typeof phone !== 'string') return false;
  const clean = phone.trim();
  if (clean.startsWith('google_') || clean.startsWith('fb_')) return false;
  return /^0[0-9]{9,10}$/.test(clean);
};

export const checkoutInfoSchema = z.object({
  nguoiNhan: z.string().min(1, 'Vui lòng nhập tên người nhận'),
  sdtNguoiNhan: z.string()
    .min(1, 'Vui lòng nhập số điện thoại người nhận')
    .refine((val) => isValidPhoneNumber(val) || val === 'logged-in', {
      message: 'Số điện thoại nhận hàng không hợp lệ (cần 10 số bắt đầu bằng 0)',
    }),
  diaChiGiaoHangText: z.string().min(1, 'Vui lòng nhập địa chỉ giao hàng'),
  soDienThoai: z.string().optional(),
  hoTen: z.string().optional(),
  email: emailSchema,
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
  // Không khai báo field này thì Zod tự strip khỏi kết quả parse (default: "strip"
  // unknown keys) — RegisterForm.vue đọc values.agree sau handleSubmit nên sẽ luôn
  // undefined dù checkbox đã tick, báo "chưa đồng ý điều khoản" mãi mãi.
  agree: z.boolean().optional(),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Mật khẩu xác nhận không khớp',
  path: ['confirmPassword'],
});
