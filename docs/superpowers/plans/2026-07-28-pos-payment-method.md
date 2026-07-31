# Ghi nhận phương thức thanh toán khi bán hàng tại quầy (POS) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Khi tạo đơn tại quầy (POS), nhân viên chọn phương thức thanh toán (Tiền mặt /
QR / Chuyển khoản / Thẻ) ngay trong màn hình giỏ hàng trước khi bấm "Tạo đơn hàng" —
lựa chọn này được ghi thật vào bảng `thanh_toan` (hạ tầng đã có sẵn nhưng chưa từng được
dùng), và hiển thị lại cho admin/nhân viên xem trong modal "Chi tiết đơn hàng".

**Architecture:** Backend: sửa 1 bug validate quá chặt (`ThanhToanRequest`), thêm 1
query/endpoint lọc theo đơn. Frontend: thêm UI chọn phương thức inline trong
`PosPanel.vue`, gọi `POST /api/thanh-toan` ngay sau khi tạo đơn+dòng sản phẩm thành
công (rollback y hệt pattern lỗi dòng sản phẩm đã có), và hiển thị lại trong
`OrdersTable.vue`.

**Tech Stack:** Spring Boot / JPA (backend), Vue 3 `<script setup>` (frontend). Không
có test runner frontend (đã xác nhận). Backend dùng JUnit 5 + Mockito + AssertJ.

## Global Constraints

- Đơn tạo tại quầy vẫn `trangThaiDonHang: 'confirmed'`, `trangThaiThanhToan: 'paid'`
  ngay khi tạo — **không đổi** logic trạng thái đơn, chỉ thêm 1 record `ThanhToan` đi
  kèm. Không có trạng thái "chờ thanh toán" trung gian nào cả.
- Không đụng gì tới `CheckoutModal.vue` (luồng online) — `selectedPayment` ở đó vẫn
  thuần UI như hiện tại.
- Không đụng gì tới cổng xác định khách hàng, modal chọn cấu hình/màu, modal chọn
  serial, mã khuyến mãi, `posPlaceOrder`'s cơ chế rollback dòng sản phẩm đã có — chỉ mở
  rộng thêm bước sau khi các bước đó đã chạy xong, theo đúng pattern rollback sẵn có.
- Giá trị `phuong_thuc_thanh_toan` phải nằm trong 8 giá trị enum DB cho phép
  (`tien_mat`, `chuyen_khoan`, `the_tin_dung`, `momo`, `vnpay`, `zalopay`, `tra_gop`,
  `khac`) — UI "QR" map vào `vnpay`, UI "Thẻ" map vào `the_tin_dung`.
- i18n: mọi key mới phải có mặt ở cả 5 locale (vi/en/ja/ko/zh), đúng namespace, đúng vị
  trí tương đối — quy ước đã có của dự án.

---

### Task 1: Backend — fix validate `ThanhToanRequest` + thêm endpoint lọc theo đơn

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/request/ThanhToanRequest.java`
- Modify: `BackEnd/src/main/java/com/example/backend/repository/ThanhToanRepository.java`
- Modify: `BackEnd/src/main/java/com/example/backend/service/ThanhToanService.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/ThanhToanController.java`
- Create: `BackEnd/src/test/java/com/example/backend/service/ThanhToanServiceTest.java`

**Interfaces:**
- Produces: `POST /api/thanh-toan` (đã có sẵn, chỉ nới validate) chấp nhận
  `maGiaoDich`/`ghiChu` = null. `GET /api/thanh-toan/don-hang/{donHangId}` (mới) trả
  `List<ThanhToanResponse>`. Dùng ở Task 4 (POS) và Task 5 (OrdersTable).

- [ ] **Step 1: Nới validate `ThanhToanRequest.java`**

Tìm đúng khối hiện có:

```java
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String phuongThucThanhToan;

    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phải lớn hơn 0")
    private BigDecimal soTien;

    @NotBlank(message = "Mã giao dịch không được để trống")
    private String maGiaoDich;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;

    @NotBlank(message = "Ghi chú không được để trống")
    private String ghiChu;
```

Thay bằng (bỏ `@NotBlank` cho `maGiaoDich` và `ghiChu` — cột DB tương ứng `NULL`-able,
endpoint này chưa từng được gọi thật nên validate chưa bao giờ khớp use-case thật:
thanh toán tiền mặt/thẻ tại quầy không có "mã giao dịch"):

```java
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String phuongThucThanhToan;

    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phải lớn hơn 0")
    private BigDecimal soTien;

    private String maGiaoDich;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;

    private String ghiChu;
```

- [ ] **Step 2: Thêm query method lọc theo đơn — `ThanhToanRepository.java`**

Tìm đúng khối hiện có:

```java
@Repository
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Integer> {
    @Query("SELECT new com.example.backend.response.ThanhToanResponse(t.thanhToanId, t.donHang.id, t.ngayThanhToan, t.phuongThucThanhToan, t.soTien, t.maGiaoDich, t.trangThai, t.ghiChu) FROM ThanhToan t")
    List<ThanhToanResponse> hienThiThanhToan();

    List<ThanhToan> findByDonHang_Id(Integer donHangId);
}
```

Thay bằng:

```java
@Repository
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Integer> {
    @Query("SELECT new com.example.backend.response.ThanhToanResponse(t.thanhToanId, t.donHang.id, t.ngayThanhToan, t.phuongThucThanhToan, t.soTien, t.maGiaoDich, t.trangThai, t.ghiChu) FROM ThanhToan t")
    List<ThanhToanResponse> hienThiThanhToan();

    // Dung cho modal "Chi tiet don hang" (OrdersTable.vue) va POS — hien lai phuong
    // thuc da thanh toan cua 1 don cu the.
    @Query("SELECT new com.example.backend.response.ThanhToanResponse(t.thanhToanId, t.donHang.id, t.ngayThanhToan, t.phuongThucThanhToan, t.soTien, t.maGiaoDich, t.trangThai, t.ghiChu) FROM ThanhToan t WHERE t.donHang.id = :donHangId")
    List<ThanhToanResponse> hienThiThanhToanTheoDonHang(Integer donHangId);

    List<ThanhToan> findByDonHang_Id(Integer donHangId);
}
```

- [ ] **Step 3: Thêm service method — `ThanhToanService.java`**

Tìm đúng khối hiện có:

```java
    public List<ThanhToanResponse> hienThiThanhToan() {
        return thanhToanRepository.hienThiThanhToan();
    }
```

Thay bằng:

```java
    public List<ThanhToanResponse> hienThiThanhToan() {
        return thanhToanRepository.hienThiThanhToan();
    }

    public List<ThanhToanResponse> hienThiThanhToanTheoDonHang(Integer donHangId) {
        return thanhToanRepository.hienThiThanhToanTheoDonHang(donHangId);
    }
```

- [ ] **Step 4: Thêm endpoint — `ThanhToanController.java`**

Tìm đúng khối hiện có:

```java
    @GetMapping
    public List<ThanhToanResponse> getAll() {
        return thanhToanService.hienThiThanhToan();
    }

    @GetMapping("/{id}")
    public ThanhToan getById(@PathVariable Integer id) {
        return thanhToanService.getById(id);
    }
```

Thay bằng:

```java
    @GetMapping
    public List<ThanhToanResponse> getAll() {
        return thanhToanService.hienThiThanhToan();
    }

    @GetMapping("/don-hang/{donHangId}")
    public List<ThanhToanResponse> getByDonHang(@PathVariable Integer donHangId) {
        return thanhToanService.hienThiThanhToanTheoDonHang(donHangId);
    }

    @GetMapping("/{id}")
    public ThanhToan getById(@PathVariable Integer id) {
        return thanhToanService.getById(id);
    }
```

(Đặt `/don-hang/{donHangId}` TRƯỚC `/{id}` trong file — không ảnh hưởng khớp lệnh của
Spring MVC vì đây là literal segment khác path variable, nhưng đặt trước cho dễ đọc,
theo đúng thứ tự các controller khác trong dự án đã làm với route con.)

- [ ] **Step 5: Cập nhật comment lớp — `ThanhToanController.java`**

Tìm đúng dòng hiện có:

```java
// Thanh toán — chỉ staff. Hiện chưa có service/component frontend nào gọi tới controller
// này (tính năng chưa được wire lên UI) — khoá trước theo nguyên tắc least-privilege.
```

Thay bằng:

```java
// Thanh toán — chỉ staff. Dùng ở POS (tạo record khi chốt đơn tại quầy) và modal
// "Chi tiết đơn hàng" (hiển thị lại phương thức đã dùng).
```

- [ ] **Step 6: Tạo file test `ThanhToanServiceTest.java`**

```java
package com.example.backend.service;

import com.example.backend.entity.DonHang;
import com.example.backend.entity.ThanhToan;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.ThanhToanRepository;
import com.example.backend.request.ThanhToanRequest;
import com.example.backend.response.ThanhToanResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThanhToanServiceTest {

    @Mock private ThanhToanRepository thanhToanRepository;
    @Mock private DonHangRepository donHangRepository;

    @InjectMocks
    private ThanhToanService service;

    // Thanh toan tien mat tai quay khong co "ma giao dich" — dung use-case chinh khien
    // validate cu (bat buoc maGiaoDich/ghiChu) sai, xem Task 1 Step 1.
    @Test
    void create_khongCoMaGiaoDichVaGhiChu_vanThanhCong() {
        DonHang donHang = new DonHang();
        when(donHangRepository.getReferenceById(1)).thenReturn(donHang);
        when(thanhToanRepository.save(any(ThanhToan.class))).thenAnswer(inv -> inv.getArgument(0));

        ThanhToanRequest req = new ThanhToanRequest();
        req.setDonHangId(1);
        req.setNgayThanhToan(LocalDateTime.now());
        req.setPhuongThucThanhToan("tien_mat");
        req.setSoTien(BigDecimal.valueOf(500000));
        req.setMaGiaoDich(null);
        req.setTrangThai("success");
        req.setGhiChu(null);

        ThanhToan saved = service.create(req);

        assertThat(saved.getMaGiaoDich()).isNull();
        assertThat(saved.getGhiChu()).isNull();
        assertThat(saved.getPhuongThucThanhToan()).isEqualTo("tien_mat");
        assertThat(saved.getDonHang()).isSameAs(donHang);
    }

    @Test
    void hienThiThanhToanTheoDonHang_goiDungRepository() {
        List<ThanhToanResponse> expected = List.of(
                new ThanhToanResponse(1, 5, LocalDateTime.now(), "tien_mat", BigDecimal.TEN, null, "success", null));
        when(thanhToanRepository.hienThiThanhToanTheoDonHang(5)).thenReturn(expected);

        List<ThanhToanResponse> result = service.hienThiThanhToanTheoDonHang(5);

        assertThat(result).isEqualTo(expected);
    }
}
```

- [ ] **Step 7: Chạy test backend**

Run (từ `BackEnd/`, git-bash):
```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"
./mvnw -o test -Dtest=ThanhToanServiceTest
```
Expected: `Tests run: 2, Failures: 0, Errors: 0`

Sau đó chạy toàn bộ test suite để chắc chắn không phá gì đang có:
```bash
./mvnw -o test
```
Expected: tất cả pass (113 test trước đó + 2 test mới = 115).

- [ ] **Step 8: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/request/ThanhToanRequest.java \
        BackEnd/src/main/java/com/example/backend/repository/ThanhToanRepository.java \
        BackEnd/src/main/java/com/example/backend/service/ThanhToanService.java \
        BackEnd/src/main/java/com/example/backend/controller/ThanhToanController.java \
        BackEnd/src/test/java/com/example/backend/service/ThanhToanServiceTest.java
git commit -m "fix(backend): relax ThanhToan validation to match schema, add get-by-order endpoint"
```

---

### Task 2: i18n — thêm key mới (5 locale)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`, `en.js`, `ja.js`, `ko.js`, `zh.js`

**Interfaces:**
- Produces: `admin.paymentMethod.{tien_mat,chuyen_khoan,the_tin_dung,vnpay}`,
  `admin.pos.paymentMethodLabel`, `admin.pos.paymentRequired`,
  `admin.errors.createPaymentError`, `admin.orderDetailModal.paymentMethod`. Dùng ở
  Task 3 (`orderStatus.js`), Task 4 (`PosPanel.vue`), Task 5 (`OrdersTable.vue`).

- [ ] **Step 1: `vi.js`**

Tìm (trong khối top-level `admin.paymentStatus`):
```js
    paymentStatus: {
      unpaid: "Chưa thanh toán",
      partial: "Thanh toán một phần",
      paid: "Đã thanh toán",
      refunded: "Hoàn tiền",
    },
```
Thay bằng:
```js
    paymentStatus: {
      unpaid: "Chưa thanh toán",
      partial: "Thanh toán một phần",
      paid: "Đã thanh toán",
      refunded: "Hoàn tiền",
    },

    paymentMethod: {
      tien_mat: "Tiền mặt",
      chuyen_khoan: "Chuyển khoản",
      the_tin_dung: "Thẻ",
      vnpay: "QR",
    },
```

Tìm (trong khối `admin.pos`):
```js
      chooseVariant: "Chọn cấu hình / màu sắc",
      continueToSerial: "Tiếp tục chọn serial →",
    },
```
Thay bằng:
```js
      chooseVariant: "Chọn cấu hình / màu sắc",
      continueToSerial: "Tiếp tục chọn serial →",
      paymentMethodLabel: "Phương thức thanh toán",
      paymentRequired: "Vui lòng chọn phương thức thanh toán!",
    },
```

Tìm (trong khối `admin.errors`):
```js
      addProductError: "Lỗi thêm sản phẩm: {message}",
    },
```
Thay bằng:
```js
      addProductError: "Lỗi thêm sản phẩm: {message}",
      createPaymentError: "Lỗi ghi nhận thanh toán: {message}",
    },
```

Tìm (trong khối `admin.orderDetailModal`):
```js
      paymentStatus: "Thanh toán",
```
Thay bằng:
```js
      paymentStatus: "Thanh toán",
      paymentMethod: "Đã trả bằng",
```

- [ ] **Step 2: `en.js`**

Tìm:
```js
    paymentStatus: {
      unpaid: "Unpaid",
      partial: "Partially paid",
      paid: "Paid",
      refunded: "Refunded",
    },
```
Thay bằng:
```js
    paymentStatus: {
      unpaid: "Unpaid",
      partial: "Partially paid",
      paid: "Paid",
      refunded: "Refunded",
    },

    paymentMethod: {
      tien_mat: "Cash",
      chuyen_khoan: "Bank transfer",
      the_tin_dung: "Card",
      vnpay: "QR",
    },
```

Tìm:
```js
      chooseVariant: "Choose configuration / color",
      continueToSerial: "Continue to serial →",
    },
```
Thay bằng:
```js
      chooseVariant: "Choose configuration / color",
      continueToSerial: "Continue to serial →",
      paymentMethodLabel: "Payment method",
      paymentRequired: "Please select a payment method!",
    },
```

Tìm:
```js
      addProductError: "Error adding product: {message}",
    },
```
Thay bằng:
```js
      addProductError: "Error adding product: {message}",
      createPaymentError: "Error recording payment: {message}",
    },
```

Tìm:
```js
      paymentStatus: "Payment",
```
Thay bằng:
```js
      paymentStatus: "Payment",
      paymentMethod: "Paid via",
```

- [ ] **Step 3: `ja.js`**

Tìm:
```js
    paymentStatus: {
      unpaid: "未払い",
      partial: "一部支払い済み",
      paid: "支払い済み",
      refunded: "返金済み",
    },
```
Thay bằng:
```js
    paymentStatus: {
      unpaid: "未払い",
      partial: "一部支払い済み",
      paid: "支払い済み",
      refunded: "返金済み",
    },

    paymentMethod: {
      tien_mat: "現金",
      chuyen_khoan: "銀行振込",
      the_tin_dung: "カード",
      vnpay: "QRコード",
    },
```

Tìm:
```js
      chooseVariant: "構成・カラーを選択",
      continueToSerial: "シリアル選択へ進む →",
    },
```
Thay bằng:
```js
      chooseVariant: "構成・カラーを選択",
      continueToSerial: "シリアル選択へ進む →",
      paymentMethodLabel: "支払い方法",
      paymentRequired: "支払い方法を選択してください！",
    },
```

Tìm:
```js
      addProductError: "商品の追加エラー：{message}",
    },
```
Thay bằng:
```js
      addProductError: "商品の追加エラー：{message}",
      createPaymentError: "支払い記録エラー：{message}",
    },
```

Tìm:
```js
      paymentStatus: "支払い",
```
Thay bằng:
```js
      paymentStatus: "支払い",
      paymentMethod: "支払い方法",
```

- [ ] **Step 4: `ko.js`**

Tìm:
```js
    paymentStatus: {
      unpaid: "미결제",
      partial: "부분 결제",
      paid: "결제 완료",
      refunded: "환불 완료",
    },
```
Thay bằng:
```js
    paymentStatus: {
      unpaid: "미결제",
      partial: "부분 결제",
      paid: "결제 완료",
      refunded: "환불 완료",
    },

    paymentMethod: {
      tien_mat: "현금",
      chuyen_khoan: "계좌이체",
      the_tin_dung: "카드",
      vnpay: "QR 코드",
    },
```

Tìm:
```js
      chooseVariant: "구성 / 색상 선택",
      continueToSerial: "시리얼 선택으로 계속 →",
    },
```
Thay bằng:
```js
      chooseVariant: "구성 / 색상 선택",
      continueToSerial: "시리얼 선택으로 계속 →",
      paymentMethodLabel: "결제 방법",
      paymentRequired: "결제 방법을 선택해 주세요!",
    },
```

Tìm:
```js
      addProductError: "상품 추가 오류: {message}",
    },
```
Thay bằng:
```js
      addProductError: "상품 추가 오류: {message}",
      createPaymentError: "결제 기록 오류: {message}",
    },
```

Tìm:
```js
      paymentStatus: "결제",
```
Thay bằng:
```js
      paymentStatus: "결제",
      paymentMethod: "결제 수단",
```

- [ ] **Step 5: `zh.js`**

Tìm:
```js
    paymentStatus: {
      unpaid: "未支付",
      partial: "部分支付",
      paid: "已支付",
      refunded: "已退款",
    },
```
Thay bằng:
```js
    paymentStatus: {
      unpaid: "未支付",
      partial: "部分支付",
      paid: "已支付",
      refunded: "已退款",
    },

    paymentMethod: {
      tien_mat: "现金",
      chuyen_khoan: "银行转账",
      the_tin_dung: "刷卡",
      vnpay: "扫码支付",
    },
```

Tìm:
```js
      chooseVariant: "选择配置 / 颜色",
      continueToSerial: "继续选择序列号 →",
    },
```
Thay bằng:
```js
      chooseVariant: "选择配置 / 颜色",
      continueToSerial: "继续选择序列号 →",
      paymentMethodLabel: "支付方式",
      paymentRequired: "请选择支付方式！",
    },
```

Tìm:
```js
      addProductError: "添加商品出错：{message}",
    },
```
Thay bằng:
```js
      addProductError: "添加商品出错：{message}",
      createPaymentError: "记录支付出错：{message}",
    },
```

Tìm:
```js
      paymentStatus: "支付状态",
```
Thay bằng:
```js
      paymentStatus: "支付状态",
      paymentMethod: "支付方式",
```

- [ ] **Step 6: Build kiểm tra cú pháp**

Run: `npm run build` (trong `FrontEnd/QLBanMayTinh/`)
Expected: `✓ built` không lỗi.

- [ ] **Step 7: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/i18n/locales/*.js
git commit -m "feat(i18n): add payment-method strings for POS + order detail (5 locales)"
```

---

### Task 3: Frontend — service layer dùng chung (`ThanhToanService.js` + `orderStatus.js`)

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/Service/ThanhToanService.js`
- Modify: `FrontEnd/QLBanMayTinh/src/utils/orderStatus.js`

**Interfaces:**
- Produces: `ThanhToanService.create(body) => Promise<Response>`,
  `ThanhToanService.getByDonHang(donHangId) => Promise<ThanhToanResponse[]>`;
  `paymentMethodLabel(m)`, `paymentMethodIcon(m)`, `POS_PAYMENT_METHODS` (mảng 4 giá
  trị enum, dùng để build danh sách nút trong `PosPanel.vue` — 1 nguồn duy nhất cho cả
  danh sách giá trị lẫn icon, tránh lặp lại như bảng màu `colorDot` từng gặp trước đây).
  Dùng ở Task 4 và Task 5.

- [ ] **Step 1: Tạo `Service/ThanhToanService.js`**

```js
import { get, post } from './api.js';

// Tao record thanh toan cho 1 don — dung o POS ngay sau khi tao don + dong san pham
// thanh cong (xem PosPanel.vue posPlaceOrder).
export const create = (body) => post('/api/thanh-toan', body);

// Toan bo record thanh toan cua 1 don — dung o modal "Chi tiet don hang" (OrdersTable.vue).
export const getByDonHang = (donHangId) => get(`/api/thanh-toan/don-hang/${donHangId}`);
```

- [ ] **Step 2: Thêm helper vào `utils/orderStatus.js`**

Tìm đúng khối cuối file hiện có:

```js
export const paymentStatusIcon = (s) => {
  if (s === 'unpaid')   return '⏳';
  if (s === 'partial')  return '💰';
  if (s === 'paid')     return '✅';
  if (s === 'refunded') return '↩️';
  return '●';
};
```

Thêm ngay sau (giữ nguyên khối trên, không đổi gì):

```js

// Phuong thuc thanh toan — dung o POS (chon luc tao don) va modal "Chi tiet don hang"
// (hien lai). 1 nguon duy nhat cho danh sach gia tri + icon, tranh 2 noi tu dinh nghia
// roi lech nhau (dung bai hoc tu vu colorDot o productGrouping.js).
export const POS_PAYMENT_METHODS = ['tien_mat', 'vnpay', 'chuyen_khoan', 'the_tin_dung'];

export const paymentMethodLabel = (m) => t(`admin.paymentMethod.${m}`);

export const paymentMethodIcon = (m) => {
  if (m === 'tien_mat')     return '💵';
  if (m === 'vnpay')        return '📱';
  if (m === 'chuyen_khoan') return '🏦';
  if (m === 'the_tin_dung') return '💳';
  return '💰';
};
```

- [ ] **Step 3: Build kiểm tra**

Run: `npm run build`
Expected: `✓ built` không lỗi.

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/ThanhToanService.js FrontEnd/QLBanMayTinh/src/utils/orderStatus.js
git commit -m "feat(frontend): add ThanhToanService + payment-method helpers"
```

---

### Task 4: `PosPanel.vue` — chọn phương thức thanh toán + ghi nhận khi tạo đơn

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue`

**Interfaces:**
- Consumes: `ThanhToanService.create` (Task 3), `POS_PAYMENT_METHODS`,
  `paymentMethodLabel`, `paymentMethodIcon` (Task 3), `admin.pos.paymentMethodLabel`,
  `admin.pos.paymentRequired`, `admin.errors.createPaymentError` (Task 2).

- [ ] **Step 1: Import mới**

Tìm đúng dòng hiện có (đầu file):

```js
import { groupBySanPham, variantCountBySanPham, configKey, configLabel, colorDot } from "../../utils/productGrouping.js";
```

Thay bằng (thêm 1 dòng import mới ngay sau, không đổi dòng trên):

```js
import { groupBySanPham, variantCountBySanPham, configKey, configLabel, colorDot } from "../../utils/productGrouping.js";
import { POS_PAYMENT_METHODS, paymentMethodLabel, paymentMethodIcon } from "../../utils/orderStatus.js";
import * as ThanhToanService from "../../Service/ThanhToanService.js";
```

- [ ] **Step 2: Thêm state `posPaymentMethod`**

Tìm đúng khối hiện có:

```js
const posPromoCode = ref("");
const posAppliedPromo = ref(null);
const posPromoMsg = ref("");
```

Thay bằng:

```js
const posPromoCode = ref("");
const posAppliedPromo = ref(null);
const posPromoMsg = ref("");
const posPaymentMethod = ref(null); // 1 trong POS_PAYMENT_METHODS — bat buoc chon truoc khi tao don
```

- [ ] **Step 3: Lưu/khôi phục `posPaymentMethod` khi giữ đơn / tiếp tục đơn giữ**

Tìm đúng khối hiện có (trong `posHoldOrder`):

```js
  heldOrders.value.unshift({
    id: Date.now(),
    heldAt: new Date().toISOString(),
    cart: posCart.value,
    phone: posPhone.value,
    foundCust: posFoundCust.value,
    promoCode: posPromoCode.value,
    appliedPromo: posAppliedPromo.value,
  });
  saveHeldOrders();
  // Chi don sach form tai cho — KHONG goi posReset() vi no se tra serial ve trong_kho.
  // Cac serial trong gio nay van phai o trang thai "giu_hang" cho toi khi tiep tuc
  // ban (Tiep tuc) hoac huy han (Xoa o danh sach don dang giu).
  posCart.value = [];
  posPhone.value = "";
  posFoundCust.value = null;
  posError.value = "";
  posSuccess.value = false;
  posPromoCode.value = "";
  posAppliedPromo.value = null;
  posPromoMsg.value = "";
  posStage.value = 'start';
  posPhoneNotFound.value = false;
};
```

Thay bằng:

```js
  heldOrders.value.unshift({
    id: Date.now(),
    heldAt: new Date().toISOString(),
    cart: posCart.value,
    phone: posPhone.value,
    foundCust: posFoundCust.value,
    promoCode: posPromoCode.value,
    appliedPromo: posAppliedPromo.value,
    paymentMethod: posPaymentMethod.value,
  });
  saveHeldOrders();
  // Chi don sach form tai cho — KHONG goi posReset() vi no se tra serial ve trong_kho.
  // Cac serial trong gio nay van phai o trang thai "giu_hang" cho toi khi tiep tuc
  // ban (Tiep tuc) hoac huy han (Xoa o danh sach don dang giu).
  posCart.value = [];
  posPhone.value = "";
  posFoundCust.value = null;
  posError.value = "";
  posSuccess.value = false;
  posPromoCode.value = "";
  posAppliedPromo.value = null;
  posPromoMsg.value = "";
  posPaymentMethod.value = null;
  posStage.value = 'start';
  posPhoneNotFound.value = false;
};
```

Tìm đúng khối hiện có (trong `posResumeHeld`):

```js
const posResumeHeld = (id) => {
  const held = heldOrders.value.find((h) => h.id === id);
  if (!held) return;
  posCart.value = held.cart;
  posPhone.value = held.phone;
  posFoundCust.value = held.foundCust;
  posPromoCode.value = held.promoCode;
  posAppliedPromo.value = held.appliedPromo;
  heldOrders.value = heldOrders.value.filter((h) => h.id !== id);
```

Thay bằng:

```js
const posResumeHeld = (id) => {
  const held = heldOrders.value.find((h) => h.id === id);
  if (!held) return;
  posCart.value = held.cart;
  posPhone.value = held.phone;
  posFoundCust.value = held.foundCust;
  posPromoCode.value = held.promoCode;
  posAppliedPromo.value = held.appliedPromo;
  posPaymentMethod.value = held.paymentMethod ?? null;
  heldOrders.value = heldOrders.value.filter((h) => h.id !== id);
```

- [ ] **Step 4: Reset `posPaymentMethod` ở `posReset()`**

Tìm đúng khối hiện có:

```js
const posReset = async () => {
  await Promise.all(posCart.value.map((item) => setSerialTrangThai(item, 'trong_kho')));
  posCart.value = [];
  posPhone.value = "";
  posFoundCust.value = null;
  posError.value = "";
  posSuccess.value = false;
  posPromoCode.value = "";
  posAppliedPromo.value = null;
  posPromoMsg.value = "";
  posStage.value = 'start';
  posPhoneNotFound.value = false;
};
```

Thay bằng:

```js
const posReset = async () => {
  await Promise.all(posCart.value.map((item) => setSerialTrangThai(item, 'trong_kho')));
  posCart.value = [];
  posPhone.value = "";
  posFoundCust.value = null;
  posError.value = "";
  posSuccess.value = false;
  posPromoCode.value = "";
  posAppliedPromo.value = null;
  posPromoMsg.value = "";
  posPaymentMethod.value = null;
  posStage.value = 'start';
  posPhoneNotFound.value = false;
};
```

- [ ] **Step 5: Validate + ghi nhận thanh toán trong `posPlaceOrder`**

Tìm đúng khối hiện có (đầu hàm):

```js
const posPlaceOrder = async () => {
  if (!posCart.value.length) { posError.value = t('admin.pos.cartEmpty'); return; }
  // Khach hang bat buoc phai duoc xac dinh (co san hoac tao moi) TRUOC khi co san pham
  // trong gio (theo luong posStage) nen o day luon phai co san posFoundCust.
  if (!posFoundCust.value) { posError.value = t('admin.pos.phoneRequired'); return; }
  if (posPlacing.value) return;
```

Thay bằng:

```js
const posPlaceOrder = async () => {
  if (!posCart.value.length) { posError.value = t('admin.pos.cartEmpty'); return; }
  // Khach hang bat buoc phai duoc xac dinh (co san hoac tao moi) TRUOC khi co san pham
  // trong gio (theo luong posStage) nen o day luon phai co san posFoundCust.
  if (!posFoundCust.value) { posError.value = t('admin.pos.phoneRequired'); return; }
  if (!posPaymentMethod.value) { posError.value = t('admin.pos.paymentRequired'); return; }
  if (posPlacing.value) return;
```

Tìm đúng khối hiện có (vòng lặp thêm dòng sản phẩm + rollback):

```js
    try {
      for (const item of posCart.value) {
        // Serial dang o "giu_hang" (tu luc chon vao gio, xem posSelectSerial) — backend chi
        // nhan gan serial dang "trong_kho" (chong ban trung bang pessimistic lock), nen phai
        // tra ve "trong_kho" ngay truoc khi gui de backend tu khoa + gan lai. Neu nhan vien
        // khac vua nhanh tay gianh mat serial trong khe ho nay, backend se bao loi dung nhu
        // thiet ke — do la hanh vi dung, khong phai bug.
        await setSerialTrangThai(item, 'trong_kho');
        const ctRes = await DonHangService.addChiTiet({
          donHangId, bienTheId: item.bienTheId, chiTietId: item.chiTietId, soLuong: item.soLuong, donGia: item.giaBan, giamGiaDong: 0,
        });
        if (!ctRes.ok) throw new Error(t('admin.errors.addProductError', { message: await ctRes.text() }));
      }
    } catch (e) {
      await DonHangService.remove(donHangId).catch(() => {});
      // Xoa xong nhung khong refresh thi danh sach don hang tren UI (da tang truoc do qua
      // SSE "don moi") van con dong "ma" cua don vua bi xoa — refresh lai cho khop backend.
      await refreshOrders();
      throw e;
    }
    posSuccess.value = true;
    posCart.value = []; posPhone.value = ""; posFoundCust.value = null;
    posPromoCode.value = ""; posAppliedPromo.value = null; posPromoMsg.value = "";
    posStage.value = 'start';
    await refreshOrders();
```

Thay bằng:

```js
    try {
      for (const item of posCart.value) {
        // Serial dang o "giu_hang" (tu luc chon vao gio, xem posSelectSerial) — backend chi
        // nhan gan serial dang "trong_kho" (chong ban trung bang pessimistic lock), nen phai
        // tra ve "trong_kho" ngay truoc khi gui de backend tu khoa + gan lai. Neu nhan vien
        // khac vua nhanh tay gianh mat serial trong khe ho nay, backend se bao loi dung nhu
        // thiet ke — do la hanh vi dung, khong phai bug.
        await setSerialTrangThai(item, 'trong_kho');
        const ctRes = await DonHangService.addChiTiet({
          donHangId, bienTheId: item.bienTheId, chiTietId: item.chiTietId, soLuong: item.soLuong, donGia: item.giaBan, giamGiaDong: 0,
        });
        if (!ctRes.ok) throw new Error(t('admin.errors.addProductError', { message: await ctRes.text() }));
      }
      // Ghi nhan phuong thuc thanh toan — cung nam trong try nay nen loi cung duoc rollback
      // (xoa don) giong het loi 1 dong san pham, khong de lai don "da thanh toan" nhung
      // thieu record thanh toan.
      const ttRes = await ThanhToanService.create({
        donHangId,
        ngayThanhToan: nowLocalIso(),
        phuongThucThanhToan: posPaymentMethod.value,
        soTien: posGrandTotal.value,
        maGiaoDich: null,
        trangThai: 'success',
        ghiChu: null,
      });
      if (!ttRes.ok) throw new Error(t('admin.errors.createPaymentError', { message: await parsePosApiError(ttRes) }));
    } catch (e) {
      await DonHangService.remove(donHangId).catch(() => {});
      // Xoa xong nhung khong refresh thi danh sach don hang tren UI (da tang truoc do qua
      // SSE "don moi") van con dong "ma" cua don vua bi xoa — refresh lai cho khop backend.
      await refreshOrders();
      throw e;
    }
    posSuccess.value = true;
    posCart.value = []; posPhone.value = ""; posFoundCust.value = null;
    posPromoCode.value = ""; posAppliedPromo.value = null; posPromoMsg.value = "";
    posPaymentMethod.value = null;
    posStage.value = 'start';
    await refreshOrders();
```

- [ ] **Step 6: Thêm UI chọn phương thức trong template**

Tìm đúng khối hiện có (giữa "Tổng tiền" và "Khách hàng"):

```html
      <!-- Tong tien -->
      <div class="p-2 border-top border-secondary d-flex flex-column gap-1">
        <div class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.subtotalLabel') }}</span><span>{{ formatPrice(posCartTotal) }}</span></div>
        <div v-if="posGiamGia > 0" class="d-flex justify-content-between text-success small"><span>{{ t('checkout.discount') }}</span><span>-{{ formatPrice(posGiamGia) }}</span></div>
        <div class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.shippingFeeLabel') }}</span><span>{{ posFee===0?t('admin.pos.free'):formatPrice(posFee) }}</span></div>
        <div class="d-flex justify-content-between fw-bold"><span>{{ t('admin.pos.totalLabel') }}</span><span>{{ formatPrice(posGrandTotal) }}</span></div>
      </div>
      <!-- Khach hang -->
```

Thay bằng:

```html
      <!-- Tong tien -->
      <div class="p-2 border-top border-secondary d-flex flex-column gap-1">
        <div class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.subtotalLabel') }}</span><span>{{ formatPrice(posCartTotal) }}</span></div>
        <div v-if="posGiamGia > 0" class="d-flex justify-content-between text-success small"><span>{{ t('checkout.discount') }}</span><span>-{{ formatPrice(posGiamGia) }}</span></div>
        <div class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.shippingFeeLabel') }}</span><span>{{ posFee===0?t('admin.pos.free'):formatPrice(posFee) }}</span></div>
        <div class="d-flex justify-content-between fw-bold"><span>{{ t('admin.pos.totalLabel') }}</span><span>{{ formatPrice(posGrandTotal) }}</span></div>
      </div>
      <!-- Phuong thuc thanh toan -->
      <div class="p-2 border-top border-secondary d-flex flex-column gap-2">
        <div class="text-uppercase text-secondary fw-bold" style="font-size:0.78rem;letter-spacing:0.04em;">{{ t('admin.pos.paymentMethodLabel') }}</div>
        <div class="d-flex gap-1">
          <button v-for="m in POS_PAYMENT_METHODS" :key="m"
                  class="btn btn-sm flex-fill d-flex flex-column align-items-center py-2"
                  style="border-radius:8px;font-size:0.65rem;"
                  :style="posPaymentMethod === m
                    ? 'background:rgba(244,63,94,0.12);border:1.5px solid var(--accent);color:var(--accent-fg);'
                    : 'background:var(--bg-input);border:1.5px solid var(--border-color-strong);color:var(--text-secondary);'"
                  @click="posPaymentMethod = m">
            <span style="font-size:1.1rem;">{{ paymentMethodIcon(m) }}</span>
            <span>{{ paymentMethodLabel(m) }}</span>
          </button>
        </div>
      </div>
      <!-- Khach hang -->
```

- [ ] **Step 7: Khóa nút "Tạo đơn hàng" nếu chưa chọn phương thức**

Tìm đúng dòng hiện có:

```html
          <button class="btn btn-sm btn-warning text-dark fw-bold" style="flex:2;" :disabled="posStage !== 'selling' || !posCart.length || posPlacing" @click="posPlaceOrder">{{ t('admin.pos.createOrder') }}</button>
```

Thay bằng:

```html
          <button class="btn btn-sm btn-warning text-dark fw-bold" style="flex:2;" :disabled="posStage !== 'selling' || !posCart.length || !posPaymentMethod || posPlacing" @click="posPlaceOrder">{{ t('admin.pos.createOrder') }}</button>
```

- [ ] **Step 8: Build kiểm tra**

Run: `npm run build`
Expected: `✓ built` không lỗi.

- [ ] **Step 9: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue
git commit -m "feat(frontend): add payment-method selection to POS, persist to thanh_toan on order creation"
```

---

### Task 5: `OrdersTable.vue` — hiển thị lại phương thức đã thanh toán

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue`

**Interfaces:**
- Consumes: `ThanhToanService.getByDonHang` (Task 3), `paymentMethodLabel`,
  `paymentMethodIcon` (Task 3), `admin.orderDetailModal.paymentMethod` (Task 2).

- [ ] **Step 1: Import mới**

Tìm đúng dòng hiện có (đầu file):

```js
import { orderStatusLabel, orderStatusColor, orderStatusIcon, paymentStatusLabel, paymentStatusColor, paymentStatusIcon } from "../../utils/orderStatus.js";
```

Thay bằng:

```js
import { orderStatusLabel, orderStatusColor, orderStatusIcon, paymentStatusLabel, paymentStatusColor, paymentStatusIcon, paymentMethodLabel, paymentMethodIcon } from "../../utils/orderStatus.js";
```

Tìm đúng dòng hiện có:

```js
import * as ChiTietSanPhamService from "../../Service/ChiTietSanPhamService.js";
```

Thay bằng (thêm 1 dòng import mới ngay sau, không đổi dòng trên):

```js
import * as ChiTietSanPhamService from "../../Service/ChiTietSanPhamService.js";
import * as ThanhToanService from "../../Service/ThanhToanService.js";
```

- [ ] **Step 2: Thêm state + tải dữ liệu trong `openOrderDetail`/`refreshOrderDetail`**

Tìm đúng khối hiện có:

```js
const showOrderDetailModal = ref(false);
const orderDetailData      = ref(null);   // don hang dang xem
const orderDetailItems     = ref([]);     // ChiTietDonHangResponse[]
const orderDetailLoading   = ref(false);

const openOrderDetail = async (o) => {
  orderDetailData.value  = o;
  orderDetailItems.value = [];
  showOrderDetailModal.value = true;
  orderDetailLoading.value = true;
  try {
    orderDetailItems.value = await ChiTietDonHangService.getByDonHang(o.donHangId).catch(() => []);
  } finally {
    orderDetailLoading.value = false;
  }
};
```

Thay bằng:

```js
const showOrderDetailModal = ref(false);
const orderDetailData      = ref(null);   // don hang dang xem
const orderDetailItems     = ref([]);     // ChiTietDonHangResponse[]
const orderDetailPayments  = ref([]);     // ThanhToanResponse[] — co the rong (don cu/don online)
const orderDetailLoading   = ref(false);

const openOrderDetail = async (o) => {
  orderDetailData.value  = o;
  orderDetailItems.value = [];
  orderDetailPayments.value = [];
  showOrderDetailModal.value = true;
  orderDetailLoading.value = true;
  try {
    orderDetailItems.value = await ChiTietDonHangService.getByDonHang(o.donHangId).catch(() => []);
    orderDetailPayments.value = await ThanhToanService.getByDonHang(o.donHangId).catch(() => []);
  } finally {
    orderDetailLoading.value = false;
  }
};
```

Tìm đúng khối hiện có:

```js
const refreshOrderDetail = async () => {
  await refreshOrders();
  const updated = OrdersStore.items.find(o => o.donHangId === orderDetailData.value?.donHangId);
  if (updated) orderDetailData.value = updated;
  orderDetailItems.value = await ChiTietDonHangService.getByDonHang(orderDetailData.value.donHangId).catch(() => []);
};
```

Thay bằng:

```js
const refreshOrderDetail = async () => {
  await refreshOrders();
  const updated = OrdersStore.items.find(o => o.donHangId === orderDetailData.value?.donHangId);
  if (updated) orderDetailData.value = updated;
  orderDetailItems.value = await ChiTietDonHangService.getByDonHang(orderDetailData.value.donHangId).catch(() => []);
  orderDetailPayments.value = await ThanhToanService.getByDonHang(orderDetailData.value.donHangId).catch(() => []);
};
```

- [ ] **Step 3: Hiển thị trong template**

Tìm đúng khối hiện có:

```html
        <div class="d-flex justify-content-between small">
          <span class="text-secondary">{{ t('admin.orderDetailModal.paymentStatus') }}</span>
          <span class="badge" :style="{ background: paymentStatusColor(orderDetailData.trangThaiThanhToan).bg, color: paymentStatusColor(orderDetailData.trangThaiThanhToan).text }">
            {{ paymentStatusIcon(orderDetailData.trangThaiThanhToan) }} {{ orderDetailData.trangThaiThanhToan ? paymentStatusLabel(orderDetailData.trangThaiThanhToan) : '—' }}
          </span>
        </div>
```

Thay bằng:

```html
        <div class="d-flex justify-content-between small">
          <span class="text-secondary">{{ t('admin.orderDetailModal.paymentStatus') }}</span>
          <span class="badge" :style="{ background: paymentStatusColor(orderDetailData.trangThaiThanhToan).bg, color: paymentStatusColor(orderDetailData.trangThaiThanhToan).text }">
            {{ paymentStatusIcon(orderDetailData.trangThaiThanhToan) }} {{ orderDetailData.trangThaiThanhToan ? paymentStatusLabel(orderDetailData.trangThaiThanhToan) : '—' }}
          </span>
        </div>
        <div v-if="orderDetailPayments.length" class="d-flex justify-content-between small">
          <span class="text-secondary">{{ t('admin.orderDetailModal.paymentMethod') }}</span>
          <span style="color:var(--text-primary);">
            <template v-for="(p, idx) in orderDetailPayments" :key="p.thanhToanId">
              {{ paymentMethodIcon(p.phuongThucThanhToan) }} {{ paymentMethodLabel(p.phuongThucThanhToan) }}<span v-if="idx < orderDetailPayments.length - 1">, </span>
            </template>
          </span>
        </div>
```

- [ ] **Step 4: Build kiểm tra**

Run: `npm run build`
Expected: `✓ built` không lỗi.

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue
git commit -m "feat(frontend): show recorded payment method in order detail modal"
```

---

### Task 6: Kiểm tra sống end-to-end (Playwright)

**Files:** không có (chỉ verify, không sửa code)

- [ ] **Step 1: Đăng nhập admin/staff, mở "Bán hàng"**

- [ ] **Step 2: Xây giỏ hàng như bình thường (xác định khách hàng → chọn sản phẩm →
  chọn cấu hình/màu → chọn serial)**

- [ ] **Step 3: Xác nhận nút "Tạo đơn hàng" bị khóa khi chưa chọn phương thức**

Chưa bấm vào khối 💵/📱/🏦/💳 nào — xác nhận nút "Tạo đơn hàng" vẫn disabled dù giỏ đã
có hàng và đã có khách hàng.

- [ ] **Step 4: Chọn 1 phương thức (vd 💵 Tiền mặt), xác nhận nút bật lên, bấm tạo đơn**

- [ ] **Step 5: Xác nhận đơn tạo thành công y hệt trước** (giỏ reset, không có gì khác
  biệt so với hành vi cũ — không có bước/trạng thái trung gian nào mới).

- [ ] **Step 6: Mở "Đơn hàng", tìm đúng đơn vừa tạo, bấm "Chi tiết"**

Xác nhận modal hiện thêm dòng "Đã trả bằng: 💵 Tiền mặt" ngay dưới dòng trạng thái
thanh toán.

- [ ] **Step 7: Thử "Giữ đơn" giữa chừng sau khi đã chọn phương thức, rồi "Tiếp tục"**

Xác nhận phương thức đã chọn được giữ nguyên khi tiếp tục đơn đó (không bị reset về
chưa chọn).

- [ ] **Step 8: Mở lại 1 đơn CŨ (tạo trước tính năng này, hoặc đơn online)**

Xác nhận modal "Chi tiết đơn hàng" không hiện dòng "Đã trả bằng" (vì không có record
`ThanhToan` nào) — không báo lỗi, không hiện dòng trống.
