import { describe, it, expect } from 'vitest';
import { registerSchema, isValidPhoneNumber, checkoutInfoSchema } from '../../utils/validators.js';

describe('registerSchema', () => {
  const validBase = {
    hoTen: 'Nguyễn Văn A',
    soDienThoai: '0900000000',
    email: '',
    username: 'nguyenvana',
    password: '123456',
    confirmPassword: '123456',
  };

  // RegisterForm.vue đọc values.agree sau khi handleSubmit parse qua schema này —
  // nếu schema không khai báo field "agree", Zod tự động strip field lạ khỏi kết quả,
  // khiến values.agree luôn undefined dù checkbox đã tick, form báo "chưa đồng ý" mãi mãi.
  it('giữ nguyên agree=true sau khi parse, không bị Zod strip mất', () => {
    const result = registerSchema.parse({ ...validBase, agree: true });
    expect(result.agree).toBe(true);
  });
});

describe('isValidPhoneNumber', () => {
  it('trả về true cho số điện thoại hợp lệ 10 số bắt đầu bằng 0', () => {
    expect(isValidPhoneNumber('0987654321')).toBe(true);
    expect(isValidPhoneNumber('0312345678')).toBe(true);
  });

  it('trả về false cho chuỗi rỗng, null, hoặc undefined', () => {
    expect(isValidPhoneNumber('')).toBe(false);
    expect(isValidPhoneNumber(null)).toBe(false);
    expect(isValidPhoneNumber(undefined)).toBe(false);
  });

  it('chặn hoàn toàn mã định danh Google SSO hoặc Facebook', () => {
    expect(isValidPhoneNumber('google_6D9nHv6pmSWMD')).toBe(false);
    expect(isValidPhoneNumber('fb_123456789')).toBe(false);
  });

  it('chặn số điện thoại chứa ký tự chữ hoặc không đủ độ dài', () => {
    expect(isValidPhoneNumber('098abc1234')).toBe(false);
    expect(isValidPhoneNumber('12345')).toBe(false);
  });
});

describe('checkoutInfoSchema', () => {
  it('thành công khi thông tin hợp lệ', () => {
    const res = checkoutInfoSchema.safeParse({
      nguoiNhan: 'Nguyễn Văn B',
      sdtNguoiNhan: '0912345678',
      diaChiGiaoHangText: '123 Nguyễn Huệ, Q.1, TP.HCM',
    });
    expect(res.success).toBe(true);
  });

  it('thất bại khi sdtNguoiNhan là mã google_...', () => {
    const res = checkoutInfoSchema.safeParse({
      nguoiNhan: 'Nguyễn Văn B',
      sdtNguoiNhan: 'google_6D9nHv6pmSWMD',
      diaChiGiaoHangText: '123 Nguyễn Huệ',
    });
    expect(res.success).toBe(false);
  });
});
