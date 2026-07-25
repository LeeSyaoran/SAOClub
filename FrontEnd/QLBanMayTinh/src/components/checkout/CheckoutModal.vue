<template>
  <div v-if="modelValue"
       class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:rgba(0,0,0,0.8); z-index:1050; backdrop-filter:blur(4px);"
       @click.self="$emit('update:modelValue', false)">

    <div class="rounded-4 d-flex flex-column"
         role="dialog" aria-modal="true"
         style="background:var(--bg-card); border:1px solid var(--border-color); width:660px; max-width:96vw; max-height:92vh; box-shadow:0 24px 80px rgba(0,0,0,0.4);">

      <!-- ══ Màn hình thành công ══ -->
      <template v-if="checkoutSuccess">
        <div class="d-flex flex-column align-items-center justify-content-center gap-4 p-5 text-center">
          <div class="d-flex align-items-center justify-content-center rounded-circle"
               style="width:72px;height:72px;background:rgba(72,199,142,0.15);color:#48c78e;font-size:2rem;">✓</div>
          <div>
            <h2 class="fw-black mb-1" style="font-size:1.4rem; color:var(--text-heading);">{{ t('checkout.successTitle') }}</h2>
            <p class="mb-0" style="font-size:0.9rem; color:var(--text-secondary);">
              {{ t('checkout.orderCode') }} <strong class="text-warning">#{{ checkoutOrderId }}</strong>
            </p>
          </div>
          <!-- Hướng dẫn thanh toán theo phương thức đã chọn -->
          <div v-if="selectedPayment === 'tien_mat'"
               class="p-3 rounded-3 text-center small"
               style="background:#1e2a1e; color:#6ee7b7; border:1px solid #2a3d2a; max-width:360px;">
            {{ t('checkout.cashInstruction', { amount: formatPrice(checkoutFinalTotal) }) }}
          </div>
          <div v-else-if="selectedPayment === 'qr'"
               class="p-3 rounded-3 text-center small"
               style="background:#1a1e2a; color:#93c5fd; border:1px solid #252e3a; max-width:360px;">
            {{ t('checkout.qrInstruction') }}
          </div>
          <div v-else
               class="p-3 rounded-3 text-center small"
               style="background:#1a1e2a; color:#93c5fd; border:1px solid #252e3a; max-width:360px;">
            {{ t('checkout.bankInstruction', { amount: formatPrice(checkoutFinalTotal) }) }}
          </div>
          <button class="btn btn-warning fw-bold px-5 rounded-pill" style="font-size:0.9rem;" @click="$emit('update:modelValue', false)">{{ t('checkout.close') }}</button>
        </div>
      </template>

      <!-- ══ Form đặt hàng ══ -->
      <template v-else>

        <!-- Header + step indicator -->
        <div class="px-4 pt-4 pb-3" style="border-bottom:1px solid var(--border-color-soft);">
          <div class="d-flex justify-content-between align-items-start">
            <div>
              <h5 class="fw-black mb-1" style="font-size:1rem; color:var(--text-heading);">
                {{ checkoutStep === 1 ? t('checkout.infoTitle') : t('checkout.paymentTitle') }}
              </h5>
              <div class="d-flex align-items-center gap-2">
                <div class="d-flex align-items-center gap-1">
                  <div class="rounded-circle d-flex align-items-center justify-content-center fw-bold"
                       style="width:20px;height:20px;font-size:10px;"
                       :style="checkoutStep >= 1 ? 'background:var(--accent);color:var(--accent-text);' : 'background:var(--bg-card-alt);color:var(--text-muted);'">1</div>
                  <span class="small" :style="checkoutStep >= 1 ? 'color:var(--accent-fg);' : 'color:var(--text-secondary);'" style="font-size:11px;">{{ t('checkout.stepInfo') }}</span>
                </div>
                <div style="font-size:10px; color:var(--text-secondary);">───</div>
                <div class="d-flex align-items-center gap-1">
                  <div class="rounded-circle d-flex align-items-center justify-content-center fw-bold"
                       style="width:20px;height:20px;font-size:10px;"
                       :style="checkoutStep >= 2 ? 'background:var(--accent);color:var(--accent-text);' : 'background:var(--bg-card-alt);color:var(--text-muted);'">2</div>
                  <span class="small" :style="checkoutStep >= 2 ? 'color:var(--accent-fg);' : 'color:var(--text-secondary);'" style="font-size:11px;">{{ t('checkout.stepPayment') }}</span>
                </div>
              </div>
            </div>
            <button class="btn-close mt-1" style="font-size:0.7rem;" :aria-label="t('common.close')" @click="$emit('update:modelValue', false)"></button>
          </div>
        </div>

        <!-- Thông báo lỗi -->
        <div v-if="checkoutError" class="mx-4 mt-3">
          <div class="alert alert-danger small py-2 mb-0 rounded-3">{{ checkoutError }}</div>
        </div>

        <!-- ── BƯỚC 1: Thông tin giao hàng ── -->
        <div v-if="checkoutStep === 1" class="overflow-y-auto flex-grow-1 px-4 py-3 d-flex flex-column gap-4">

          <!-- Giỏ hàng tóm tắt -->
          <div>
            <div class="fw-semibold mb-2" style="font-size:11px; letter-spacing:0.06em; text-transform:uppercase; color:var(--text-secondary);">{{ t('checkout.orderSummary', { count: cart.length }) }}</div>
            <div class="d-flex flex-column gap-1 rounded-3 p-2" style="background:var(--bg-card-alt);">
              <div v-for="item in cart" :key="item.bienTheId"
                   class="d-flex align-items-center gap-3 px-2 py-1">
                <div style="width:36px;height:36px;flex-shrink:0;">
                  <img v-if="item.hinhAnhChinh" :src="item.hinhAnhChinh" :alt="item.tenSanPham" style="width:36px;height:36px;object-fit:contain;border-radius:6px;" />
                  <div v-else class="d-flex align-items-center justify-content-center rounded-2" style="width:36px;height:36px;background:var(--bg-card-inset);font-size:1rem;">💻</div>
                </div>
                <span class="flex-grow-1 small text-truncate" style="color:var(--text-primary);">{{ item.tenSanPham }}</span>
                <span class="small flex-shrink-0" style="color:var(--text-secondary);">×{{ item.quantity }}</span>
                <span class="text-warning fw-semibold small flex-shrink-0">{{ formatPrice(item.giaBan * item.quantity) }}</span>
              </div>
            </div>
          </div>

          <!-- Thông tin khách hàng -->
          <div>
            <div class="fw-semibold mb-2" style="font-size:11px; letter-spacing:0.06em; text-transform:uppercase; color:var(--text-secondary);">{{ t('checkout.customerHeading') }}</div>
            <!-- Đã đăng nhập: đã biết chắc là khách hàng nào, khỏi cần tìm theo SĐT nữa -->
            <div v-if="isLoggedInCustomer" class="small p-2 rounded-3 mb-2" style="background:rgba(72,199,142,0.1);color:#48c78e;">
              {{ t('checkout.loggedInAs') }} <strong>{{ checkoutForm.hoTen }}</strong> · {{ checkoutForm.soDienThoai }}
            </div>
            <template v-else>
              <div class="d-flex gap-2 mb-2">
                <input v-model="checkoutForm.soDienThoai"
                       class="form-control form-control-sm"
                       style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:10px;"
                       :placeholder="t('checkout.phonePlaceholder')" @keyup.enter="lookupCustomer" />
                <button class="btn btn-sm btn-outline-warning flex-shrink-0 px-3" style="border-radius:10px;" @click="lookupCustomer">{{ t('checkout.find') }}</button>
              </div>
              <div v-if="foundCustomer" class="small p-2 rounded-3 mb-2" style="background:rgba(72,199,142,0.1);color:#48c78e;">
                {{ t('checkout.foundCustomer') }} <strong>{{ foundCustomer.hoTen }}</strong>
              </div>
              <div v-else-if="checkoutForm.soDienThoai" class="small p-2 rounded-3 mb-2" style="background:var(--bg-card-alt); color:var(--text-secondary);">
                {{ t('checkout.newCustomer') }}
              </div>
            </template>
            <div class="row g-2">
              <div class="col-6">
                <input v-model="checkoutForm.hoTen" class="form-control form-control-sm"
                       style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:10px;"
                       :placeholder="t('checkout.fullNamePlaceholder')" />
              </div>
              <div class="col-6">
                <input v-model="checkoutForm.email" class="form-control form-control-sm"
                       style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:10px;"
                       :placeholder="t('checkout.emailPlaceholder')" />
              </div>
            </div>
          </div>

          <!-- Thông tin giao hàng -->
          <div>
            <div class="fw-semibold mb-2" style="font-size:11px; letter-spacing:0.06em; text-transform:uppercase; color:var(--text-secondary);">{{ t('checkout.shippingHeading') }}</div>
            <div class="row g-2 mb-2">
              <div class="col-6">
                <input v-model="checkoutForm.nguoiNhan" class="form-control form-control-sm"
                       style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:10px;"
                       :placeholder="t('checkout.receiverPlaceholder')" />
              </div>
              <div class="col-6">
                <input v-model="checkoutForm.sdtNguoiNhan" class="form-control form-control-sm"
                       style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:10px;"
                       :placeholder="t('checkout.receiverPhonePlaceholder')" />
              </div>
            </div>
            <AddressPicker v-model="checkoutForm.diaChiGiaoHangText" :placeholder="t('checkout.addressPlaceholder')" />
          </div>

          <!-- Mã khuyến mãi -->
          <div>
            <div class="fw-semibold mb-2" style="font-size:11px; letter-spacing:0.06em; text-transform:uppercase; color:var(--text-secondary);">{{ t('checkout.promoHeading') }}</div>

            <!-- Mã đang áp dụng được cho đơn này, giảm nhiều nhất xếp trước -->
            <div v-if="eligiblePromos.length" class="d-flex flex-column gap-2 mb-2">
              <div v-for="(p, idx) in eligiblePromos" :key="p.khuyenMaiId"
                   class="d-flex align-items-center justify-content-between p-2 rounded-3"
                   style="cursor:pointer;border:1px solid;"
                   :style="appliedPromo?.khuyenMaiId===p.khuyenMaiId ? 'border-color:var(--accent);background:rgba(244,63,94,0.08);' : 'border-color:var(--border-color-soft);background:var(--bg-card-alt);'"
                   @click="selectPromo(p)">
                <div class="min-width-0">
                  <div class="d-flex align-items-center gap-2">
                    <span class="fw-bold small" style="color:var(--text-heading);">{{ p.maKhuyenMai }}</span>
                    <span v-if="idx===0" class="badge" style="background:rgba(250,204,21,0.15);color:#facc15;font-size:0.63rem;">🔥 {{ t('checkout.bestPromo') }}</span>
                  </div>
                  <div class="text-truncate" style="font-size:11px;color:var(--text-secondary);">{{ p.tenKhuyenMai }}</div>
                </div>
                <div class="text-warning fw-bold small flex-shrink-0 ms-2">− {{ formatPrice(p.discount) }}</div>
              </div>
            </div>

            <div v-if="eligiblePromos.length === 0" class="small px-1 mb-2" style="color:var(--text-secondary);">{{ t('checkout.noPromo') }}</div>

            <!-- Nhập tay mã không nằm trong danh sách gợi ý ở trên (vd mã riêng không công khai) -->
            <div class="d-flex gap-2">
              <input v-model="checkoutForm.maKhuyenMai" class="form-control form-control-sm"
                     style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:10px;"
                     :placeholder="t('checkout.promoPlaceholder')" @keyup.enter="applyPromo" />
              <button class="btn btn-sm flex-shrink-0" style="background:var(--bg-hover);color:var(--text-primary);border-radius:10px;" @click="applyPromo">{{ t('checkout.apply') }}</button>
            </div>
            <div v-if="promoMsg" class="small mt-2 px-1" :class="appliedPromo ? 'text-success' : 'text-danger'">{{ promoMsg }}</div>
          </div>

          <!-- Voucher cá nhân (đổi từ điểm) -->
          <div v-if="isLoggedInCustomer && eligibleVouchers.length">
            <div class="fw-semibold mb-2" style="font-size:11px; letter-spacing:0.06em; text-transform:uppercase; color:var(--text-secondary);">{{ t('checkout.voucherHeading') }}</div>
            <div class="d-flex flex-column gap-2">
              <div v-for="v in eligibleVouchers" :key="v.phieuId"
                   class="d-flex align-items-center justify-content-between p-2 rounded-3"
                   style="cursor:pointer;border:1px solid;"
                   :style="appliedVoucher?.phieuId===v.phieuId ? 'border-color:var(--accent);background:rgba(244,63,94,0.08);' : 'border-color:var(--border-color-soft);background:var(--bg-card-alt);'"
                   @click="selectVoucher(v)">
                <span class="fw-bold small" style="color:var(--text-heading);">{{ v.maPhieu }}</span>
                <div class="text-warning fw-bold small flex-shrink-0 ms-2">− {{ formatPrice(v.discount) }}</div>
              </div>
            </div>
          </div>

          <!-- Tổng tiền -->
          <div class="p-3 rounded-3 d-flex flex-column gap-2" style="background:var(--bg-card-alt);border:1px solid var(--border-color-soft);">
            <div class="d-flex justify-content-between small" style="color:var(--text-secondary);">
              <span>{{ t('checkout.subtotal') }}</span><span>{{ formatPrice(cartTotal) }}</span>
            </div>
            <div class="d-flex justify-content-between small" style="color:var(--text-secondary);">
              <span>{{ t('checkout.shippingFee') }}</span>
              <span :class="phiVanChuyen === 0 ? 'text-success' : ''">{{ phiVanChuyen === 0 ? t('checkout.free') : formatPrice(phiVanChuyen) }}</span>
            </div>
            <div v-if="checkoutGiamGia > 0" class="d-flex justify-content-between small text-success">
              <span>{{ t('checkout.discount') }}</span><span>− {{ formatPrice(checkoutGiamGia) }}</span>
            </div>
            <div class="d-flex justify-content-between fw-bold pt-2" style="border-top:1px solid var(--border-color);">
              <span style="color:var(--text-heading);">{{ t('checkout.total') }}</span>
              <strong class="text-warning" style="font-size:1.05rem;">{{ formatPrice(checkoutTotal) }}</strong>
            </div>
          </div>

        </div><!-- /bước 1 -->

        <!-- ── BƯỚC 2: Phương thức thanh toán ── -->
        <div v-else class="overflow-y-auto flex-grow-1 px-4 py-3 d-flex flex-column gap-4">

          <!-- Số tiền cần thanh toán -->
          <div class="text-center py-2">
            <div class="small mb-1" style="color:var(--text-secondary);">{{ t('checkout.totalToPay') }}</div>
            <div class="fw-black text-warning" style="font-size:2rem;">{{ formatPrice(checkoutTotal) }}</div>
          </div>

          <!-- Chọn phương thức -->
          <div>
            <div class="fw-semibold mb-3" style="font-size:11px; letter-spacing:0.06em; text-transform:uppercase; color:var(--text-secondary);">{{ t('checkout.choosePayment') }}</div>
            <div class="d-flex flex-column gap-2">

              <!-- Tiền mặt -->
              <label class="d-flex align-items-center gap-3 p-3 rounded-3 cursor-pointer"
                     style="border:2px solid; cursor:pointer;"
                     :style="selectedPayment==='tien_mat' ? 'border-color:var(--accent);background:rgba(244,63,94,0.08);' : 'border-color:var(--border-color-soft);background:var(--bg-card-alt);'"
                     @click="selectedPayment='tien_mat'">
                <div class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0"
                     style="width:42px;height:42px;background:#2a2000;font-size:1.3rem;">💵</div>
                <div class="flex-grow-1">
                  <div class="fw-bold" style="font-size:0.9rem; color:var(--text-heading);">{{ t('checkout.cashTitle') }}</div>
                  <div style="font-size:11px; color:var(--text-secondary);">{{ t('checkout.cashDesc') }}</div>
                </div>
                <div class="rounded-circle border d-flex align-items-center justify-content-center flex-shrink-0"
                     style="width:20px;height:20px;"
                     :style="selectedPayment==='tien_mat' ? 'border-color:var(--accent);background:var(--accent);' : 'border-color:var(--border-color-strong);background:transparent;'">
                  <div v-if="selectedPayment==='tien_mat'" style="width:8px;height:8px;border-radius:50%;background:var(--accent-text);"></div>
                </div>
              </label>

              <!-- QR Code -->
              <label class="d-flex align-items-center gap-3 p-3 rounded-3"
                     style="border:2px solid; cursor:pointer;"
                     :style="selectedPayment==='qr' ? 'border-color:var(--accent);background:rgba(244,63,94,0.08);' : 'border-color:var(--border-color-soft);background:var(--bg-card-alt);'"
                     @click="selectedPayment='qr'">
                <div class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0"
                     style="width:42px;height:42px;background:#0a1a2a;font-size:1.3rem;">📱</div>
                <div class="flex-grow-1">
                  <div class="fw-bold" style="font-size:0.9rem; color:var(--text-heading);">{{ t('checkout.qrTitle') }}</div>
                  <div style="font-size:11px; color:var(--text-secondary);">{{ t('checkout.qrDesc') }}</div>
                </div>
                <div class="rounded-circle border d-flex align-items-center justify-content-center flex-shrink-0"
                     style="width:20px;height:20px;"
                     :style="selectedPayment==='qr' ? 'border-color:var(--accent);background:var(--accent);' : 'border-color:var(--border-color-strong);background:transparent;'">
                  <div v-if="selectedPayment==='qr'" style="width:8px;height:8px;border-radius:50%;background:var(--accent-text);"></div>
                </div>
              </label>

              <!-- Chuyển khoản -->
              <label class="d-flex align-items-center gap-3 p-3 rounded-3"
                     style="border:2px solid; cursor:pointer;"
                     :style="selectedPayment==='chuyen_khoan' ? 'border-color:var(--accent);background:rgba(244,63,94,0.08);' : 'border-color:var(--border-color-soft);background:var(--bg-card-alt);'"
                     @click="selectedPayment='chuyen_khoan'">
                <div class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0"
                     style="width:42px;height:42px;background:#0a1a0a;font-size:1.3rem;">🏦</div>
                <div class="flex-grow-1">
                  <div class="fw-bold" style="font-size:0.9rem; color:var(--text-heading);">{{ t('checkout.bankTitle') }}</div>
                  <div style="font-size:11px; color:var(--text-secondary);">{{ t('checkout.bankDesc') }}</div>
                </div>
                <div class="rounded-circle border d-flex align-items-center justify-content-center flex-shrink-0"
                     style="width:20px;height:20px;"
                     :style="selectedPayment==='chuyen_khoan' ? 'border-color:var(--accent);background:var(--accent);' : 'border-color:var(--border-color-strong);background:transparent;'">
                  <div v-if="selectedPayment==='chuyen_khoan'" style="width:8px;height:8px;border-radius:50%;background:var(--accent-text);"></div>
                </div>
              </label>

            </div>
          </div>

          <!-- QR Code hiển thị khi chọn QR -->
          <Transition name="fade">
          <div v-if="selectedPayment === 'qr'" class="d-flex flex-column align-items-center gap-3 p-4 rounded-3" style="background:var(--bg-card-inset);border:1px solid var(--border-color-soft);">
            <div class="fw-bold small" style="color:var(--text-heading);">{{ t('checkout.scanQr') }}</div>
            <img v-if="!qrImageFailed" :src="qrImageUrl" alt="VietQR" @error="qrImageFailed = true"
                 style="width:220px;height:220px;border-radius:12px;background:#fff;padding:6px;" />
            <div v-else class="d-flex flex-column align-items-center justify-content-center text-center small"
                 style="width:220px;height:220px;border-radius:12px;background:var(--bg-card-alt);color:var(--text-secondary);gap:6px;">
              <span style="font-size:1.8rem;">📵</span>{{ t('checkout.qrImageFailed') }}
            </div>
            <div class="text-center small" style="line-height:1.8; color:var(--text-secondary);">
              {{ t('checkout.bank') }} <strong style="color:var(--text-heading);">Vietcombank (VCB)</strong><br />
              {{ t('checkout.accountNumber') }} <strong class="text-warning">9876543210</strong><br />
              {{ t('checkout.accountName') }} <strong style="color:var(--text-heading);">CÔNG TY SAO LAPTOP</strong><br />
              {{ t('checkout.amount') }} <strong class="text-warning">{{ formatPrice(checkoutTotal) }}</strong><br />
              {{ t('checkout.content') }} <strong style="color:var(--text-heading);">Thanh toan SAO LAPTOP</strong>
            </div>
          </div>
          </Transition>

          <!-- Thông tin chuyển khoản thủ công -->
          <Transition name="fade">
          <div v-if="selectedPayment === 'chuyen_khoan'" class="p-4 rounded-3 small" style="background:var(--bg-card-inset);border:1px solid var(--border-color-soft);line-height:2;">
            <div class="fw-bold mb-2" style="color:var(--text-heading);">{{ t('checkout.bankInfoHeading') }}</div>
            <div style="color:var(--text-secondary);">
              {{ t('checkout.bank') }} <strong style="color:var(--text-heading);">Vietcombank (VCB)</strong><br />
              {{ t('checkout.accountNumber') }} <strong class="text-warning">9876543210</strong><br />
              {{ t('checkout.accountNameFull') }} <strong style="color:var(--text-heading);">CÔNG TY SAO LAPTOP</strong><br />
              {{ t('checkout.amount') }} <strong class="text-warning">{{ formatPrice(checkoutTotal) }}</strong><br />
              {{ t('checkout.contentShort') }} <strong style="color:var(--text-heading);">Thanh toan SAO LAPTOP</strong>
            </div>
          </div>
          </Transition>

        </div><!-- /bước 2 -->

        <!-- Trạng thái tiến trình khi đang đặt hàng — aria-live để screen reader cũng đọc được -->
        <div v-if="checkoutLoading" class="px-4 pb-2 small text-center" style="color:var(--text-secondary);" role="status" aria-live="polite">
          {{ checkoutProgress }}
        </div>

        <!-- Footer: nút điều hướng bước -->
        <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-top:1px solid var(--border-color-soft);">
          <button v-if="checkoutStep === 1"
                  class="btn btn-sm btn-outline-secondary px-4" style="border-radius:10px;"
                  @click="$emit('update:modelValue', false)">{{ t('checkout.cancel') }}</button>
          <button v-else
                  class="btn btn-sm btn-outline-secondary px-4" style="border-radius:10px;"
                  :disabled="checkoutLoading"
                  @click="checkoutStep = 1">{{ t('checkout.back') }}</button>

          <button v-if="checkoutStep === 1"
                  class="btn btn-warning fw-bold px-5" style="border-radius:10px;"
                  @click="goToPayment">
            {{ t('checkout.continue') }}
          </button>
          <button v-else
                  class="btn btn-warning fw-bold px-5" style="border-radius:10px;"
                  :disabled="checkoutLoading"
                  @click="placeOrder">
            {{ checkoutLoading ? t('checkout.processing') : t('checkout.confirmOrder') }}
          </button>
        </div>

      </template>
    </div><!-- /modal box -->
  </div><!-- /checkout overlay -->
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue';
import { t } from '../../i18n/index.js';
import { AuthStore } from '../../stores/index.js';
import { nowLocalIso } from '../../utils/datetime.js';
import { formatPrice as formatPriceRaw } from '../../utils/formatPrice.js';
import AddressPicker from './AddressPicker.vue';
import * as KhachHangService from '../../Service/KhachHangService.js';
import * as KhuyenMaiService  from '../../Service/KhuyenMaiService.js';
import * as DonHangService    from '../../Service/DonHangService.js';
import * as PhieuGiamGiaCaNhanService from '../../Service/PhieuGiamGiaCaNhanService.js';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  cart:       { type: Array,   required: true }, // giỏ hàng: { bienTheId, tenSanPham, hinhAnhChinh, giaBan, quantity }
  cartTotal:  { type: Number,  default: 0 },      // tổng tiền hàng (chưa ship/giảm giá)
});

// update:modelValue = đóng modal; order-placed = đơn đã tạo xong, cha xoá giỏ hàng
const emit = defineEmits(['update:modelValue', 'order-placed']);

const checkoutStep    = ref(1);     // 1 = thông tin giao hàng, 2 = phương thức thanh toán
const checkoutSuccess = ref(false); // Đặt hàng thành công chưa
const checkoutLoading = ref(false); // Đang xử lý API
const checkoutProgress = ref('');   // Bước hiện tại trong placeOrder() — hiện cho người dùng biết đơn đang xử lý tới đâu
const checkoutError   = ref('');    // Thông báo lỗi khi checkout
const checkoutOrderId    = ref(null);  // ID đơn hàng sau khi đặt xong
const checkoutFinalTotal = ref(0);     // Tổng tiền lúc đặt hàng (lưu trước khi cha xóa giỏ)
const allPromos       = ref([]);    // Cache danh sách khuyến mãi
const foundCustomer   = ref(null);  // Khách hàng tìm thấy qua SĐT
const appliedPromo    = ref(null);  // Khuyến mãi đã áp dụng
const promoMsg        = ref('');    // Thông báo kết quả áp dụng mã
const myVouchers      = ref([]);    // voucher cá nhân còn dùng được của khách đang checkout
const appliedVoucher  = ref(null);  // voucher cá nhân đang chọn (loại trừ với appliedPromo)
const selectedPayment = ref('tien_mat'); // 'tien_mat' | 'qr' | 'chuyen_khoan'

// VietQR — tạo QR code thật từ API vietqr.io
const qrImageFailed = ref(false); // Ảnh QR không tải được (mạng chặn/API ngoài lỗi) — vẫn cho thanh toán bằng thông tin TK bên dưới
const qrImageUrl = computed(() => {
  const bank    = 'VCB';
  const account = '9876543210';
  const info    = encodeURIComponent('Thanh toan SAO LAPTOP');
  const name    = encodeURIComponent('SAO LAPTOP');
  return `https://img.vietqr.io/image/${bank}-${account}-compact2.png?amount=${checkoutTotal.value}&addInfo=${info}&accountName=${name}`;
});

// Form thông tin đặt hàng (reactive để Vue theo dõi thay đổi)
const checkoutForm = reactive({
  soDienThoai:          '', // SĐT tìm kiếm khách hàng
  hoTen:                '', // Họ tên khách hàng
  email:                '', // Email
  nguoiNhan:            '', // Tên người nhận hàng
  sdtNguoiNhan:         '', // SĐT người nhận
  diaChiGiaoHangText:   '', // Địa chỉ giao hàng
  maKhuyenMai:          '', // Mã khuyến mãi nhập vào
});

// Khách đã đăng nhập (tài khoản khach_hang) — bỏ ô tìm SĐT, đã biết chắc là ai rồi.
const isLoggedInCustomer = computed(() => AuthStore.user?.role === 'khach_hang' && !!AuthStore.user?.soDienThoai);

// Phí vận chuyển: miễn phí nếu đơn từ 300k
const phiVanChuyen = computed(() => props.cartTotal >= 300000 ? 0 : 30000);

// Số tiền 1 mã khuyến mãi cụ thể giảm được cho đơn hiện tại — dùng chung cho cả mã đang
// áp dụng (checkoutGiamGia) lẫn để xếp hạng "giảm nhiều nhất" trong eligiblePromos.
const calcDiscountFor = (p) => {
  if (!p) return 0;
  if (p.donHangToiThieu && props.cartTotal < Number(p.donHangToiThieu)) return 0;
  if (p.loai === 'percent') {
    // Giảm theo % nhưng không vượt quá giaTriToiDa
    let d = props.cartTotal * Number(p.giaTri) / 100;
    if (p.giaTriToiDa) d = Math.min(d, Number(p.giaTriToiDa));
    return d;
  }
  return Number(p.giaTri) || 0; // Giảm theo số tiền cố định
};
const checkoutGiamGia = computed(() => calcDiscountFor(appliedPromo.value) + calcDiscountFor(appliedVoucher.value));

// Mã khuyến mãi còn hiệu lực + đủ điều kiện áp dụng cho đơn hiện tại (đạt đơn tối thiểu,
// còn hạn dùng, chưa hết lượt) — sắp xếp giảm nhiều nhất lên đầu để khách chọn nhanh,
// khỏi phải gõ tay từng mã để dò xem cái nào lợi nhất.
const eligiblePromos = computed(() => {
  const now = new Date();
  return allPromos.value
    .filter(p => p.trangThai === 'active')
    .filter(p => !p.ngayBatDau || new Date(p.ngayBatDau) <= now)
    .filter(p => !p.ngayKetThuc || new Date(p.ngayKetThuc) > now)
    .filter(p => !p.soLuongToiDa || (p.soLanDaDung ?? 0) < p.soLuongToiDa)
    .map(p => ({ ...p, discount: calcDiscountFor(p) }))
    .filter(p => p.discount > 0)
    .sort((a, b) => b.discount - a.discount);
});

// Voucher cá nhân còn dùng được — chưa dùng, chưa hết hạn. Không lọc theo donHangToiThieu
// (voucher cá nhân không có trường này, khác khuyen_mai).
const eligibleVouchers = computed(() => {
  const now = new Date();
  return myVouchers.value
    .filter(v => !v.daSuDung && new Date(v.ngayHetHan) > now)
    .map(v => ({ ...v, discount: calcDiscountFor(v) }))
    .filter(v => v.discount > 0)
    .sort((a, b) => b.discount - a.discount);
});

// Chọn voucher cá nhân — loại trừ với mã khuyến mãi công khai (chỉ dùng 1 trong 2).
const selectVoucher = (v) => {
  if (appliedVoucher.value?.phieuId === v.phieuId) { appliedVoucher.value = null; return; }
  appliedVoucher.value = v;
  appliedPromo.value = null;
  checkoutForm.maKhuyenMai = '';
  promoMsg.value = '';
};

// Tổng tiền thanh toán cuối cùng
const checkoutTotal = computed(() =>
  Math.max(0, props.cartTotal + phiVanChuyen.value - checkoutGiamGia.value)
);

const formatPrice = (value) => (value == null ? t('productDetail.contact') : formatPriceRaw(value));

// Đóng bằng phím Escape — không đóng khi đang gọi API để tránh rời màn hình giữa chừng
const onKeydown = (e) => { if (e.key === 'Escape' && !checkoutLoading.value) emit('update:modelValue', false); };

// Mở modal thanh toán và load dữ liệu cần thiết — chạy khi cha set modelValue = true
// Khách đã đăng nhập: điền thẳng từ thông tin phiên đăng nhập (đáng tin cậy, luôn có
// sẵn ngay lúc login) — KHÔNG dùng KhachHangService.getAll() (chỉ nhân viên/admin được
// gọi, khách gọi sẽ bị 403) để dò theo SĐT như cách cũ. Tách riêng thành hàm để gọi lại
// được cả khi khách đăng nhập NGAY TRONG LÚC modal đang mở (trước đây chỉ điền 1 lần lúc
// mở modal — nếu khách mở giỏ hàng trước rồi mới đăng nhập mà không đóng/mở lại modal,
// các ô vẫn trống dù banner "đã đăng nhập" đã hiện đúng).
const fillFromLoggedInAccount = async () => {
  if (!(AuthStore.user?.role === 'khach_hang' && AuthStore.user?.soDienThoai)) return;
  checkoutForm.soDienThoai  = AuthStore.user.soDienThoai;
  checkoutForm.hoTen        = AuthStore.user.hoTen || '';
  checkoutForm.email        = AuthStore.user.email || '';
  checkoutForm.nguoiNhan    = AuthStore.user.hoTen || '';
  checkoutForm.sdtNguoiNhan = AuthStore.user.soDienThoai;
  foundCustomer.value = { khachHangId: AuthStore.user.id, hoTen: AuthStore.user.hoTen };
  // Địa chỉ không có trong phiên đăng nhập — xem hồ sơ của chính mình (endpoint tự-xem
  // cho phép khách xem đúng bản ghi của họ), không có thì để trống, khách tự nhập.
  const full = await KhachHangService.getById(AuthStore.user.id).catch(() => null);
  if (full?.diaChi) checkoutForm.diaChiGiaoHangText = full.diaChi;
};

watch(() => props.modelValue, async (open) => {
  if (open) window.addEventListener('keydown', onKeydown);
  else window.removeEventListener('keydown', onKeydown);
  if (!open) return;
  checkoutStep.value     = 1;
  checkoutSuccess.value  = false;
  checkoutError.value    = '';
  checkoutProgress.value = '';
  promoMsg.value        = '';
  appliedPromo.value    = null;
  foundCustomer.value   = null;
  selectedPayment.value = 'tien_mat';
  qrImageFailed.value   = false;
  Object.keys(checkoutForm).forEach(k => { checkoutForm[k] = ''; });
  if (!allPromos.value.length) {
    allPromos.value = await KhuyenMaiService.getAll().catch(() => []);
  }
  appliedVoucher.value = null;
  if (isLoggedInCustomer.value) {
    myVouchers.value = await PhieuGiamGiaCaNhanService.getCuaToi().catch(() => []);
  }
  await fillFromLoggedInAccount();
});

// Khách đăng nhập NGAY TRONG LÚC modal đang mở (vd bấm "Đăng nhập" từ 1 modal khác chồng
// lên mà không đóng modal thanh toán) — điền lại ngay, khỏi phải đóng/mở lại modal.
watch(() => AuthStore.user, () => {
  if (props.modelValue) fillFromLoggedInAccount();
});

// Tìm khách hàng theo số điện thoại — endpoint công khai riêng cho checkout (khách vãng
// lai chưa đăng nhập vẫn gọi được), khác getAll() (chỉ nhân viên/admin).
const lookupCustomer = async () => {
  const phone = checkoutForm.soDienThoai.trim();
  if (!phone) { foundCustomer.value = null; return; }
  const c = await KhachHangService.findByPhone(phone).catch(() => null);
  foundCustomer.value = c || null;
  if (c) {
    // Tự điền thông tin nếu tìm thấy khách hàng
    checkoutForm.hoTen              = c.hoTen;
    checkoutForm.email              = c.email || '';
    checkoutForm.nguoiNhan          = c.hoTen;
    checkoutForm.sdtNguoiNhan       = c.soDienThoai;
    checkoutForm.diaChiGiaoHangText = c.diaChi || '';
  }
};

// Kiểm tra và áp dụng mã khuyến mãi
const applyPromo = () => {
  const code = checkoutForm.maKhuyenMai.trim().toUpperCase();
  if (!code) { appliedPromo.value = null; promoMsg.value = ''; return; }
  const p = allPromos.value.find(
    (x) => x.maKhuyenMai?.toUpperCase() === code && x.trangThai === 'active'
  );
  if (p) {
    // Cùng quy tắc "chỉ 1 trong 2" như selectPromo() — backend cũng chặn dùng đồng thời
    // mã khuyến mãi công khai + voucher cá nhân, gõ tay mã mới không phải ngoại lệ.
    appliedVoucher.value = null;
    appliedPromo.value = p;
    promoMsg.value     = t('checkout.promoSuccess', { name: p.tenKhuyenMai });
  } else {
    appliedPromo.value = null;
    promoMsg.value     = t('checkout.promoInvalid');
  }
};

// Chọn thẳng 1 mã trong danh sách gợi ý — bấm lần nữa vào mã đang áp dụng để bỏ chọn.
const selectPromo = (p) => {
  appliedVoucher.value = null;
  if (appliedPromo.value?.khuyenMaiId === p.khuyenMaiId) {
    checkoutForm.maKhuyenMai = '';
    appliedPromo.value = null;
    promoMsg.value = '';
    return;
  }
  checkoutForm.maKhuyenMai = p.maKhuyenMai;
  applyPromo();
};

// Kiểm tra thông tin bước 1 trước khi sang bước thanh toán — chặn sớm thay vì để
// backend từ chối rồi hiện lỗi JSON thô cho khách.
const goToPayment = () => {
  checkoutError.value = '';
  if (!checkoutForm.soDienThoai.trim()) { checkoutError.value = t('checkout.errPhoneRequired'); return; }
  if (!foundCustomer.value && !checkoutForm.hoTen.trim()) { checkoutError.value = t('checkout.errNameRequired'); return; }
  if (!checkoutForm.nguoiNhan.trim()) { checkoutError.value = t('checkout.errReceiverRequired'); return; }
  if (!checkoutForm.sdtNguoiNhan.trim()) { checkoutError.value = t('checkout.errReceiverPhoneRequired'); return; }
  if (!checkoutForm.diaChiGiaoHangText.trim()) { checkoutError.value = t('checkout.errAddressRequired'); return; }
  checkoutStep.value = 2;
};

// Chuyển lỗi validate dạng JSON {"field":"message"} tu backend thanh 1 dong text
// de doc, thay vi hien nguyen JSON tho cho khach hang.
const parseApiError = async (res, fallbackPrefix) => {
  const raw = await res.text();
  try {
    const obj = JSON.parse(raw);
    const messages = Object.values(obj).filter((v) => typeof v === 'string');
    if (messages.length) return messages.join(' · ');
  } catch { /* khong phai JSON, dung raw text */ }
  return `${fallbackPrefix}: ${res.status} ${raw}`;
};

// Gửi đơn hàng lên API
const placeOrder = async () => {
  checkoutError.value    = '';
  checkoutLoading.value  = true;
  checkoutProgress.value = t('checkout.progressCustomer');
  try {
    let khachHangId = foundCustomer.value?.khachHangId;

    // Nếu không tìm thấy khách hàng → tạo mới
    if (!khachHangId) {
      const custBody = {
        hoTen:        checkoutForm.hoTen,
        soDienThoai:  checkoutForm.soDienThoai,
        email:        checkoutForm.email,
        diaChi:       checkoutForm.diaChiGiaoHangText || 'Chua cap nhat',
        loaiKhach:    'ca_nhan',
        diemTichLuy:  0,
        trangThai:    'active',
      };
      const r = await KhachHangService.createGuest(custBody);
      if (!r.ok) throw new Error(await parseApiError(r, t('checkout.createCustomerError')));
      const newC  = await r.json();
      khachHangId = newC.khachHangId;
    }

    // Tạo đơn hàng chính
    checkoutProgress.value = t('checkout.progressOrder');
    const orderBody = {
      khachHangId,
      nguoiNhan:          checkoutForm.nguoiNhan,
      sdtNguoiNhan:       checkoutForm.sdtNguoiNhan,
      diaChiGiaoHangText: checkoutForm.diaChiGiaoHangText,
      khuyenMaiId:        appliedPromo.value?.khuyenMaiId ?? null,
      phieuGiamGiaCaNhanId: appliedVoucher.value?.phieuId ?? null,
      tongTien:           props.cartTotal,
      giamGia:            checkoutGiamGia.value,
      phiVanChuyen:       phiVanChuyen.value,
      // thanhTien bỏ qua — computed column trong DB (tong_tien - giam_gia + phi_van_chuyen)
      ngayDat:            nowLocalIso(),
      trangThaiDonHang:   'pending',
      trangThaiThanhToan: 'unpaid',
      kenhBan:            'online',
    };
    const orderRes = await DonHangService.create(orderBody);
    if (!orderRes.ok)
      throw new Error(await parseApiError(orderRes, t('checkout.createOrderError')));
    const createdOrder = await orderRes.json();
    const donHangId    = createdOrder.id;

    // Thêm từng sản phẩm vào chi tiết đơn hàng — nếu 1 dòng lỗi giữa chừng thì huỷ
    // luôn đơn hàng vừa tạo, tránh để lại đơn "pending" thiếu sản phẩm.
    try {
      let itemIndex = 0;
      for (const item of props.cart) {
        itemIndex += 1;
        checkoutProgress.value = t('checkout.progressItems', { current: itemIndex, total: props.cart.length });
        const itemRes = await DonHangService.addChiTiet({
          donHangId,
          bienTheId:   item.bienTheId,
          soLuong:     item.quantity,
          donGia:      item.giaBan,
          giamGiaDong: 0,
        });
        if (!itemRes.ok)
          throw new Error(await parseApiError(itemRes, t('checkout.addItemError', { name: item.tenSanPham })));
      }
    } catch (e) {
      await DonHangService.remove(donHangId).catch(() => {});
      throw e;
    }

    // Lưu tổng tiền trước khi cha xóa giỏ (props.cartTotal sẽ về 0 sau khi cart rỗng)
    checkoutFinalTotal.value = checkoutTotal.value;
    checkoutOrderId.value    = donHangId;
    checkoutSuccess.value    = true;
    emit('order-placed');
  } catch (e) {
    checkoutError.value = e.message;
  } finally {
    checkoutLoading.value  = false;
    checkoutProgress.value = '';
  }
};
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.fade-enter-from, .fade-leave-to       { opacity: 0; transform: translateY(8px); }
</style>
