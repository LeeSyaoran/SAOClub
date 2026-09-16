# UI Optimization Plan — SAOClub

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tối ưu hệ thống màu, typography và spacing toàn bộ admin tabs + shared components — hợp tông hồng-tím brand, light mode sáng thoáng, dark mode dễ đọc.

**Architecture:** Đổi tập trung ở `theme.css` (CSS variables), `admin-list-theme.css` (shared admin classes), `Modal.vue` (shared overlay). KHÔNG đổi từng component riêng lẻ trừ khi cần thiết.

**Tech Stack:** Vanilla CSS (CSS variables), Bootstrap 5.3 utilities, no new dependencies.

**Spec:** Theme colors from `theme.css`, admin classes from `admin-list-theme.css`.

---

## Global Constraints

- Light mode default (user confirmed)
- Brand accent gradient: `#7c3aed` (purple) → `#f43f5e` (pink) — dùng xuyên suốt
- Dark mode: giữ nguyên accent gradient, chỉ cải thiện contrast
- Language: tiếng Việt labels, commit messages tiếng Anh

---

## Cấu trúc File thay đổi

| File | Thay đổi |
|------|---------|
| `src/assets/theme.css` | CSS variables: màu, spacing, typography |
| `src/assets/admin-list-theme.css` | Shared classes: alt-card, alt-toolbar, alt-btn |
| `src/components/common/Modal.vue` | Overlay + card styling |
| `src/components/auth/LoginForm.vue` | Đã xong — KHÔNG sửa lại |
| `src/components/checkout/CheckoutModal.vue` | Overlay + card — dùng Modal chung hoặc inline style |

---

## Task 1: Tối ưu theme.css — Màu nền & Text (Light Mode)

**Files:**
- Modify: `src/assets/theme.css:45-79`

**Changes:**

- [ ] **Step 1: Đổi accent-2 light mode từ teal → purple brand**

```css
/* Trước */
--accent-2:    #0e7490;  /* teal */

/* Sau */
--accent-2:    #7c3aed;  /* purple brand */
```

- [ ] **Step 2: Cải thiện light mode background — bớt hồng, thêm trắng sáng**

```css
/* Trước */
--bg-page:           #fdf5fa;  /* hồng đậm */
--bg-page-alt:       #ffffff;
--bg-card:           #ffffff;
--bg-card-alt:       #fbf5f8;  /* hồng nhạt */
--bg-card-inset:     #f7eef4;  /* hồng */

/* Sau */
--bg-page:           #faf8fc;  /* trắng tím rất nhạt */
--bg-page-alt:       #ffffff;
--bg-card:           #ffffff;
--bg-card-alt:       #f8f4fc;  /* tím nhạt */
--bg-card-inset:     #f3f0f8;  /* tím nhạt */
--bg-input:          #ffffff;
```

- [ ] **Step 3: Cải thiện border light mode — nhẹ hơn, dịu mắt**

```css
/* Trước */
--border-color:        #f3dce9;
--border-color-strong: #dba9c7;
--border-color-soft:   #f8ecf3;

/* Sau */
--border-color:        #e8e0f0;  /* tím xám nhạt */
--border-color-strong: #c4b0dc;  /* tím lavender */
--border-color-soft:   #f0ecf8;  /* tím rất nhạt */
```

- [ ] **Step 4: Cải thiện text light mode — bớt nặng, dễ đọc**

```css
/* Trước */
--text-heading:   #1a1033;  /* rất đậm */
--text-primary:   #2e2350;  /* đậm */
--text-secondary: #6b6890;
--text-muted:     #9c97b8;

/* Sau */
--text-heading:   #1e1b2e;  /* đậm vừa */
--text-primary:   #3d3455;  /* dễ đọc hơn */
--text-secondary: #6b6280;  /* nhẹ hơn */
--text-muted:     #9890a8;  /* nhẹ hơn */
```

- [ ] **Step 5: Đổi gradient brand trong light mode cho nổi bật**

```css
/* Trước */
--gradient-brand: linear-gradient(135deg, var(--accent-2), var(--accent));
/* (accent-2 = teal → accent = hồng đậm = gradient teal-hồng, KHÔNG phải tím-hồng brand) */

/* Sau */
--gradient-brand: linear-gradient(135deg, #7c3aed, #f43f5e);  /* purple → pink brand */
```

- [ ] **Step 6: Cải thiện shadow light mode — bớt pink, dùng shadow trung tính**

```css
/* Trước */
--shadow-color: rgba(225,29,72,0.1);  /* pink */

/* Sau */
--shadow-color: rgba(124, 58, 237, 0.08);  /* tím nhạt trung hòa */
```

**Commit:**
```bash
git add src/assets/theme.css
git commit -m "refactor(theme): align light mode with brand purple-pink, improve contrast"
```

---

## Task 2: Tối ưu theme.css — Dark Mode & Global Styles

**Files:**
- Modify: `src/assets/theme.css:8-43`, `:81-191`

**Changes:**

- [ ] **Step 1: Cải thiện text contrast trong dark mode — dễ đọc hơn**

```css
/* Trước */
--text-heading:   #ffffff;
--text-primary:   #e2e8f0;
--text-secondary: #a5a3c4;
--text-muted:     #6b6890;

/* Sau */
--text-heading:   #ffffff;
--text-primary:   #f0f0f8;  /* sáng hơn 1 chút */
--text-secondary: #b8b4cc;  /* sáng hơn */
--text-muted:     #7874a0;  /* sáng hơn */
```

- [ ] **Step 2: Cải thiện bg-input trong dark mode — nổi bật hơn**

```css
/* Trước */
--bg-input:  #222230;  /* gần bg-card */

/* Sau */
--bg-input:  #252535;  /* tách biệt hơn với card */
```

- [ ] **Step 3: Global focus glow — dùng màu brand thay vì hardcoded**

```css
/* Trước */
input:focus, select:focus, textarea:focus {
  box-shadow: var(--shadow-inset), 0 0 0 3px rgba(225,29,72,0.2);
}

/* Sau */
input:focus, select:focus, textarea:focus {
  box-shadow: var(--shadow-inset), 0 0 0 3px rgba(124,58,237,0.3);
}
```

- [ ] **Step 4: Cải thiện .btn-warning gradient hover trong dark mode**

```css
/* Kiểm tra hiện tại — btn-warning dùng accent-2 (tím) hover ở dark mode, KHÔNG cần đổi
   vì đã đúng brand. Nhưng cần thêm transition mượt hơn. */
.btn-warning {
  background-color: var(--accent) !important;
  border-color: var(--accent) !important;
  color: var(--accent-text) !important;
  transition: all 0.2s ease !important;  /* THÊM */
}
```

**Commit:**
```bash
git add src/assets/theme.css
git commit -m "refactor(theme): improve dark mode contrast, refine focus glow"
```

---

## Task 3: Tối ưu admin-list-theme.css — Shared Admin Classes

**Files:**
- Modify: `src/assets/admin-list-theme.css`

**Changes:**

- [ ] **Step 1: Kiểm tra và cải thiện alt-card — light mode border**

```css
/* Trước */
.alt-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color-strong);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

/* Sau — border nhẹ hơn, shadow có điểm nhấn tím */
.alt-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-md), 0 0 0 1px var(--border-color-soft);
}
```

- [ ] **Step 2: Kiểm tra alt-btn variants — primary/ghost/danger đã hợp tông chưa**

Kiểm tra các class hiện tại trong `admin-list-theme.css`:

```css
/* Tìm tất cả alt-btn variants và đảm bảo dùng CSS variables đúng */
```

- [ ] **Step 3: Kiểm tra alt-toolbar — light mode background**

```css
/* Kiểm tra alt-toolbar background có dùng var(--bg-card-alt) đúng không.
   Nếu background cứng, đổi thành var(--bg-card-alt) */
```

**Commit:**
```bash
git add src/assets/admin-list-theme.css
git commit -m "refactor(theme): refine admin alt-card borders, use CSS variables consistently"
```

---

## Task 4: Tối ưu Modal.vue — Overlay & Card (Shared)

**Files:**
- Modify: `src/components/common/Modal.vue`

**Changes:**

- [ ] **Step 1: Overlay — trắng mờ nhẹ, blur vừa (hiện tại đã tốt sau các lần sửa)**

```css
/* Giữ nguyên: background:rgba(255,255,255,0.35); backdrop-filter:blur(3px); */
/* Nếu muốn dịu hơn nữa trên màn hình sáng: giảm xuống 0.25 */
```

- [ ] **Step 2: Modal card — thêm viền tím nhạt nhẹ**

```css
/* Hiện tại: border:1px solid #f3b8d0 */
/* Cập nhật thành border-color dùng CSS var để tự đổi theo theme */
```

```html
<!-- Trước -->
:style="...border:1px solid #f3b8d0;..."

<!-- Sau -->
:style="...border:1px solid var(--border-color-strong);..."
```

**Commit:**
```bash
git add src/components/common/Modal.vue
git commit -m "refactor(theme): Modal uses CSS variables for borders"
```

---

## Task 5: Tối ưu OrdersTable — Modal Order Detail & Responsive

**Files:**
- Modify: `src/components/admin/OrdersTable.vue:1023-1024`

**Changes:**

- [ ] **Step 1: Đổi inline style modal → dùng class có sẵn hoặc CSS variables**

```html
<!-- Trước -->
<div class="alt-card d-flex flex-column" style="width:840px;max-width:96vw;max-height:92vh;border-radius:14px;">

<!-- Sau — bỏ border-radius vì alt-card đã có -->
<div class="alt-card d-flex flex-column" style="width:840px;max-width:96vw;max-height:92vh;">
```

- [ ] **Step 2: Đảm bảo sidebar cố định — thêm overflow-y cho body**

```html
<!-- Tìm phần body scroll trong order detail modal, đảm bảo có overflow-y-auto -->
<div class="overflow-y-auto d-flex flex-grow-1">
  <!-- cột trái: sản phẩm -->
  <div class="overflow-y-auto flex-grow-1" style="border-right:1px solid var(--border-color-soft);">
  <!-- cột phải: trạng thái — KHÔNG có overflow-y-auto, giữ nguyên -->
```

**Commit:**
```bash
git add src/components/admin/OrdersTable.vue
git commit -m "fix(orders): ensure order detail modal body scrolls independently"
```

---

## Task 6: Tối ưu CheckoutModal — Overlay & Card

**Files:**
- Modify: `src/components/checkout/CheckoutModal.vue`

**Changes:**

- [ ] **Step 1: Kiểm tra CheckoutModal có dùng Modal.vue chung không**

```bash
grep -n "position-fixed\|backdrop-filter\|rgba(255" src/components/checkout/CheckoutModal.vue
```

- [ ] **Step 2: Nếu có inline overlay, cập nhật tương tự Modal.vue**

```html
<!-- Nếu có: style="background:rgba(...)" → đổi thành dùng CSS variable hoặc cùng giá trị -->
```

**Commit:**
```bash
git add src/components/checkout/CheckoutModal.vue
git commit -m "refactor(theme): CheckoutModal uses consistent overlay style"
```

---

## Task 7: Kiểm tra & Fix từng Tab Admin — Nếu cần

**Files:** (lazy load — chỉ sửa nếu phát hiện vấn đề khi test)

- `src/pages/AdminPage.vue` — Dashboard, Settings
- `src/components/admin/AdminDashboard.vue`
- `src/components/admin/AdminReports.vue`
- `src/components/admin/ProductsTable.vue`
- `src/components/admin/CustomersTable.vue`
- `src/components/admin/StaffTable.vue`
- `src/components/admin/PosPanel.vue`
- `src/components/admin/InventoryPanel.vue`

**Changes:**

- [ ] **Step 1: Test toàn bộ tabs sau khi apply Tasks 1-4**

Chạy app, duyệt từng tab:
- Dashboard → Reports → Sản phẩm → Biến thể → Đơn hàng → Khách hàng → Nhân viên → Bán hàng → Kho → Trả hàng → Đánh giá → Khuyến mãi → Đổi thưởng → Cài đặt

- [ ] **Step 2: Screenshot từng tab — phát hiện vấn đề cụ thể**

- [ ] **Step 3: Fix từng tab — ưu tiên: spacing > color > typography**

**Commit:**
```bash
git add src/pages/ src/components/admin/
git commit -m "fix(theme): per-tab UI refinements after global theme update"
```

---

## Verification Checklist

- [ ] Light mode: trang chủ + tất cả admin tabs hiển thị đúng tông tím-hồng
- [ ] Dark mode: tất cả tabs đọc được, contrast đủ
- [ ] Modal overlay: trong suốt vừa phải, không chói, không vàng
- [ ] Form inputs: focus glow tím, border nhẹ nhưng nổi bật
- [ ] Buttons: hover/active có transition mượt
- [ ] Cards: shadow đều, border nhẹ, không đơn điệu
- [ ] Mobile responsive: sidebar collapse, content scroll đúng
- [ ] Không có console errors về CSS variables
