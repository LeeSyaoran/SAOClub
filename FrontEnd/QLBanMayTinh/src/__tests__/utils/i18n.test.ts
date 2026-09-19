import { describe, it, expect } from 'vitest';
import { t } from '../../i18n/index.js';
import { statusLabel } from '../../utils/adminFormat.js';

describe('i18n and user-friendly display tests', () => {
  it('should translate login submit and submitting without raw keys', () => {
    expect(t('login.submit')).toBe('ĐĂNG NHẬP HỆ THỐNG');
    expect(t('login.submitting')).toBe('Đang đăng nhập...');
    expect(t('login.orLoginWith')).toBe('Hoặc đăng nhập với');
  });

  it('should translate register account prompt without raw keys', () => {
    expect(t('register.hasAccount')).toBe('Đã có tài khoản?');
    expect(t('register.haveAccount')).toBe('Đã có tài khoản?');
    expect(t('register.submitting')).toBe('Đang đăng ký...');
  });

  it('should translate admin status labels regardless of casing', () => {
    expect(statusLabel('active')).toBe('Hoạt động');
    expect(statusLabel('ACTIVE')).toBe('Hoạt động');
    expect(statusLabel('cho_xu_ly')).toBe('Chờ xử lý');
    expect(statusLabel('CHO_XU_LY')).toBe('Chờ xử lý');
    expect(statusLabel('con_hang')).toBe('Còn hàng');
    expect(statusLabel('CON_HANG')).toBe('Còn hàng');
    expect(statusLabel(null)).toBe('—');
    expect(statusLabel(undefined)).toBe('—');
    expect(statusLabel('')).toBe('—');
  });

  it('should gracefully fallback to original string if status is unknown', () => {
    const unknown = 'CustomStatusTest';
    expect(statusLabel(unknown)).toBe(unknown);
    expect(statusLabel(unknown)).not.toContain('admin.statusLabel');
  });

  it('should support fallback argument in t()', () => {
    expect(t('totally.nonexistent.key', 'Dự phòng thân thiện')).toBe('Dự phòng thân thiện');
    expect(t('totally.nonexistent.key', { name: 'SAO' }, 'Xin chào {name}')).toBe('Xin chào SAO');
  });

  it('should translate admin keys previously reported as missing', () => {
    expect(t('admin.customerDetail.wishlistEmpty')).toBe('Khách hàng chưa có sản phẩm yêu thích nào.');
    expect(t('admin.pos.barcodePlaceholder')).toBe('Quét mã vạch hoặc nhập mã serial / SKU...');
    expect(t('admin.toast.serialDeleted', { serial: 'SN12345' })).toBe('Đã xóa serial SN12345 khỏi kho');
    expect(t('admin.saving')).toBe('Đang lưu...');
    expect(t('checkout.errValidation')).toBe('Vui lòng kiểm tra lại thông tin giao hàng.');
    expect(t('checkout.promoChecking')).toBe('Đang kiểm tra mã...');
  });
});
