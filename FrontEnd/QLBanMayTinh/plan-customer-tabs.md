# Plan: Customer Detail Modal — 3 Tabs (Info, Orders, Chat) + Floating Chat Widget

## Context

`CustomersTable.vue` hiện hiện thẻ khách hàng và mở modal chi tiết dạng form chỉnh sửa inline. User muốn:

1. **Modal 3 tabs** thay vì 1 form thẳng:
   - **Thông tin khách hàng**: hiện thị các trường thông tin kiểu "xem" (label/value), chỉ khi nhấn nút "Chỉnh sửa" mới hiện form để sửa
   - **Đơn hàng**: hiện tất cả đơn của khách, click vào đơn hiện modal chi tiết đơn hàng đó
   - **Chat**: khung chat CSKH ↔ khách
2. **Floating chat widget** ở góc dưới phải `AccountPage.vue` — nút hình tròn nhỏ, khách hàng cũng dùng để chat với CSKH

User đã chọn: **Giữ modal popup** (không tách trang), **Danh sách đơn dạng inline** (click đơn mở modal chi tiết), **Mock data trước** cho chat.

## File chỉnh sửa

### 1. `src/components/admin/CustomersTable.vue` (refactor lớn)

**Bỏ**: toàn bộ form inline trong modal (dòng 168-238 hiện tại), state `savingDetail`, `detailFormError`, `selectedCustomer`.

**Thêm**:
- Import `CustomerDetailModal.vue` mới
- Modal mới render `<CustomerDetailModal :customer="c" @close="..." />`
- 3 mode trong form vẫn xài lại `customerFormModalRef` cho nút "Thêm khách hàng"
- Refactor lại function `getAvatarUrl`, `getInitials`, `getAvatarBgColor` → chuyển sang `CustomerDetailModal.vue` (vì modal sẽ tự render avatar to)

### 2. `src/components/admin/CustomerDetailModal.vue` (tạo mới)

Component modal 3 tabs, nhận prop `:customer` (object từ store), emit `close`.

```vue
<template>
  <Modal>
    <Header + Avatar + Tên + 3 tab>
      <Tab "info">    Hiện info dạng read-only + nút "Chỉnh sửa" → toggle form edit
      <Tab "orders">  Bảng danh sách đơn + click mở OrderDetailModal
      <Tab "chat">    Khung chat
</template>
```

**State cục bộ:**
- `activeTab = ref('info')`
- `editingInfo = ref(false)` — toggle giữa xem/sửa
- `infoForm = ref({...})` — clone của `customer` để form edit
- `orders = ref([])`, `ordersLoading`, `selectedOrder = ref(null)` — orders list + modal chi tiết
- `chatMessages = ref([])`, `chatInput = ref('')` — mock chat

**Logic:**
- `ensureOrders()`: nếu store OrdersStore.items rỗng → gọi `DonHangService.getByKhachHang(customer.khachHangId)` (file `DonHangService.js:18` đã có sẵn), lọc theo `khachHangId`, lưu vào `orders.value`
- `openOrder(order)`: set `selectedOrder`, mở sub-modal
- `startEditInfo()`: clone `customer` → `infoForm`, bật editing mode
- `cancelEditInfo()`: reset form
- `saveInfo()`: validate + `KhachHangService.save(customer.khachHangId, body)` + `refreshCustomers()` + đóng edit
- `sendChat()`: push message vào mảng

**Hiện thị info (mode xem):**
```html
<div class="info-grid">
  <div class="info-row"><span class="info-label">Họ tên</span><span class="info-value">{{ customer.hoTen }}</span></div>
  ...
</div>
<button @click="startEditInfo">Chỉnh sửa</button>
```

**Hiện thị info (mode edit):**
Form giống `CustomerFormModal.vue` — lấy lại từ file đó.

**Hiện thị orders:**
Bảng đơn giản columns: Mã đơn / Ngày đặt / Tổng tiền / Trạng thái. Click row → mở `OrderDetailModal`. Tái sử dụng cấu trúc từ `OrdersTable.vue` (src/components/admin/) vì đã có sẵn format giá/ngày/trạng thái.

**Sub-modal OrderDetailModal:**
Có thể tạo inline trong file này (component nhỏ) — hiển thị danh sách sản phẩm + tổng tiền + trạng thái. Tái sử dụng `formatPrice`, `formatDate`, `orderStatusLabel`, `orderStatusColor` từ `utils/adminFormat.js` + `utils/orderStatus.js`.

### 3. `src/components/account/ChatWidget.vue` (tạo mới)

Component chat nổi cho AccountPage:

```vue
<template>
  <button class="chat-fab" @click="open = !open">
    <MessageCircle icon />
  </button>
  <div v-if="open" class="chat-popup">
    <Header với avatar CSKH + đóng>
    <Body danh sách messages>
    <Composer input + gửi>
  </div>
</template>
```

**State:**
- `open = ref(false)`
- `messages = ref([])` mock data
- `input = ref('')`
- `me = ref({ hoTen: 'CSKH', role: 'support' })`

**Style:**
- Fixed bottom-right (bottom: 20px, right: 20px)
- Nút FAB tròn 56px, gradient pink (3D style giống các nút đã có)
- Popup 320px × 480px, border-radius, shadow

### 4. `src/pages/AccountPage.vue`

**Thêm** `<ChatWidget />` ở cuối template (sau khi đóng `<template>` chính, hoặc trước `</template>` cuối).

Không cần thêm store, không truyền prop — component tự quản lý state mock.

## File tham chiếu / tái sử dụng

| File | Dùng cho |
|---|---|
| [src/services/KhachHangService.js](src/services/KhachHangService.js) | `save()`, `getById()` |
| [src/services/DonHangService.js:18](src/services/DonHangService.js#L18) | `getByKhachHang(id)` |
| [src/stores/orders.js](src/stores/orders.js) | `OrdersStore.items` |
| [src/utils/orderStatus.js](src/utils/orderStatus.js) | `orderStatusLabel`, `orderStatusColor` |
| [src/utils/adminFormat.js](src/utils/adminFormat.js) | `formatPrice`, `formatDate`, `statusLabel` |
| [src/components/admin/CustomerFormModal.vue](src/components/admin/CustomerFormModal.vue) | Mẫu form edit info (cấu trúc + validate) |
| [src/assets/admin-theme.css](src/assets/admin-theme.css) | CSS variables `--pink-*`, `--sh-btn`, `--sh-btn-hover` cho 3D button |

## Verification

1. **Mở admin → tab Khách hàng**: thấy layout 5 cột thẻ như cũ, mỗi thẻ có avatar Facebook-style
2. **Click vào 1 thẻ**: modal popup mở với avatar to + tên + 3 tabs (Thông tin / Đơn hàng / Chat)
3. **Tab "Thông tin"**: hiện các dòng label/value, nút "Chỉnh sửa" → form edit bật lên với input + nút Lưu/Hủy
4. **Sửa → Lưu**: gọi API, refresh store, đóng edit mode, hiện toast thành công
5. **Tab "Đơn hàng"**: hiện danh sách các đơn của khách (lọc theo khachHangId), click row → sub-modal chi tiết
6. **Tab "Chat"**: hiện mock messages, gõ và nhấn Gửi → message mới xuất hiện bên phải (CSKH)
7. **AccountPage**: góc dưới phải có nút tròn icon chat, click mở popup chat nhỏ

## Lưu ý

- Modal edit info dùng lại cấu trúc form trong `CustomerFormModal.vue` (validation hoTen/soDienThoai/diaChi bắt buộc)
- Không tạo store/dao mới cho chat — dùng ref cục bộ, sau này có thể thay bằng WebSocket
- CSS scoped cho 2 file mới để không ảnh hưởng component khác
- Giữ hiệu ứng 3D button cho nút ChatWidget FAB (đồng bộ phong cách admin)
