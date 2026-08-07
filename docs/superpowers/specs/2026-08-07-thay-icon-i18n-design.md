# Thay emoji/mũi tên ẩn trong file dịch bằng icon Lucide (sub-project C)

## Bối cảnh

Kiểm tra trực tiếp app đang chạy (Docker, cả storefront lẫn dashboard) sau khi hoàn tất sub-project #1 (thay icon emoji trong template) phát hiện: nhiều nơi trên storefront vẫn hiện emoji thật (`🚚 Giao nhanh 2H`, `🛒 Thêm vào giỏ`, `🔍 Lọc nâng cao`...). Nguyên nhân: các emoji này nằm trong **giá trị chuỗi dịch** (`i18n/locales/*.js`, cả 5 ngôn ngữ vi/en/zh/ja/ko) chứ không nằm trong template `.vue` — sub-project #1 loại trừ toàn bộ file dịch khỏi phạm vi (coi là "văn phong/copy"), nên các trường hợp này bị lọt.

Nhìn lại, phần lớn các emoji/mũi tên này đóng vai trò **icon chức năng thật sự** (gắn trên nút, badge, link) — chỉ là bị implement tắt bằng cách nhét thẳng ký tự vào chuỗi dịch thay vì dùng icon ở tầng template. Đây là sub-project #3 trong chuỗi polish giao diện (đứng trước sub-project "màu dashboard Two-Accent Rule" và "2 anti-pattern WarrantyPanel/AccountPage" theo thứ tự người dùng chọn: C → A → B).

## Phạm vi

**28 key** trên cả 5 file dịch, tương ứng với các điểm gọi `t('key')` sau (đã xác minh chính xác từng namespace và dòng):

| # | Key (namespace.leaf) | Emoji/ký tự | Icon Lucide | File render (dòng) |
|---|---|---|---|---|
| 1 | `home.promoLink1` | 🎓 | `GraduationCap` | CustomerPage.vue:567 |
| 2 | `home.promoLink2` | 🔥 | `Flame` | CustomerPage.vue:568 |
| 3 | `home.promoLink3` | 💻 | `Laptop` | CustomerPage.vue:569 |
| 4 | `home.promoLink4` | 🔄 | `RefreshCw` | CustomerPage.vue:570 |
| 5 | `home.dealTabDeal` | 🔥 | `Flame` | CustomerPage.vue:98 |
| 6 | `home.advFilter` | 🔍 | `SlidersHorizontal` | CustomerPage.vue:676 |
| 7 | `home.fastDelivery` | 🚚 | `Truck` | ProductCard.vue:57 |
| 8 | `home.addToCart` | 🛒 | `ShoppingCart` | ProductCard.vue:77 |
| 9 | `cart.freeShipNote` | 🚚 | `Truck` | CartSummary.vue:7 |
| 10 | `checkout.foundCustomer` | ✓ | `CheckCircle2` | CheckoutModal.vue:105 |
| 11 | `checkout.loggedInAs` | ✓ | `CheckCircle2` | CheckoutModal.vue:94 |
| 12 | `checkout.back` | ← | `ArrowLeft` | CheckoutModal.vue:314 |
| 13 | `checkout.continue` | → | `ArrowRight` | CheckoutModal.vue:319 |
| 14 | `checkout.cashInstruction` | 💵 | `Banknote` | CheckoutModal.vue:24 |
| 15 | `checkout.qrInstruction` | ✅ | `CheckCircle2` | CheckoutModal.vue:29 |
| 16 | `checkout.bankInstruction` | 🏦 | `Landmark` | CheckoutModal.vue:34 |
| 17 | `productFilter.clearFilter` | ✕ | `X` | ProductFilter.vue:106 |
| 18 | `productDetail.freeShipping` | 🚚 | `Truck` | ProductDetail.vue:77 |
| 19 | `productDetail.addToCart` | 🛒 | `ShoppingCart` | ProductDetail.vue:215 |
| 20 | `productCompare.added` | ✓ | `CheckCircle2` | ProductCard.vue:93 |
| 21 | `admin.orders.backToToday` | ← | `ArrowLeft` | OrdersTable.vue:574 |
| 22 | `admin.orders.backToDateList` | ‹ | `ChevronLeft` | OrdersTable.vue:593 |
| 23 | `admin.addItemDetailModal.freeshipNote` | 🚚 | `Truck` | OrdersTable.vue:705 |
| 24 | `admin.customerDetail.back` | ← | `ArrowLeft` | CustomerDetailPage.vue:84 |
| 25 | `admin.customerDetail.giftPoints` | 🎁 | `Gift` | CustomerDetailPage.vue:94 |
| 26 | `admin.customerDetail.giftVoucher` | 🎟️ | `Ticket` | CustomerDetailPage.vue:95 |
| 27 | `admin.pos.orderCreated` | ✓ | `CheckCircle2` | PosPanel.vue:732 |
| 28 | `admin.pos.continueToSerial` | → | `ArrowRight` | PosPanel.vue:795 |

**Ngoài phạm vi (giữ nguyên, không đụng):**
- `home.sortPriceAsc`/`sortPriceDesc` ("Giá thấp → cao"/"Giá cao → thấp") — mũi tên là liên từ mô tả trong text option dropdown, không phải icon gắn trên control.
- `admin.dashboard.backToToday` ("Về hôm nay", không có mũi tên) — đã kiểm tra, không có ký tự cần đổi.
- `productCompare.addToCart` ("Thêm vào giỏ", không có emoji) — đã kiểm tra.
- `admin.giftPointsModal.*`, `admin.giftVoucherModal.*` (TangDiemModal.vue, TangVoucherModal.vue) — namespace khác, không có emoji.
- Toàn bộ emoji còn lại trong file dịch (copy toast, mô tả marketing dài...) — giữ nguyên, đúng tinh thần "văn phong" đã thống nhất từ sub-project #1.

## Cách làm

Với mỗi key trong bảng trên:
1. **5 file dịch:** xoá phần emoji/mũi tên + khoảng trắng theo sau khỏi giá trị chuỗi, giữ nguyên phần chữ còn lại (dịch đúng theo từng ngôn ngữ, không chỉ xoá ở bản `vi.js`).
2. **1 file `.vue`** (đúng dòng nêu trên): thêm icon Lucide tương ứng ngay trước `{{ t('key') }}`, theo đúng quy ước đã dùng xuyên suốt sub-project #1 (`currentColor` mặc định, size suy từ ngữ cảnh hiện tại — nút nhỏ ~13-14px, badge ~12-13px), import icon từ `@lucide/vue`.

**Trường hợp đặc biệt — `checkout.cashInstruction`/`bankInstruction` có tham số `{amount}`:** giữ nguyên cú pháp `{amount}` trong chuỗi dịch, chỉ xoá phần emoji ở đầu; icon vẫn thêm ở template như bình thường, đứng trước toàn bộ câu.

## Rủi ro đã biết (rút kinh nghiệm từ sub-project #1)

Nhiều file đích (`CheckoutModal.vue`, `CustomerPage.vue`, `OrdersTable.vue`, `AdminDashboard.vue`/`PosPanel.vue`) đang có code người dùng viết dở chưa commit. Bắt buộc dùng đúng kỹ thuật đã rút ra: dựng bản sạch từ `git show HEAD:<path>` qua Bash (không PowerShell, tránh BOM), áp đúng edit, stage qua `git hash-object`/`update-index`, **và đồng bộ working tree thật** (không chỉ commit) — đây là lỗi hệ thống đã phát hiện và fix ở lần trước, phải chủ động lặp lại đúng quy trình 2 bước (commit sạch + working tree đồng bộ) ngay từ đầu, không đợi review cuối mới bắt.

File dịch (`i18n/locales/*.js`) chưa từng bị chạm trong sub-project #1 — cần kiểm tra riêng xem có thay đổi chưa commit nào không trước khi sửa.

## Kiểm tra

Không có logic nghiệp vụ mới — kiểm tra bằng: build production (`npm run build`) để bắt icon import sai tên; quét lại toàn bộ 5 file dịch xác nhận 28 giá trị đã sạch emoji; mở app thật (đã có sẵn qua Docker) kiểm tra trực quan các trang: trang chủ (promo links, ticker, filter, product card), chi tiết sản phẩm, giỏ hàng, checkout (cả 3 bước + 3 phương thức thanh toán), admin orders (nút quay lại), admin customer detail, POS.

## Việc không làm trong spec này

- Không đụng `home.sortPriceAsc/sortPriceDesc` hay bất kỳ emoji "văn phong" nào khác ngoài 28 key liệt kê.
- Không đổi cấu trúc/copy của bản dịch ngoài việc xoá emoji.
- Không làm sub-project A (màu dashboard) hay B (WarrantyPanel/AccountPage anti-pattern) — đó là 2 spec riêng, làm sau theo đúng thứ tự đã chọn.
