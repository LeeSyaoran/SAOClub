import { describe, it, expect, vi, beforeEach } from 'vitest';

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
  it('should return correct icon for each status', async () => {
    const { orderStatusIcon } = await import('../../utils/orderStatus.js');
    expect(orderStatusIcon('pending')).toBe('⏳');
    expect(orderStatusIcon('confirmed')).toBe('✅');
    expect(orderStatusIcon('processing')).toBe('📦');
    expect(orderStatusIcon('shipping')).toBe('🚚');
    expect(orderStatusIcon('out_for_delivery')).toBe('🛵');
    expect(orderStatusIcon('awaiting_confirmation')).toBe('📬');
    expect(orderStatusIcon('delivered')).toBe('🎉');
    expect(orderStatusIcon('cancelled')).toBe('❌');
    expect(orderStatusIcon('returned')).toBe('↩️');
  });

  it('should return default icon for unknown status', async () => {
    const { orderStatusIcon } = await import('../../utils/orderStatus.js');
    expect(orderStatusIcon('unknown')).toBe('●');
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
  it('should return correct icon for each status', async () => {
    const { paymentStatusIcon } = await import('../../utils/orderStatus.js');
    expect(paymentStatusIcon('unpaid')).toBe('⏳');
    expect(paymentStatusIcon('partial')).toBe('💰');
    expect(paymentStatusIcon('paid')).toBe('✅');
    expect(paymentStatusIcon('refunded')).toBe('↩️');
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
  it('should return correct icon for each method', async () => {
    const { paymentMethodIcon } = await import('../../utils/orderStatus.js');
    expect(paymentMethodIcon('tien_mat')).toBe('💵');
    expect(paymentMethodIcon('vnpay')).toBe('📱');
    expect(paymentMethodIcon('chuyen_khoan')).toBe('🏦');
    expect(paymentMethodIcon('the_tin_dung')).toBe('💳');
  });

  it('should return default icon for unknown method', async () => {
    const { paymentMethodIcon } = await import('../../utils/orderStatus.js');
    expect(paymentMethodIcon('unknown')).toBe('💰');
  });
});
