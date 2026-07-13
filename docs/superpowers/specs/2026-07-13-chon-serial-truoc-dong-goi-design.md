# Thiết kế: Chọn serial trước khi đóng gói đơn hàng online

## Bối cảnh

Hiện tại, khi khách đặt đơn online, mỗi dòng `ChiTietDonHang` tự động FIFO-chọn serial (`ChiTietSanPham`) và đánh dấu `trangThai = "da_ban"` ngay lúc tạo đơn (`ChiTietDonHangService.create()`), bất kể đơn đang ở trạng thái `pending`. Admin không hề có cơ hội xem/chọn lại serial trước khi đóng gói.

Yêu cầu: khi admin đóng gói đơn (chuyển trạng thái `confirmed` → `processing`, nút "📦 Đóng gói" trong `AdminPage.vue`), admin phải chọn/xác nhận serial cụ thể cho từng sản phẩm trong đơn trước khi đơn được đóng gói.

## Phạm vi

- **Áp dụng**: chỉ đơn có `kenhBan = "online"` (khách tự đặt qua checkout).
- **Không áp dụng**: đơn `kenhBan = "in_store"` (bán tại quầy) — giữ nguyên hành vi cũ 100% (gán serial + `da_ban` ngay lúc tạo dòng đơn, không qua bước xác nhận/đóng gói).
- Hỗ trợ `so_luong > 1` trên 1 dòng đơn hàng (nhiều serial cho 1 dòng) qua bảng join mới.

## Kiến trúc

Tận dụng trạng thái `giu_hang` (đã định nghĩa trong domain — xem `chi_tiet_san_pham.trang_thai` — nhưng hiện chưa có chỗ nào set giá trị này) làm bước "giữ chỗ" trung gian giữa `trong_kho` và `da_ban`:

1. **Đặt hàng (online)**: FIFO chọn đủ `soLuong` serial `trong_kho` như cũ, nhưng đánh dấu `giu_hang` thay vì `da_ban`. Lưu liên kết dòng đơn ↔ (nhiều) serial vào bảng join mới `chi_tiet_don_hang_serial`.
2. **Xác nhận đơn** (`pending` → `confirmed`): không đổi gì so với hiện tại.
3. **Đóng gói** (`confirmed` → `processing`, chỉ đơn online): admin xem lại danh sách serial đã giữ chỗ sẵn (pre-filled), có thể đổi, rồi xác nhận qua 1 endpoint gộp duy nhất → chốt serial thành `da_ban` + chuyển trạng thái đơn `processing` trong cùng 1 transaction.
4. **Hủy đơn** ở bất kỳ bước nào: giải phóng toàn bộ serial (cả từ FK đơn cũ lẫn bảng join mới) về `trong_kho`.

## Backend

### Entity mới: `ChiTietDonHangSerial`

Bảng `chi_tiet_don_hang_serial`, theo đúng style các entity hiện có (`@Data`, `IDENTITY` id, `@ManyToOne` + `@JoinColumn`):

```java
@Entity
@Table(name = "chi_tiet_don_hang_serial")
public class ChiTietDonHangSerial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_don_hang_serial_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chi_tiet_don_hang_id")
    private ChiTietDonHang chiTietDonHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chi_tiet_id")
    private ChiTietSanPham chiTietSanPham;
}
```

Chỉ dùng cho đơn online. Đơn tại quầy tiếp tục dùng field `ChiTietDonHang.chiTietSanPham` (FK đơn) như cũ, không đổi.

Cần migration SQL tạo bảng này (unique constraint trên `(chi_tiet_don_hang_id, chi_tiet_id)` để tránh gán trùng serial vào cùng 1 dòng nhiều lần).

Repository mới `ChiTietDonHangSerialRepository` với ít nhất: `findByChiTietDonHang_Id(Integer id)`, `deleteByChiTietDonHang_Id(Integer id)`.

### `ChiTietDonHangService.create()` — rẽ nhánh theo kênh bán

```
nếu donHang.kenhBan == "online":
    FIFO chọn đủ soLuong serial trong_kho (như logic hiện tại)
    mỗi serial: trangThai = "giu_hang"
    lưu (chiTietDonHang, serial) vào ChiTietDonHangSerial cho từng serial
    VẪN set ChiTietDonHang.chiTietSanPham = serial đại diện (serial đầu tiên) — không để null,
    để các nơi hiển thị dựa trên FK này (chi tiết đơn, bảo hành...) vẫn có dữ liệu trước khi
    đóng gói; bảng join mới mới là nguồn đầy đủ khi so_luong > 1
ngược lại (in_store / khác):
    giữ nguyên 100% logic hiện tại (chiTietId chỉ định tay hoặc FIFO, set "da_ban", gán FK đơn)
```

Lịch sử tồn kho (`LichSuTonKho`, field `loaiBienDong`): nhánh online ghi `loaiBienDong = "giu_hang"` thay vì `"xuat_ban"` — đây là lúc trigger DB `trg_CapNhatTonKhoThucTe` thực sự trừ tồn kho thực tế (serial rời khỏi `trong_kho`). Lúc đóng gói (`giu_hang` → `da_ban`), serial không quay lại `trong_kho` nên trigger không chạy lại — không cần ghi thêm dòng `LichSuTonKho` mới ở bước này (không có biến động số lượng tồn kho thực tế nào để log).

**Lưu ý quan trọng (phát hiện ở whole-branch review, đã sửa):** `loaiBienDong` **không phải** chuỗi tự do — cột `loai_bien_dong` có ràng buộc `CHECK` (`CK_lsdk_loai`) trong `Database/QLBanMayTinh.sql`, chỉ cho phép `nhap|xuat_ban|tra_hang|dieu_chinh|huy`. Giá trị `"giu_hang"` phải được thêm vào danh sách cho phép trong constraint này, nếu không insert sẽ bị SQL Server từ chối và toàn bộ giao dịch đặt hàng online sẽ rollback.

### Endpoint mới: `PATCH /api/don-hang/{id}/dong-goi`

Chỉ dùng cho đơn online, gộp "chọn serial cho từng dòng + chuyển trạng thái processing" thành 1 transaction:

**Request:**
```json
{
  "lines": [
    { "chiTietDonHangId": 123, "serialIds": [45, 46] }
  ]
}
```

**Logic:**
1. Với mỗi dòng: đếm `serialIds` phải bằng đúng `soLuong` của dòng đó — sai thì lỗi 400, nêu rõ dòng nào thiếu/thừa.
2. Mỗi serial trong `serialIds` phải đang `trong_kho`, hoặc đang `giu_hang` **và** thuộc chính dòng đơn này (qua bảng join) — nếu không, lỗi 400 (serial đã bị đơn khác lấy hoặc không tồn tại).
3. Giải phóng (set `trong_kho`) các serial đang giữ chỗ cho dòng này nhưng không còn trong `serialIds` mới (admin đã đổi ý).
4. Cập nhật bảng join `chi_tiet_don_hang_serial` khớp với `serialIds` mới.
5. Set toàn bộ serial trong `serialIds` → `trangThai = "da_ban"`.
6. Set `donHang.trangThaiDonHang = "processing"`.
7. Toàn bộ trong 1 `@Transactional`, lỗi ở bước nào rollback hết, đơn không bị kẹt ở trạng thái nửa vời.

**Validate đơn hàng**: chỉ cho phép gọi endpoint này khi `donHang.kenhBan == "online"` và trạng thái hiện tại là `confirmed` — sai thì 400.

### `releaseSerialsToStock` (đã có, dùng khi hủy/xóa đơn)

Mở rộng thêm: ngoài trả field `ChiTietDonHang.chiTietSanPham` (đơn tại quầy) về `trong_kho`, cũng lặp qua `ChiTietDonHangSerial` của đơn (đơn online) và trả toàn bộ serial liên kết về `trong_kho`, rồi xóa các dòng join.

## Frontend (`AdminPage.vue`)

### Gate điểm chuyển `processing`

Cả `advanceOrderStatus` (nút "📦 Đóng gói" nhanh trên bảng) và `saveOrderStatus` (modal "Cập nhật trạng thái" sửa tay): nếu đích đến là `"processing"` **và** `o.kenhBan === "online"` → mở modal "Chọn serial" mới thay vì PATCH thẳng qua endpoint update chung. Nếu `kenhBan === "in_store"`, hành vi giữ nguyên y hệt hiện tại (PATCH thẳng).

### Modal "Chọn serial" (component mới)

- Load danh sách dòng đơn (`ChiTietDonHangService.getByDonHangId` hoặc tương đương hiện có) kèm `bienTheId`, `soLuong`, và serial đang `giu_hang` sẵn cho dòng đó (response cần trả thêm field này — mở rộng `ChiTietDonHangResponse` hoặc thêm endpoint riêng).
- Với mỗi dòng: hiển thị danh sách serial khả dụng cho `bienTheId` đó (gọi `GET /api/chi-tiet-san-pham/bien-the/{bienTheId}`, lọc client-side còn `trong_kho` hoặc đang `giu_hang` cho chính dòng này), cho phép chọn đúng `soLuong` serial — mặc định tick sẵn các serial đã giữ chỗ.
- Nút xác nhận: disable nếu có dòng nào chưa chọn đủ số lượng.
- Submit → gọi `PATCH .../dong-goi` với payload theo lựa chọn hiện tại → thành công thì đóng modal, refresh danh sách đơn hàng.

## Xử lý lỗi

| Tình huống | Xử lý |
|---|---|
| Đóng gói mà 1 dòng chưa đủ serial | Chặn ở cả frontend (disable nút) lẫn backend (400), nêu rõ dòng thiếu |
| Serial được chọn đã bị đơn khác lấy mất giữa lúc mở modal và lúc submit | Backend trả 400 nêu rõ serial nào không còn hợp lệ, frontend báo lỗi, admin chọn lại |
| Hủy đơn sau khi đã giữ chỗ (`giu_hang`) hoặc đã đóng gói (`da_ban`) | `releaseSerialsToStock` trả toàn bộ serial liên quan (cả FK đơn cũ và bảng join mới) về `trong_kho` |
| Đơn `in_store` gọi nhầm endpoint `dong-goi` | Backend từ chối (400) vì kiểm tra `kenhBan == "online"` |

## Kiểm thử

Thêm test ở tầng service cho nhánh logic mới (theo pattern test hiện có trong repo, nếu có; nếu chưa có test nào cho tầng service thì viết 1 test đơn giản không cần framework phức tạp):

1. Đặt đơn online, `soLuong = 2` → xác nhận 2 serial được đánh `giu_hang` và liên kết đúng qua bảng join, field `ChiTietDonHang.chiTietSanPham` được gán serial đại diện (serial đầu tiên).
2. Gọi `dong-goi` với đúng serial đã giữ chỗ → xác nhận serial chuyển `da_ban`, đơn chuyển `processing`.
3. Gọi `dong-goi` thiếu serial cho 1 dòng → xác nhận bị từ chối, trạng thái đơn không đổi.
4. Hủy đơn đang ở trạng thái đã giữ chỗ (`giu_hang`) → xác nhận serial quay lại `trong_kho`.

## Ngoài phạm vi (không làm trong lần này)

- Không đổi hành vi đơn `in_store`.
- Không xử lý trường hợp đổi `bienTheId`/`soLuong` của dòng đơn sau khi đã giữ chỗ serial (nếu cần sửa dòng đơn, admin hủy dòng và tạo lại).
- Không thêm cơ chế hết hạn giữ chỗ tự động (VD: tự nhả serial nếu đơn `pending` quá lâu không được xác nhận) — nếu cần, sẽ là 1 tính năng riêng sau này.
