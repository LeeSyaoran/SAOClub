import { describe, it, expect, vi } from 'vitest';

vi.mock('../../i18n/index.js', () => ({ t: (key: string) => key }));

import { variantsForDetail } from '../../utils/productGrouping.js';

describe('variantsForDetail', () => {
  const items = [
    { sanPhamId: 1, bienTheId: 10, mauSac: 'Đen' },
    { sanPhamId: 1, bienTheId: 11, mauSac: 'Trắng' },
    { sanPhamId: 2, bienTheId: 20, mauSac: 'Bạc' },
  ];

  it('không truyền onlyBienTheIds: trả về cả họ biến thể của sanPhamId (hành vi cũ, ProductsTable.vue)', () => {
    const result = variantsForDetail(items, 1, null);
    expect(result.map((v) => v.bienTheId)).toEqual([10, 11]);
  });

  // Modal "Chi tiết" mở từ 1 dòng đơn hàng chỉ nên hiện đúng biến thể khách đã mua, không
  // phải cả họ biến thể của sản phẩm (trước đây OrdersTable.vue chỉ truyền được sanPhamId,
  // không có cách lọc còn lại đúng 1 biến thể — xem comment cũ ở openVariantDetail()).
  it('truyền 1 id (số): chỉ trả về đúng 1 biến thể khách đã chọn', () => {
    const result = variantsForDetail(items, 1, 11);
    expect(result.map((v) => v.bienTheId)).toEqual([11]);
  });

  // Khách mua cùng 1 sản phẩm nhưng nhiều biến thể trong CÙNG đơn (vd máy A biến thể 1 và
  // 2) — bấm "Chi tiết" ở dòng nào cũng phải hiện đủ các biến thể đã mua trong đơn đó, không
  // chỉ đúng 1 dòng vừa bấm và cũng không phải cả họ biến thể ngoài catalogue.
  it('truyền mảng nhiều id: trả về đúng các biến thể đã mua, bỏ qua biến thể khác cùng sanPhamId', () => {
    const result = variantsForDetail(items, 1, [10, 11]);
    expect(result.map((v) => v.bienTheId)).toEqual([10, 11]);
  });
});
