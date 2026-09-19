<template>
  <footer
    style="background:#ffffff; color:#475569; font-size:13px; border-top:1px solid #e2e8f0; margin-top:40px;"
  >
    <!-- ─── Phần chính: 4 cột ─── -->
    <div class="container-xl py-5">
      <div class="row g-4">
        <!-- Cột 1: Hotline + Thanh toán -->
        <div class="col-12 col-sm-6 col-lg-3">
          <h4
            class="fw-black text-uppercase mb-3"
            style="font-size:13px; letter-spacing:0.06em; color:#be185d;"
          >
            {{ t('footer.hotlineHeading') }}
          </h4>
          <ul class="list-unstyled d-flex flex-column gap-2 mb-4">
            <li class="fw-medium lh-base" style="color:#475569;">
              <span style="color:#64748b;">{{ t('footer.hotlineSales') }}</span>
              <span class="fw-bold ms-1" style="color:#2563eb;">1800.2044</span>
              <span class="d-block" style="color:#94a3b8; font-size:11px;">{{ t('footer.hotlineSalesHours') }}</span>
            </li>
            <li class="fw-medium lh-base" style="color:#475569;">
              <span style="color:#64748b;">{{ t('footer.hotlineComplaint') }}</span>
              <span class="fw-bold ms-1" style="color:#2563eb;">1800.2063</span>
              <span class="d-block" style="color:#94a3b8; font-size:11px;">{{ t('footer.hotlineComplaintHours') }}</span>
            </li>
          </ul>

          <h5 class="fw-bold mb-2" style="color:#0f172a; font-size:12px; text-transform:uppercase; letter-spacing:0.04em;">
            {{ t('footer.paymentHeading') }}
          </h5>
          <div class="d-flex flex-wrap gap-2">
            <div
              v-for="pay in paymentMethods" :key="pay"
              class="px-2 py-1 rounded-2 fw-bold text-center"
              style="background:#f1f5f9; border:1px solid #e2e8f0; color:#475569; font-size:11px; min-width:48px;"
            >
              {{ pay }}
            </div>
          </div>
        </div>

        <!-- Cột 2: Đăng ký nhận khuyến mãi (chỉ nút, bỏ form) -->
        <div class="col-12 col-sm-6 col-lg-3">
          <h4
            class="fw-black text-uppercase mb-3"
            style="font-size:13px; letter-spacing:0.06em; color:#be185d;"
          >
            {{ t('footer.newsletterHeading') }}
          </h4>
          <div
            class="p-4 rounded-3 text-center"
            style="background:linear-gradient(135deg, #fdf2f8 0%, #fce7f3 100%); border:1px solid #f9a8d4;"
          >
            <div
              class="rounded-circle mx-auto mb-3 d-flex align-items-center justify-content-center"
              style="width:48px; height:48px; background:linear-gradient(135deg, #ec4899, #be185d);"
            >
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
            </div>
            <p class="mb-3" style="font-size:13px; line-height:1.5; color:#475569;">
              {{ newsletterParts[0] }}
              <span class="fw-bold" style="color:#ec4899;">{{ t('footer.newsletterDiscount') }}</span>
              {{ newsletterParts[1] }}
            </p>
            <button
              class="btn w-100 fw-bold text-white"
              style="background:linear-gradient(135deg, #ec4899, #be185d); border:none; padding:10px; border-radius:8px;"
              @click="emit('open-register')"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px;">
                <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="8.5" cy="7" r="4" />
                <line x1="20" y1="8" x2="20" y2="14" />
                <line x1="23" y1="11" x2="17" y2="11" />
              </svg>
              {{ t('footer.subscribe') }}
            </button>
          </div>
        </div>

        <!-- Cột 3: Chính sách -->
        <div class="col-12 col-sm-6 col-lg-3">
          <h4
            class="fw-black text-uppercase mb-3"
            style="font-size:13px; letter-spacing:0.06em; color:#be185d;"
          >
            {{ t('footer.policyHeading') }}
          </h4>
          <ul class="list-unstyled d-flex flex-column gap-2">
            <li v-for="item in policies" :key="item">
              <a
                href="#"
                class="text-decoration-none d-inline-block"
                style="color:#475569; font-size:12px; transition:color 0.2s;"
                @mouseenter="e => e.target.style.color='#ec4899'"
                @mouseleave="e => e.target.style.color='#475569'"
              >
                <span style="color:#cbd5e1;">›</span> {{ item }}
              </a>
            </li>
          </ul>
        </div>

        <!-- Cột 4: Kết nối + Tải app -->
        <div class="col-12 col-sm-6 col-lg-3">
          <h4
            class="fw-black text-uppercase mb-3"
            style="font-size:13px; letter-spacing:0.06em; color:#be185d;"
          >
            {{ t('footer.connectHeading') }}
          </h4>
          <div class="d-flex gap-2 mb-4">
            <a
              href="#" class="d-flex align-items-center justify-content-center rounded-circle text-decoration-none"
              style="width:36px; height:36px; background:#f1f5f9; border:1px solid #e2e8f0; color:#475569; transition:all 0.2s;"
              :aria-label="t('footer.facebookLabel')"
              @mouseenter="e => { e.currentTarget.style.background='#1877f2'; e.currentTarget.style.color='#fff'; e.currentTarget.style.borderColor='#1877f2'; }"
              @mouseleave="e => { e.currentTarget.style.background='#f1f5f9'; e.currentTarget.style.color='#475569'; e.currentTarget.style.borderColor='#e2e8f0'; }"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <path d="M22 12c0-5.52-4.48-10-10-10S2 6.48 2 12c0 4.84 3.44 8.87 8 9.8V15H8v-3h2V9.5C10 7.57 11.57 6 13.5 6H16v3h-2c-.55 0-1 .45-1 1v2h3v3h-3v6.95c4.56-.93 8-4.96 8-9.8z" />
              </svg>
            </a>
            <a
              href="#" class="d-flex align-items-center justify-content-center rounded-circle text-decoration-none"
              style="width:36px; height:36px; background:#f1f5f9; border:1px solid #e2e8f0; color:#475569; transition:all 0.2s;"
              :aria-label="t('footer.youtubeLabel')"
              @mouseenter="e => { e.currentTarget.style.background='#ff0000'; e.currentTarget.style.color='#fff'; e.currentTarget.style.borderColor='#ff0000'; }"
              @mouseleave="e => { e.currentTarget.style.background='#f1f5f9'; e.currentTarget.style.color='#475569'; e.currentTarget.style.borderColor='#e2e8f0'; }"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z" />
              </svg>
            </a>
            <a
              href="#" class="d-flex align-items-center justify-content-center rounded-circle text-decoration-none"
              style="width:36px; height:36px; background:#f1f5f9; border:1px solid #e2e8f0; color:#475569; transition:all 0.2s;"
              :aria-label="t('footer.instagramLabel')"
              @mouseenter="e => { e.currentTarget.style.background='linear-gradient(45deg, #f09433, #e6683c, #dc2743, #cc2366, #bc1888)'; e.currentTarget.style.color='#fff'; e.currentTarget.style.borderColor='transparent'; }"
              @mouseleave="e => { e.currentTarget.style.background='#f1f5f9'; e.currentTarget.style.color='#475569'; e.currentTarget.style.borderColor='#e2e8f0'; }"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" stroke="currentColor" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="2" width="20" height="20" rx="5" />
                <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z" />
                <line x1="17.5" y1="6.5" x2="17.51" y2="6.5" />
              </svg>
            </a>
          </div>

          <h5 class="fw-black text-uppercase mb-2" style="font-size:12px; letter-spacing:0.06em; color:#be185d;">
            {{ t('footer.appHeading') }}
          </h5>
          <div class="d-flex flex-column gap-2">
            <button
              class="btn fw-bold d-flex align-items-center gap-2"
              style="background:#0f172a; color:#fff; border:none; padding:10px 14px; border-radius:8px; font-size:12px;"
              @mouseenter="e => e.currentTarget.style.background='#1e293b'"
              @mouseleave="e => e.currentTarget.style.background='#0f172a'"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M17.05 20.28c-.98.95-2.05.8-3.08.35-1.09-.46-2.09-.48-3.24 0-1.44.62-2.2.44-3.06-.35C2.79 15.25 3.51 7.59 9.05 7.31c1.35.07 2.29.74 3.08.8 1.18-.24 2.31-.93 3.57-.84 1.51.12 2.65.72 3.4 1.8-3.12 1.87-2.38 5.98.48 7.13-.57 1.5-1.31 2.99-2.54 4.09l.01-.01zM12.03 7.25c-.15-2.23 1.66-4.07 3.74-4.25.29 2.58-2.34 4.5-3.74 4.25z" />
              </svg>
              <div class="d-flex flex-column align-items-start" style="line-height:1.1;">
                <span style="font-size:9px; opacity:0.8;">Tải trên</span>
                <span style="font-size:13px;">App Store</span>
              </div>
            </button>
            <button
              class="btn fw-bold d-flex align-items-center gap-2"
              style="background:#0f172a; color:#fff; border:none; padding:10px 14px; border-radius:8px; font-size:12px;"
              @mouseenter="e => e.currentTarget.style.background='#1e293b'"
              @mouseleave="e => e.currentTarget.style.background='#0f172a'"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M3 20.5V3.5c0-.41.18-.78.46-1.04l9.92 9.92-9.92 9.92c-.28-.26-.46-.63-.46-1.04zm13.81-5.06L4.06 21.5l11.34-7.45 1.41.78zM21.16 12.06l-2.79-1.61-1.96 1.55 1.96 1.55 2.79-1.49c.83-.46.83-1.45 0-1zm-1.96-1.55L4.06 2.5l11.34 7.45-1.41.78z" />
              </svg>
              <div class="d-flex flex-column align-items-start" style="line-height:1.1;">
                <span style="font-size:9px; opacity:0.8;">Tải trên</span>
                <span style="font-size:13px;">Google Play</span>
              </div>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── Phần SEO 4 cột ─── -->
    <div style="background:#f8fafc; border-top:1px solid #e2e8f0;">
      <div class="container-xl py-4">
        <div class="row g-4" style="font-size:12px;">
          <div class="col-12 col-sm-6 col-lg-3">
            <h5 class="fw-black mb-2" style="color:#0f172a; font-size:13px;">{{ t('footer.seoPhoneHeading') }}</h5>
            <p class="mb-0 lh-base" style="color:#64748b;">
              {{ t('footer.seoPhoneText') }}
            </p>
          </div>
          <div class="col-12 col-sm-6 col-lg-3">
            <h5 class="fw-black mb-2" style="color:#0f172a; font-size:13px;">{{ t('footer.seoLaptopHeading') }}</h5>
            <p class="mb-0 lh-base" style="color:#64748b;">
              {{ t('footer.seoLaptopText') }}
            </p>
          </div>
          <div class="col-12 col-sm-6 col-lg-3">
            <h5 class="fw-black mb-2" style="color:#0f172a; font-size:13px;">{{ t('footer.seoAccessoryHeading') }}</h5>
            <p class="mb-0 lh-base" style="color:#64748b;">
              {{ t('footer.seoAccessoryText') }}
            </p>
          </div>
          <div class="col-12 col-sm-6 col-lg-3">
            <h5 class="fw-black mb-2" style="color:#0f172a; font-size:13px;">{{ t('footer.seoSystemHeading') }}</h5>
            <p class="mb-0 lh-base" style="color:#64748b;">
              {{ t('footer.seoSystemText') }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── Phần thông tin công ty ─── -->
    <div class="container-xl py-4">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
        <div class="d-flex flex-column gap-1" style="font-size:12px;">
          <p class="mb-0 fw-bold" style="color:#0f172a;">{{ t('footer.companyName') }}</p>
          <p class="mb-0" style="color:#64748b;">{{ t('footer.address') }}</p>
          <p class="mb-0" style="color:#64748b;">{{ t('footer.hotlineFooter') }}</p>
        </div>
        <div class="d-flex gap-2 align-items-center flex-shrink-0">
          <div
            class="px-3 py-2 rounded-2 fw-bold"
            style="background:#f1f5f9; border:1px solid #e2e8f0; font-size:11px; color:#10b981;"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" style="vertical-align:-2px;">
              <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z" />
            </svg>
            {{ t('footer.badgeMinistry') }}
          </div>
          <div
            class="px-3 py-2 rounded-2 fw-bold"
            style="background:#f1f5f9; border:1px solid #e2e8f0; font-size:11px; color:#475569;"
          >
            <strong style="letter-spacing:0.06em; color:#0f172a;">DMCA</strong> PROTECTED
          </div>
        </div>
      </div>
    </div>

    <!-- ─── Copyright ─── -->
    <div
      class="text-center py-3"
      style="background:#f8fafc; border-top:1px solid #e2e8f0; font-size:11px; color:#64748b;"
    >
      {{ t('footer.copyright', { year: currentYear }) }}
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue';
import { t } from '../../i18n/index.js';

const emit = defineEmits(["open-register"]);

const currentYear = new Date().getFullYear();
const paymentMethods = ['Visa', 'MasterCard', 'VNPAY', 'Momo', 'ZaloPay', 'Trả Góp 0%'];
const policies = computed(() => t('footer.policies'));
const newsletterParts = computed(() => t('footer.newsletterText').split('{discount}'));
</script>