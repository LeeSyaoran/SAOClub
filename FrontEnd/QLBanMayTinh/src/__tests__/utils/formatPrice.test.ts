import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockDinhDangSo = { dinhDangSo: 'vi' };

vi.mock('../../stores/settings.js', () => ({
  SettingsStore: mockDinhDangSo,
}));

beforeEach(() => {
  mockDinhDangSo.dinhDangSo = 'vi';
});

describe('formatPrice', () => {
  it('should format VND with Vietnamese locale by default', async () => {
    const { formatPrice } = await import('../../utils/formatPrice.js');
    expect(formatPrice(1500000)).toBe('1.500.000 ₫');
  });

  it('should format VND with English locale when dinhDangSo is en', async () => {
    mockDinhDangSo.dinhDangSo = 'en';
    const { formatPrice } = await import('../../utils/formatPrice.js');
    expect(formatPrice(1500000)).toBe('₫1,500,000');
  });

  it('should handle zero', async () => {
    const { formatPrice } = await import('../../utils/formatPrice.js');
    expect(formatPrice(0)).toBe('0 ₫');
  });

  it('should handle undefined/null as zero', async () => {
    const { formatPrice } = await import('../../utils/formatPrice.js');
    expect(formatPrice(undefined as any)).toBe('0 ₫');
    expect(formatPrice(null as any)).toBe('0 ₫');
  });

  it('should handle large numbers', async () => {
    const { formatPrice } = await import('../../utils/formatPrice.js');
    expect(formatPrice(999999999)).toBe('999.999.999 ₫');
  });
});
