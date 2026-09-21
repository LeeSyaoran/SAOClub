import { t } from "../i18n/index.js";

// Gộp các biến thể theo sản phẩm và chọn biến thể đại diện
export const groupBySanPham = (items) => [
  ...items
    .reduce((map, p) => {
      const ex = map.get(p.sanPhamId);
      if (!ex) { map.set(p.sanPhamId, p); return map; }
      const pActive = p.trangThai === 'active';
      const exActive = ex.trangThai === 'active';
      if (pActive !== exActive ? pActive : Number(p.giaBan) < Number(ex.giaBan))
        map.set(p.sanPhamId, p);
      return map;
    }, new Map())
    .values(),
];

// Đếm số lượng biến thể theo từng sản phẩm
export const variantCountBySanPham = (items) => {
  const map = new Map();
  items.forEach((p) => map.set(p.sanPhamId, (map.get(p.sanPhamId) || 0) + 1));
  return map;
};

// Lọc danh sách biến thể hiển thị cho modal chi tiết sản phẩm
export const variantsForDetail = (items, sanPhamId, onlyBienTheIds) => {
  const family = items.filter((p) => p.sanPhamId === sanPhamId);
  if (onlyBienTheIds == null) return family;
  const allow = new Set(Array.isArray(onlyBienTheIds) ? onlyBienTheIds : [onlyBienTheIds]);
  return family.filter((p) => allow.has(p.bienTheId));
};

// Tạo mã đại diện cho cấu hình
export const configKey = (v) => `${v.cpu ?? ''}|${v.ram ?? ''}|${v.oCung ?? ''}`;

// Nhãn hiển thị 2 dòng cho nút chọn cấu hình
export const configLabel = (v) => ({
  line1: v.cpu || v.ram || t('productDetail.defaultConfig'),
  line2: [v.ram, v.oCung].filter(Boolean).join(' · '),
});

// Lấy mã màu HEX đại diện cho tên màu sắc
export const colorDot = (mauSac) => {
  if (!mauSac) return '#555';
  const s = mauSac.toLowerCase();
  const map = [
    ['đen','#18181b'], ['den','#18181b'],
    ['trắng','#e4e4e7'], ['trang','#e4e4e7'],
    ['bạc','#94a3b8'], ['bac','#94a3b8'],
    ['xám','#6b7280'], ['xam','#6b7280'],
    ['đỏ','#dc2626'], ['do','#dc2626'],
    ['xanh lá','#16a34a'], ['xanh la','#16a34a'],
    ['xanh dương','#2563eb'], ['xanh duong','#2563eb'],
    ['xanh','#2563eb'],
    ['vàng','#ca8a04'], ['vang','#ca8a04'],
    ['hồng','#ec4899'], ['hong','#ec4899'],
    ['tím','#9333ea'], ['tim','#9333ea'],
    ['cam','#ea580c'],
    ['nâu','#92400e'], ['nau','#92400e'],
  ];
  const found = map.find(([k]) => s.includes(k));
  return found ? found[1] : '#555';
};
