<template>
  <!-- ========================================================
    Footer.vue — Chân trang của giao diện khách hàng
    Gồm 3 tầng: thông tin + đăng ký | từ khoá SEO | bản quyền
  ======================================================== -->

  <footer style="background:#111; color:#9ca3af; font-size:12px; border-top:1px solid #262626; margin-top:40px;">

    <!-- ── Tầng 1: Thông tin 4 cột ── -->
    <div class="container-xl py-5">
      <div class="row g-4">

        <!-- Cột 1: Tổng đài hỗ trợ + phương thức thanh toán -->
        <div class="col-12 col-sm-6 col-xl-3">
          <h4 class="fw-black text-white text-uppercase mb-3"
              style="font-size:13px; letter-spacing:0.04em;">
            Tổng đài hỗ trợ miễn phí
          </h4>
          <!-- Danh sách số điện thoại -->
          <ul class="list-unstyled d-flex flex-column gap-2 mb-3">
            <li class="fw-medium lh-base">
              Mua hàng - bảo hành:
              <span class="text-warning fw-bold">1800.2044</span>
              <span style="color:#6b7280; font-size:10px;"> (7h30 - 22h00)</span>
            </li>
            <li class="fw-medium lh-base">
              Khiếu nại &amp; Góp ý:
              <span class="text-warning fw-bold">1800.2063</span>
              <span style="color:#6b7280; font-size:10px;"> (8h00 - 21h30)</span>
            </li>
          </ul>

          <!-- Phương thức thanh toán -->
          <div>
            <h5 class="fw-bold mb-2" style="color:#d1d5db; font-size:12px;">Phương thức thanh toán</h5>
            <!-- Các tag thanh toán -->
            <div class="d-flex flex-wrap gap-1">
              <span v-for="pay in paymentMethods" :key="pay"
                    class="badge fw-bold"
                    style="background:#1f1f1f; border:1px solid #3f3f3f; color:#d1d5db; border-radius:4px; font-size:10px;">
                {{ pay }}
              </span>
            </div>
          </div>
        </div><!-- /cột 1 -->

        <!-- Cột 2: Đăng ký nhận tin khuyến mãi -->
        <div class="col-12 col-sm-6 col-xl-3">
          <h4 class="fw-black text-white text-uppercase mb-3"
              style="font-size:13px; letter-spacing:0.04em;">
            Đăng ký nhận tin khuyến mãi
          </h4>
          <!-- Box đăng ký -->
          <div class="p-3 rounded-4" style="background:rgba(255,255,255,0.04); border:1px solid #2a2a2a;">
            <p class="mb-2" style="font-size:11px; line-height:1.6;">
              Nhận ngay voucher
              <span class="text-warning fw-bold">giảm giá 10%</span>
              dành cho thành viên mới theo dõi hệ thống.
            </p>
            <!-- Input email -->
            <input type="email"
                   class="form-control form-control-sm mb-2"
                   style="background:#0a0a0a; border-color:#2a2a2a; color:#e5e7eb; font-size:12px; font-weight:600; border-radius:10px;"
                   placeholder="Nhập email của bạn" />
            <!-- Input số điện thoại -->
            <input type="text"
                   class="form-control form-control-sm mb-2"
                   style="background:#0a0a0a; border-color:#2a2a2a; color:#e5e7eb; font-size:12px; font-weight:600; border-radius:10px;"
                   placeholder="Nhập số điện thoại của bạn" />
            <!-- Nút đăng ký -->
            <button
                type="button"
                class="btn btn-link btn-sm text-warning fw-bold p-0"
                @click="emit('open-register')"
            >
              Đăng ký ngay
            </button>
          </div>
        </div><!-- /cột 2 -->

        <!-- Cột 3: Chính sách & Hỗ trợ -->
        <div class="col-12 col-sm-6 col-xl-3">
          <h4 class="fw-black text-white text-uppercase mb-3"
              style="font-size:13px; letter-spacing:0.04em;">
            Chính sách &amp; Hỗ trợ
          </h4>
          <!-- Danh sách liên kết chính sách -->
          <ul class="list-unstyled d-flex flex-column gap-2">
            <li v-for="item in policies" :key="item">
              <a href="#"
                 class="text-decoration-none fw-semibold"
                 style="color:#9ca3af; font-size:11px;"
                 @mouseenter="e => e.target.style.color='#facc15'"
                 @mouseleave="e => e.target.style.color='#9ca3af'">
                • {{ item }}
              </a>
            </li>
          </ul>
        </div><!-- /cột 3 -->

        <!-- Cột 4: Mạng xã hội + Tải app -->
        <div class="col-12 col-sm-6 col-xl-3">
          <h4 class="fw-black text-white text-uppercase mb-3"
              style="font-size:13px; letter-spacing:0.04em;">
            Kết nối với SAOPHONE
          </h4>

          <!-- Các nút mạng xã hội -->
          <div class="d-flex gap-2 mb-4">
            <!-- Facebook -->
            <a href="#" class="d-flex align-items-center justify-content-center rounded-circle text-decoration-none"
               style="width:32px; height:32px; background:#1f1f1f; border:1px solid #3f3f3f; color:#9ca3af;"
               @mouseenter="e => e.currentTarget.style.color='#facc15'"
               @mouseleave="e => e.currentTarget.style.color='#9ca3af'">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                <path d="M22 12c0-5.52-4.48-10-10-10S2 6.48 2 12c0 4.84 3.44 8.87 8 9.8V15H8v-3h2V9.5C10 7.57 11.57 6 13.5 6H16v3h-2c-.55 0-1 .45-1 1v2h3v3h-3v6.95c4.56-.93 8-4.96 8-9.8z"/>
              </svg>
            </a>
            <!-- YouTube -->
            <a href="#" class="d-flex align-items-center justify-content-center rounded-circle text-decoration-none"
               style="width:32px; height:32px; background:#1f1f1f; border:1px solid #3f3f3f; color:#9ca3af;"
               @mouseenter="e => e.currentTarget.style.color='#facc15'"
               @mouseleave="e => e.currentTarget.style.color='#9ca3af'">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/>
              </svg>
            </a>
            <!-- Instagram -->
            <a href="#" class="d-flex align-items-center justify-content-center rounded-circle text-decoration-none"
               style="width:32px; height:32px; background:#1f1f1f; border:1px solid #3f3f3f; color:#9ca3af;"
               @mouseenter="e => e.currentTarget.style.color='#facc15'"
               @mouseleave="e => e.currentTarget.style.color='#9ca3af'">
              <svg width="14" height="14" viewBox="0 0 24 24" stroke="currentColor" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="2" width="20" height="20" rx="5"/>
                <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"/>
                <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"/>
              </svg>
            </a>
          </div>

          <!-- Tải ứng dụng -->
          <h5 class="fw-black text-white text-uppercase mb-2" style="font-size:11px; letter-spacing:0.04em;">
            Tải ứng dụng hệ thống
          </h5>
          <div class="d-flex align-items-center gap-3 p-2 rounded-3"
               style="background:rgba(255,255,255,0.03); border:1px solid #2a2a2a;">
            <!-- QR code placeholder -->
            <div class="rounded-2 flex-shrink-0"
                 style="width:44px; height:44px; background:radial-gradient(circle, #1f1f1f, #0a0a0a);"></div>
            <!-- Nút tải app -->
            <div class="d-flex flex-column gap-1">
              <button class="btn btn-sm fw-black text-white"
                      style="background:#0a0a0a; border:1px solid #3f3f3f; border-radius:6px; font-size:10px; white-space:nowrap;">
                Google Play
              </button>
              <button class="btn btn-sm fw-black text-white"
                      style="background:#0a0a0a; border:1px solid #3f3f3f; border-radius:6px; font-size:10px; white-space:nowrap;">
                App Store
              </button>
            </div>
          </div>
        </div><!-- /cột 4 -->

      </div><!-- /row -->
    </div><!-- /tầng 1 -->

    <!-- ── Tầng 2: Từ khoá SEO ── -->
    <div style="background:#0a0a0a; border-top:1px solid #1a1a1a; border-bottom:1px solid #1a1a1a;">
      <div class="container-xl py-4">
        <div class="row g-3" style="font-size:11px;">
          <!-- Mỗi nhóm từ khoá SEO -->
          <div class="col-12 col-sm-6 col-xl-3">
            <h5 class="fw-black mb-1" style="color:#d1d5db; font-size:12px;">Điện thoại di động</h5>
            <p class="mb-0 lh-base fw-medium" style="color:#6b7280;">
              iPhone 17 Pro Max, Samsung Galaxy S26 Ultra, Oppo Find X9 Ultra, Xiaomi 14T.
            </p>
          </div>
          <div class="col-12 col-sm-6 col-xl-3">
            <h5 class="fw-black mb-1" style="color:#d1d5db; font-size:12px;">Laptop &amp; Máy tính</h5>
            <p class="mb-0 lh-base fw-medium" style="color:#6b7280;">
              MacBook Pro M4, Laptop Asus Gaming, Laptop Dell XPS, HP Omnibook, Lenovo Legion Go 2.
            </p>
          </div>
          <div class="col-12 col-sm-6 col-xl-3">
            <h5 class="fw-black mb-1" style="color:#d1d5db; font-size:12px;">Phụ kiện cao cấp</h5>
            <p class="mb-0 lh-base fw-medium" style="color:#6b7280;">
              Tai nghe Bluetooth, Củ cáp sạc nhanh, Sạc dự phòng Anker, Bao da ốp lưng, Cường lực chính hãng.
            </p>
          </div>
          <div class="col-12 col-sm-6 col-xl-3">
            <h5 class="fw-black mb-1" style="color:#d1d5db; font-size:12px;">Hệ thống phân phối</h5>
            <p class="mb-0 lh-base fw-medium" style="color:#6b7280;">
              Hệ thống quản lý kho và phân phối sản phẩm công nghệ ứng dụng mô hình Agile/Scrum.
            </p>
          </div>
        </div>
      </div>
    </div><!-- /tầng 2 -->

    <!-- ── Tầng 3: Thông tin công ty + badges ── -->
    <div class="container-xl py-4">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
        <!-- Địa chỉ + hotline -->
        <div class="d-flex flex-column gap-1" style="font-size:11px;">
          <p class="mb-0 fw-bold" style="color:#9ca3af;">Hệ thống bán lẻ thiết bị công nghệ SAOPHONE</p>
          <p class="mb-0">Cơ sở vận hành: Trịnh Văn Bô, Nam Từ Liêm, Hà Nội</p>
          <p class="mb-0">Hotline: 0123.456.789 | Thiết kế và phát triển bởi Đội ngũ Phần mềm FPT Polytechnic.</p>
        </div>
        <!-- Badges chứng nhận -->
        <div class="d-flex gap-2 align-items-center flex-shrink-0">
          <div class="px-2 py-1 rounded-2 fw-bold"
               style="background:#1f1f1f; border:1px solid #3f3f3f; font-size:10px; color:#34d399;">
            ĐÃ THÔNG BÁO BỘ CÔNG THƯƠNG
          </div>
          <div class="px-2 py-1 rounded-2 fw-bold"
               style="background:#1f1f1f; border:1px solid #3f3f3f; font-size:10px; color:#9ca3af;">
            <strong class="text-white" style="letter-spacing:0.06em;">DMCA</strong> PROTECTED
          </div>
        </div>
      </div>
    </div><!-- /tầng 3 -->

    <!-- ── Copyright ── -->
    <div class="text-center py-3"
         style="background:#0a0a0a; border-top:1px solid #1a1a1a; font-size:10px; color:#4b5563;">
      &copy; {{ currentYear }} SAOPHONE. All rights reserved.
    </div>

  </footer>
</template>

<script setup>
// Lấy năm hiện tại để hiển thị copyright
const currentYear = new Date().getFullYear();

// Danh sách phương thức thanh toán được hỗ trợ
const paymentMethods = ['Visa', 'MasterCard', 'VNPAY', 'Momo', 'ZaloPay', 'Trả Góp 0%'];

// Danh sách các liên kết chính sách
const policies = [
  'Chính sách đổi trả 7 ngày',
  'Chính sách bảo hành sản phẩm',
  'Bảo mật thông tin khách hàng',
  'Tra cứu hóa đơn điện tử',
  'Chính sách giao hàng',
  'Điều khoản dịch vụ và quy chế hoạt động'
];
</script>

<!-- Không còn CSS scoped — toàn bộ giao diện dùng Bootstrap utility classes -->
