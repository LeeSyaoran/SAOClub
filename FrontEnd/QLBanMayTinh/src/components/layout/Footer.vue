<template>
  <!-- ========================================================
    Footer.vue — Chân trang của giao diện khách hàng
    Gồm 3 tầng: thông tin + đăng ký | từ khoá SEO | bản quyền
  ======================================================== -->

  <footer style="background:var(--bg-card-inset); color:var(--text-secondary); font-size:12px; border-top:1px solid var(--border-color); margin-top:40px;">

    <!-- ── Tầng 1: Thông tin 4 cột ── -->
    <div class="container-xl py-5">
      <div class="row g-4">

        <!-- Cột 1: Tổng đài hỗ trợ + phương thức thanh toán -->
        <div class="col-12 col-sm-6 col-xl-3">
          <h4 class="fw-black text-uppercase mb-3"
              style="font-size:13px; letter-spacing:0.04em; color:var(--text-heading);">
            {{ t('footer.hotlineHeading') }}
          </h4>
          <!-- Danh sách số điện thoại -->
          <ul class="list-unstyled d-flex flex-column gap-2 mb-3">
            <li class="fw-medium lh-base">
              {{ t('footer.hotlineSales') }}
              <span class="text-warning fw-bold">1800.2044</span>
              <span style="color:var(--text-muted); font-size:10px;"> {{ t('footer.hotlineSalesHours') }}</span>
            </li>
            <li class="fw-medium lh-base">
              {{ t('footer.hotlineComplaint') }}
              <span class="text-warning fw-bold">1800.2063</span>
              <span style="color:var(--text-muted); font-size:10px;"> {{ t('footer.hotlineComplaintHours') }}</span>
            </li>
          </ul>

          <!-- Phương thức thanh toán -->
          <div>
            <h5 class="fw-bold mb-2" style="color:var(--text-primary); font-size:12px;">{{ t('footer.paymentHeading') }}</h5>
            <!-- Các tag thanh toán -->
            <div class="d-flex flex-wrap gap-1">
              <span v-for="pay in paymentMethods" :key="pay"
                    class="badge fw-bold"
                    style="background:var(--bg-input); border:1px solid var(--border-color-strong); color:var(--text-primary); border-radius:4px; font-size:10px;">
                {{ pay }}
              </span>
            </div>
          </div>
        </div><!-- /cột 1 -->

        <!-- Cột 2: Đăng ký nhận tin khuyến mãi -->
        <div class="col-12 col-sm-6 col-xl-3">
          <h4 class="fw-black text-uppercase mb-3"
              style="font-size:13px; letter-spacing:0.04em; color:var(--text-heading);">
            {{ t('footer.newsletterHeading') }}
          </h4>
          <!-- Box đăng ký -->
          <div class="p-3 rounded-4" style="background:var(--bg-hover); border:1px solid var(--border-color);">
            <p class="mb-2" style="font-size:11px; line-height:1.6;">
              {{ newsletterParts[0] }}
              <span class="text-warning fw-bold">{{ t('footer.newsletterDiscount') }}</span>
              {{ newsletterParts[1] }}
            </p>
            <!-- Chưa có backend cho newsletter — disable để không trông như nút chết -->
            <!-- Input email -->
            <input type="email" disabled
                   class="form-control form-control-sm mb-2"
                   style="background:var(--bg-card-inset); border-color:var(--border-color); color:var(--text-primary); font-size:12px; font-weight:600; border-radius:10px;"
                   :placeholder="t('footer.emailPlaceholder')" />
            <!-- Input số điện thoại -->
            <input type="text" disabled
                   class="form-control form-control-sm mb-2"
                   style="background:var(--bg-card-inset); border-color:var(--border-color); color:var(--text-primary); font-size:12px; font-weight:600; border-radius:10px;"
                   :placeholder="t('footer.phonePlaceholder')" />
            <!-- Nút đăng ký -->
            <button class="btn btn-warning btn-sm w-100 fw-black" disabled
                    style="border-radius:10px; font-size:12px;">
              {{ t('footer.subscribe') }}
            </button>
          </div>
        </div><!-- /cột 2 -->

        <!-- Cột 3: Chính sách & Hỗ trợ -->
        <div class="col-12 col-sm-6 col-xl-3">
          <h4 class="fw-black text-uppercase mb-3"
              style="font-size:13px; letter-spacing:0.04em; color:var(--text-heading);">
            {{ t('footer.policyHeading') }}
          </h4>
          <!-- Danh sách liên kết chính sách -->
          <ul class="list-unstyled d-flex flex-column gap-2">
            <li v-for="item in policies" :key="item">
              <a href="#"
                 class="text-decoration-none fw-semibold"
                 style="color:var(--text-secondary); font-size:11px;"
                 @mouseenter="e => e.target.style.color='var(--accent)'"
                 @mouseleave="e => e.target.style.color='var(--text-secondary)'">
                • {{ item }}
              </a>
            </li>
          </ul>
        </div><!-- /cột 3 -->

        <!-- Cột 4: Mạng xã hội + Tải app -->
        <div class="col-12 col-sm-6 col-xl-3">
          <h4 class="fw-black text-uppercase mb-3"
              style="font-size:13px; letter-spacing:0.04em; color:var(--text-heading);">
            {{ t('footer.connectHeading') }}
          </h4>

          <!-- Các nút mạng xã hội -->
          <div class="d-flex gap-2 mb-4">
            <!-- Facebook -->
            <a href="#" class="d-flex align-items-center justify-content-center rounded-circle text-decoration-none"
               style="width:32px; height:32px; background:var(--bg-input); border:1px solid var(--border-color-strong); color:var(--text-secondary);"
               @mouseenter="e => e.currentTarget.style.color='var(--accent)'"
               @mouseleave="e => e.currentTarget.style.color='var(--text-secondary)'">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                <path d="M22 12c0-5.52-4.48-10-10-10S2 6.48 2 12c0 4.84 3.44 8.87 8 9.8V15H8v-3h2V9.5C10 7.57 11.57 6 13.5 6H16v3h-2c-.55 0-1 .45-1 1v2h3v3h-3v6.95c4.56-.93 8-4.96 8-9.8z"/>
              </svg>
            </a>
            <!-- YouTube -->
            <a href="#" class="d-flex align-items-center justify-content-center rounded-circle text-decoration-none"
               style="width:32px; height:32px; background:var(--bg-input); border:1px solid var(--border-color-strong); color:var(--text-secondary);"
               @mouseenter="e => e.currentTarget.style.color='var(--accent)'"
               @mouseleave="e => e.currentTarget.style.color='var(--text-secondary)'">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/>
              </svg>
            </a>
            <!-- Instagram -->
            <a href="#" class="d-flex align-items-center justify-content-center rounded-circle text-decoration-none"
               style="width:32px; height:32px; background:var(--bg-input); border:1px solid var(--border-color-strong); color:var(--text-secondary);"
               @mouseenter="e => e.currentTarget.style.color='var(--accent)'"
               @mouseleave="e => e.currentTarget.style.color='var(--text-secondary)'">
              <svg width="14" height="14" viewBox="0 0 24 24" stroke="currentColor" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="2" width="20" height="20" rx="5"/>
                <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"/>
                <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"/>
              </svg>
            </a>
          </div>

          <!-- Tải ứng dụng -->
          <h5 class="fw-black text-uppercase mb-2" style="font-size:11px; letter-spacing:0.04em; color:var(--text-heading);">
            {{ t('footer.appHeading') }}
          </h5>
          <div class="d-flex align-items-center gap-3 p-2 rounded-3"
               style="background:var(--bg-hover); border:1px solid var(--border-color);">
            <!-- QR code placeholder -->
            <div class="rounded-2 flex-shrink-0"
                 style="width:44px; height:44px; background:radial-gradient(circle, var(--bg-input), var(--bg-card-inset));"></div>
            <!-- Nút tải app -->
            <div class="d-flex flex-column gap-1">
              <button class="btn btn-sm fw-black"
                      style="background:var(--bg-card-inset); border:1px solid var(--border-color-strong); border-radius:6px; font-size:10px; white-space:nowrap; color:var(--text-heading);">
                Google Play
              </button>
              <button class="btn btn-sm fw-black"
                      style="background:var(--bg-card-inset); border:1px solid var(--border-color-strong); border-radius:6px; font-size:10px; white-space:nowrap; color:var(--text-heading);">
                App Store
              </button>
            </div>
          </div>
        </div><!-- /cột 4 -->

      </div><!-- /row -->
    </div><!-- /tầng 1 -->

    <!-- ── Tầng 2: Từ khoá SEO ── -->
    <div style="background:var(--bg-page-alt); border-top:1px solid var(--border-color-soft); border-bottom:1px solid var(--border-color-soft);">
      <div class="container-xl py-4">
        <div class="row g-3" style="font-size:11px;">
          <!-- Mỗi nhóm từ khoá SEO -->
          <div class="col-12 col-sm-6 col-xl-3">
            <h5 class="fw-black mb-1" style="color:var(--text-primary); font-size:12px;">{{ t('footer.seoPhoneHeading') }}</h5>
            <p class="mb-0 lh-base fw-medium" style="color:var(--text-muted);">
              {{ t('footer.seoPhoneText') }}
            </p>
          </div>
          <div class="col-12 col-sm-6 col-xl-3">
            <h5 class="fw-black mb-1" style="color:var(--text-primary); font-size:12px;">{{ t('footer.seoLaptopHeading') }}</h5>
            <p class="mb-0 lh-base fw-medium" style="color:var(--text-muted);">
              {{ t('footer.seoLaptopText') }}
            </p>
          </div>
          <div class="col-12 col-sm-6 col-xl-3">
            <h5 class="fw-black mb-1" style="color:var(--text-primary); font-size:12px;">{{ t('footer.seoAccessoryHeading') }}</h5>
            <p class="mb-0 lh-base fw-medium" style="color:var(--text-muted);">
              {{ t('footer.seoAccessoryText') }}
            </p>
          </div>
          <div class="col-12 col-sm-6 col-xl-3">
            <h5 class="fw-black mb-1" style="color:var(--text-primary); font-size:12px;">{{ t('footer.seoSystemHeading') }}</h5>
            <p class="mb-0 lh-base fw-medium" style="color:var(--text-muted);">
              {{ t('footer.seoSystemText') }}
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
          <p class="mb-0 fw-bold" style="color:var(--text-secondary);">{{ t('footer.companyName') }}</p>
          <p class="mb-0">{{ t('footer.address') }}</p>
          <p class="mb-0">{{ t('footer.hotlineFooter') }}</p>
        </div>
        <!-- Badges chứng nhận -->
        <div class="d-flex gap-2 align-items-center flex-shrink-0">
          <div class="px-2 py-1 rounded-2 fw-bold"
               style="background:var(--bg-input); border:1px solid var(--border-color-strong); font-size:10px; color:#34d399;">
            {{ t('footer.badgeMinistry') }}
          </div>
          <div class="px-2 py-1 rounded-2 fw-bold"
               style="background:var(--bg-input); border:1px solid var(--border-color-strong); font-size:10px; color:var(--text-secondary);">
            <strong style="letter-spacing:0.06em; color:var(--text-heading);">DMCA</strong> PROTECTED
          </div>
        </div>
      </div>
    </div><!-- /tầng 3 -->

    <!-- ── Copyright ── -->
    <div class="text-center py-3"
         style="background:var(--bg-page-alt); border-top:1px solid var(--border-color-soft); font-size:10px; color:var(--text-muted);">
      {{ t('footer.copyright', { year: currentYear }) }}
    </div>

  </footer>
</template>

<script setup>
import { computed } from 'vue';
import { t } from '../../i18n/index.js';

// Lấy năm hiện tại để hiển thị copyright
const currentYear = new Date().getFullYear();

// Danh sách phương thức thanh toán được hỗ trợ
const paymentMethods = ['Visa', 'MasterCard', 'VNPAY', 'Momo', 'ZaloPay', 'Trả Góp 0%'];

// Danh sách các liên kết chính sách (dịch theo ngôn ngữ hiện tại)
const policies = computed(() => t('footer.policies'));

// Câu giới thiệu newsletter tách quanh chỗ highlight "{discount}"
const newsletterParts = computed(() => t('footer.newsletterText').split('{discount}'));
</script>

<!-- Không còn CSS scoped — toàn bộ giao diện dùng Bootstrap utility classes -->
