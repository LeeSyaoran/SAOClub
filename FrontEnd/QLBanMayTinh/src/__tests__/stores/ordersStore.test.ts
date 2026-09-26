import { describe, it, expect, vi, beforeEach } from 'vitest';
import { OrdersStore, resetOrders, refreshOrders, ensureOrders } from '../../stores/orders.js';
import * as DonHangService from '../../services/DonHangService.js';

vi.mock('../../services/DonHangService.js', () => ({
  getAll: vi.fn(),
}));

describe('OrdersStore', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetOrders();
  });

  it('resetOrders - xóa toàn bộ items và đặt loaded về false', () => {
    OrdersStore.items = [{ donHangId: 1 } as any];
    OrdersStore.loaded = true;

    resetOrders();

    expect(OrdersStore.items).toEqual([]);
    expect(OrdersStore.loaded).toBe(false);
  });

  it('refreshOrders - tải danh sách đơn hàng và cập nhật trạng thái', async () => {
    const mockList = [{ donHangId: 10, tongTien: 15000000 }];
    vi.mocked(DonHangService.getAll).mockResolvedValue(mockList as any);

    const resultPromise = refreshOrders();
    expect(OrdersStore.loading).toBe(true);

    const result = await resultPromise;

    expect(OrdersStore.loading).toBe(false);
    expect(OrdersStore.loaded).toBe(true);
    expect(OrdersStore.items).toEqual(mockList);
    expect(result).toEqual(mockList);
  });

  it('refreshOrders - xử lý lỗi khi API getAll thất bại', async () => {
    vi.mocked(DonHangService.getAll).mockRejectedValue(new Error('Network error'));

    const result = await refreshOrders();

    expect(OrdersStore.loading).toBe(false);
    expect(OrdersStore.loaded).toBe(true);
    expect(OrdersStore.items).toEqual([]);
    expect(result).toEqual([]);
  });

  it('ensureOrders - chỉ gọi refreshOrders một lần khi đã có promise đang thực hiện', async () => {
    const mockList = [{ donHangId: 20 }];
    vi.mocked(DonHangService.getAll).mockResolvedValue(mockList as any);

    const p1 = ensureOrders();
    const p2 = ensureOrders();

    expect(p1).toBe(p2);
    await Promise.all([p1, p2]);

    expect(DonHangService.getAll).toHaveBeenCalledTimes(1);
  });
});
