---
name: SAOPHONE (SAOClub)
description: Nền tảng bán lẻ laptop trực tuyến kèm dashboard quản trị nội bộ, tông tím-neon + hồng "Vibrant & Block-based"
colors:
  bg-page: "#12121d"
  bg-page-alt: "#0c0c15"
  bg-card: "#1d1d28"
  bg-card-inset: "#161620"
  bg-input: "#222230"
  border-default: "#2d283c"
  border-strong: "#4c1d95"
  text-heading: "#ffffff"
  text-body: "#e2e8f0"
  text-secondary: "#a5a3c4"
  text-muted: "#6b6890"
  accent-primary: "#f43f5e"
  accent-primary-fg: "#f06b81"
  accent-secondary: "#7c3aed"
  accent-tertiary: "#a78bfa"
  accent-ink: "#ffffff"
  state-success: "#16a34a"
  state-danger: "#ef4444"
  state-info: "#2563eb"
typography:
  headline:
    fontFamily: "Rubik, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "clamp(1.1rem, 2.5vw, 1.5rem)"
    fontWeight: 800
    lineHeight: 1.2
    letterSpacing: "-0.02em"
  title:
    fontFamily: "Rubik, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "0.95rem"
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: "normal"
  body:
    fontFamily: "'Nunito Sans', -apple-system, 'Segoe UI', sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: "normal"
  label:
    fontFamily: "'Nunito Sans', -apple-system, 'Segoe UI', sans-serif"
    fontSize: "0.6875rem"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "0.06em"
rounded:
  sm: "8px"
  md: "12px"
  lg: "14px"
  xl: "16px"
  pill: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "16px"
  lg: "24px"
  xl: "48px"
components:
  button-primary:
    backgroundColor: "{colors.accent-primary}"
    textColor: "{colors.accent-ink}"
    rounded: "{rounded.sm}"
    padding: "8px 16px"
  button-primary-hover:
    backgroundColor: "{colors.accent-secondary}"
    textColor: "{colors.accent-ink}"
    rounded: "{rounded.sm}"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.text-secondary}"
    rounded: "{rounded.pill}"
  input-field:
    backgroundColor: "{colors.bg-input}"
    textColor: "{colors.text-body}"
    rounded: "{rounded.md}"
    padding: "8px 12px"
  card-product:
    backgroundColor: "{colors.bg-card}"
    rounded: "{rounded.lg}"
    padding: "8px"
  badge-pill:
    backgroundColor: "{colors.accent-primary}"
    textColor: "{colors.accent-ink}"
    rounded: "{rounded.pill}"
    padding: "2px 10px"
---

# Design System: SAOPHONE

## 1. Overview

**Creative North Star: "Neon Block Arcade"**

SAOPHONE bán laptop online và điều hành cửa hàng qua một dashboard nội bộ trong cùng một hệ giao diện. Bản sắc thị giác chuyển từ "vàng-đen gaming" sang **Vibrant & Block-based**: nền chàm-đen sâu (`#12121d`), một cặp accent hồng-tím neon (`#f43f5e` hồng chủ đạo cho CTA/giá, `#7c3aed` tím cho hover/nhấn phụ) — năng lượng arcade/khu vui chơi điện tử, táo bạo và trẻ trung hơn, thay vì phong cách "deal sốc" đơn sắc trước đó. **Đã hiệu chỉnh contrast**: rose/tím vốn "tối" hơn vàng cũ (chỉ đạt ~5.1:1 và ~3.3:1 so với ~11.7:1 của vàng), nên nền/viền trung tính được giảm chroma tím ~45% cho đỡ đục, và có riêng bản `accent-primary-fg` sáng hơn (~6.4:1) dành cho accent-làm-màu-chữ.

Storefront và admin dùng chung một hệ token; storefront được phép rực rỡ hơn (banner, ticker, mega-menu), admin tiết chế hơn để không cản trở thao tác dữ liệu — nguyên tắc "một ngôn ngữ, hai âm lượng" vẫn giữ nguyên qua lần đổi màu này.

**Key Characteristics:**
- Nền chàm-đen sâu (`#12121d`/`#0c0c15`), chroma tím đã giảm để không "đục" — không phải xám trung tính hay đen thuần, nhưng cũng không ngả tím quá đậm.
- Hai accent: hồng `#f43f5e` (nền đặc: CTA, badge, trạng thái active) + tím `#7c3aed` (hover, gradient, nhấn phụ) — không đơn sắc như hệ cũ. Khi accent làm **màu chữ** trên nền tối, dùng bản sáng hơn `accent-primary-fg` (`#f06b81`), không dùng `#f43f5e` trực tiếp.
- Font kép có chủ đích: **Rubik** (hình học, đậm) cho tiêu đề, **Nunito Sans** (bo tròn, thân thiện) cho thân chữ — tương phản rõ giữa hai vai trò.
- Phẳng lúc nghỉ; nổi bật bằng shadow tối + glow hồng/tím khi hover/active.
- Bo góc theo bậc rõ ràng: nút 8px, input/nút pill 12px, card 14px, modal 16px, badge/chip luôn pill (không đổi so với hệ cũ).
- Vẫn sống được trong cả dark/light theme và 5 ngôn ngữ (vi/en/ja/ko/zh) — light theme dùng cùng accent, đảo cực nền/chữ.
- **Bảng màu trạng thái đơn hàng/serial/gauge (`orderStatus.js`, `stockDetailStatusColor`, `gaugeColor`) là hệ màu ngữ nghĩa riêng, đã khoá — không đổi theo accent thương hiệu.**

## 2. Colors

Bảng màu "Vibrant & Block-based" — **dark và light theme dùng hai tông accent khác nhau có chủ đích**, không phải cùng giá trị đảo nền. Rose/tím neon rực trên nền tối nhưng quá nhạt/thiếu tương phản trên nền sáng; light theme dùng bản đậm hơn (jewel-tone) cùng họ hue để vẫn là một thương hiệu.

### Primary
- **Dark — Arcade Rose** (`#f43f5e`): nền đặc cho CTA/nút/badge/dot chọn — KHÔNG dùng làm màu chữ trên nền tối (chỉ ~5.1:1, chữ nhỏ sẽ đuối).
- **Dark — Arcade Rose FG** (`#f06b81`, accent-primary-fg): bản sáng hơn của Rose, dùng khi accent là màu CHỮ trên nền tối (giá, label, link nhỏ, badge outline) — đạt ~6.4:1.
- **Light — Crimson Rose** (`#e11d48`, accent trong `[data-theme="light"]`): bản đậm/bão hòa hơn (kiểu Tailwind rose-600), dùng được luôn cho cả nền đặc lẫn chữ trên nền sáng (~4.7:1 trên trắng) — không cần bản fg riêng như dark. `#f43f5e` chỉ đạt ~3.67:1 trên trắng (dưới AA) nên không tái sử dụng cho light.

### Secondary
- **Dark — Neon Violet** (`#7c3aed`, accent-2 trong `[data-theme="dark"]`): hover/gradient của Arcade Rose, điểm nhấn phụ. Không dùng làm màu chữ trên dark (~3.3:1, dưới AA).
- **Dark — Soft Lilac** (`#a78bfa`, tertiary): tint/badge phụ khi cần một bậc nhạt hơn tím — chỉ dùng trên dark theme, tiết chế.
- **Light — Deep Teal** (`#0e7490`, accent-2 trong `[data-theme="light"]`): **không dùng tím trên light** — tím lệch tông, tối và "lạc quẻ" trên nền sáng. Teal đậm là cặp tương phản ấm-lạnh với Crimson Rose, đạt ~5.35:1 trên trắng (đủ để làm chữ), dùng cho hover/gradient/nhấn phụ giống vai trò của tím bên dark.

### Neutral
- **Void Indigo** (`#12121d` bg-page / `#0c0c15` bg-page-alt, dark theme): nền trang mặc định. Đã giảm ~45% chroma tím so với bản đầu (`#0f0f23`) để đỡ "đục" — vẫn giữ hue/độ sáng, chỉ bớt bão hòa.
- **Panel** (`#1d1d28` bg-card / `#191924` bg-card-alt / `#161620` bg-card-inset, dark): bề mặt card, panel, modal — cùng mức giảm chroma.
- **Input Well** (`#222230` bg-input, dark): nền ô nhập liệu, select.
- **Hairline** (`#2d283c` border-default / `#4c1d95` border-strong, dark): viền phân tách; border-strong giữ nguyên tím đậm có chủ đích, border-default đã giảm chroma cùng dải nền.
- **Blush Paper** (`#fdf5fa` bg-page / `#ffffff` bg-page-alt & bg-card, light theme): nền trang sáng — tinh chỉnh nghiêng nhẹ về hồng (thay vì lavender trung tính) để đồng bộ với accent Crimson Rose mới.
- **Blush Hairline** (`#f3dce9` border-default / `#dba9c7` border-strong, light): viền trên nền sáng, cùng họ hue với accent thay vì tím lavender cũ.
- **Chalk** (`#ffffff` text-heading): tiêu đề, giá trị nhấn mạnh.
- **Fog** (`#e2e8f0` text-body): thân chữ mặc định.
- **Ash** (`#a5a3c4` text-secondary): chữ phụ, mô tả.
- **Smoke** (`#6b6890` text-muted): chữ mờ nhất, placeholder.

### Status (không đổi vai trò, cập nhật 1 giá trị)
- **Signal Green** (`#16a34a`): còn hàng, toast thành công.
- **Signal Red** (`#ef4444`): hết hàng, lỗi, hủy — cập nhật từ `#dc2626` theo bảng Destructive mới.
- **Signal Blue** (`#2563eb`): toast thông tin trung tính.
- **Bảng màu trạng thái đơn hàng/serial** (`utils/orderStatus.js`, `stockDetailStatusColor` trong AdminPage, `gaugeColor` đèn giao thông xanh/vàng/đỏ) là hệ màu ngữ nghĩa độc lập, đã khoá từ trước — **không nằm trong hệ accent này, không tự ý đổi**.

### Named Rules
**The Two-Accent Rule.** Hồng (`#f43f5e`) là tín hiệu hành động chính; tím (`#7c3aed`) chỉ xuất hiện ở trạng thái hover/gradient hoặc nhấn phụ — không dùng tím làm CTA độc lập, tránh loãng vai trò "hành động chính" của hồng.

**The Fill-vs-Text Rule.** `#f43f5e` (accent-primary) chỉ dùng làm nền đặc, viền, hoặc dot/fill — không bao giờ làm màu chữ trên nền tối. Khi accent cần render thành text (giá, label, link nhỏ, badge outline), dùng `accent-primary-fg` (`#f06b81`). Lý do: rose là hue tối hơn vàng cũ, dùng thẳng làm chữ chỉ đạt ~5.1:1 — sát ngưỡng AA, chữ nhỏ khó đọc.

**The No-Violet-On-Light Rule.** Toàn bộ họ tím/violet (`#7c3aed` accent-2, `#a78bfa` tertiary, border-strong tím...) chỉ thuộc về dark theme. Light theme dùng Crimson Rose (`#e11d48`) + Deep Teal (`#0e7490`) — không đảo màu dark sang light; hai theme có bảng phối riêng, cùng chung tinh thần "Vibrant & Block-based" nhưng khác accent-2.

## 3. Typography

**Display Font:** Rubik (400–900) — hình học, đậm nét, mang tinh thần "block-based".
**Body Font:** Nunito Sans (300–700) — bo tròn, dễ đọc, thân thiện.

**Character:** Cặp đôi tương phản rõ: Rubik cho mọi tiêu đề thật (`h1`-`h6`), Nunito Sans cho toàn bộ phần còn lại. Đây là điểm khác biệt lớn nhất so với hệ cũ (vốn dùng một font Inter xuyên suốt) — tạo phân cấp bằng cả font lẫn weight, không chỉ weight.

### Hierarchy
- **Headline** (Rubik 800, `clamp(1.1rem, 2.5vw, 1.5rem)`, lh 1.2, letter-spacing `-0.02em`): tên thương hiệu, tiêu đề hero/banner.
- **Title** (Rubik 700, 0.95rem, lh 1.3): tên panel, tiêu đề modal, tên item nổi bật.
- **Body** (Nunito Sans 400–500, 0.8125rem/13px, lh 1.6): tên sản phẩm, mô tả, nội dung form.
- **Label** (Nunito Sans 700, 0.6875rem/11px, letter-spacing `0.06em`, thường uppercase): badge, chip, tab, nhãn nhóm.

### Named Rules
**The Two-Font Rule.** Rubik chỉ xuất hiện trên thẻ heading thật (`h1`-`h6`); mọi nơi khác — kể cả text đậm (`fw-bold`/`fw-black`) trên `div`/`span`/`button` — dùng Nunito Sans. Không lạm dụng Rubik cho toàn bộ text đậm, giữ ranh giới rõ giữa "tiêu đề" và "nhấn mạnh".

## 4. Elevation

Phẳng lúc nghỉ, nổi bật khi tương tác — giữ nguyên triết lý cũ, chỉ đổi màu glow từ vàng sang hồng/tím.

### Shadow Vocabulary
- **Lift** (`box-shadow: 0 8px 24px rgba(0,0,0,0.4)`): hover trên product card.
- **Overlay** (`box-shadow: 0 24px 80px rgba(0,0,0,0.4)`): modal.
- **Slide Panel** (`box-shadow: -12px 0 48px rgba(0,0,0,0.4)`): panel giỏ hàng trượt từ phải.
- **Accent Glow** (`box-shadow: 0 0 0 3px rgba(244,63,94,0.18), 0 0 20px rgba(244,63,94,0.25)`): viền/aura hồng cho CTA/input/card khi hover hoặc focus. Đã triển khai thật ở `:focus-visible` toàn site (`main.css`).

### Named Rules
**The Idle-Is-Flat Rule.** Không phần tử nào có shadow khi ở trạng thái nghỉ. Shadow và glow chỉ xuất hiện để phản hồi một hành động.

## 5. Components

### Buttons
- **Shape:** góc bo 8px cho nút hình chữ nhật; pill (999px) cho nút chip/filter/CTA tròn.
- **Primary:** nền `#f43f5e`, chữ trắng, fw-bold, padding 8px 16px. Class Bootstrap `btn-warning`/`text-warning`/`border-warning`/`bg-warning` được override trong `theme.css` để tự động ăn theo accent mới — không cần sửa từng component.
- **Hover / Focus:** nền chuyển sang `#7c3aed` hoặc thêm Accent Glow hồng.
- **Outline/Ghost:** nền trong suốt, viền `border-color-strong`, chữ `text-secondary` → chuyển accent khi active/hover.

### Chips / Badges
- **Style:** luôn pill, nền accent đặc khi active, nền tint hồng nhạt (`rgba(244,63,94,0.12)`) + chữ hồng khi là badge/tag trang trí.

### Cards / Containers
- **Corner Style:** 14px product card, 16px modal/panel lớn, 8–12px card nhỏ admin (không đổi).
- **Background:** `bg-card` (#1a1a2e) trên nền `bg-page` (#0f0f23).
- **Border:** 1px `border-color`; không dùng border-left màu làm accent (cấm).

### Inputs / Fields
- **Style:** nền `bg-input` (#1e1e38), viền `border-color-strong`, bo góc 12px.
- **Focus:** Accent Glow hồng (đã triển khai qua `:focus-visible` toàn site).

### Navigation
- Header sticky, nền `bg-header`. Logo dùng Headline weight (Rubik). Hover trên link điều hướng chuyển sang accent hồng.

### Toast Notification (signature component)
Nền màu đặc theo state: xanh (`#16a34a`) thành công, đỏ (`#ef4444`) lỗi, xanh dương (`#2563eb`) thông tin — chữ trắng, icon glyph đơn, trượt vào từ phải.

## 6. Do's and Don'ts

### Do:
- **Do** dùng hồng (`#f43f5e`) là tín hiệu hành động chính; tím (`#7c3aed`) chỉ cho hover/gradient/nhấn phụ.
- **Do** giữ ranh giới Rubik (heading thật) vs. Nunito Sans (mọi thứ khác) — không trộn lẫn.
- **Do** giữ mọi phần tử phẳng lúc nghỉ; chỉ thêm shadow/glow khi có tương tác.
- **Do** đảm bảo contrast ≥4.5:1 cho body text và ≥3:1 cho text lớn/label (WCAG AA, theo PRODUCT.md).
- **Do** giữ nguyên bảng màu trạng thái đơn hàng/serial/gauge — đó là hệ ngữ nghĩa độc lập, không phải accent thương hiệu.
- **Do** thiết kế mọi component để hoạt động tốt ở cả 5 ngôn ngữ và cả light/dark theme.

### Don't:
- **Don't** dùng tím làm CTA độc lập cạnh tranh vai trò với hồng.
- **Don't** dùng `border-left`/`border-right` dày làm dải màu trang trí.
- **Don't** dùng gradient text (`background-clip: text`).
- **Don't** dùng glassmorphism trang trí ở trạng thái tĩnh.
- **Don't** copy mẫu "hero-metric" SaaS chung chung hay lưới card giống hệt nhau.
- **Don't** thêm shadow/glow ở trạng thái nghỉ.
- **Don't** áp Rubik cho text đậm không phải heading — giữ đúng The Two-Font Rule.
