# Product

## Register

product

## Users

**Khách hàng** — mua laptop/máy tính online: duyệt sản phẩm, so sánh biến thể/cấu hình, thêm giỏ hàng, thanh toán, theo dõi đơn hàng và tài khoản cá nhân. Có thể là sinh viên, dân văn phòng, gamer, người tìm đồ họa/kỹ thuật — nhóm sản phẩm trải rộng (office, gaming, macbook, đồ họa, linh kiện, hàng cũ giá rẻ).

**Nhân viên nội bộ** (admin, nhân viên, quản kho) — vận hành cửa hàng qua AdminPage: quản lý đơn hàng, kho, sản phẩm/biến thể, khuyến mãi, khách hàng, theo dõi số liệu qua biểu đồ (doanh thu, tồn kho, xu hướng). Công cụ dùng hàng ngày trong ca làm việc, cần thao tác nhanh và rõ ràng hơn là gây ấn tượng.

## Product Purpose

SAOClub là nền tảng thương mại điện tử bán máy tính/laptop tích hợp quản lý vận hành nội bộ (kho, đơn hàng, nhân viên) trong cùng một ứng dụng. Thành công = khách mua hàng trôi chảy, ít ma sát (tìm sản phẩm → giỏ hàng → thanh toán), và nhân viên xử lý đơn/kho nhanh, chính xác trên dashboard admin.

## Brand Personality

**Năng động — rực rỡ — arcade.** (Cập nhật: đổi từ hệ vàng-đen "Overclock Arena" sang hệ tím-neon + hồng "Vibrant & Block-based" theo yêu cầu redesign qua ui-ux-pro-max.) Accent chính là hồng `#f43f5e` (CTA/giá) + tím `#7c3aed` (hover/nhấn phụ), nền chàm-đen sâu `#0f0f23`, font kép Rubik (tiêu đề) + Nunito Sans (thân chữ). Vẫn giữ tinh thần tương phản cao, năng lượng gaming/trẻ trung — không phải hướng premium tối giản kiểu Apple — chỉ đổi bảng màu/font, không đổi triết lý bold. Trang khách hàng (storefront) được phép mang năng lượng thương hiệu mạnh hơn (hero, banner, ticker khuyến mãi); trang admin giữ cùng ngôn ngữ thị giác nhưng tiết chế hơn để không cản trở thao tác dữ liệu.

## Anti-references

Áp dụng các cấm kỵ chung của impeccable (side-stripe border, gradient text, glassmorphism trang trí, hero-metric template, card grid giống hệt nhau, eyebrow/numbered-section mặc định) — tránh giao diện đọc như "AI tạo" ngay cả khi năng lượng tổng thể là bold/rực rỡ. Bảng màu trạng thái đơn hàng/serial/gauge (`utils/orderStatus.js`, `stockDetailStatusColor`, `gaugeColor` trong AdminPage) là hệ màu ngữ nghĩa đã khoá từ trước — không nằm trong phạm vi đổi màu thương hiệu này.

## Design Principles

- **Bold over restrained** — tím-hồng neon tương phản cao trên nền chàm-đen, không pha loãng thành theme trung tính an toàn.
- **Một ngôn ngữ thiết kế, hai âm lượng** — storefront và admin dùng chung token/component, storefront to tiếng hơn (marketing), admin trầm hơn (công cụ làm việc).
- **Tốc độ thao tác trên admin là ưu tiên** — với nhân viên, rõ ràng và nhanh quan trọng hơn hiệu ứng hình ảnh.
- **Sẵn sàng đa ngôn ngữ & đa theme** — mọi quyết định UI phải hoạt động tốt với 5 ngôn ngữ (vi/en/ja/ko/zh, độ dài chữ khác nhau) và cả dark/light theme (đã có CSS vars ở `theme.css`).
- **Bold nhưng vẫn tiếp cận được** — năng lượng cao không đánh đổi contrast hay khả năng dùng bàn phím.

## Accessibility & Inclusion

WCAG 2.1 AA: contrast ≥4.5:1 cho body text, ≥3:1 cho text lớn/UI component; điều hướng đầy đủ bằng bàn phím; aria-label cho control tương tác (nút icon, toggle theme, đóng modal...); có phương án `prefers-reduced-motion` cho mọi animation; layout chịu được text dài/ngắn khác nhau giữa 5 ngôn ngữ đang hỗ trợ.
