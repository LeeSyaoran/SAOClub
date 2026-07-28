// Gộp danh sách biến thể phẳng (từ API /api/san-pham/hien-thi, 1 dòng/biến thể) theo
// sanPhamId — dùng chung cho lưới sản phẩm trang khách hàng (App.vue) và màn Bán hàng
// tại quầy (PosPanel.vue), tránh 2 nơi tự viết lại quy tắc chọn biến thể đại diện rồi
// lệch nhau (từng là nguồn gốc 1 bug: card hiện sai "Hết hàng").

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
