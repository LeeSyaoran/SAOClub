// Gộp danh sách biến thể phẳng (từ API /api/san-pham/hien-thi, 1 dòng/biến thể) theo
// sanPhamId — dùng chung cho lưới sản phẩm trang khách hàng (App.vue) và màn Bán hàng
// tại quầy (PosPanel.vue), tránh 2 nơi tự viết lại quy tắc chọn biến thể đại diện rồi
// lệch nhau (từng là nguồn gốc 1 bug: card hiện sai "Hết hàng").

import { t } from "../i18n/index.js";

// 1 phần tử / sanPhamId — ưu tiên biến thể còn hàng (active), rồi mới đến giá thấp nhất
// trong nhóm đó làm đại diện. Nếu chỉ so giá thấp nhất, 1 biến thể hết hàng trùng giá với
// biến thể còn hàng khác sẽ khiến cả card hiện "Hết hàng" dù sản phẩm vẫn mua được.
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

// sanPhamId → số biến thể trong `items` — dùng để quyết định hiện tiền tố "Từ" trên card
// (nhiều biến thể = giá đại diện chỉ là giá thấp nhất, không phải giá duy nhất).
export const variantCountBySanPham = (items) => {
  const map = new Map();
  items.forEach((p) => map.set(p.sanPhamId, (map.get(p.sanPhamId) || 0) + 1));
  return map;
};

// Danh sách biến thể hiện trong modal "Chi tiết sản phẩm" (ProductDetailModal.vue) — mặc
// định cả họ biến thể của sanPhamId (ProductsTable.vue: xem/so sánh toàn bộ biến thể).
// Truyền onlyBienTheIds (1 id hoặc mảng nhiều id) khi mở từ đơn hàng (OrdersTable.vue) để
// chỉ hiện đúng (các) biến thể khách đã mua trong đơn đó — vd đơn có cùng 1 sản phẩm nhưng
// 2 biến thể khác nhau thì hiện cả 2, không phải cả họ biến thể ngoài catalogue.
export const variantsForDetail = (items, sanPhamId, onlyBienTheIds) => {
  const family = items.filter((p) => p.sanPhamId === sanPhamId);
  if (onlyBienTheIds == null) return family;
  const allow = new Set(Array.isArray(onlyBienTheIds) ? onlyBienTheIds : [onlyBienTheIds]);
  return family.filter((p) => allow.has(p.bienTheId));
};

// 3 hàm thuần dưới đây (không phụ thuộc reactive state) dùng chung cho ProductDetail.vue
// và PosPanel.vue — trước đây bị copy trùng ở 2 nơi, bao gồm cả bảng mau-hex ~24 dòng,
// nên sửa/thêm màu ở 1 nơi rất dễ quên nơi kia (đúng loại bug groupBySanPham ở trên đã
// từng gặp). Các computed dùng những hàm này (variants/configs/colorsForConfig/...) vẫn
// tách riêng ở mỗi component vì đóng gói local reactive state khác nhau.

export const configKey = (v) => `${v.cpu ?? ''}|${v.ram ?? ''}|${v.oCung ?? ''}`;

// Nhãn 2 dòng cho nút cấu hình
export const configLabel = (v) => ({
  line1: v.cpu || v.ram || t('productDetail.defaultConfig'),
  line2: [v.ram, v.oCung].filter(Boolean).join(' · '),
});

// Màu dot cho color swatch
export const colorDot = (mauSac) => {
  if (!mauSac) return '#555';
  const s = mauSac.toLowerCase();
  // Thứ tự quan trọng: khóa cụ thể ('xanh lá', 'xanh dương') phải đứng trước khóa chung
  // ('xanh') vì .find() lấy match đầu tiên theo includes().
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
