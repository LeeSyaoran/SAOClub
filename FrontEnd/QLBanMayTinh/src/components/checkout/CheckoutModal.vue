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
            v-else
            class="p-3 rounded-3 text-start small border"
            style="background:var(--bg-card-alt); border-color:var(--border-color-soft); max-width:440px;"
          >
            <div class="d-flex align-items-center gap-2 mb-1 text-primary fw-bold">
              <Smartphone :size="16" /> Thanh toán VietQR
            </div>
            <div style="color:var(--text-secondary); line-height:1.6;">
              {{ t('checkout.qrInstruction') }}
            </div>
            <!-- Nút xác nhận đã chuyển khoản -->
            <div class="mt-2 d-flex align-items-center gap-2 p-2 rounded-2 small" style="background:rgba(72,199,142,0.08);border:1px dashed #48c78e;font-size:11.5px;color:#059669;">
              <CheckCircle2 :size="13" />
              <span>Mã nội dung: <strong class="font-monospace">{{ qrTransferContent }}</strong></span>
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
        <div class="px-4 py-3 border-bottom d-flex align-items-center justify-content-between flex-wrap gap-3" style="border-color:var(--border-color-soft) !important; background:var(--bg-card);">
          <div class="d-flex align-items-center gap-3">
            <h5 class="fw-bold mb-0 text-truncate" style="font-size:1.1rem; color:var(--text-heading);">
              {{ checkoutStep === 1 ? t('checkout.infoTitle') : t('checkout.paymentTitle') }}
            </h5>
            <span class="badge rounded-pill px-2.5 py-1" style="background:var(--bg-card-alt); color:var(--text-secondary); font-size:11px; font-weight:600; border:1px solid var(--border-color-soft);">
              {{ cart.length }} món
            </span>
          </div>

          <!-- Stepper indicator bar -->
          <div class="checkout-stepper d-flex align-items-center gap-2.5">
            <!-- Bước 1 -->
            <button
              type="button"
              class="stepper-step btn btn-sm p-0 d-flex align-items-center gap-2 border-0 bg-transparent text-start"
              :class="{ 'step-active': checkoutStep === 1, 'step-done': checkoutStep > 1 }"
              :disabled="checkoutLoading"
              @click="checkoutStep = 1"
            >
              <div class="step-circle rounded-circle d-flex align-items-center justify-content-center fw-bold">
                <Check v-if="checkoutStep > 1" :size="13" />
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
            <div v-if="checkoutStep === 1" class="d-flex flex-column gap-3.5">
              <!-- Khách vãng lai: Khung tra cứu nhanh bằng SĐT (nếu chưa đăng nhập) -->
              <div v-if="!isLoggedInCustomer" class="card border rounded-3 p-3" style="background:var(--bg-card-alt); border-color:var(--border-color-soft) !important;">
                <div class="d-flex align-items-center justify-content-between flex-wrap gap-2.5">
                  <span class="small fw-semibold text-secondary" style="font-size:12.5px;">Đã từng mua hàng tại SAOClub?</span>
                  <div class="d-flex gap-2">
                    <div class="input-group input-group-sm" style="max-width:220px;">
                      <span class="input-group-text border-end-0 px-2.5" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary);">
                        <Phone :size="14" />
                      </span>
                      <input
                        v-model="checkoutForm.soDienThoai"
                        type="tel"
                        class="form-control border-start-0"
                        style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);height:36px;"
                        :placeholder="t('checkout.phonePlaceholder')"
                        @keyup.enter="lookupCustomer"
                      />
                    </div>
                    <button type="button" class="btn btn-sm btn-outline-warning flex-shrink-0 px-3 fw-semibold" style="border-radius:8px;" @click="lookupCustomer">
                      {{ t('checkout.find') }}
                    </button>
                  </div>
                </div>
                <div v-if="foundCustomer" class="small p-2 rounded-2 mt-2.5 d-flex align-items-center gap-2" style="background:rgba(72,199,142,0.1);color:#48c78e;">
                  <CheckCircle2 :size="14" class="flex-shrink-0" />
                  <span>{{ t('checkout.foundCustomer') }} <strong>{{ foundCustomer.hoTen }}</strong></span>
                </div>
              </div>

              <!-- Thẻ thông tin nhận hàng -->
              <div class="card border rounded-4 overflow-hidden" style="background:var(--bg-card); border-color:var(--border-color-soft) !important; box-shadow: 0 4px 20px rgba(0,0,0,0.03);">
                <!-- Header Thẻ Giao Hàng -->
                <div class="p-3.5 border-bottom d-flex align-items-center justify-content-between flex-wrap gap-3" style="border-color:var(--border-color-soft) !important; background:var(--bg-card-alt);">
                  <div class="d-flex align-items-center gap-3">
                    <div class="d-flex align-items-center justify-content-center rounded-3 shadow-xs flex-shrink-0" style="width:40px; height:40px; background:linear-gradient(135deg, rgba(244,63,94,0.15), rgba(219,39,119,0.08)); color:#e11d48;">
                      <MapPin :size="20" />
                    </div>
                    <div>
                      <div class="d-flex align-items-center gap-2 mb-0.5">
                        <span class="fw-bold" style="font-size:0.98rem; color:var(--text-heading);">{{ t('checkout.shippingHeading') }}</span>
                        <span class="badge rounded-pill px-2.5 py-1" style="background:rgba(244,63,94,0.1); color:#e11d48; font-size:11px; font-weight:700;">Giao tận nơi</span>
                      </div>
                      <div class="text-secondary small" style="font-size:12.5px;">Chuyển phát nhanh từ trung tâm SAOClub</div>
                    </div>
                  </div>

                  <!-- Nút thay đổi thông tin (Chỉ xem <-> Chỉnh sửa) -->
                  <button
                    v-if="!isEditingShipping"
                    type="button"
                    class="btn btn-sm btn-outline-danger d-inline-flex align-items-center gap-2 px-3.5 py-1.5 rounded-pill shadow-xs transition-all"
                    style="font-size:12.5px; font-weight:600;"
                    @click="isEditingShipping = true"
                  >
                    <Pencil :size="13" />
                    <span>Thay đổi thông tin</span>
                  </button>
                  <button
                    v-else
                    type="button"
                    class="btn btn-sm btn-success d-inline-flex align-items-center gap-2 px-3.5 py-1.5 rounded-pill shadow-xs transition-all"
                    style="font-size:12.5px; font-weight:600;"
                    :disabled="isSavingShipping"
                    @click="saveShippingInfo"
                  >
                    <span v-if="isSavingShipping" class="spinner-border spinner-border-sm" style="width:13px;height:13px;"></span>
                    <Check v-else :size="14" />
                    <span>{{ isSavingShipping ? 'Đang lưu...' : 'Lưu thông tin' }}</span>
                  </button>
                </div>

                <div class="p-3.5">
                  <!-- CHẾ ĐỘ CHỈ XEM (View-only: thông tin không thể nhập, chỉ có thể xem) -->
                  <div v-if="!isEditingShipping" class="d-flex flex-column gap-3.5">
                    <!-- Hộp thông tin người nhận & Địa chỉ -->
                    <div class="p-3.5 rounded-3" style="background:var(--bg-card-alt); border:1px solid var(--border-color-soft);">
                      <!-- Khách hàng & SĐT -->
                      <div class="d-flex align-items-center justify-content-between flex-wrap gap-3 pb-3 mb-3 border-bottom" style="border-color:var(--border-color-soft) !important;">
                        <div class="d-flex align-items-center gap-3">
                          <div class="rounded-circle d-flex align-items-center justify-content-center text-white fw-bold shadow-xs flex-shrink-0" style="width:42px; height:42px; background:linear-gradient(135deg, #f43f5e, #db2777); font-size:15px;">
                            {{ (checkoutForm.nguoiNhan || 'U').charAt(0).toUpperCase() }}
                          </div>
                          <div>
                            <div class="fw-bold" style="color:var(--text-heading); font-size:15px; line-height:1.3; margin-bottom:3px;">
                              {{ checkoutForm.nguoiNhan || 'Chưa có tên người nhận' }}
                            </div>
                            <div v-if="checkoutForm.email" class="text-secondary small d-flex align-items-center gap-2" style="font-size:12.5px;">
                              <Mail :size="13" class="text-muted flex-shrink-0" />
                              <span>{{ checkoutForm.email }}</span>
                            </div>
                          </div>
                        </div>

                        <div>
                          <span class="badge rounded-pill px-3.5 py-1.5 fw-bold font-monospace d-inline-flex align-items-center gap-2" style="background:rgba(59,130,246,0.1); color:#2563eb; font-size:12.5px;">
                            <Phone :size="13" />
                            <span>{{ checkoutForm.sdtNguoiNhan || '—' }}</span>
                          </span>
                        </div>
                      </div>

                      <!-- Địa chỉ chi tiết -->
                      <div class="d-flex align-items-start gap-3.5">
                        <div class="d-flex align-items-center justify-content-center rounded-3 flex-shrink-0" style="width:38px; height:38px; background:rgba(244,63,94,0.1); color:#e11d48; margin-top:2px;">
                          <MapPin :size="20" />
                        </div>
                        <div class="flex-grow-1 min-w-0">
                          <div class="small text-muted fw-bold mb-1" style="font-size:11px; text-transform:uppercase; letter-spacing:0.6px;">Địa chỉ nhận hàng</div>
                          <div class="fw-semibold" style="color:var(--text-heading); font-size:14px; line-height:1.6;">
                            {{ checkoutForm.diaChiGiaoHangText || 'Chưa cập nhật địa chỉ nhận hàng' }}
                          </div>
                          <div v-if="checkoutForm.ghiChu" class="mt-2.5 p-2.5 rounded-2 small d-flex align-items-center gap-2.5" style="background:rgba(245,158,11,0.08); color:#b45309; font-size:12px; border-left:3px solid #f59e0b;">
                            <FileText :size="14" class="flex-shrink-0" />
                            <span>Ghi chú: {{ checkoutForm.ghiChu }}</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- Thẻ tính phí vận chuyển theo cự ly kho SAOClub (134 Cầu Diễn) -->
                    <div class="p-3.5 rounded-3" style="background:linear-gradient(135deg, rgba(244,63,94,0.04) 0%, rgba(219,39,119,0.08) 100%); border:1px solid rgba(244,63,94,0.2);">
                      <!-- Tuyến vận chuyển -->
                      <div class="d-flex align-items-center justify-content-between flex-wrap gap-3 mb-2.5 pb-2.5 border-bottom" style="border-color:rgba(244,63,94,0.12) !important;">
                        <div class="d-flex align-items-center gap-2.5">
                          <div class="d-flex align-items-center justify-content-center rounded-2 flex-shrink-0" style="width:30px; height:30px; background:rgba(244,63,94,0.12); color:#e11d48;">
                            <Store :size="15" />
                          </div>
                          <div class="small" style="font-size:12.5px; color:var(--text-secondary); line-height:1.4;">
                            Kho xuất phát: <strong style="color:var(--text-heading); margin-left:3px;">134 Cầu Diễn, Minh Khai, Hà Nội</strong>
                          </div>
                        </div>

                        <div class="d-inline-flex align-items-center gap-2 badge rounded-pill px-3 py-1.5 font-monospace shadow-xs" style="background:#fff; color:#e11d48; border:1px solid rgba(244,63,94,0.25); font-size:12px; font-weight:700;">
                          <Navigation :size="13" />
                          <span v-if="shippingDistanceLoading">Đang tính km...</span>
                          <span v-else-if="shippingDistanceKm > 0">~{{ shippingDistanceKm }} km</span>
                          <span v-else>Đang chờ địa chỉ</span>
                        </div>
                      </div>

                      <!-- Dịch vụ & Phí ship thực tế -->
                      <div class="d-flex align-items-center justify-content-between flex-wrap gap-3">
                        <div class="d-flex align-items-center gap-2.5 flex-wrap">
                          <span v-if="shippingDistanceKm <= 25 && shippingDistanceKm > 0" class="badge rounded-pill px-3 py-1.5 fw-semibold d-inline-flex align-items-center gap-2" style="background:rgba(72,199,142,0.15); color:#059669; font-size:12px;">
                            <Sparkles :size="13" /> Giao hỏa tốc 2H
                          </span>
                          <span v-else class="badge rounded-pill px-3 py-1.5 fw-semibold d-inline-flex align-items-center gap-2" style="background:rgba(59,130,246,0.12); color:#2563eb; font-size:12px;">
                            <Truck :size="13" /> Giao hàng tiêu chuẩn
                          </span>
                          <span class="small text-muted" style="font-size:12px;">(Cước tự động theo km)</span>
                        </div>

                        <div class="d-flex align-items-center gap-2">
                          <span class="small text-secondary" style="font-size:12.5px;">Phí ship:</span>
                          <span v-if="phiVanChuyen === 0" class="badge bg-success text-white rounded-pill px-3 py-1 fw-bold" style="font-size:12px;">
                            MIỄN PHÍ
                          </span>
                          <span v-else class="fw-black font-monospace fs-5" style="color:#e11d48;">
                            {{ formatPrice(phiVanChuyen) }}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- CHẾ ĐỘ CHỈNH SỬA (Kích hoạt khi bấm nút "Thay đổi thông tin" hoặc chưa có thông tin) -->
                  <div v-else class="row g-3">
                    <div class="col-12">
                      <div class="p-3 rounded-3 small d-flex align-items-center justify-content-between gap-3" style="background:rgba(59,130,246,0.06); border:1px solid rgba(59,130,246,0.15); color:#1d4ed8; font-size:12.5px;">
                        <div class="d-flex align-items-center gap-2.5">
                          <Pencil :size="15" class="flex-shrink-0" />
                          <span>Nhập thông tin người nhận và địa chỉ, phí ship sẽ tự động tính theo số km thực tế.</span>
                        </div>
                        <span class="badge rounded-pill px-2.5 py-1 d-none d-sm-inline" style="background:rgba(59,130,246,0.12); color:#1d4ed8; font-size:11px; font-weight:600;">Tự động tính km</span>
                      </div>
                    </div>

                    <!-- Họ tên người nhận -->
                    <div class="col-md-6">
                      <label class="form-label small fw-semibold mb-1.5" style="color:var(--text-secondary); font-size:12.5px;">
                        {{ t('checkout.receiverPlaceholder') }} <span class="text-danger">*</span>
                      </label>
                      <div class="input-group" style="height:42px;">
                        <span class="input-group-text border-end-0 px-3 d-flex align-items-center justify-content-center" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary); width:44px;">
                          <User :size="16" />
                        </span>
                        <input
                          v-model="checkoutForm.nguoiNhan"
                          type="text"
                          class="form-control border-start-0"
                          style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:0 10px 10px 0;height:42px;font-size:13.5px;padding-left:12px;"
                          placeholder="Ví dụ: Nguyễn Văn A"
                          required
                        />
                      </div>
                    </div>

                    <!-- Số điện thoại nhận hàng -->
                    <div class="col-md-6">
                      <label class="form-label small fw-semibold mb-1.5" style="color:var(--text-secondary); font-size:12.5px;">
                        {{ t('checkout.receiverPhonePlaceholder') }} <span class="text-danger">*</span>
                      </label>
                      <div class="input-group" style="height:42px;">
                        <span class="input-group-text border-end-0 px-3 d-flex align-items-center justify-content-center" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary); width:44px;">
                          <Phone :size="16" />
                        </span>
                        <input
                          v-model="checkoutForm.sdtNguoiNhan"
                          type="tel"
                          class="form-control border-start-0"
                          :class="{ 'is-invalid': phoneTouched && !isReceiverPhoneValid }"
                          style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:0 10px 10px 0;height:42px;font-size:13.5px;padding-left:12px;"
                          placeholder="Ví dụ: 0987654321"
                          required
                          @blur="phoneTouched = true"
                        />
                      </div>
                      <div v-if="phoneTouched && !isReceiverPhoneValid" class="small text-danger mt-1" style="font-size:11.5px;">
                        Số điện thoại gồm 10 chữ số bắt đầu bằng 0
                      </div>
                    </div>

                    <!-- Email nhận thông báo -->
                    <div class="col-12">
                      <label class="form-label small fw-semibold mb-1.5" style="color:var(--text-secondary); font-size:12.5px;">
                        {{ t('checkout.emailPlaceholder') }} <span class="text-muted fw-normal">(để nhận hóa đơn & mã tra cứu)</span>
                      </label>
                      <div class="input-group" style="height:42px;">
                        <span class="input-group-text border-end-0 px-3 d-flex align-items-center justify-content-center" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary); width:44px;">
                          <Mail :size="16" />
                        </span>
                        <input
                          v-model="checkoutForm.email"
                          type="email"
                          class="form-control border-start-0"
                          style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:0 10px 10px 0;height:42px;font-size:13.5px;padding-left:12px;"
                          placeholder="tenban@gmail.com"
                        />
                      </div>
                    </div>

                    <!-- Địa chỉ nhận hàng -->
                    <div class="col-12">
                      <label class="form-label small fw-semibold mb-1.5" style="color:var(--text-secondary); font-size:12.5px;">
                        {{ t('checkout.addressPlaceholder') }} <span class="text-danger">*</span>
                      </label>
                      <AddressPicker
                        v-model="checkoutForm.diaChiGiaoHangText"
                        placeholder="Nhập địa chỉ giao hàng (Ví dụ: 86 Cầu Giấy, Hà Nội)..."
                        @select-coords="onLocationCoords"
                      />

                      <!-- Thẻ tính cước & cự ly trực quan cập nhật theo thời gian thực -->
                      <div class="p-3.5 rounded-3 mt-3" style="background:var(--bg-card-alt); border:1px solid var(--border-color-soft);">
                        <div class="d-flex align-items-center justify-content-between flex-wrap gap-2.5 mb-2.5">
                          <div class="d-flex align-items-center gap-2.5">
                            <Store :size="15" class="text-danger flex-shrink-0" />
                            <span class="small" style="font-size:12.5px; color:var(--text-secondary);">Kho gửi: <strong style="color:var(--text-heading); margin-left:2px;">134 Cầu Diễn, Bắc Từ Liêm, Hà Nội</strong></span>
                          </div>
                          <span class="badge rounded-pill px-3 py-1.5 font-monospace shadow-xs" style="background:rgba(244,63,94,0.1); color:#e11d48; font-size:12px; font-weight:700;">
                            <Navigation :size="12" class="me-1.5" />
                            <span v-if="shippingDistanceLoading">Đang tính km...</span>
                            <span v-else-if="shippingDistanceKm > 0">~{{ shippingDistanceKm }} km</span>
                            <span v-else>Đang chờ địa chỉ</span>
                          </span>
                        </div>
                        <div class="d-flex align-items-center justify-content-between flex-wrap gap-2.5 pt-2.5 border-top" style="border-color:var(--border-color-soft) !important;">
                          <div class="d-flex align-items-center gap-2">
                            <span v-if="shippingDistanceKm <= 25 && shippingDistanceKm > 0" class="badge rounded-pill px-2.5 py-1 fw-semibold d-inline-flex align-items-center gap-1.5" style="background:rgba(72,199,142,0.15); color:#059669; font-size:11.5px;">
                              <Sparkles :size="12" /> Hỏa tốc 2H
                            </span>
                            <span v-else class="badge rounded-pill px-2.5 py-1 fw-semibold d-inline-flex align-items-center gap-1.5" style="background:rgba(59,130,246,0.12); color:#2563eb; font-size:11.5px;">
                              <Truck :size="12" /> Tiêu chuẩn
                            </span>
                            <span class="small text-muted" style="font-size:11.5px;">(Cước tự động theo km)</span>
                          </div>
                          <div class="d-flex align-items-center gap-2">
                            <span class="small text-secondary" style="font-size:12.5px;">Phí ship:</span>
                            <strong class="font-monospace fs-5 fw-black" style="color:#e11d48;">{{ formatPrice(phiVanChuyen) }}</strong>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- Ghi chú giao hàng -->
                    <div class="col-12">
                      <label class="form-label small fw-semibold mb-1.5" style="color:var(--text-secondary); font-size:12.5px;">
                        {{ t('checkout.orderNotes') }}
                      </label>
                      <div class="input-group" style="height:42px;">
                        <span class="input-group-text border-end-0 px-3 d-flex align-items-center justify-content-center" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary); width:44px;">
                          <FileText :size="16" />
                        </span>
                        <input
                          v-model="checkoutForm.ghiChu"
                          type="text"
                          class="form-control border-start-0"
                          style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:0 10px 10px 0;height:42px;font-size:13.5px;padding-left:12px;"
                          :placeholder="t('checkout.orderNotesPlaceholder')"
                        />
                      </div>
                    </div>

                    <!-- Nút xác nhận lưu thông tin giao hàng -->
                    <div class="col-12 d-flex align-items-center justify-content-end gap-2 pt-1">
                      <button
                        type="button"
                        class="btn btn-sm btn-danger px-4 py-2.5 rounded-pill shadow-xs fw-semibold d-inline-flex align-items-center gap-2"
                        :disabled="isSavingShipping"
                        @click="saveShippingInfo"
                      >
                        <span v-if="isSavingShipping" class="spinner-border spinner-border-sm" style="width:14px;height:14px;"></span>
                        <Check v-else :size="15" />
                        <span>{{ isSavingShipping ? 'Đang lưu...' : 'Xác nhận thông tin giao hàng' }}</span>
                      </button>
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
                </div>
              </div>

              <!-- Thông báo khi chọn QR: Mã QR sẽ xuất hiện sau khi nhấn Xác nhận thanh toán -->
              <Transition name="fade">
                <div v-if="selectedPayment === 'qr'" class="card border rounded-3 p-3.5" style="background:var(--bg-card-alt); border-color:var(--border-color-soft) !important;">
                  <div class="d-flex align-items-center gap-3">
                    <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width:46px; height:46px; background:#fff7ed; color:#ea580c; border:1px solid #ffedd5;">
                      <Smartphone :size="24" />
                    </div>
                    <div>
                      <div class="fw-bold" style="font-size:0.92rem; color:var(--text-heading);">Mã QR thanh toán VietQR (Timo)</div>
                      <div class="small mt-0.5" style="font-size:12px; color:var(--text-secondary); line-height:1.45;">
                        Mã QR chuẩn Timo / Napas247 kèm thông tin STK và cú pháp chuyển khoản tự động sẽ <strong>hiển thị ngay khi bạn nhấn "Xác nhận thanh toán"</strong>.
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

              <div class="order-items-list d-flex flex-column gap-2.5" style="max-height:180px; overflow-y:auto;">
                <div
                  v-for="item in cart" :key="item.bienTheId"
                  class="order-item d-flex align-items-center gap-3 p-2.5 rounded-3 border"
                  style="background:var(--bg-card); border-color:var(--border-color-soft) !important;"
                >
                  <div class="order-item-img flex-shrink-0 rounded-2 overflow-hidden border d-flex align-items-center justify-content-center" style="width:44px;height:44px;background:var(--bg-card-inset); border-color:var(--border-color-soft) !important;">
                    <img v-if="item.hinhAnhChinh" :src="item.hinhAnhChinh" :alt="item.tenSanPham" style="width:100%;height:100%;object-fit:contain;" />
                    <Laptop v-else :size="18" class="text-muted" />
                  </div>
                  <div class="flex-grow-1 min-w-0">
                    <div class="small fw-semibold text-truncate" style="color:var(--text-heading); font-size:12.5px;">{{ item.tenSanPham }}</div>
                    <div class="d-flex align-items-center gap-2 mt-1">
                      <span class="badge rounded-pill bg-secondary-subtle text-secondary px-2 py-0.5" style="font-size:10px;">×{{ item.quantity }}</span>
                      <span class="small text-warning fw-bold font-monospace" style="font-size:12.5px;">{{ formatPrice(item.giaBan * item.quantity) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <hr class="my-2.5" style="border-color:var(--border-color-soft);" />

            <!-- Mã khuyến mãi & Voucher cá nhân -->
            <div class="mb-3">
              <div class="fw-bold small text-uppercase mb-2.5" style="letter-spacing:0.04em; color:var(--text-secondary); font-size:11px;">
                {{ t('checkout.promoHeading') }}
              </div>

              <!-- Ô nhập mã khuyến mãi -->
              <div class="d-flex gap-2 mb-2.5">
                <div class="input-group input-group-sm">
                  <span class="input-group-text border-end-0 px-2.5" style="background:var(--bg-input); border-color:var(--border-color-strong); color:var(--text-secondary);">
                    <Tag :size="15" />
                  </span>
                  <input
                    v-model="checkoutForm.maKhuyenMai"
                    type="text"
                    class="form-control border-start-0"
                    style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);text-transform:uppercase;height:38px;"
                    :placeholder="t('checkout.promoPlaceholder')"
                    @keyup.enter="applyPromo"
                  />
                </div>
                <button
                  type="button"
                  class="btn btn-sm btn-outline-warning flex-shrink-0 px-3 fw-semibold"
                  style="border-radius:8px; height:38px;"
                  @click="applyPromo"
                >
                  {{ t('checkout.apply') }}
                </button>
              </div>

              <!-- Trạng thái áp dụng mã -->
              <div v-if="promoMsg" class="small px-1 mb-2.5 d-flex align-items-center gap-2" :class="appliedPromo ? 'text-success' : 'text-danger'" style="font-size:12px;">
                <CheckCircle2 v-if="appliedPromo" :size="14" />
                <AlertCircle v-else :size="14" />
                <span>{{ promoMsg }}</span>
              </div>

              <!-- Thẻ mã đang áp dụng -->
              <div v-if="appliedPromo" class="p-2.5 rounded-3 d-flex align-items-center justify-content-between border mb-2.5" style="background:rgba(72,199,142,0.1); border-color:rgba(72,199,142,0.3) !important;">
                <div class="d-flex align-items-center gap-2.5">
                  <div class="rounded-2 d-flex align-items-center justify-content-center flex-shrink-0" style="width:26px; height:26px; background:rgba(72,199,142,0.2); color:#059669;">
                    <Tag :size="13" />
                  </div>
                  <div>
                    <span class="fw-bold small text-success">{{ appliedPromo.maKhuyenMai }}</span>
                    <span class="small text-muted ms-1.5">(-{{ formatPrice(calcDiscountFor(appliedPromo)) }})</span>
                  </div>
                </div>
                <button type="button" class="btn btn-sm btn-link text-danger p-0 small" style="text-decoration:none; font-size:11.5px;" @click="removePromo">
                  {{ t('checkout.removeVoucher') }}
                </button>
              </div>

              <!-- Danh sách Voucher cá nhân đổi từ điểm -->
              <div v-if="isLoggedInCustomer && eligibleVouchers.length" class="mt-2.5">
                <div class="d-flex align-items-center justify-content-between mb-1.5">
                  <span class="small fw-semibold" style="color:var(--text-secondary); font-size:11.5px;">
                    {{ t('checkout.voucherHeading') }} ({{ eligibleVouchers.length }})
                  </span>
                </div>
                <div class="d-flex flex-column gap-2" style="max-height:120px; overflow-y:auto;">
                  <div
                    v-for="v in eligibleVouchers" :key="v.phieuId"
                    class="voucher-card p-2.5 rounded-3 d-flex align-items-center justify-content-between border cursor-pointer"
                    :class="{ 'voucher-selected': appliedVoucher?.phieuId === v.phieuId }"
                    @click="selectVoucher(v)"
                  >
                    <div class="d-flex align-items-center gap-2.5">
                      <div class="rounded-2 d-flex align-items-center justify-content-center flex-shrink-0" style="width:26px; height:26px; background:rgba(245,158,11,0.12); color:#f59e0b;">
                        <Percent :size="13" />
                      </div>
                      <span class="fw-bold small font-monospace" style="color:var(--text-heading); font-size:12.5px; letter-spacing:0.5px;">{{ v.maPhieu }}</span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                      <span class="text-warning fw-bold small font-monospace" style="font-size:12.5px;">−{{ formatPrice(v.discount) }}</span>
                      <CheckCircle2 v-if="appliedVoucher?.phieuId === v.phieuId" :size="15" class="text-success" />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <hr class="my-2.5" style="border-color:var(--border-color-soft);" />

            <!-- Bảng phân tích chi phí -->
            <div class="mt-auto d-flex flex-column gap-2.5 pt-2">
              <div class="d-flex justify-content-between small" style="color:var(--text-secondary); font-size:12.5px;">
                <span>{{ t('checkout.subtotal') }}</span>
                <span class="font-monospace fw-semibold" style="color:var(--text-heading);">{{ formatPrice(cartTotal) }}</span>
              </div>

              <div class="d-flex justify-content-between small align-items-center" style="color:var(--text-secondary); font-size:12.5px;">
                <div class="d-flex align-items-center gap-2">
                  <span>{{ t('checkout.shippingFee') }}</span>
                  <span v-if="shippingDistanceKm > 0" class="badge rounded-pill px-2 py-0.5" style="background:rgba(219,39,119,0.1); color:var(--pink-600); font-size:10.5px;">
                    ~{{ shippingDistanceKm }} km
                  </span>
                </div>
                <span v-if="phiVanChuyen === 0" class="badge bg-success-subtle text-success rounded-pill px-2.5 py-0.5" style="font-size:11px;">
                  {{ t('checkout.free') }}
                </span>
                <span v-else class="font-monospace fw-semibold" style="color:var(--text-heading);">{{ formatPrice(phiVanChuyen) }}</span>
              </div>

              <div v-if="checkoutGiamGia > 0" class="d-flex justify-content-between small text-success align-items-center" style="font-size:12.5px;">
                <span class="d-flex align-items-center gap-1.5"><Tag :size="13" /> {{ t('checkout.discount') }}</span>
                <span class="font-monospace fw-bold">− {{ formatPrice(checkoutGiamGia) }}</span>
              </div>

              <div class="d-flex justify-content-between align-items-end pt-2.5 mt-1 border-top" style="border-color:var(--border-color-soft) !important;">
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
              class="btn btn-warning fw-bold px-4 py-2 rounded-3 d-flex align-items-center gap-2.5 shadow-sm"
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

    <!-- Modal Quét Mã QR SePay / VietQR Độc Lập (Ảnh 1 & 2) -->
    <QrPaymentModal
      v-model="showQrModal"
      :order="createdOrderObject"
      :items="createdOrderObject?.items || []"
      @close="onQrModalClose"
      @paid="onQrModalPaid"
    />
  </div><!-- /checkout-backdrop -->
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue';
import { t } from '../../i18n/index.js';
import { AuthStore, setSession } from '../../stores/index.js';
import { nowLocalIso } from '../../utils/datetime.js';
import { formatPrice as formatPriceRaw } from '../../utils/formatPrice.js';
import { checkoutInfoSchema, isValidPhoneNumber } from '../../utils/validators.js';
import {
  CheckCircle2, Laptop, Banknote, Smartphone, Landmark, ImageOff,
  ArrowLeft, ArrowRight, User, Phone, Mail, MapPin, FileText, Tag,
  Copy, Check, AlertCircle, AlertTriangle, X, ShieldCheck, Percent,
  Pencil, Truck, Store, Navigation, Sparkles,
} from '@lucide/vue';
import AddressPicker from './AddressPicker.vue';
import QrPaymentModal from './QrPaymentModal.vue';
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
const selectedPayment    = ref('tien_mat'); // 'tien_mat' | 'qr'
const showQrModal        = ref(false);       // Hiển thị modal quét mã QR SePay/VietQR
const createdOrderObject = ref(null);        // Đơn hàng vừa tạo để truyền sang QR modal

const onQrModalClose = () => {
  showQrModal.value = false;
  // Đóng modal đặt hàng khi khách thoát khỏi màn hình quét QR mà chưa thanh toán
  emit('update:modelValue', false);
};

const onQrModalPaid = (o) => {
  showQrModal.value = false;
  checkoutSuccess.value = true;
};
const phoneTouched       = ref(false); // Đã chạm vào ô SĐT nhận hàng chưa
const copiedField        = ref('');    // Tên trường vừa sao chép
let copyTimer            = null;

// VietQR API — Timo bank, tài khoản thật
const STORE_BANK     = 'TIMO';
const STORE_ACCOUNT  = '0338861232';
const STORE_HOLDER   = 'LE HUY DO';

const qrImageFailed = ref(false);
const qrTransferContent = computed(() =>
  checkoutOrderCode.value ? `SAO ${checkoutOrderCode.value}` : 'SAO LAPTOP'
);
const qrImageUrl = computed(() => {
  const info = encodeURIComponent(qrTransferContent.value);
  const name = encodeURIComponent(STORE_HOLDER);
  return `https://img.vietqr.io/image/${STORE_BANK}-${STORE_ACCOUNT}-compact2.png?amount=${checkoutTotal.value}&addInfo=${info}&accountName=${name}`;
});

// Form thông tin đặt hàng
const idempotencyKey = ref(''); // Unique key để tránh tạo đơn trùng
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
const isLoggedInCustomer = computed(() => !!AuthStore.user);

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

// Sinh idempotency key (UUID v4 đơn giản)
const genIdempotencyKey = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = Math.random() * 16 | 0;
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
  });
};

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

// Chế độ chỉnh sửa thông tin giao hàng
const isEditingShipping = ref(false);
const isSavingShipping  = ref(false);

// Đồng bộ thông tin người nhận vào hồ sơ cá nhân của khách hàng đã đăng nhập
const syncCustomerProfile = async () => {
  const customerId = AuthStore.user?.id || AuthStore.user?.khachHangId || foundCustomer.value?.khachHangId;
  if (!customerId || (AuthStore.user && AuthStore.user.role && AuthStore.user.role !== 'khach_hang')) {
    return false;
  }

  const nguoiNhan = checkoutForm.nguoiNhan?.trim();
  const sdt = checkoutForm.sdtNguoiNhan?.trim();
  const diaChi = checkoutForm.diaChiGiaoHangText?.trim();
  const email = checkoutForm.email?.trim() || null;

  // Cần tên người nhận, SĐT hợp lệ và địa chỉ nhận hàng
  if (!nguoiNhan || !isValidPhoneNumber(sdt) || !diaChi) {
    return false;
  }

  try {
    const current = await KhachHangService.getById(customerId).catch(() => null);
    const body = {
      ...(current || {}),
      hoTen: nguoiNhan,
      soDienThoai: sdt,
      email: email || current?.email || AuthStore.user?.email || null,
      diaChi: diaChi,
      loaiKhach: current?.loaiKhach || 'ca_nhan',
      diemTichLuy: current?.diemTichLuy ?? 0,
      trangThai: current?.trangThai || 'active',
    };

    const res = await KhachHangService.save(customerId, body);
    if (res && (res.ok || res.status === 200 || res.status === 204)) {
      if (AuthStore.user) {
        setSession({
          ...AuthStore.user,
          hoTen: body.hoTen,
          soDienThoai: body.soDienThoai,
          email: body.email,
          diaChi: body.diaChi,
        });
      }
      return true;
    }
  } catch (err) {
    console.warn('[CheckoutModal] Lỗi khi đồng bộ thông tin cá nhân:', err);
  }
  return false;
};

const saveShippingInfo = async () => {
  if (!checkoutForm.nguoiNhan?.trim()) {
    validationWarning.value = 'Vui lòng nhập tên người nhận';
    return;
  }
  if (!isReceiverPhoneValid.value) {
    phoneTouched.value = true;
    validationWarning.value = 'Vui lòng nhập số điện thoại nhận hàng hợp lệ (10 chữ số bắt đầu bằng 0)';
    return;
  }
  if (!checkoutForm.diaChiGiaoHangText?.trim()) {
    validationWarning.value = 'Vui lòng nhập địa chỉ giao hàng';
    return;
  }
  validationWarning.value = '';
  isSavingShipping.value = true;
  try {
    await syncCustomerProfile();
  } finally {
    isSavingShipping.value = false;
  }
  isEditingShipping.value = false;
  fetchShippingFee();
  try {
    const storageKey = AuthStore.user?.id ? `saoclub_shipping_info_${AuthStore.user.id}` : 'saoclub_guest_shipping_info';
    localStorage.setItem(storageKey, JSON.stringify({
      nguoiNhan: checkoutForm.nguoiNhan,
      sdtNguoiNhan: checkoutForm.sdtNguoiNhan,
      email: checkoutForm.email,
      diaChiGiaoHangText: checkoutForm.diaChiGiaoHangText,
      ghiChu: checkoutForm.ghiChu,
    }));
    localStorage.removeItem('saoclub_shipping_info');
  } catch {}
};

// Cửa hàng cố định: 134 Cầu Diễn, Minh Khai, Bắc Từ Liêm, Hà Nội
const STORE_LOCATION = {
  address: '134 Cầu Diễn, Minh Khai, Bắc Từ Liêm, Hà Nội',
  lat: 21.0538,
  lon: 105.7485,
};

const shippingDistanceKm = ref(0);
const shippingDistanceLoading = ref(false);
const recipientCoords = ref(null);

// Tính khoảng cách giữa 2 tọa độ theo công thức Haversine (km)
const calculateDistanceKm = (lat1, lon1, lat2, lon2) => {
  const R = 6371;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round((R * c) * 10) / 10;
};

// Bảng tính phí vận chuyển theo số km thực tế từ 134 Cầu Diễn:
const calcFeeByDistance = (km) => {
  if (km == null || isNaN(km) || km < 0) return 30000;
  if (km <= 3) return 15000;      // Rất gần (< 3km, cùng khu vực Cầu Diễn/Minh Khai): 15.000đ
  if (km <= 7) return 20000;      // Nội thành gần (Bắc Từ Liêm, Cầu Giấy, Mỹ Đình): 20.000đ
  if (km <= 15) return 25000;     // Nội thành Hà Nội (Ba Đình, Đống Đa, Thanh Xuân, Hoàn Kiếm): 25.000đ
  if (km <= 25) return 30000;     // Ngoại thành gần (Hoàng Mai, Hà Đông, Long Biên, Gia Lâm): 30.000đ
  if (km <= 40) return 40000;     // Ngoại thành xa (Sơn Tây, Ba Vì, Sóc Sơn, Mê Linh): 40.000đ
  if (km <= 100) return 50000;    // Các tỉnh lân cận Hà Nội (Bắc Ninh, Vĩnh Phúc, Hưng Yên): 50.000đ
  if (km <= 300) return 60000;    // Miền Bắc (Hải Phòng, Quảng Ninh, Thái Nguyên, Nam Định): 60.000đ
  if (km <= 800) return 75000;    // Miền Trung (Thanh Hóa, Nghệ An, Huế, Đà Nẵng): 75.000đ
  return 90000;                   // Miền Nam / Xuyên Việt (TP.HCM, Bình Dương, Cần Thơ): 90.000đ
};

// Bảng tọa độ dự phòng đầy đủ các quận/huyện Hà Nội và các tỉnh thành toàn quốc
const VN_LOCATIONS = [
  { keywords: ['cầu diễn', 'cau dien', 'phúc diễn', 'phuc dien', 'phú diễn', 'phu dien', 'tây tựu', 'tay tuu', 'nhổn', 'nhon', 'minh khai'], lat: 21.0538, lon: 105.7485 },
  { keywords: ['bắc từ liêm', 'bac tu liem', 'đông ngạc', 'dong ngac', 'xuân đỉnh', 'xuan dinh', 'cổ nhuế', 'co nhue', 'thụy phương', 'thuy phuong'], lat: 21.0650, lon: 105.7720 },
  { keywords: ['nam từ liêm', 'nam tu liem', 'mỹ đình', 'my dinh', 'mễ trì', 'me tri', 'trung văn', 'trung van', 'tây mỗ', 'tay mo', 'đại mỗ', 'dai mo'], lat: 21.0180, lon: 105.7640 },
  { keywords: ['cầu giấy', 'cau giay', 'dịch vọng', 'dich vong', 'nghĩa tân', 'nghia tan', 'mai dịch', 'mai dich', 'quan hoa', 'trung hòa', 'trung hoa', 'yên hòa', 'yen hoa'], lat: 21.0362, lon: 105.7906 },
  { keywords: ['trạm trôi', 'tram troi', 'hoài đức', 'hoai duc', 'kim chung', 'di trạch', 'an khánh', 'an khanh'], lat: 21.0200, lon: 105.7100 },
  { keywords: ['đan phượng', 'dan phuong', 'thị trấn phùng', 'phung'], lat: 21.0850, lon: 105.6700 },
  { keywords: ['tây hồ', 'tay ho', 'nhật tân', 'quảng an', 'thụy khuê', 'thuy khue', 'xuân la', 'xuan la'], lat: 21.0718, lon: 105.8230 },
  { keywords: ['ba đình', 'ba dinh', 'kim mã', 'kim ma', 'liễu giai', 'lieu giai', 'giảng võ', 'giang vo', 'đội cấn', 'doi can'], lat: 21.0341, lon: 105.8244 },
  { keywords: ['đống đa', 'dong da', 'láng hạ', 'lang ha', 'xã đàn', 'xa dan', 'ô chợ dừa', 'chùa bộc', 'thái hà', 'thai ha'], lat: 21.0180, lon: 105.8290 },
  { keywords: ['thanh xuân', 'thanh xuan', 'nguyễn trãi', 'nguyen trai', 'nhân chính', 'nhan chinh', 'khương đình', 'khuong dinh'], lat: 20.9980, lon: 105.8080 },
  { keywords: ['hoàn kiếm', 'hoan kiem', 'phố cổ', 'tràng tiền', 'hàng bài', 'hàng bạc', 'hàng đào'], lat: 21.0285, lon: 105.8542 },
  { keywords: ['hai bà trưng', 'hai ba trung', 'bạch mai', 'bach mai', 'bách khoa', 'phố huế', 'đại cồ việt'], lat: 21.0060, lon: 105.8520 },
  { keywords: ['hà đông', 'ha dong', 'quang trung', 'văn quán', 'van quan', 'mộ lao', 'mo lao', 'la khê', 'la khe'], lat: 20.9720, lon: 105.7760 },
  { keywords: ['hoàng mai', 'hoang mai', 'linh đàm', 'linh dam', 'giáp bát', 'giap bat', 'định công', 'dinh cong'], lat: 20.9760, lon: 105.8450 },
  { keywords: ['long biên', 'long bien', 'bồ đề', 'bo de', 'ngọc lâm', 'ngoc lam', 'việt hưng', 'viet hung', 'sài đồng', 'sai dong'], lat: 21.0420, lon: 105.8920 },
  { keywords: ['gia lâm', 'gia lam', 'đông anh', 'dong anh', 'thanh trì', 'thanh tri', 'thường tín', 'thuong tin'], lat: 21.0280, lon: 105.9400 },
  { keywords: ['thạch thất', 'thach that', 'quốc oai', 'quoc oai', 'mê linh', 'me linh', 'sóc sơn', 'soc son'], lat: 21.0000, lon: 105.5800 },
  { keywords: ['sơn tây', 'son tay', 'ba vì', 'ba vi', 'chương mỹ', 'chuong my'], lat: 21.1350, lon: 105.5050 },
  { keywords: ['hà nội', 'ha noi', 'hn'], lat: 21.0285, lon: 105.8542 },
  { keywords: ['vĩnh phúc', 'vinh phuc', 'vĩnh yên', 'vinh yen'], lat: 21.3089, lon: 105.6049 },
  { keywords: ['bắc ninh', 'bac ninh', 'từ sơn', 'tu son'], lat: 21.1861, lon: 106.0763 },
  { keywords: ['hưng yên', 'hung yen', 'phố nối', 'pho noi', 'văn giang', 'van giang', 'ecopark'], lat: 20.6464, lon: 106.0511 },
  { keywords: ['hải dương', 'hai duong'], lat: 20.9373, lon: 106.3150 },
  { keywords: ['hải phòng', 'hai phong', 'ngô quyền', 'hồng bàng', 'lê chân'], lat: 20.8449, lon: 106.6881 },
  { keywords: ['quảng ninh', 'quang ninh', 'hạ long', 'ha long', 'cẩm phả', 'cam pha', 'uông bí', 'uong bi'], lat: 20.9505, lon: 107.0734 },
  { keywords: ['hà nam', 'ha nam', 'phủ lý', 'phu ly'], lat: 20.5453, lon: 105.9124 },
  { keywords: ['nam định', 'nam dinh'], lat: 20.4344, lon: 106.1774 },
  { keywords: ['ninh bình', 'ninh binh'], lat: 20.2506, lon: 105.9745 },
  { keywords: ['thái nguyên', 'thai nguyen'], lat: 21.5942, lon: 105.8482 },
  { keywords: ['bắc giang', 'bac giang'], lat: 21.2731, lon: 106.1946 },
  { keywords: ['phú thọ', 'phu tho', 'việt trì', 'viet tri'], lat: 21.3227, lon: 105.4019 },
  { keywords: ['hòa bình', 'hoa binh'], lat: 20.8172, lon: 105.3376 },
  { keywords: ['thái bình', 'thai binh'], lat: 20.4463, lon: 106.3365 },
  { keywords: ['thanh hóa', 'thanh hoa', 'sầm sơn', 'sam son'], lat: 19.8067, lon: 105.7852 },
  { keywords: ['nghệ an', 'nghe an', 'vinh', 'cửa lò', 'cua lo'], lat: 18.6734, lon: 105.6813 },
  { keywords: ['hà tĩnh', 'ha tinh'], lat: 18.3429, lon: 105.9059 },
  { keywords: ['quảng bình', 'quang binh', 'đồng hới', 'dong hoi'], lat: 17.4739, lon: 106.6001 },
  { keywords: ['quảng trị', 'quang tri', 'đông hà', 'dong ha'], lat: 16.8163, lon: 107.1004 },
  { keywords: ['huế', 'hue', 'thừa thiên', 'thua thien'], lat: 16.4637, lon: 107.5909 },
  { keywords: ['đà nẵng', 'da nang', 'hải châu', 'thanh khê', 'sơn trà', 'ngũ hành sơn'], lat: 16.0544, lon: 108.2022 },
  { keywords: ['quảng nam', 'quang nam', 'hội an', 'hoi an'], lat: 15.5683, lon: 108.4831 },
  { keywords: ['quảng ngãi', 'quang ngai'], lat: 15.1213, lon: 108.7924 },
  { keywords: ['bình định', 'binh dinh', 'quy nhơn', 'quy nhon'], lat: 13.7820, lon: 109.2197 },
  { keywords: ['khánh hòa', 'khanh hoa', 'nha trang', 'cam ranh'], lat: 12.2388, lon: 109.1967 },
  { keywords: ['lâm đồng', 'lam dong', 'đà lạt', 'da lat'], lat: 11.9404, lon: 108.4583 },
  { keywords: ['đắk lắk', 'dak lak', 'buôn ma thuột', 'buon ma thuot'], lat: 12.6667, lon: 108.0500 },
  { keywords: ['hồ chí minh', 'ho chi minh', 'tp.hcm', 'tphcm', 'sài gòn', 'sai gon', 'quận 1', 'quận 2', 'quận 3', 'quận 4', 'quận 5', 'quận 6', 'quận 7', 'quận 8', 'quận 9', 'quận 10', 'quận 11', 'quận 12', 'bình thạnh', 'gò vấp', 'phú nhuận', 'tân bình', 'tân phú', 'bình tân', 'thủ đức', 'nhà bè', 'hóc môn', 'củ chi', 'bình chánh'], lat: 10.7769, lon: 106.7009 },
  { keywords: ['bình dương', 'binh duong', 'thủ dầu một', 'dĩ an', 'thuận an'], lat: 11.0000, lon: 106.6667 },
  { keywords: ['đồng nai', 'dong nai', 'biên hòa', 'bien hoa'], lat: 10.9574, lon: 106.8427 },
  { keywords: ['bà rịa', 'ba ria', 'vũng tàu', 'vung tau'], lat: 10.3460, lon: 107.0843 },
  { keywords: ['cần thơ', 'can tho', 'ninh kiều', 'an giang', 'kiên giang', 'cà mau'], lat: 10.0452, lon: 105.7469 },
];

const estimateCoordinatesFromAddress = (addr) => {
  if (!addr || typeof addr !== 'string') return null;
  const clean = addr.toLowerCase().trim();
  for (const item of VN_LOCATIONS) {
    for (const kw of item.keywords) {
      if (clean.includes(kw)) {
        return { lat: item.lat, lon: item.lon };
      }
    }
  }
  return null;
};

// Geocode địa chỉ sang tọa độ chính xác qua Nominatim
const geocodeAddress = async (addr) => {
  if (!addr || addr.trim().length < 3) return null;
  try {
    const url = `https://nominatim.openstreetmap.org/search?format=json&countrycodes=vn&limit=1&q=${encodeURIComponent(addr)}`;
    const res = await fetch(url);
    if (res.ok) {
      const list = await res.json();
      if (list && list.length > 0) {
        return { lat: Number(list[0].lat), lon: Number(list[0].lon) };
      }
    }
  } catch {}
  return estimateCoordinatesFromAddress(addr);
};

const explicitCoordsAddress = ref('');
let geocodeDebounceTimer = null;

const onLocationCoords = ({ lat, lon, address }) => {
  if (lat && lon) {
    recipientCoords.value = { lat, lon };
    explicitCoordsAddress.value = address || checkoutForm.diaChiGiaoHangText || '';
    const km = calculateDistanceKm(STORE_LOCATION.lat, STORE_LOCATION.lon, lat, lon);
    shippingDistanceKm.value = km;
    phiVanChuyenRef.value = calcFeeByDistance(km);
  }
};

// Phí vận chuyển
const phiVanChuyenRef = ref(0);
const phiVanChuyen = computed(() => phiVanChuyenRef.value);

const handleAddressChange = (addr) => {
  const cleanAddr = (addr || '').trim();
  if (!cleanAddr) {
    shippingDistanceKm.value = 0;
    phiVanChuyenRef.value = 30000;
    recipientCoords.value = null;
    explicitCoordsAddress.value = '';
    return;
  }

  // Nếu người dùng chọn tọa độ từ gợi ý bản đồ và địa chỉ trùng khớp chính xác
  if (recipientCoords.value && explicitCoordsAddress.value && explicitCoordsAddress.value === cleanAddr) {
    const km = calculateDistanceKm(STORE_LOCATION.lat, STORE_LOCATION.lon, recipientCoords.value.lat, recipientCoords.value.lon);
    shippingDistanceKm.value = km;
    phiVanChuyenRef.value = calcFeeByDistance(km);
    return;
  }

  // Khi người dùng thay đổi chữ trong ô địa chỉ -> xóa tọa độ cũ để không dùng nhầm tọa độ trước đó
  recipientCoords.value = null;
  explicitCoordsAddress.value = '';

  // 1. Phản hồi NGAY LẬP TỨC (0ms) từ từ điển địa danh VN (các quận huyện Hà Nội & tỉnh thành)
  const estimated = estimateCoordinatesFromAddress(cleanAddr);
  if (estimated) {
    const km = calculateDistanceKm(STORE_LOCATION.lat, STORE_LOCATION.lon, estimated.lat, estimated.lon);
    shippingDistanceKm.value = km;
    phiVanChuyenRef.value = calcFeeByDistance(km);
  }

  // 2. Debounce geocoding qua OpenStreetMap để lấy tọa độ GPS chính xác từng số nhà/con đường
  clearTimeout(geocodeDebounceTimer);
  shippingDistanceLoading.value = true;
  geocodeDebounceTimer = setTimeout(async () => {
    try {
      const coords = await geocodeAddress(cleanAddr);
      if (coords?.lat && coords?.lon) {
        recipientCoords.value = coords;
        explicitCoordsAddress.value = cleanAddr;
        const km = calculateDistanceKm(STORE_LOCATION.lat, STORE_LOCATION.lon, coords.lat, coords.lon);
        shippingDistanceKm.value = km;
        phiVanChuyenRef.value = calcFeeByDistance(km);
      } else if (!estimated) {
        shippingDistanceKm.value = 8.5;
        phiVanChuyenRef.value = 25000;
      }
    } catch {
      if (!estimated) {
        shippingDistanceKm.value = 8.5;
        phiVanChuyenRef.value = 25000;
      }
    } finally {
      shippingDistanceLoading.value = false;
    }
  }, 400);
};

const fetchShippingFee = () => {
  handleAddressChange(checkoutForm.diaChiGiaoHangText);
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

// Tự động điền thông tin tài khoản đăng nhập và khôi phục thông tin mua hàng trước đó
const fillFromLoggedInAccount = async () => {
  // Luôn xóa sạch key cũ dùng chung không phân biệt tài khoản để loại bỏ dữ liệu demo / rác
  try {
    localStorage.removeItem('saoclub_shipping_info');
  } catch {}

  // 1. Tự động lấy từ tài khoản đăng nhập hiện tại
  if (AuthStore.user) {
    const user = AuthStore.user;
    let full = null;
    if (user.id) {
      try {
        full = await KhachHangService.getById(user.id);
      } catch {}
    }

    // Họ tên
    const hoTen = user.hoTen || full?.hoTen || user.username || '';
    if (hoTen) {
      if (!checkoutForm.hoTen) checkoutForm.hoTen = hoTen;
      if (!checkoutForm.nguoiNhan) checkoutForm.nguoiNhan = hoTen;
    }

    // SĐT: ưu tiên số hợp lệ, loại trừ chuỗi google_<uid>
    const phoneCandidates = [user.soDienThoai, full?.soDienThoai, user.sdt, full?.sdt];
    const validPhone = phoneCandidates.find(p => isValidPhoneNumber(p));
    if (validPhone) {
      if (!checkoutForm.soDienThoai) checkoutForm.soDienThoai = validPhone;
      if (!checkoutForm.sdtNguoiNhan) checkoutForm.sdtNguoiNhan = validPhone;
    }

    // Email
    const email = user.email || full?.email || '';
    if (email && !checkoutForm.email) {
      checkoutForm.email = email;
    }

    // Địa chỉ nhận hàng
    const addr = user.diaChi || full?.diaChi || user.dia_chi || user.address || '';
    if (addr && !checkoutForm.diaChiGiaoHangText) {
      checkoutForm.diaChiGiaoHangText = addr;
    }

    foundCustomer.value = {
      khachHangId: user.id,
      hoTen: hoTen || 'Khách hàng',
    };

    // Kiểm tra xem chính tài khoản này trước đó đã từng lưu thông tin trên thiết bị này chưa
    try {
      const userSaved = JSON.parse(localStorage.getItem(`saoclub_shipping_info_${user.id}`) || 'null');
      if (userSaved) {
        if (!checkoutForm.sdtNguoiNhan && isValidPhoneNumber(userSaved.sdtNguoiNhan)) {
          checkoutForm.sdtNguoiNhan = userSaved.sdtNguoiNhan;
          if (!checkoutForm.soDienThoai) checkoutForm.soDienThoai = userSaved.sdtNguoiNhan;
        }
        if (!checkoutForm.diaChiGiaoHangText && userSaved.diaChiGiaoHangText) {
          checkoutForm.diaChiGiaoHangText = userSaved.diaChiGiaoHangText;
        }
        if (!checkoutForm.ghiChu && userSaved.ghiChu) {
          checkoutForm.ghiChu = userSaved.ghiChu;
        }
      }
    } catch {}
  } else {
    // 2. Chỉ dành riêng cho khách vãng lai (chưa đăng nhập tài khoản)
    try {
      const guestSaved = JSON.parse(localStorage.getItem('saoclub_guest_shipping_info') || 'null');
      if (guestSaved) {
        if (!checkoutForm.nguoiNhan && guestSaved.nguoiNhan) checkoutForm.nguoiNhan = guestSaved.nguoiNhan;
        if (!checkoutForm.sdtNguoiNhan && isValidPhoneNumber(guestSaved.sdtNguoiNhan)) {
          checkoutForm.sdtNguoiNhan = guestSaved.sdtNguoiNhan;
          if (!checkoutForm.soDienThoai) checkoutForm.soDienThoai = guestSaved.sdtNguoiNhan;
        }
        if (!checkoutForm.email && guestSaved.email) checkoutForm.email = guestSaved.email;
        if (!checkoutForm.diaChiGiaoHangText && guestSaved.diaChiGiaoHangText) checkoutForm.diaChiGiaoHangText = guestSaved.diaChiGiaoHangText;
        if (!checkoutForm.ghiChu && guestSaved.ghiChu) checkoutForm.ghiChu = guestSaved.ghiChu;
      }
    } catch {}
  }
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
  idempotencyKey.value     = genIdempotencyKey(); // Sinh key mới mỗi lần mở modal
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
  isEditingShipping.value = !checkoutForm.diaChiGiaoHangText?.trim() || !checkoutForm.sdtNguoiNhan?.trim() || !checkoutForm.nguoiNhan?.trim();
  handleAddressChange(checkoutForm.diaChiGiaoHangText);
});

watch(() => AuthStore.user, () => {
  if (props.modelValue) fillFromLoggedInAccount();
});

watch(() => checkoutForm.diaChiGiaoHangText, (newAddr) => {
  if (props.modelValue) {
    handleAddressChange(newAddr);
  }
});

watch(() => props.cartTotal, () => {
  if (props.modelValue) {
    fetchShippingFee();
  }
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
const goToPayment = async () => {
  checkoutError.value = '';
  validationWarning.value = '';
  phoneTouched.value = true;

  // Nếu đang ở chế độ chỉnh sửa thông tin giao hàng thì lưu trước
  if (isEditingShipping.value) {
    await saveShippingInfo();
    if (validationWarning.value) return;
  } else {
    await syncCustomerProfile();
  }

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
    let khachHangId = AuthStore.user?.id || AuthStore.user?.khachHangId || foundCustomer.value?.khachHangId;

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
    } else if (AuthStore.user?.role === 'khach_hang') {
      // Khách đăng nhập: Đồng bộ thông tin cá nhân vào DB và session
      await syncCustomerProfile();
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
      idempotencyKey:       idempotencyKey.value,   // ← Chống tạo đơn trùng
      phuongThucThanhToan:  selectedPayment.value,  // ← Lưu phương thức
      items: props.cart.map(item => ({
        bienTheId: item.bienTheId,
        soLuong:   item.quantity,
      })),
    };

    const orderRes = await DonHangService.checkoutComplete(orderBody);
    if (!orderRes.ok)
      throw new Error(await parseApiError(orderRes, t('checkout.createOrderError')));
    const createdOrder = await orderRes.json();

    // Lưu thông tin giao hàng thành công để tự động điền các lần sau
    try {
      const storageKey = AuthStore.user?.id ? `saoclub_shipping_info_${AuthStore.user.id}` : 'saoclub_guest_shipping_info';
      localStorage.setItem(storageKey, JSON.stringify({
        nguoiNhan: checkoutForm.nguoiNhan,
        sdtNguoiNhan: checkoutForm.sdtNguoiNhan,
        email: checkoutForm.email,
        diaChiGiaoHangText: checkoutForm.diaChiGiaoHangText,
        ghiChu: checkoutForm.ghiChu,
      }));
      localStorage.removeItem('saoclub_shipping_info');
    } catch {}

    checkoutFinalTotal.value = checkoutTotal.value;
    checkoutOrderId.value    = createdOrder.id;
    checkoutOrderCode.value  = createdOrder.maDonHang || `#${createdOrder.id}`;

    createdOrderObject.value = {
      ...createdOrder,
      id: createdOrder.id,
      maDonHang: createdOrder.maDonHang || checkoutOrderCode.value,
      thanhTien: checkoutTotal.value,
      tongTien: checkoutTotal.value,
      tamTinh: props.cartTotal,
      phiVanChuyen: phiVanChuyen.value,
      giamGia: checkoutGiamGia.value,
      nguoiNhan: checkoutForm.nguoiNhan,
      sdtNguoiNhan: checkoutForm.sdtNguoiNhan,
      email: checkoutForm.email,
      diaChiGiaoHangText: checkoutForm.diaChiGiaoHangText,
      ghiChu: checkoutForm.ghiChu,
      trangThaiThanhToan: 'unpaid',
      trangThaiDonHang: 'pending',
      items: props.cart.map(item => ({
        bienTheId: item.bienTheId,
        tenSanPham: item.tenSanPham,
        donGia: item.donGia ?? item.giaBan ?? 0,
        thanhTien: item.thanhTien ?? ((item.giaBan ?? item.donGia ?? 0) * (item.quantity || 1)),
        soLuong: item.quantity || 1,
        hinhAnh: item.hinhAnhChinh || item.hinhAnh,
      }))
    };

    emit('order-placed', createdOrderObject.value);

    if (selectedPayment.value === 'qr') {
      showQrModal.value = true;
    } else {
      checkoutSuccess.value = true;
    }
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
  padding: 5px 14px;
  border-radius: 24px;
  border: 1px solid var(--border-color-soft);
}
.stepper-step {
  transition: opacity 0.2s;
  cursor: pointer;
}
.step-circle {
  width: 26px;
  height: 26px;
  font-size: 11.5px;
  background: var(--bg-card-inset);
  color: var(--text-muted);
  border: 1px solid var(--border-color-soft);
  transition: all 0.2s ease;
}
.step-label {
  font-size: 12.5px;
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
  width: 28px;
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
