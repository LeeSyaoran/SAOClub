import { describe, it, expect, vi } from 'vitest';
import {
  statusLabel,
  formatPrice,
  formatDate,
  formatDateTime,
  toLocalDT,
  boDauTiengViet,
} from '../../utils/adminFormat.js';

vi.mock('../../i18n/index.js', () => ({
  t: vi.fn((key: string) => {
    const dict: Record<string, string> = {
      'admin.statusLabel.completed': 'Hoàn thành',
      'admin.statusLabel.pending': 'Chờ xử lý',
      'orderStatus.shipping': 'Đang giao hàng',
      'admin.returnStatus.rejected': 'Từ chối trả hàng',
    };
    return dict[key] || key;
  }),
}));

vi.mock('../../utils/formatPrice.js', () => ({
  formatPrice: vi.fn((val) => `${val} ₫`),
}));

describe('adminFormat utility', () => {
  describe('statusLabel', () => {
    it('trả về "—" khi giá trị là null hoặc chuỗi rỗng', () => {
      expect(statusLabel(null)).toBe('—');
      expect(statusLabel('')).toBe('—');
      expect(statusLabel(undefined)).toBe('—');
    });

    it('tra cứu đúng nhãn trạng thái từ admin.statusLabel', () => {
      expect(statusLabel('completed')).toBe('Hoàn thành');
      expect(statusLabel('pending')).toBe('Chờ xử lý');
    });

    it('tra cứu nhãn trạng thái từ orderStatus khi không có trong admin.statusLabel', () => {
      expect(statusLabel('shipping')).toBe('Đang giao hàng');
    });

    it('tra cứu nhãn trạng thái từ admin.returnStatus', () => {
      expect(statusLabel('rejected')).toBe('Từ chối trả hàng');
    });

    it('trả về chuỗi gốc khi không tìm thấy bản dịch', () => {
      expect(statusLabel('unknown_status')).toBe('unknown_status');
    });
  });

  describe('formatPrice', () => {
    it('trả về "—" khi giá trị là null hoặc undefined', () => {
      expect(formatPrice(null)).toBe('—');
      expect(formatPrice(undefined)).toBe('—');
    });

    it('gọi hàm formatPrice gốc khi có giá trị hợp lệ', () => {
      expect(formatPrice(500000)).toBe('500000 ₫');
    });
  });

  describe('formatDate and formatDateTime', () => {
    it('formatDate trả về "—" khi đầu vào rỗng', () => {
      expect(formatDate(null)).toBe('—');
      expect(formatDate('')).toBe('—');
    });

    it('formatDate định dạng ngày hợp lệ theo chuẩn vi-VN', () => {
      const res = formatDate('2026-09-22T08:00:00Z');
      expect(res).toBeTruthy();
      expect(res).not.toBe('—');
    });

    it('formatDateTime trả về "—" khi đầu vào rỗng', () => {
      expect(formatDateTime(null)).toBe('—');
      expect(formatDateTime('')).toBe('—');
    });

    it('formatDateTime định dạng ngày giờ hợp lệ theo chuẩn vi-VN', () => {
      const res = formatDateTime('2026-09-22T08:00:00Z');
      expect(res).toBeTruthy();
      expect(res).not.toBe('—');
    });
  });

  describe('toLocalDT', () => {
    it('trả về null khi truyền giá trị falsy', () => {
      expect(toLocalDT(null)).toBeNull();
      expect(toLocalDT('')).toBeNull();
    });

    it('thêm :00 nếu chuỗi có độ dài 16 (YYYY-MM-DDTHH:mm)', () => {
      expect(toLocalDT('2026-09-22T10:30')).toBe('2026-09-22T10:30:00');
    });

    it('cắt lấy 19 ký tự đầu nếu chuỗi dài hơn (loại bỏ millisecond/timezone)', () => {
      expect(toLocalDT('2026-09-22T10:30:45.123Z')).toBe('2026-09-22T10:30:45');
    });
  });

  describe('boDauTiengViet', () => {
    it('xử lý chuỗi rỗng hoặc null', () => {
      expect(boDauTiengViet(null)).toBe('');
      expect(boDauTiengViet('')).toBe('');
    });

    it('loại bỏ chuẩn xác các dấu thanh tiếng Việt và chuyển thường', () => {
      expect(boDauTiengViet('Máy Tính Xách Tay')).toBe('may tinh xach tay');
      expect(boDauTiengViet('Cấu Hình Khủng')).toBe('cau hinh khung');
    });

    it('chuyển đổi ký tự đ và Đ thành d', () => {
      expect(boDauTiengViet('Đơn Hàng Được Đặt')).toBe('don hang duoc dat');
    });
  });
});
