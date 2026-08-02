import { describe, it, expect } from 'vitest';
import { registerSchema } from '../../utils/validators.js';

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
