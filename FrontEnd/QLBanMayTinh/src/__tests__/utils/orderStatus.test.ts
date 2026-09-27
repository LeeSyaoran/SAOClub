import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  Clock, CheckCircle2, Package, Truck, Bike, Inbox, PartyPopper, XCircle, Undo2,
  Wallet, Banknote, Smartphone, Landmark, CreditCard, Circle,
} from '@lucide/vue';

const mockT = vi.fn((key: string) => key);

vi.mock('../../i18n/index.js', () => ({ t: mockT }));

beforeEach(() => {
  mockT.mockImplementation((key: string) => key);
});

describe('orderStatusLabel', () => {
  it('should return translated label for each status', async () => {
    const { orderStatusLabel } = await import('../../utils/orderStatus.js');
    expect(orderStatusLabel('pending')).toBe('orderStatus.pending');
    expect(orderStatusLabel('delivered')).toBe('orderStatus.delivered');
    expect(mockT).toHaveBeenCalledWith('orderStatus.pending');
  });
});

describe('orderStatusColor', () => {
  it('should return correct color for pending', async () => {
    const { orderStatusColor } = await import('../../utils/orderStatus.js');
    expect(orderStatusColor('pending')).toEqual({ bg: 'rgba(148,163,184,0.15)', text: '#94a3b8' });
  });

  it('should return correct color for delivered', async () => {
    const { orderStatusColor } = await import('../../utils/orderStatus.js');
    expect(orderStatusColor('delivered')).toEqual({ bg: 'rgba(34,197,94,0.15)', text: '#22c55e' });
  });

  it('should return correct color for awaiting_confirmation', async () => {
    const { orderStatusColor } = await import('../../utils/orderStatus.js');
    expect(orderStatusColor('awaiting_confirmation')).toEqual({ bg: 'rgba(45,212,191,0.15)', text: '#2dd4bf' });
  });

  it('should return default color for unknown status', async () => {
    const { orderStatusColor } = await import('../../utils/orderStatus.js');
    expect(orderStatusColor('unknown')).toEqual({ bg: 'rgba(107,114,128,0.15)', text: '#9ca3af' });
  });
});

describe('orderStatusIcon', () => {
  it.each([
    ['pending', Clock],
    ['confirmed', CheckCircle2],
    ['processing', Package],
    ['shipping', Truck],
    ['out_for_delivery', Bike],
    ['awaiting_confirmation', Inbox],
    ['delivered', PartyPopper],
    ['cancelled', XCircle],
    ['returned', Undo2],
  ])('should map %s to its exact icon component', async (status, expectedIcon) => {
    const { orderStatusIcon } = await import('../../utils/orderStatus.js');
    expect(orderStatusIcon(status)).toBe(expectedIcon);
  });

  it('should return default icon component for unknown status', async () => {
    const { orderStatusIcon } = await import('../../utils/orderStatus.js');
    expect(orderStatusIcon('unknown')).toBe(Circle);
  });
});

describe('paymentStatusLabel', () => {
  it('should return translated label via t()', async () => {
    const { paymentStatusLabel } = await import('../../utils/orderStatus.js');
    expect(paymentStatusLabel('paid')).toBe('admin.paymentStatus.paid');
    expect(mockT).toHaveBeenCalledWith('admin.paymentStatus.paid');
  });
});

describe('paymentStatusColor', () => {
  it('should return correct color for paid', async () => {
    const { paymentStatusColor } = await import('../../utils/orderStatus.js');
    expect(paymentStatusColor('paid')).toEqual({ bg: 'rgba(34,197,94,0.15)', text: '#22c55e' });
  });

  it('should return default for unknown payment status', async () => {
    const { paymentStatusColor } = await import('../../utils/orderStatus.js');
    expect(paymentStatusColor('unknown')).toEqual({ bg: 'rgba(107,114,128,0.15)', text: '#9ca3af' });
  });
});

describe('paymentStatusIcon', () => {
  it.each([
    ['unpaid', Clock],
    ['partial', Wallet],
    ['paid', CheckCircle2],
    ['refunded', Undo2],
  ])('should map %s to its exact icon component', async (status, expectedIcon) => {
    const { paymentStatusIcon } = await import('../../utils/orderStatus.js');
    expect(paymentStatusIcon(status)).toBe(expectedIcon);
  });

  it('should return default icon component for unknown status', async () => {
    const { paymentStatusIcon } = await import('../../utils/orderStatus.js');
    expect(paymentStatusIcon('unknown')).toBe(Circle);
  });
});

describe('POS_PAYMENT_METHODS', () => {
  it('should list all payment methods', async () => {
    const { POS_PAYMENT_METHODS } = await import('../../utils/orderStatus.js');
    expect(POS_PAYMENT_METHODS).toEqual(['tien_mat', 'vnpay', 'chuyen_khoan', 'the_tin_dung']);
  });
});

describe('paymentMethodLabel', () => {
  it('should return translated label', async () => {
    const { paymentMethodLabel } = await import('../../utils/orderStatus.js');
    expect(paymentMethodLabel('tien_mat')).toBe('admin.paymentMethod.tien_mat');
    expect(mockT).toHaveBeenCalledWith('admin.paymentMethod.tien_mat');
  });
});

describe('paymentMethodIcon', () => {
  it.each([
    ['tien_mat', Banknote],
    ['vnpay', Smartphone],
    ['chuyen_khoan', Landmark],
    ['the_tin_dung', CreditCard],
  ])('should map %s to its exact icon component', async (method, expectedIcon) => {
    const { paymentMethodIcon } = await import('../../utils/orderStatus.js');
    expect(paymentMethodIcon(method)).toBe(expectedIcon);
  });

  it('should return default icon component for unknown method', async () => {
    const { paymentMethodIcon } = await import('../../utils/orderStatus.js');
    expect(paymentMethodIcon('unknown')).toBe(Wallet);
  });
});

describe('isQrPayment', () => {
  it('should return false for null/empty order', async () => {
    const { isQrPayment } = await import('../../utils/orderStatus.js');
    expect(isQrPayment(null)).toBe(false);
  });

  it('should return false for COD or tien_mat even if kenhBan is online', async () => {
    const { isQrPayment } = await import('../../utils/orderStatus.js');
    expect(isQrPayment({ phuongThucThanhToan: 'tien_mat', kenhBan: 'online' })).toBe(false);
    expect(isQrPayment({ phuongThucThanhToan: 'cod', kenhBan: 'online' })).toBe(false);
  });

  it('should return false when phuongThucThanhToan is missing or null', async () => {
    const { isQrPayment } = await import('../../utils/orderStatus.js');
    expect(isQrPayment({ phuongThucThanhToan: null, kenhBan: 'online' })).toBe(false);
    expect(isQrPayment({ kenhBan: 'online' })).toBe(false);
  });

  it('should return true for qr or bank transfer', async () => {
    const { isQrPayment } = await import('../../utils/orderStatus.js');
    expect(isQrPayment({ phuongThucThanhToan: 'qr' })).toBe(true);
    expect(isQrPayment({ phuongThucThanhToan: 'chuyen_khoan' })).toBe(true);
    expect(isQrPayment({ phuongThucThanhToan: 'vietqr' })).toBe(true);
  });

  it('should return true if thanhToans list contains a qr or transfer method', async () => {
    const { isQrPayment } = await import('../../utils/orderStatus.js');
    expect(isQrPayment({ thanhToans: [{ phuongThucThanhToan: 'chuyen_khoan' }] })).toBe(true);
  });
});

describe('getCodEffectiveStatus', () => {
  it('should return "Đang đóng gói" when trangThaiDonHang is processing', async () => {
    const { getCodEffectiveStatus } = await import('../../utils/orderStatus.js');
    const res = getCodEffectiveStatus({ trangThaiDonHang: 'processing' });
    expect(res.label).toBe('Đang đóng gói');
  });

  it('should return "Đã lên đơn" when trangThaiDonHang is confirmed', async () => {
    const { getCodEffectiveStatus } = await import('../../utils/orderStatus.js');
    const res = getCodEffectiveStatus({ trangThaiDonHang: 'confirmed' });
    expect(res.label).toBe('Đã lên đơn');
  });
});

describe('QR Timeline Step Logic', () => {
  it('when unpaid and pending: only tao_don is done, cho_thanh_toan is next', async () => {
    const { isQrStepDone, isQrStepNext, getQrEffectiveStatus } = await import('../../utils/orderStatus.js');
    const order = { phuongThucThanhToan: 'vietqr', trangThaiThanhToan: 'unpaid', trangThaiDonHang: 'pending' };

    expect(isQrStepDone(order, 'tao_don')).toBe(true);
    expect(isQrStepDone(order, 'cho_thanh_toan')).toBe(false);
    expect(isQrStepDone(order, 'cho_xu_ly')).toBe(false);
    expect(isQrStepDone(order, 'da_len_don')).toBe(false);

    expect(isQrStepNext(order, 'cho_thanh_toan')).toBe(true);
    expect(isQrStepNext(order, 'cho_xu_ly')).toBe(false);
    expect(isQrStepNext(order, 'da_len_don')).toBe(false);

    expect(getQrEffectiveStatus(order).label).toBe('Chờ thanh toán QR');
  });

  it('when paid and pending (admin has not processed yet): cho_thanh_toan is done, cho_xu_ly is NOT done, cho_xu_ly is next (glowing)', async () => {
    const { isQrStepDone, isQrStepNext, getQrEffectiveStatus } = await import('../../utils/orderStatus.js');
    const order = { phuongThucThanhToan: 'vietqr', trangThaiThanhToan: 'paid', trangThaiDonHang: 'pending' };

    expect(isQrStepDone(order, 'tao_don')).toBe(true);
    expect(isQrStepDone(order, 'cho_thanh_toan')).toBe(true);
    // 'cho_xu_ly' MUST NOT be done because admin has not processed yet
    expect(isQrStepDone(order, 'cho_xu_ly')).toBe(false);
    expect(isQrStepDone(order, 'da_len_don')).toBe(false);

    // 'cho_xu_ly' is the NEXT step (glowing) - waiting for admin action
    expect(isQrStepNext(order, 'cho_xu_ly')).toBe(true);
    expect(isQrStepNext(order, 'da_len_don')).toBe(false);

    expect(getQrEffectiveStatus(order).label).toBe('Chờ xử lý');
  });

  it('when paid and confirmed (admin has processed): da_len_don is done, dang_dong_goi is next (glowing)', async () => {
    const { isQrStepDone, isQrStepNext, getQrEffectiveStatus } = await import('../../utils/orderStatus.js');
    const order = { phuongThucThanhToan: 'vietqr', trangThaiThanhToan: 'paid', trangThaiDonHang: 'confirmed' };

    expect(isQrStepDone(order, 'tao_don')).toBe(true);
    expect(isQrStepDone(order, 'cho_thanh_toan')).toBe(true);
    expect(isQrStepDone(order, 'cho_xu_ly')).toBe(true);
    // 'da_len_don' has finished -> marked done (checked)
    expect(isQrStepDone(order, 'da_len_don')).toBe(true);
    expect(isQrStepDone(order, 'dang_dong_goi')).toBe(false);

    // Next step to be performed ('dang_dong_goi') is glowing
    expect(isQrStepNext(order, 'da_len_don')).toBe(false);
    expect(isQrStepNext(order, 'dang_dong_goi')).toBe(true);

    expect(getQrEffectiveStatus(order).label).toBe('Đã lên đơn');
  });

  it('when paid and processing: da_len_don is done, dang_dong_goi is done, dang_giao_hang is next (glowing)', async () => {
    const { isQrStepDone, isQrStepNext, getQrEffectiveStatus } = await import('../../utils/orderStatus.js');
    const order = { phuongThucThanhToan: 'vietqr', trangThaiThanhToan: 'paid', trangThaiDonHang: 'processing' };

    expect(isQrStepDone(order, 'tao_don')).toBe(true);
    expect(isQrStepDone(order, 'cho_thanh_toan')).toBe(true);
    expect(isQrStepDone(order, 'cho_xu_ly')).toBe(true);
    expect(isQrStepDone(order, 'da_len_don')).toBe(true);
    expect(isQrStepDone(order, 'dang_dong_goi')).toBe(true);
    expect(isQrStepDone(order, 'dang_giao_hang')).toBe(false);

    // Next step to be performed ('dang_giao_hang') is glowing
    expect(isQrStepNext(order, 'dang_dong_goi')).toBe(false);
    expect(isQrStepNext(order, 'dang_giao_hang')).toBe(true);

    expect(getQrEffectiveStatus(order).label).toBe('Đang đóng gói');
  });

  it('when paid and out_for_delivery: dang_giao_hang is done, da_giao is next (glowing)', async () => {
    const { isQrStepDone, isQrStepNext, getQrEffectiveStatus } = await import('../../utils/orderStatus.js');
    const order = { phuongThucThanhToan: 'vietqr', trangThaiThanhToan: 'paid', trangThaiDonHang: 'out_for_delivery' };

    expect(isQrStepDone(order, 'tao_don')).toBe(true);
    expect(isQrStepDone(order, 'cho_thanh_toan')).toBe(true);
    expect(isQrStepDone(order, 'cho_xu_ly')).toBe(true);
    expect(isQrStepDone(order, 'da_len_don')).toBe(true);
    expect(isQrStepDone(order, 'dang_dong_goi')).toBe(true);
    expect(isQrStepDone(order, 'dang_giao_hang')).toBe(true);
    expect(isQrStepDone(order, 'da_giao')).toBe(false);

    // Next step to be performed ('da_giao') is glowing
    expect(isQrStepNext(order, 'dang_giao_hang')).toBe(false);
    expect(isQrStepNext(order, 'da_giao')).toBe(true);

    expect(getQrEffectiveStatus(order).label).toBe('Đang giao hàng');
  });

  it('when paid and delivered: all 7 steps are done, none active', async () => {
    const { isQrStepDone, isQrStepNext, getQrEffectiveStatus } = await import('../../utils/orderStatus.js');
    const order = { phuongThucThanhToan: 'vietqr', trangThaiThanhToan: 'paid', trangThaiDonHang: 'delivered' };

    expect(isQrStepDone(order, 'tao_don')).toBe(true);
    expect(isQrStepDone(order, 'cho_thanh_toan')).toBe(true);
    expect(isQrStepDone(order, 'cho_xu_ly')).toBe(true);
    expect(isQrStepDone(order, 'da_len_don')).toBe(true);
    expect(isQrStepDone(order, 'dang_dong_goi')).toBe(true);
    expect(isQrStepDone(order, 'dang_giao_hang')).toBe(true);
    expect(isQrStepDone(order, 'da_giao')).toBe(true);

    expect(isQrStepNext(order, 'da_giao')).toBe(false);

    expect(getQrEffectiveStatus(order).label).toBe('Đã giao');
  });
});

