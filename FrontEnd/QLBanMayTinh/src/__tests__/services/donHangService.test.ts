import { describe, it, expect, vi, beforeEach } from 'vitest';
import * as DonHangService from '../../services/DonHangService.js';
import * as api from '../../services/api.js';

vi.mock('../../services/api.js', () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  del: vi.fn(),
  authHeaders: vi.fn(() => ({ Authorization: 'Bearer token123' })),
}));

describe('DonHangService API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('tinhPhiVanChuyen - gọi POST /api/don-hang/tinh-phi-van-chuyen', async () => {
    const payload = { provinceId: 201, districtId: 1442 };
    vi.mocked(api.post).mockResolvedValue({ phiVanChuyen: 30000 });

    const res = await DonHangService.tinhPhiVanChuyen(payload);

    expect(api.post).toHaveBeenCalledWith('/api/don-hang/tinh-phi-van-chuyen', payload);
    expect(res).toEqual({ phiVanChuyen: 30000 });
  });

  it('getPage - gọi GET /api/don-hang với query params', async () => {
    vi.mocked(api.get).mockResolvedValue({ content: [{ donHangId: 1 }] });

    const res = await DonHangService.getPage({ page: 1, size: 10, khachHangId: 5 });

    expect(api.get).toHaveBeenCalledWith('/api/don-hang?page=1&size=10&khachHangId=5');
    expect(res).toEqual({ content: [{ donHangId: 1 }] });
  });

  it('getAll - gọi getPage size 200 và trả về mảng content', async () => {
    vi.mocked(api.get).mockResolvedValue({
      content: [{ donHangId: 10 }, { donHangId: 20 }],
    });

    const items = await DonHangService.getAll();

    expect(api.get).toHaveBeenCalledWith('/api/don-hang?page=0&size=200');
    expect(items).toHaveLength(2);
    expect(items[0].donHangId).toBe(10);
  });

  it('create - gọi POST /api/don-hang', async () => {
    const body = { khachHangId: 1, tongTien: 10000000 };
    vi.mocked(api.post).mockResolvedValue({ donHangId: 99, ...body });

    const result = await DonHangService.create(body);

    expect(api.post).toHaveBeenCalledWith('/api/don-hang', body);
    expect(result.donHangId).toBe(99);
  });

  it('checkoutComplete - gọi POST /api/don-hang/checkout-complete', async () => {
    const payload = { items: [{ bienTheId: 1, soLuong: 2 }] };
    vi.mocked(api.post).mockResolvedValue({ status: 'success' });

    const result = await DonHangService.checkoutComplete(payload);

    expect(api.post).toHaveBeenCalledWith('/api/don-hang/checkout-complete', payload);
    expect(result.status).toBe('success');
  });

  it('update - gọi PUT /api/don-hang/update/:id', async () => {
    const body = { ghiChu: 'Giao buổi sáng' };
    vi.mocked(api.put).mockResolvedValue({ success: true });

    await DonHangService.update(12, body);

    expect(api.put).toHaveBeenCalledWith('/api/don-hang/update/12', body);
  });

  it('remove - gọi DEL /api/don-hang/delete/:id', async () => {
    vi.mocked(api.del).mockResolvedValue({ success: true });

    await DonHangService.remove(15);

    expect(api.del).toHaveBeenCalledWith('/api/don-hang/delete/15');
  });

  it('xacNhan - gọi PATCH /api/don-hang/:id/xac-nhan', async () => {
    const body = { serials: ['SER-001'] };
    vi.mocked(api.patch).mockResolvedValue({ status: 'confirmed' });

    await DonHangService.xacNhan(50, body);

    expect(api.patch).toHaveBeenCalledWith('/api/don-hang/50/xac-nhan', body);
  });

  it('xacNhanDaNhanHang - gọi PATCH /api/don-hang/:id/xac-nhan-nhan-hang', async () => {
    vi.mocked(api.patch).mockResolvedValue({ status: 'delivered' });

    await DonHangService.xacNhanDaNhanHang(50);

    expect(api.patch).toHaveBeenCalledWith('/api/don-hang/50/xac-nhan-nhan-hang', {});
  });

  it('getRecentForPos - gọi GET /api/don-hang/pos/recent', async () => {
    vi.mocked(api.get).mockResolvedValue([{ donHangId: 101 }]);

    const res = await DonHangService.getRecentForPos();

    expect(api.get).toHaveBeenCalledWith('/api/don-hang/pos/recent');
    expect(res).toEqual([{ donHangId: 101 }]);
  });
});
