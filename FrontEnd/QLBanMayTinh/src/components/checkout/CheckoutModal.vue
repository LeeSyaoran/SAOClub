<template>
  <div
    v-if="modelValue"
    class="checkout-backdrop position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background:rgba(0,0,0,0.75); z-index:1050; backdrop-filter:blur(6px);"
    @click.self="onBackdropClick"
  >
    <div
      class="checkout-modal-container rounded-4 d-flex flex-column"
      role="dialog"
      aria-modal="true"
      style="background:var(--bg-card); border:1px solid var(--border-color); width:920px; max-width:96vw; max-height:92vh; box-shadow:0 24px 80px rgba(0,0,0,0.5); overflow:hidden;"
    >
      <!-- ══ MÀN HÌNH ĐẶT HÀNG THÀNH CÔNG ══ -->
      <template v-if="checkoutSuccess">
        <div class="d-flex flex-column align-items-center justify-content-center gap-4 p-5 text-center my-auto">
          <div
            class="d-flex align-items-center justify-content-center rounded-circle success-badge-glow"
            style="width:80px;height:80px;background:rgba(72,199,142,0.15);color:#48c78e;"
          >
            <CheckCircle2 :size="42" />
          </div>
          <div>
            <span class="badge bg-success-subtle text-success px-3 py-2 rounded-pill fw-semibold mb-2" style="font-size:0.8rem;">
              <ShieldCheck :size="14" class="me-1" style="vertical-align:-2px;" /> Đơn hàng đã được tiếp nhận
            </span>
            <h2 class="fw-black mb-1 mt-2" style="font-size:1.5rem; color:var(--text-heading);">{{ t('checkout.successTitle') }}</h2>
            <p class="mb-0" style="font-size:0.95rem; color:var(--text-secondary);">
              {{ t('checkout.orderCode') }} <strong class="text-warning font-monospace fs-5 ms-1">{{ checkoutOrderCode }}</strong>
            </p>
          </div>

          <div
            v-if="selectedPayment === 'tien_mat'"
            class="p-3 rounded-3 text-start small border"
            style="background:var(--bg-card-alt); border-color:var(--border-color-soft); max-width:440px;"
          >
            <div class="d-flex align-items-center gap-2 mb-1 text-success fw-bold">
              <Banknote :size="16" /> Thanh toán khi nhận hàng (COD)
            </div>
            <div style="color:var(--text-secondary); line-height:1.6;">
              {{ t('checkout.cashInstruction', { amount: formatPrice(checkoutFinalTotal) }) }}
            </div>
          </div>
          <div
            v-else-if="selectedPayment === 'qr'"
            class="p-3 rounded-3 text-start small border"
            style="background:var(--bg-card-alt); border-color:var(--border-color-soft); max-width:440px;"
          >
            <div class="d-flex align-items-center gap-2 mb-1 text-primary fw-bold">
              <Smartphone :size="16" /> Thanh toán VietQR
            </div>
            <div style="color:var(--text-secondary); line-height:1.6;">
              {{ t('checkout.qrInstruction') }}
            </div>
          </div>
          <div
            v-else
            class="p-3 rounded-3 text-start small border"
            style="background:var(--bg-card-alt); border-color:var(--border-color-soft); max-width:440px;"
          >
            <div class="d-flex align-items-center gap-2 mb-1 text-info fw-bold">
              <Landmark :size="16" /> Chuyển khoản ngân hàng
            </div>
            <div style="color:var(--text-secondary); line-height:1.6;">
              {{ t('checkout.bankInstruction', { amount: formatPrice(checkoutFinalTotal) }) }}
            </div>
          </div>

          <button
            class="btn btn-warning fw-bold px-5 py-2 rounded-pill shadow-sm"
            style="font-size:0.95rem;"
            @click="$emit('update:modelValue', false)"
          >
            {{ t('checkout.close') }}
          </button>
        </div>
      </template>

      <!-- ══ FORM ĐẶT HÀNG 2 CỘT ══ -->
      <template v-else>
        <!-- Modal Header & Stepper -->
        <div class="px-4 py-3 border-bottom d-flex align-items-center justify-content-between flex-wrap gap-2" style="border-color:var(--border-color-soft) !important; background:var(--bg-card);">
          <div class="d-flex align-items-center gap-3">
            <h5 class="fw-bold mb-0 text-truncate" style="font-size:1.1rem; color:var(--text-heading);">
              {{ checkoutStep === 1 ? t('checkout.infoTitle') : t('checkout.paymentTitle') }}
            </h5>
            <span class="badge rounded-pill px-2.5 py-1" style="background:var(--bg-card-alt); color:var(--text-secondary); font-size:11px; font-weight:600; border:1px solid var(--border-color-soft);">
              {{ cart.length }} món
            </span>
          </div>

          <!-- Stepper indicator bar -->
          <div class="checkout-stepper d-flex align-items-center gap-2">
            <!-- Bước 1 -->
            <button
              type="button"
              class="stepper-step btn btn-sm p-0 d-flex align-items-center gap-2 border-0 bg-transparent text-start"
              :class="{ 'step-active': checkoutStep === 1, 'step-done': checkoutStep > 1 }"
              :disabled="checkoutLoading"
              @click="checkoutStep = 1"
            >
              <div class="step-circle rounded-circle d-flex align-items-center justify-content-center fw-bold">
                <Check v-if="checkoutStep > 1" :size="12" />
                <span v-else>1</span>
              </div>
              <span class="step-label fw-semibold d-none d-sm-inline">{{ t('checkout.stepInfo') }}</span>
            </button>

            <!-- Line nối -->
            <div class="stepper-line" :class="{ 'line-active': checkoutStep >= 2 }"></div>

            <!-- Bước 2 -->
            <button
              type="button"
              class="stepper-step btn btn-sm p-0 d-flex align-items-center gap-2 border-0 bg-transparent text-start"
              :class="{ 'step-active': checkoutStep === 2 }"
              :disabled="checkoutStep === 1 && !canGoToPayment"
              @click="goToPayment"
            >
              <div class="step-circle rounded-circle d-flex align-items-center justify-content-center fw-bold">
                2
              </div>
              <span class="step-label fw-semibold d-none d-sm-inline">{{ t('checkout.stepPayment') }}</span>
            </button>
          </div>

          <!-- Nút đóng -->
          <button
            type="button"
            class="btn btn-sm btn-outline-secondary p-1 rounded-circle d-flex align-items-center justify-content-center"
            style="width:30px; height:30px;"
            :aria-label="t('common.close')"
            :disabled="checkoutLoading"
            @click="$emit('update:modelValue', false)"
          >
            <X :size="16" />
          </button>
        </div>

        <!-- Notification Toast when copying bank info -->
        <div v-if="copiedField" class="copy-alert text-center py-1.5 px-3 small fw-semibold bg-success text-white" style="font-size:12px;">
          ✓ {{ t('checkout.copied') }} ({{ copiedField }})
        </div>

        <!-- Modal Body: 2 cột trên Desktop (Col Left: Form / Col Right: Order summary) -->
        <div class="row g-0 flex-grow-1 overflow-hidden" style="min-height:0;">
          <!-- ── CỘT TRÁI: Nhập liệu thông tin / Phương thức thanh toán ── -->
          <div class="col-lg-7 d-flex flex-column overflow-y-auto px-4 py-3 custom-scrollbar" style="max-height:calc(92vh - 145px);">
            <!-- BƯỚC 1: Thông tin giao hàng -->
            <div v-if="checkoutStep === 1" class="d-flex flex-column gap-3">
              <!-- Thẻ trạng thái tài khoản -->
              <div class="card border rounded-3 p-3" style="background:var(--bg-card-alt); border-color:var(--border-color-soft) !important;">
                <div class="d-flex align-items-center justify-content-between mb-2">
                  <div class="d-flex align-items-center gap-2">
                    <div class="rounded-circle d-flex align-items-center justify-content-center text-warning" style="width:32px;height:32px;background:rgba(250,204,21,0.12);">
                      <User :size="16" />
                    </div>
                    <div>
                      <div class="small fw-bold" style="color:var(--text-heading);">
                        {{ isLoggedInCustomer ? accountDisplayName : t('checkout.customerHeading') }}
                      </div>
                      <div class="small text-truncate" style="color:var(--text-secondary); font-size:12px;">
                        <span v-if="accountDisplayContact">{{ accountDisplayContact }}</span>
                        <span v-else-if="!isLoggedInCustomer">Khách vãng lai</span>
                        <span v-else class="text-warning">Chưa cập nhật số điện thoại</span>
                      </div>
                    </div>
                  </div>
                  <span v-if="isLoggedInCustomer" class="badge bg-success-subtle text-success border border-success-subtle rounded-pill small" style="font-size:11px;">
                    Đã đăng nhập
                  </span>
                </div>

                <!-- Cảnh báo nếu tài khoản chưa có SĐT hợp lệ -->
                <div v-if="isLoggedInCustomer && !userHasValidPhone" class="alert alert-warning py-1.5 px-2.5 mb-0 mt-1 small rounded-2 d-flex align-items-center gap-2" style="font-size:11.5px;">
                  <AlertTriangle :size="14" class="text-warning flex-shrink-0" />
                  <span>Tài khoản của bạn chưa có số điện thoại. Vui lòng nhập số điện thoại nhận hàng bên dưới để shipper tiện liên hệ.</span>
                </div>

                <!-- Khách vãng lai: Khung tra cứu nhanh -->
                <template v-if="!isLoggedInCustomer">
                  <div class="d-flex gap-2 mt-2">
                    <div class="input-group input-group-sm">
                      <span class="input-group-text border-end-0" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary);">
                        <Phone :size="14" />
                      </span>
                      <input
                        v-model="checkoutForm.soDienThoai"
                        type="tel"
                        class="form-control border-start-0"
                        style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);"
                        :placeholder="t('checkout.phonePlaceholder')"
                        @keyup.enter="lookupCustomer"
                      />
                    </div>
                    <button type="button" class="btn btn-sm btn-outline-warning flex-shrink-0 px-3" style="border-radius:8px;" @click="lookupCustomer">
                      {{ t('checkout.find') }}
                    </button>
                  </div>
                  <div v-if="foundCustomer" class="small p-2 rounded-2 mt-2" style="background:rgba(72,199,142,0.1);color:#48c78e;">
                    <CheckCircle2 :size="13" style="vertical-align:-2px;" /> {{ t('checkout.foundCustomer') }} <strong>{{ foundCustomer.hoTen }}</strong>
                  </div>
                </template>
              </div>

              <!-- Thẻ thông tin nhận hàng -->
              <div class="card border rounded-3 p-3" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
                <div class="fw-bold mb-3 d-flex align-items-center gap-2" style="font-size:0.95rem; color:var(--text-heading);">
                  <MapPin :size="16" class="text-danger" />
                  <span>{{ t('checkout.shippingHeading') }}</span>
                </div>

                <div class="row g-3">
                  <!-- Họ tên người nhận -->
                  <div class="col-md-6">
                    <label class="form-label small fw-semibold mb-1" style="color:var(--text-secondary); font-size:12px;">
                      {{ t('checkout.receiverPlaceholder') }} <span class="text-danger">*</span>
                    </label>
                    <div class="input-group input-group-sm">
                      <span class="input-group-text border-end-0" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary);">
                        <User :size="14" />
                      </span>
                      <input
                        v-model="checkoutForm.nguoiNhan"
                        type="text"
                        class="form-control border-start-0"
                        style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:0 8px 8px 0;height:38px;"
                        :placeholder="t('checkout.fullNamePlaceholder')"
                        required
                      />
                    </div>
                  </div>

                  <!-- Số điện thoại nhận hàng -->
                  <div class="col-md-6">
                    <label class="form-label small fw-semibold mb-1" style="color:var(--text-secondary); font-size:12px;">
                      {{ t('checkout.receiverPhonePlaceholder') }} <span class="text-danger">*</span>
                    </label>
                    <div class="input-group input-group-sm">
                      <span class="input-group-text border-end-0" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary);">
                        <Phone :size="14" />
                      </span>
                      <input
                        v-model="checkoutForm.sdtNguoiNhan"
                        type="tel"
                        class="form-control border-start-0"
                        :class="{ 'is-invalid': phoneTouched && !isReceiverPhoneValid }"
                        style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:0 8px 8px 0;height:38px;"
                        placeholder="Ví dụ: 0987654321"
                        required
                        @blur="phoneTouched = true"
                      />
                    </div>
                    <div v-if="phoneTouched && !isReceiverPhoneValid" class="small text-danger mt-1" style="font-size:11px;">
                      Số điện thoại gồm 10 chữ số bắt đầu bằng 0
                    </div>
                  </div>

                  <!-- Email nhận thông báo -->
                  <div class="col-12">
                    <label class="form-label small fw-semibold mb-1" style="color:var(--text-secondary); font-size:12px;">
                      {{ t('checkout.emailPlaceholder') }} <span class="text-muted fw-normal">(để nhận email xác nhận & hóa đơn)</span>
                    </label>
                    <div class="input-group input-group-sm">
                      <span class="input-group-text border-end-0" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary);">
                        <Mail :size="14" />
                      </span>
                      <input
                        v-model="checkoutForm.email"
                        type="email"
                        class="form-control border-start-0"
                        style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:0 8px 8px 0;height:38px;"
                        placeholder="tenban@gmail.com"
                      />
                    </div>
                  </div>

                  <!-- Địa chỉ nhận hàng -->
                  <div class="col-12">
                    <label class="form-label small fw-semibold mb-1" style="color:var(--text-secondary); font-size:12px;">
                      {{ t('checkout.addressPlaceholder') }} <span class="text-danger">*</span>
                    </label>
                    <AddressPicker v-model="checkoutForm.diaChiGiaoHangText" :placeholder="t('checkout.addressPlaceholder')" />
                  </div>

                  <!-- Ghi chú giao hàng -->
                  <div class="col-12">
                    <label class="form-label small fw-semibold mb-1" style="color:var(--text-secondary); font-size:12px;">
                      {{ t('checkout.orderNotes') }}
                    </label>
                    <div class="input-group input-group-sm">
                      <span class="input-group-text border-end-0" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary);">
                        <FileText :size="14" />
                      </span>
                      <input
                        v-model="checkoutForm.ghiChu"
                        type="text"
                        class="form-control border-start-0"
                        style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:0 8px 8px 0;height:38px;"
                        :placeholder="t('checkout.orderNotesPlaceholder')"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div><!-- /Bước 1 -->

            <!-- BƯỚC 2: Phương thức thanh toán -->
            <div v-else class="d-flex flex-column gap-3">
              <div class="card border rounded-3 p-3" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
                <div class="fw-bold mb-3 d-flex align-items-center gap-2" style="font-size:0.95rem; color:var(--text-heading);">
                  <Banknote :size="16" class="text-warning" />
                  <span>{{ t('checkout.choosePayment') }}</span>
                </div>

                <div class="d-flex flex-column gap-2.5">
                  <!-- COD: Tiền mặt -->
                  <label
                    class="payment-option-card d-flex align-items-center gap-3 p-3 rounded-3 cursor-pointer"
                    :class="{ 'payment-selected': selectedPayment === 'tien_mat' }"
                    @click="selectedPayment = 'tien_mat'"
                  >
                    <div class="payment-icon-box rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="background:rgba(250,204,21,0.12); color:#facc15;">
                      <Banknote :size="20" />
                    </div>
                    <div class="flex-grow-1">
                      <div class="d-flex align-items-center gap-2">
                        <span class="fw-bold" style="font-size:0.92rem; color:var(--text-heading);">{{ t('checkout.cashTitle') }}</span>
                        <span class="badge rounded-pill bg-success-subtle text-success small" style="font-size:10px;">Đồng kiểm</span>
                      </div>
                      <div class="small" style="font-size:12px; color:var(--text-secondary);">{{ t('checkout.cashDesc') }}</div>
                    </div>
                    <div class="payment-radio-circle rounded-circle border d-flex align-items-center justify-content-center flex-shrink-0">
                      <div v-if="selectedPayment === 'tien_mat'" class="payment-radio-dot"></div>
                    </div>
                  </label>

                  <!-- VietQR: Quét mã QR -->
                  <label
                    class="payment-option-card d-flex align-items-center gap-3 p-3 rounded-3 cursor-pointer"
                    :class="{ 'payment-selected': selectedPayment === 'qr' }"
                    @click="selectedPayment = 'qr'"
                  >
                    <div class="payment-icon-box rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="background:rgba(96,165,250,0.12); color:#60a5fa;">
                      <Smartphone :size="20" />
                    </div>
                    <div class="flex-grow-1">
                      <div class="d-flex align-items-center gap-2">
                        <span class="fw-bold" style="font-size:0.92rem; color:var(--text-heading);">{{ t('checkout.qrTitle') }}</span>
                        <span class="badge rounded-pill bg-danger-subtle text-danger small" style="font-size:10px;">{{ t('checkout.recommended') }}</span>
                      </div>
                      <div class="small" style="font-size:12px; color:var(--text-secondary);">{{ t('checkout.qrDesc') }}</div>
                    </div>
                    <div class="payment-radio-circle rounded-circle border d-flex align-items-center justify-content-center flex-shrink-0">
                      <div v-if="selectedPayment === 'qr'" class="payment-radio-dot"></div>
                    </div>
                  </label>

                  <!-- Chuyển khoản ngân hàng thủ công -->
                  <label
                    class="payment-option-card d-flex align-items-center gap-3 p-3 rounded-3 cursor-pointer"
                    :class="{ 'payment-selected': selectedPayment === 'chuyen_khoan' }"
                    @click="selectedPayment = 'chuyen_khoan'"
                  >
                    <div class="payment-icon-box rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="background:rgba(52,211,153,0.12); color:#34d399;">
                      <Landmark :size="20" />
                    </div>
                    <div class="flex-grow-1">
                      <div class="fw-bold" style="font-size:0.92rem; color:var(--text-heading);">{{ t('checkout.bankTitle') }}</div>
                      <div class="small" style="font-size:12px; color:var(--text-secondary);">{{ t('checkout.bankDesc') }}</div>
                    </div>
                    <div class="payment-radio-circle rounded-circle border d-flex align-items-center justify-content-center flex-shrink-0">
                      <div v-if="selectedPayment === 'chuyen_khoan'" class="payment-radio-dot"></div>
                    </div>
                  </label>
                </div>
              </div>

              <!-- Chi tiết thông tin QR -->
              <Transition name="fade">
                <div v-if="selectedPayment === 'qr'" class="card border rounded-3 p-3.5" style="background:var(--bg-card-alt); border-color:var(--border-color-soft) !important;">
                  <div class="d-flex flex-column align-items-center gap-3 text-center">
                    <span class="badge bg-primary-subtle text-primary px-3 py-1 rounded-pill small fw-semibold">
                      {{ t('checkout.scanQr') }}
                    </span>

                    <div class="qr-frame p-2 bg-white rounded-3 shadow-sm position-relative">
                      <img
                        v-if="!qrImageFailed"
                        :src="qrImageUrl"
                        alt="VietQR"
                        style="width:200px; height:200px; object-fit:contain; border-radius:8px;"
                        @error="qrImageFailed = true"
                      />
                      <div
                        v-else
                        class="d-flex flex-column align-items-center justify-content-center text-center small text-muted"
                        style="width:200px; height:200px;"
                      >
                        <ImageOff :size="32" class="mb-2 text-danger" />
                        {{ t('checkout.qrImageFailed') }}
                      </div>
                    </div>

                    <!-- Bảng thông tin thanh toán có nút sao chép -->
                    <div class="w-100 rounded-3 p-2.5 small text-start border" style="background:var(--bg-card); border-color:var(--border-color-soft) !important; font-size:12.5px;">
                      <div class="d-flex align-items-center justify-content-between py-1 border-bottom" style="border-color:var(--border-color-soft) !important;">
                        <span style="color:var(--text-secondary);">{{ t('checkout.bank') }}</span>
                        <strong style="color:var(--text-heading);">Vietcombank (VCB)</strong>
                      </div>
                      <div class="d-flex align-items-center justify-content-between py-1 border-bottom" style="border-color:var(--border-color-soft) !important;">
                        <span style="color:var(--text-secondary);">{{ t('checkout.accountNumber') }}</span>
                        <div class="d-flex align-items-center gap-1.5">
                          <strong class="text-warning font-monospace">9876543210</strong>
                          <button
                            type="button"
                            class="btn btn-sm btn-link p-0 text-muted hover-text-primary"
                            title="Sao chép số tài khoản"
                            @click="copyToClipboard('9876543210', 'Số tài khoản')"
                          >
                            <Copy :size="13" />
                          </button>
                        </div>
                      </div>
                      <div class="d-flex align-items-center justify-content-between py-1 border-bottom" style="border-color:var(--border-color-soft) !important;">
                        <span style="color:var(--text-secondary);">{{ t('checkout.accountName') }}</span>
                        <strong style="color:var(--text-heading);">CÔNG TY SAO LAPTOP</strong>
                      </div>
                      <div class="d-flex align-items-center justify-content-between py-1 border-bottom" style="border-color:var(--border-color-soft) !important;">
                        <span style="color:var(--text-secondary);">{{ t('checkout.amount') }}</span>
                        <div class="d-flex align-items-center gap-1.5">
                          <strong class="text-warning font-monospace">{{ formatPrice(checkoutTotal) }}</strong>
                          <button
                            type="button"
                            class="btn btn-sm btn-link p-0 text-muted hover-text-primary"
                            title="Sao chép số tiền"
                            @click="copyToClipboard(checkoutTotal.toString(), 'Số tiền')"
                          >
                            <Copy :size="13" />
                          </button>
                        </div>
                      </div>
                      <div class="d-flex align-items-center justify-content-between py-1">
                        <span style="color:var(--text-secondary);">{{ t('checkout.content') }}</span>
                        <div class="d-flex align-items-center gap-1.5">
                          <strong class="text-warning font-monospace">Thanh toan SAO LAPTOP</strong>
                          <button
                            type="button"
                            class="btn btn-sm btn-link p-0 text-muted hover-text-primary"
                            title="Sao chép nội dung chuyển khoản"
                            @click="copyToClipboard('Thanh toan SAO LAPTOP', 'Nội dung chuyển khoản')"
                          >
                            <Copy :size="13" />
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </Transition>

              <!-- Chi tiết chuyển khoản thủ công -->
              <Transition name="fade">
                <div v-if="selectedPayment === 'chuyen_khoan'" class="card border rounded-3 p-3.5" style="background:var(--bg-card-alt); border-color:var(--border-color-soft) !important;">
                  <div class="fw-bold mb-2 small text-uppercase" style="color:var(--text-heading); letter-spacing:0.04em;">
                    {{ t('checkout.bankInfoHeading') }}
                  </div>
                  <div class="w-100 rounded-3 p-3 small text-start border d-flex flex-column gap-2" style="background:var(--bg-card); border-color:var(--border-color-soft) !important; font-size:12.5px;">
                    <div class="d-flex align-items-center justify-content-between">
                      <span style="color:var(--text-secondary);">{{ t('checkout.bank') }}</span>
                      <strong style="color:var(--text-heading);">Vietcombank (VCB)</strong>
                    </div>
                    <div class="d-flex align-items-center justify-content-between">
                      <span style="color:var(--text-secondary);">{{ t('checkout.accountNumber') }}</span>
                      <div class="d-flex align-items-center gap-1.5">
                        <strong class="text-warning font-monospace fs-6">9876543210</strong>
                        <button
                          type="button"
                          class="btn btn-sm btn-outline-warning py-0.5 px-2 rounded-2"
                          style="font-size:11px;"
                          @click="copyToClipboard('9876543210', 'Số tài khoản')"
                        >
                          <Copy :size="12" class="me-1" /> {{ t('checkout.copy') }}
                        </button>
                      </div>
                    </div>
                    <div class="d-flex align-items-center justify-content-between">
                      <span style="color:var(--text-secondary);">{{ t('checkout.accountNameFull') }}</span>
                      <strong style="color:var(--text-heading);">CÔNG TY SAO LAPTOP</strong>
                    </div>
                    <div class="d-flex align-items-center justify-content-between">
                      <span style="color:var(--text-secondary);">{{ t('checkout.amount') }}</span>
                      <strong class="text-warning font-monospace fs-6">{{ formatPrice(checkoutTotal) }}</strong>
                    </div>
                    <div class="d-flex align-items-center justify-content-between">
                      <span style="color:var(--text-secondary);">{{ t('checkout.contentShort') }}</span>
                      <div class="d-flex align-items-center gap-1.5">
                        <strong class="text-warning font-monospace">Thanh toan SAO LAPTOP</strong>
                        <button
                          type="button"
                          class="btn btn-sm btn-outline-warning py-0.5 px-2 rounded-2"
                          style="font-size:11px;"
                          @click="copyToClipboard('Thanh toan SAO LAPTOP', 'Nội dung chuyển khoản')"
                        >
                          <Copy :size="12" class="me-1" /> {{ t('checkout.copy') }}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </Transition>
            </div><!-- /Bước 2 -->
          </div>

          <!-- ── CỘT PHẢI: Tóm tắt đơn hàng & Khuyến mãi & Chi tiết thanh toán ── -->
          <div class="col-lg-5 border-start d-flex flex-column overflow-y-auto px-4 py-3 custom-scrollbar" style="background:var(--bg-card-alt); border-color:var(--border-color-soft) !important; max-height:calc(92vh - 145px);">
            <!-- Danh sách sản phẩm -->
            <div class="mb-3">
              <div class="d-flex align-items-center justify-content-between mb-2">
                <span class="fw-bold small text-uppercase" style="letter-spacing:0.04em; color:var(--text-secondary); font-size:11px;">
                  {{ t('checkout.orderSummary', { count: cart.length }) }}
                </span>
                <span class="small text-muted" style="font-size:11px;">{{ cart.reduce((sum, i) => sum + i.quantity, 0) }} chiếc</span>
              </div>

              <div class="order-items-list d-flex flex-column gap-2" style="max-height:180px; overflow-y:auto;">
                <div
                  v-for="item in cart" :key="item.bienTheId"
                  class="order-item d-flex align-items-center gap-2.5 p-2 rounded-3 border"
                  style="background:var(--bg-card); border-color:var(--border-color-soft) !important;"
                >
                  <div class="order-item-img flex-shrink-0 rounded-2 overflow-hidden border d-flex align-items-center justify-content-center" style="width:42px;height:42px;background:var(--bg-card-inset); border-color:var(--border-color-soft) !important;">
                    <img v-if="item.hinhAnhChinh" :src="item.hinhAnhChinh" :alt="item.tenSanPham" style="width:100%;height:100%;object-fit:contain;" />
                    <Laptop v-else :size="18" class="text-muted" />
                  </div>
                  <div class="flex-grow-1 min-w-0">
                    <div class="small fw-semibold text-truncate" style="color:var(--text-heading); font-size:12.5px;">{{ item.tenSanPham }}</div>
                    <div class="d-flex align-items-center gap-2 mt-0.5">
                      <span class="badge rounded-pill bg-secondary-subtle text-secondary px-1.5 py-0.5" style="font-size:10px;">×{{ item.quantity }}</span>
                      <span class="small text-warning fw-bold font-monospace" style="font-size:12px;">{{ formatPrice(item.giaBan * item.quantity) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <hr class="my-2" style="border-color:var(--border-color-soft);" />

            <!-- Mã khuyến mãi & Voucher cá nhân -->
            <div class="mb-3">
              <div class="fw-bold small text-uppercase mb-2" style="letter-spacing:0.04em; color:var(--text-secondary); font-size:11px;">
                {{ t('checkout.promoHeading') }}
              </div>

              <!-- Ô nhập mã khuyến mãi -->
              <div class="d-flex gap-2 mb-2">
                <div class="input-group input-group-sm">
                  <span class="input-group-text border-end-0" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary);">
                    <Tag :size="14" />
                  </span>
                  <input
                    v-model="checkoutForm.maKhuyenMai"
                    type="text"
                    class="form-control border-start-0"
                    style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);text-transform:uppercase;height:36px;"
                    :placeholder="t('checkout.promoPlaceholder')"
                    @keyup.enter="applyPromo"
                  />
                </div>
                <button
                  type="button"
                  class="btn btn-sm btn-outline-warning flex-shrink-0 px-3 fw-semibold"
                  style="border-radius:8px;"
                  @click="applyPromo"
                >
                  {{ t('checkout.apply') }}
                </button>
              </div>

              <!-- Trạng thái áp dụng mã -->
              <div v-if="promoMsg" class="small px-1 mb-2 d-flex align-items-center gap-1.5" :class="appliedPromo ? 'text-success' : 'text-danger'" style="font-size:11.5px;">
                <CheckCircle2 v-if="appliedPromo" :size="13" />
                <AlertCircle v-else :size="13" />
                <span>{{ promoMsg }}</span>
              </div>

              <!-- Thẻ mã đang áp dụng -->
              <div v-if="appliedPromo" class="p-2 rounded-3 d-flex align-items-center justify-content-between border mb-2" style="background:rgba(72,199,142,0.1); border-color:rgba(72,199,142,0.3) !important;">
                <div class="d-flex align-items-center gap-2">
                  <Tag :size="14" class="text-success" />
                  <div>
                    <span class="fw-bold small text-success">{{ appliedPromo.maKhuyenMai }}</span>
                    <span class="small text-muted ms-1">(-{{ formatPrice(calcDiscountFor(appliedPromo)) }})</span>
                  </div>
                </div>
                <button type="button" class="btn btn-sm btn-link text-danger p-0 small" style="text-decoration:none; font-size:11px;" @click="removePromo">
                  {{ t('checkout.removeVoucher') }}
                </button>
              </div>

              <!-- Danh sách Voucher cá nhân đổi từ điểm -->
              <div v-if="isLoggedInCustomer && eligibleVouchers.length" class="mt-2">
                <div class="d-flex align-items-center justify-content-between mb-1">
                  <span class="small fw-semibold" style="color:var(--text-secondary); font-size:11.5px;">
                    {{ t('checkout.voucherHeading') }} ({{ eligibleVouchers.length }})
                  </span>
                </div>
                <div class="d-flex flex-column gap-1.5" style="max-height:110px; overflow-y:auto;">
                  <div
                    v-for="v in eligibleVouchers" :key="v.phieuId"
                    class="voucher-card p-2 rounded-2 d-flex align-items-center justify-content-between border cursor-pointer"
                    :class="{ 'voucher-selected': appliedVoucher?.phieuId === v.phieuId }"
                    @click="selectVoucher(v)"
                  >
                    <div class="d-flex align-items-center gap-2">
                      <Percent :size="13" class="text-warning" />
                      <span class="fw-bold small" style="color:var(--text-heading); font-size:12px;">{{ v.maPhieu }}</span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                      <span class="text-warning fw-bold small font-monospace" style="font-size:12px;">−{{ formatPrice(v.discount) }}</span>
                      <CheckCircle2 v-if="appliedVoucher?.phieuId === v.phieuId" :size="14" class="text-success" />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <hr class="my-2" style="border-color:var(--border-color-soft);" />

            <!-- Bảng phân tích chi phí -->
            <div class="mt-auto d-flex flex-column gap-2 pt-2">
              <div class="d-flex justify-content-between small" style="color:var(--text-secondary); font-size:12.5px;">
                <span>{{ t('checkout.subtotal') }}</span>
                <span class="font-monospace fw-semibold" style="color:var(--text-heading);">{{ formatPrice(cartTotal) }}</span>
              </div>

              <div class="d-flex justify-content-between small align-items-center" style="color:var(--text-secondary); font-size:12.5px;">
                <span>{{ t('checkout.shippingFee') }}</span>
                <span v-if="phiVanChuyen === 0" class="badge bg-success-subtle text-success rounded-pill px-2 py-0.5" style="font-size:11px;">
                  {{ t('checkout.free') }}
                </span>
                <span v-else class="font-monospace fw-semibold" style="color:var(--text-heading);">{{ formatPrice(phiVanChuyen) }}</span>
              </div>

              <div v-if="checkoutGiamGia > 0" class="d-flex justify-content-between small text-success align-items-center" style="font-size:12.5px;">
                <span class="d-flex align-items-center gap-1"><Tag :size="12" /> {{ t('checkout.discount') }}</span>
                <span class="font-monospace fw-bold">− {{ formatPrice(checkoutGiamGia) }}</span>
              </div>

              <div class="d-flex justify-content-between align-items-end pt-2 mt-1 border-top" style="border-color:var(--border-color-soft) !important;">
                <div>
                  <div class="fw-bold" style="font-size:0.95rem; color:var(--text-heading);">{{ t('checkout.total') }}</div>
                  <div class="text-muted small" style="font-size:11px;">{{ t('checkout.vatIncluded') }}</div>
                </div>
                <div class="text-end">
                  <div class="text-warning fw-black font-monospace fs-4" style="line-height:1.1;">{{ formatPrice(checkoutTotal) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ══ FOOTER CHUẨN HÓA: Banner thông báo lỗi + Nút hành động ══ -->
        <div class="px-4 py-3 border-top" style="border-color:var(--border-color-soft) !important; background:var(--bg-card);">
          <!-- Dedicated Alert Message Box -->
          <div v-if="checkoutError" class="alert alert-danger py-2 px-3 small rounded-3 mb-3 d-flex align-items-center gap-2" role="alert">
            <AlertCircle :size="16" class="text-danger flex-shrink-0" />
            <span class="flex-grow-1">{{ checkoutError }}</span>
          </div>

          <div v-else-if="validationWarning" class="alert alert-warning py-2 px-3 small rounded-3 mb-3 d-flex align-items-center gap-2" role="alert">
            <AlertTriangle :size="16" class="text-warning flex-shrink-0" />
            <span class="flex-grow-1">{{ validationWarning }}</span>
          </div>

          <!-- Loading Progress Text -->
          <div v-if="checkoutLoading" class="small text-center mb-2 text-warning d-flex align-items-center justify-content-center gap-2">
            <div class="spinner-border spinner-border-sm text-warning" role="status"></div>
            <span>{{ checkoutProgress }}</span>
          </div>

          <!-- Action Buttons Bar -->
          <div class="d-flex justify-content-between align-items-center">
            <!-- Nút Quay lại / Hủy -->
            <button
              v-if="checkoutStep === 1"
              type="button"
              class="btn btn-outline-secondary px-3 py-2 rounded-3 small fw-semibold"
              style="font-size:0.88rem;"
              :disabled="checkoutLoading"
              @click="$emit('update:modelValue', false)"
            >
              {{ t('checkout.cancel') }}
            </button>
            <button
              v-else
              type="button"
              class="btn btn-outline-secondary px-3 py-2 rounded-3 small fw-semibold d-flex align-items-center gap-1.5"
              style="font-size:0.88rem;"
              :disabled="checkoutLoading"
              @click="checkoutStep = 1"
            >
              <ArrowLeft :size="14" /> {{ t('checkout.back') }}
            </button>

            <!-- Nút Tiếp tục / Xác nhận đặt hàng -->
            <button
              v-if="checkoutStep === 1"
              type="button"
              class="btn btn-warning fw-bold px-4 py-2 rounded-3 d-flex align-items-center gap-2 shadow-sm"
              style="font-size:0.92rem;"
              @click="goToPayment"
            >
              <span>{{ t('checkout.continue') }}</span>
              <ArrowRight :size="15" />
            </button>
            <button
              v-else
              type="button"
              class="btn btn-warning fw-bold px-4 py-2 rounded-3 d-flex align-items-center gap-2 shadow-sm"
              style="font-size:0.92rem;"
              :disabled="checkoutLoading"
              @click="placeOrder"
            >
              <span v-if="checkoutLoading" class="spinner-border spinner-border-sm"></span>
              <CheckCircle2 v-else :size="16" />
              <span>{{ checkoutLoading ? t('checkout.processing') : t('checkout.confirmOrder') }}</span>
            </button>
          </div>
        </div>
      </template>
    </div><!-- /checkout-modal-container -->
  </div><!-- /checkout-backdrop -->
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue';
import { t } from '../../i18n/index.js';
import { AuthStore } from '../../stores/index.js';
import { nowLocalIso } from '../../utils/datetime.js';
import { formatPrice as formatPriceRaw } from '../../utils/formatPrice.js';
import { checkoutInfoSchema, isValidPhoneNumber } from '../../utils/validators.js';
import {
  CheckCircle2, Laptop, Banknote, Smartphone, Landmark, ImageOff,
  ArrowLeft, ArrowRight, User, Phone, Mail, MapPin, FileText, Tag,
  Copy, Check, AlertCircle, AlertTriangle, X, ShieldCheck, Percent,
} from '@lucide/vue';
import AddressPicker from './AddressPicker.vue';
import * as KhachHangService from '../../services/KhachHangService.js';
import * as KhuyenMaiService from '../../services/KhuyenMaiService.js';
import * as DonHangService from '../../services/DonHangService.js';
import * as PhieuGiamGiaCaNhanService from '../../services/PhieuGiamGiaCaNhanService.js';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  cart:       { type: Array,   required: true },
  cartTotal:  { type: Number,  default: 0 },
});

const emit = defineEmits(['update:modelValue', 'order-placed']);

const checkoutStep       = ref(1);     // 1 = thông tin giao hàng, 2 = phương thức thanh toán
const checkoutSuccess    = ref(false); // Đặt hàng thành công chưa
const checkoutLoading    = ref(false); // Đang xử lý API
const checkoutProgress   = ref('');   // Tiến trình xử lý
const checkoutError      = ref('');    // Thông báo lỗi
const validationWarning  = ref('');    // Cảnh báo thiếu thông tin
const checkoutOrderId    = ref(null);  // ID đơn hàng
const checkoutOrderCode  = ref('');    // Mã đơn hàng hiển thị
const checkoutFinalTotal = ref(0);     // Tổng tiền lưu lại lúc đặt hàng
const allPromos          = ref([]);    // Cache danh sách khuyến mãi
const foundCustomer      = ref(null);  // Khách hàng tìm thấy qua SĐT
const appliedPromo       = ref(null);  // Khuyến mãi đã áp dụng
const promoMsg           = ref('');    // Kết quả áp dụng mã
const myVouchers         = ref([]);    // Voucher cá nhân của khách
const appliedVoucher     = ref(null);  // Voucher cá nhân đang chọn
const selectedPayment    = ref('tien_mat'); // 'tien_mat' | 'qr' | 'chuyen_khoan'
const phoneTouched       = ref(false); // Đã chạm vào ô SĐT nhận hàng chưa
const copiedField        = ref('');    // Tên trường vừa sao chép
let copyTimer            = null;

// VietQR API
const qrImageFailed = ref(false);
const qrImageUrl = computed(() => {
  const bank    = 'VCB';
  const account = '9876543210';
  const info    = encodeURIComponent('Thanh toan SAO LAPTOP');
  const name    = encodeURIComponent('SAO LAPTOP');
  return `https://img.vietqr.io/image/${bank}-${account}-compact2.png?amount=${checkoutTotal.value}&addInfo=${info}&accountName=${name}`;
});

// Form thông tin đặt hàng
const checkoutForm = reactive({
  soDienThoai:        '', // SĐT tìm kiếm khách hàng
  hoTen:              '', // Họ tên khách hàng
  email:              '', // Email
  nguoiNhan:          '', // Tên người nhận hàng
  sdtNguoiNhan:       '', // SĐT người nhận
  diaChiGiaoHangText: '', // Địa chỉ giao hàng
  ghiChu:             '', // Ghi chú đơn hàng
  maKhuyenMai:        '', // Mã khuyến mãi nhập vào
});

// Khách hàng đã đăng nhập
const isLoggedInCustomer = computed(() => AuthStore.user?.role === 'khach_hang');

// Kiểm tra user có SĐT hợp lệ trong profile không (loại bỏ chuỗi google_<uid>)
const userHasValidPhone = computed(() => isValidPhoneNumber(AuthStore.user?.soDienThoai));

// Tên hiển thị tài khoản
const accountDisplayName = computed(() => AuthStore.user?.hoTen || AuthStore.user?.username || 'Khách hàng');

// Thông tin liên hệ hiển thị cạnh tên tài khoản (Ưu tiên SĐT hợp lệ, sau đó email)
const accountDisplayContact = computed(() => {
  if (userHasValidPhone.value) return AuthStore.user.soDienThoai;
  if (AuthStore.user?.email) return AuthStore.user.email;
  return '';
});

// Kiểm tra SĐT người nhận hợp lệ theo định dạng
const isReceiverPhoneValid = computed(() => {
  if (!checkoutForm.sdtNguoiNhan) return false;
  return isValidPhoneNumber(checkoutForm.sdtNguoiNhan);
});

// Kiểm tra có thể sang bước thanh toán không
const canGoToPayment = computed(() => {
  return !!checkoutForm.nguoiNhan?.trim() &&
    isReceiverPhoneValid.value &&
    !!checkoutForm.diaChiGiaoHangText?.trim();
});

// Phí vận chuyển
const phiVanChuyenRef = ref(0);
const phiVanChuyen = computed(() => phiVanChuyenRef.value);

const fetchShippingFee = async () => {
  if (!checkoutForm.diaChiGiaoHangText) {
    phiVanChuyenRef.value = props.cartTotal >= 300000 ? 0 : 30000;
    return;
  }
  try {
    const res = await DonHangService.tinhPhiVanChuyen({
      diaChi: checkoutForm.diaChiGiaoHangText,
      items: props.cart.map(i => ({ giaBan: i.giaBan, soLuong: i.quantity })),
    });
    if (res.ok) {
      const data = await res.json();
      phiVanChuyenRef.value = data.phiVanChuyen ?? 0;
    } else {
      phiVanChuyenRef.value = props.cartTotal >= 300000 ? 0 : 30000;
    }
  } catch {
    phiVanChuyenRef.value = props.cartTotal >= 300000 ? 0 : 30000;
  }
};

// Tính số tiền giảm giá
const calcDiscountFor = (p) => {
  if (!p) return 0;
  if (p.donHangToiThieu && props.cartTotal < Number(p.donHangToiThieu)) return 0;
  if (p.loai === 'percent') {
    let d = props.cartTotal * Number(p.giaTri) / 100;
    if (p.giaTriToiDa) d = Math.min(d, Number(p.giaTriToiDa));
    return d;
  }
  return Number(p.giaTri) || 0;
};

const checkoutGiamGia = computed(() => calcDiscountFor(appliedPromo.value) + calcDiscountFor(appliedVoucher.value));

// Voucher cá nhân
const eligibleVouchers = computed(() => {
  const now = new Date();
  return myVouchers.value
    .filter(v => !v.daSuDung && new Date(v.ngayHetHan) > now)
    .map(v => ({ ...v, discount: calcDiscountFor(v) }))
    .filter(v => v.discount > 0)
    .sort((a, b) => b.discount - a.discount);
});

const selectVoucher = (v) => {
  if (appliedVoucher.value?.phieuId === v.phieuId) {
    appliedVoucher.value = null;
    return;
  }
  appliedVoucher.value = v;
  appliedPromo.value = null;
  checkoutForm.maKhuyenMai = '';
  promoMsg.value = '';
};

const removePromo = () => {
  appliedPromo.value = null;
  checkoutForm.maKhuyenMai = '';
  promoMsg.value = '';
};

// Tổng tiền thanh toán
const checkoutTotal = computed(() =>
  Math.max(0, props.cartTotal + phiVanChuyen.value - checkoutGiamGia.value)
);

const formatPrice = (value) => (value == null ? t('productDetail.contact') : formatPriceRaw(value));

// Sao chép thông tin ngân hàng 1-click
const copyToClipboard = async (text, label) => {
  try {
    await navigator.clipboard.writeText(text);
    copiedField.value = label;
    clearTimeout(copyTimer);
    copyTimer = setTimeout(() => { copiedField.value = ''; }, 2200);
  } catch {
    const el = document.createElement('textarea');
    el.value = text;
    document.body.appendChild(el);
    el.select();
    document.execCommand('copy');
    document.body.removeChild(el);
    copiedField.value = label;
    clearTimeout(copyTimer);
    copyTimer = setTimeout(() => { copiedField.value = ''; }, 2200);
  }
};

const onKeydown = (e) => {
  if (e.key === 'Escape' && !checkoutLoading.value) emit('update:modelValue', false);
};

const onBackdropClick = () => {
  if (!checkoutLoading.value) emit('update:modelValue', false);
};

// Tự động điền thông tin tài khoản đăng nhập (Khắc phục triệt để lỗi rò rỉ mã google_<uid>)
const fillFromLoggedInAccount = async () => {
  if (!isLoggedInCustomer.value) return;
  const full = await KhachHangService.getById(AuthStore.user.id).catch(() => null);

  // Chỉ gán SĐT nếu SĐT đó hợp lệ, tuyệt đối không gán google_<uid>
  if (userHasValidPhone.value && !checkoutForm.soDienThoai) {
    checkoutForm.soDienThoai = AuthStore.user.soDienThoai;
  }
  if (userHasValidPhone.value && !checkoutForm.sdtNguoiNhan) {
    checkoutForm.sdtNguoiNhan = AuthStore.user.soDienThoai;
  } else if (isValidPhoneNumber(full?.soDienThoai) && !checkoutForm.sdtNguoiNhan) {
    checkoutForm.sdtNguoiNhan = full.soDienThoai;
  }

  if (AuthStore.user.hoTen && !checkoutForm.hoTen) {
    checkoutForm.hoTen = AuthStore.user.hoTen;
  }
  if (AuthStore.user.hoTen && !checkoutForm.nguoiNhan) {
    checkoutForm.nguoiNhan = AuthStore.user.hoTen;
  }
  if (AuthStore.user.email && !checkoutForm.email) {
    checkoutForm.email = AuthStore.user.email;
  }
  if (full?.diaChi && !checkoutForm.diaChiGiaoHangText) {
    checkoutForm.diaChiGiaoHangText = full.diaChi;
  }

  foundCustomer.value = {
    khachHangId: AuthStore.user.id,
    hoTen: AuthStore.user.hoTen || full?.hoTen,
  };
};

watch(() => props.modelValue, async (open) => {
  if (open) window.addEventListener('keydown', onKeydown);
  else window.removeEventListener('keydown', onKeydown);
  if (!open) return;

  checkoutStep.value       = 1;
  checkoutSuccess.value    = false;
  checkoutLoading.value    = false;
  checkoutError.value      = '';
  validationWarning.value  = '';
  checkoutProgress.value   = '';
  promoMsg.value           = '';
  appliedPromo.value       = null;
  foundCustomer.value      = null;
  phoneTouched.value       = false;
  selectedPayment.value    = 'tien_mat';
  qrImageFailed.value      = false;

  Object.keys(checkoutForm).forEach(k => { checkoutForm[k] = ''; });

  if (!allPromos.value.length) {
    allPromos.value = await KhuyenMaiService.getAll().catch(() => []);
  }
  appliedVoucher.value = null;
  if (isLoggedInCustomer.value) {
    myVouchers.value = await PhieuGiamGiaCaNhanService.getCuaToi().catch(() => []);
  }

  await fillFromLoggedInAccount();
  fetchShippingFee();
});

watch(() => AuthStore.user, () => {
  if (props.modelValue) fillFromLoggedInAccount();
});

watch(() => [checkoutForm.diaChiGiaoHangText, props.cartTotal], () => {
  if (props.modelValue) fetchShippingFee();
});

// Tìm khách hàng theo số điện thoại (cho khách vãng lai)
const lookupCustomer = async () => {
  const phone = checkoutForm.soDienThoai.trim();
  if (!isValidPhoneNumber(phone)) {
    validationWarning.value = 'Vui lòng nhập số điện thoại hợp lệ (10 chữ số) để tra cứu';
    return;
  }
  validationWarning.value = '';
  const c = await KhachHangService.findByPhone(phone).catch(() => null);
  foundCustomer.value = c || null;
  if (c) {
    checkoutForm.hoTen              = c.hoTen || '';
    checkoutForm.email              = c.email || '';
    checkoutForm.nguoiNhan          = c.hoTen || '';
    checkoutForm.sdtNguoiNhan       = c.soDienThoai || '';
    checkoutForm.diaChiGiaoHangText = c.diaChi || '';
  }
};

// Áp dụng mã khuyến mãi
const applyPromo = async () => {
  const code = checkoutForm.maKhuyenMai.trim();
  if (!code) {
    appliedPromo.value = null;
    promoMsg.value = '';
    return;
  }
  appliedPromo.value = null;
  promoMsg.value = t('checkout.promoChecking');
  try {
    const res = await KhuyenMaiService.kiemTra(code);
    if (!res.ok) {
      let errMsg = t('checkout.promoInvalid');
      try { const e = await res.json(); errMsg = e.message || errMsg; } catch {}
      promoMsg.value = errMsg;
      return;
    }
    const data = await res.json();
    if (!data.valid) {
      promoMsg.value = data.message || t('checkout.promoInvalid');
      return;
    }
    appliedVoucher.value = null;
    appliedPromo.value = {
      khuyenMaiId:     data.khuyenMaiId,
      tenKhuyenMai:    data.tenKhuyenMai,
      maKhuyenMai:     code.toUpperCase(),
      loai:            data.loai,
      giaTri:          data.giaTri,
      giaTriToiDa:     data.giaTriToiDa,
      donHangToiThieu: data.donHangToiThieu,
    };
    promoMsg.value = t('checkout.promoSuccess', { name: data.tenKhuyenMai });
  } catch {
    promoMsg.value = t('checkout.promoInvalid');
  }
};

// Chuyển sang bước 2 (Thanh toán)
const goToPayment = () => {
  checkoutError.value = '';
  validationWarning.value = '';
  phoneTouched.value = true;

  const data = {
    nguoiNhan: checkoutForm.nguoiNhan,
    sdtNguoiNhan: checkoutForm.sdtNguoiNhan,
    diaChiGiaoHangText: checkoutForm.diaChiGiaoHangText,
    soDienThoai: checkoutForm.soDienThoai,
    hoTen: checkoutForm.hoTen,
    email: checkoutForm.email,
  };

  const result = checkoutInfoSchema.safeParse(data);
  if (!result.success) {
    const fieldErrors = result.error.flatten().fieldErrors;
    const firstError = Object.values(fieldErrors).flat().find(Boolean);
    validationWarning.value = firstError || t('checkout.errValidation');
    return;
  }

  checkoutStep.value = 2;
};

const parseApiError = async (res, fallbackPrefix) => {
  const raw = await res.text();
  try {
    const obj = JSON.parse(raw);
    const messages = Object.values(obj).filter((v) => typeof v === 'string');
    if (messages.length) return messages.join(' · ');
  } catch {}
  return `${fallbackPrefix}: ${res.status} ${raw}`;
};

// Đặt hàng
const placeOrder = async () => {
  checkoutError.value = '';
  validationWarning.value = '';

  if (!isReceiverPhoneValid.value) {
    checkoutError.value = 'Số điện thoại nhận hàng không hợp lệ (cần 10 số bắt đầu bằng 0)';
    checkoutStep.value = 1;
    return;
  }
  if (!checkoutForm.diaChiGiaoHangText.trim()) {
    checkoutError.value = 'Vui lòng điền địa chỉ giao hàng trước khi đặt hàng';
    checkoutStep.value = 1;
    return;
  }

  checkoutLoading.value  = true;
  checkoutProgress.value = t('checkout.progressCustomer');

  try {
    let khachHangId = foundCustomer.value?.khachHangId;

    if (!khachHangId) {
      // Khách vãng lai: tạo mới
      const custBody = {
        hoTen:       checkoutForm.nguoiNhan || checkoutForm.hoTen,
        soDienThoai: checkoutForm.sdtNguoiNhan,
        email:       checkoutForm.email,
        diaChi:      checkoutForm.diaChiGiaoHangText || 'Chua cap nhat',
        loaiKhach:   'ca_nhan',
        diemTichLuy: 0,
        trangThai:   'active',
      };
      const r = await KhachHangService.createGuest(custBody);
      if (!r.ok) throw new Error(await parseApiError(r, t('checkout.createCustomerError')));
      const newC = await r.json();
      khachHangId = newC.khachHangId;
    } else if (AuthStore.user?.role === 'khach_hang' && AuthStore.user.id === khachHangId) {
      // Khách đăng nhập: Đồng bộ số điện thoại thật & địa chỉ mới vào profile DB
      const syncBody = {
        hoTen:       checkoutForm.nguoiNhan || AuthStore.user.hoTen,
        soDienThoai: checkoutForm.sdtNguoiNhan,
        email:       checkoutForm.email || AuthStore.user.email,
        diaChi:      checkoutForm.diaChiGiaoHangText,
      };
      KhachHangService.save(khachHangId, syncBody).catch(() => {});
      // Cập nhật SĐT thật vào phiên đăng nhập
      AuthStore.user.soDienThoai = checkoutForm.sdtNguoiNhan;
    }

    checkoutProgress.value = t('checkout.progressOrder');
    const orderBody = {
      khachHangId,
      nguoiNhan:            checkoutForm.nguoiNhan,
      sdtNguoiNhan:         checkoutForm.sdtNguoiNhan,
      diaChiGiaoHangText:   checkoutForm.diaChiGiaoHangText,
      khuyenMaiId:          appliedPromo.value?.khuyenMaiId ?? null,
      phieuGiamGiaCaNhanId: appliedVoucher.value?.phieuId ?? null,
      tongTien:             props.cartTotal,
      giamGia:              checkoutGiamGia.value,
      phiVanChuyen:         phiVanChuyen.value,
      ngayDat:              nowLocalIso(),
      trangThaiDonHang:     'pending',
      trangThaiThanhToan:   'unpaid',
      kenhBan:              'online',
      ghiChu:               checkoutForm.ghiChu || null,
      items: props.cart.map(item => ({
        bienTheId: item.bienTheId,
        soLuong:   item.quantity,
      })),
    };

    const orderRes = await DonHangService.checkoutComplete(orderBody);
    if (!orderRes.ok)
      throw new Error(await parseApiError(orderRes, t('checkout.createOrderError')));
    const createdOrder = await orderRes.json();

    checkoutFinalTotal.value = checkoutTotal.value;
    checkoutOrderId.value    = createdOrder.id;
    checkoutOrderCode.value  = createdOrder.maDonHang || `#${createdOrder.id}`;
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
/* Stepper Styles */
.checkout-stepper {
  background: var(--bg-card-alt);
  padding: 4px 12px;
  border-radius: 24px;
  border: 1px solid var(--border-color-soft);
}
.stepper-step {
  transition: opacity 0.2s;
  cursor: pointer;
}
.step-circle {
  width: 24px;
  height: 24px;
  font-size: 11px;
  background: var(--bg-card-inset);
  color: var(--text-muted);
  border: 1px solid var(--border-color-soft);
  transition: all 0.2s ease;
}
.step-label {
  font-size: 12px;
  color: var(--text-secondary);
}
.step-active .step-circle {
  background: var(--accent);
  color: var(--accent-text, #fff);
  border-color: var(--accent);
  box-shadow: 0 0 10px rgba(244, 63, 94, 0.4);
}
.step-active .step-label {
  color: var(--accent-fg, var(--accent));
}
.step-done .step-circle {
  background: #48c78e;
  color: #fff;
  border-color: #48c78e;
}
.step-done .step-label {
  color: #48c78e;
}
.stepper-line {
  width: 24px;
  height: 2px;
  background: var(--border-color-soft);
  transition: background 0.2s;
}
.line-active {
  background: #48c78e;
}

/* Payment Option Cards */
.payment-option-card {
  border: 2px solid var(--border-color-soft);
  background: var(--bg-card);
  transition: all 0.2s ease;
  cursor: pointer;
}
.payment-option-card:hover {
  border-color: var(--border-color-strong);
  transform: translateY(-1px);
}
.payment-selected {
  border-color: var(--accent) !important;
  background: rgba(244, 63, 94, 0.05) !important;
  box-shadow: 0 4px 14px rgba(244, 63, 94, 0.1);
}
.payment-radio-circle {
  width: 20px;
  height: 20px;
  border-color: var(--border-color-strong) !important;
  background: transparent;
}
.payment-selected .payment-radio-circle {
  border-color: var(--accent) !important;
  background: var(--accent) !important;
}
.payment-radio-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fff;
}

/* Voucher Cards */
.voucher-card {
  background: var(--bg-card);
  border-color: var(--border-color-soft) !important;
  transition: all 0.15s ease;
}
.voucher-card:hover {
  border-color: var(--accent) !important;
}
.voucher-selected {
  border-color: var(--accent) !important;
  background: rgba(244, 63, 94, 0.08) !important;
}

/* Custom scrollbars */
.custom-scrollbar::-webkit-scrollbar {
  width: 5px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: var(--border-color-strong);
  border-radius: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

/* Success Glow */
.success-badge-glow {
  box-shadow: 0 0 30px rgba(72, 199, 142, 0.3);
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
</style>
