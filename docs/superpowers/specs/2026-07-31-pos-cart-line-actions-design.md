# Thao tác trên dòng giỏ hàng + nút Chi tiết sản phẩm ở POS — Design Spec

## Bối cảnh

Sau tính năng gộp lưới sản phẩm + modal chọn cấu hình/màu ở POS (`2026-07-28-pos-variant-picker-design.md`, đã xong), `PosPanel.vue` còn 3 khoảng trống:

1. Không có cách xem "sản phẩm này gồm những biến thể nào" mà không đi qua luồng thêm-vào-giỏ (nút "Thêm vào giỏ" hiện mở thẳng modal chọn cấu hình/màu, bị chặn bởi cổng xác định khách hàng).
2. `posCart` hiển thị phẳng — mua 2 máy cùng biến thể ra 2 dòng riêng, mỗi dòng chỉ có 1 nút ✕ xóa thẳng, không xác nhận, không có cách xóa gộp hay đổi sang serial khác.
3. `ChiTietSanPhamModal` bên đơn hàng (`OrdersTable.vue`) đã lọc đúng theo biến thể đã mua (việc này đã xong, không thuộc phạm vi doc này).

## Kiến trúc

Thuần frontend, chỉ sửa `PosPanel.vue` + 5 file locale. Không đổi API/backend, không đổi `posCart` (vẫn là mảng phẳng 1 phần tử/serial — nguồn sự thật cho `posCartTotal`, `posPlaceOrder`, giữ đơn) — chỉ thêm 1 computed gộp cho hiển thị.

### A. Nút "Chi tiết" trên card sản phẩm POS

Thêm nút phụ cạnh "Thêm vào giỏ" trên mỗi card ở `posProductGroups`, mở lại `ProductDetailModal.vue` (component xem-thuần đã dùng ở `ProductsTable.vue`/`OrdersTable.vue`, import mới vào `PosPanel.vue`). Không qua `posOpenVariantPicker`, không bị chặn bởi `posStage` — nhân viên duyệt biến thể của sản phẩm bất kỳ lúc nào, kể cả trước khi xác định khách hàng. Tái dùng key `admin.products.detail`.

### B. Gộp dòng giỏ hàng theo biến thể

Thêm computed:

```js
const posCartGroups = computed(() => {
  const map = new Map();
  posCart.value.forEach((item) => {
    if (!map.has(item.bienTheId)) map.set(item.bienTheId, { ...item, items: [] });
    map.get(item.bienTheId).items.push(item);
  });
  return [...map.values()];
});
```

Template lặp qua `posCartGroups` thay vì `posCart`:
- Group 1 serial: hiện y hệt UI hiện tại (S/N + nút 🔄 đổi serial + nút ✕ xóa).
- Group ≥2 serial: hiện dòng tổng ("Tên SP × N", tổng giá = `giaBan * N`) + nút **"Xóa tất cả (N)"** ở đầu group; bên dưới liệt kê từng S/N riêng, mỗi S/N vẫn có 🔄 + ✕ riêng (xóa đúng 1 serial trong group — đây là "xóa theo lựa chọn").

### C. Xác nhận trước khi xóa + xóa gộp

`posRemove(chiTietId)` bọc thêm `askConfirm()` (dialog dùng chung `stores/confirm.js`, đã mount sẵn ở `AdminPage.vue`/`StaffPage.vue`, không cần thêm gì) hiện tên sản phẩm + S/N trước khi xóa + trả serial về `trong_kho`.

Thêm `posRemoveGroup(g)` — confirm 1 lần rồi xóa toàn bộ `g.items`, trả tất cả serial trong group về `trong_kho`.

### D. Đổi serial (nút 🔄 trên từng dòng)

`posOpenSerialPicker(p, swapChiTietId = null)` thêm tham số tùy chọn — lời gọi cũ từ `posConfirmVariant` không đổi (mặc định `null` = luồng thêm mới, hành vi giữ nguyên y hệt).

`posSelectSerial` rẽ nhánh: nếu `swapChiTietId` khác `null` → **thay** phần tử đó trong `posCart` bằng serial mới vừa chọn (trả serial cũ về `trong_kho`, gán serial mới `giu_hang`) thay vì `push` thêm dòng. Modal "Chọn serial" đang có không đổi UI — serial cũ tự động không hiện lại trong danh sách (đã lọc `trangThai === 'trong_kho'`, serial đang `giu_hang` bị loại sẵn).

## Phạm vi KHÔNG đổi

- `posStage`/cổng xác định khách hàng, `posOpenVariantPicker`, modal chọn cấu hình/màu.
- `posCart` (cấu trúc mảng phẳng), `posCartTotal`, `posPlaceOrder`, giữ đơn, khuyến mãi.
- Backend — 0 file trong `BackEnd/` bị đụng tới.

## i18n (5 locale: vi/en/ja/ko/zh)

Key mới trong namespace `admin.pos`: `swapSerial`, `confirmRemove` (placeholder `{name}`, `{serial}`), `confirmRemoveGroup` (placeholder `{name}`, `{count}`), `removeAll`. Tái dùng `admin.products.detail` cho nút Chi tiết.

## Testing

Không có framework test frontend cho component Vue thuần UI (theo đúng convention dự án) — verify bằng `npm run build` (cú pháp/type) + kiểm tra thủ công qua browser (Playwright hoặc trực tiếp): card sản phẩm mở đúng modal chi tiết, mua 2 máy cùng biến thể ra 1 group ×2, xóa tất cả / xóa 1 serial / đổi serial đều cập nhật đúng giỏ hàng và trạng thái serial (`trong_kho`/`giu_hang`), tạo đơn hoàn tất bình thường.
