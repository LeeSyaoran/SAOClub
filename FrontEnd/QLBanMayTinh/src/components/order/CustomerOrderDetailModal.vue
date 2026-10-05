<template>
  <div
    class="customer-order-modal-backdrop position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background: rgba(15, 23, 42, 0.65); z-index: 1060; backdrop-filter: blur(6px)"
    @click.self="$emit('close')"
  >
    <div
      class="customer-order-modal d-flex flex-column shadow-2xl position-relative overflow-hidden"
      style="
        width: 880px;
        max-width: 96vw;
        max-height: 92vh;
        border-radius: 22px;
        background: var(--bg-card, #ffffff);
        border: 1px solid var(--border, #fed7aa);
        box-shadow: 0 25px 50px -12px rgba(15, 23, 42, 0.25);
      "
    >
      <!-- Header -->
      <div
        class="d-flex justify-content-between align-items-center px-4 py-3"
        style="border-bottom: 1px solid var(--border, #f1f5f9)"
        :style="
          isQrPayment(order)
            ? 'background:linear-gradient(135deg, #fff7ed 0%, #ffffff 100%);'
            : 'background:linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);'
        "
      >
        <div class="d-flex align-items-center gap-2.5">
          <div
            class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
            style="width: 38px; height: 38px"
            :style="
              isQrPayment(order)
                ? 'background:linear-gradient(135deg, #ea580c 0%, #f97316 100%); color:#fff;'
                : 'background:linear-gradient(135deg, #475569 0%, #64748b 100%); color:#fff;'
            "
          >
            <QrCode v-if="isQrPayment(order)" :size="20" />
            <Banknote v-else :size="20" />
          </div>
          <div>
            <div class="d-flex align-items-center gap-2">
              <span class="fw-bold" style="font-size: 1.1rem; color: #0f172a">
                Đơn hàng #{{ displayOrderCode }}
              </span>
              <span
                v-if="order?.kenhBan"
                class="badge"
                :style="{
                  background: channelColor(order.kenhBan).bg,
                  color: channelColor(order.kenhBan).text,
                  fontSize: '0.72rem',
                }"
              >
                {{ channelLabel(order.kenhBan) }}
              </span>
              <!-- Badge phân biệt phương thức thanh toán -->
              <span
                v-if="isQrPayment(order)"
                class="badge d-inline-flex align-items-center gap-1"
                style="
                  background: #fff7ed;
                  color: #ea580c;
                  border: 1px solid #fed7aa;
                  font-size: 0.72rem;
                  font-weight: 600;
                "
              >
                <QrCode :size="11" /> VietQR
              </span>
              <span
                v-else
                class="badge d-inline-flex align-items-center gap-1"
                style="
                  background: #f1f5f9;
                  color: #475569;
                  border: 1px solid #cbd5e1;
                  font-size: 0.72rem;
                  font-weight: 600;
                "
              >
                <Banknote :size="11" /> COD
              </span>
            </div>
            <div class="text-secondary small mt-0.5" style="font-size: 0.78rem">
              {{ formatPendingTime(order?.ngayDat) }} · {{ displayItems.length }} sản phẩm
            </div>
          </div>
        </div>

        <div class="d-flex align-items-center gap-2">
          <!-- Status pill: Luôn hiển thị cho cả đơn COD và QR -->
          <span
            class="badge d-inline-flex align-items-center gap-1.5 px-3 py-1.5 rounded-pill fw-bold"
            :style="{
              background: effectiveStatusStyle.bg,
              color: effectiveStatusStyle.text,
              border:
                isQrPayment(order) && order.trangThaiThanhToan === 'unpaid'
                  ? '1.5px solid #fed7aa'
                  : '1px solid rgba(0,0,0,0.06)',
              fontSize: '0.8rem',
            }"
          >
            <component :is="effectiveStatusIcon" :size="13" />
            {{ effectiveStatusLabel }}
          </span>

          <!-- Close X button -->
          <button
            type="button"
            class="btn-close-modal d-flex align-items-center justify-content-center"
            style="
              width: 32px;
              height: 32px;
              border-radius: 50%;
              background: #f8fafc;
              border: 1px solid #e2e8f0;
              color: #64748b;
              cursor: pointer;
            "
            title="Đóng"
            @click="$emit('close')"
          >
            <X :size="16" />
          </button>
        </div>
      </div>

      <!-- Body: 2 Columns -->
      <div class="d-flex flex-grow-1 overflow-hidden customer-order-modal-body">
        <!-- Cột trái: Chi tiết sản phẩm, Banner thanh toán & Tổng kết tài chính -->
        <div
          class="customer-order-left overflow-y-auto flex-grow-1 p-3 p-md-4"
          style="border-right: 1px solid #f1f5f9"
        >
          <!-- BANNER 1: THANH TOÁN QR NỔI BẬT (CHỈ hiển thị cho đơn thanh toán trước qua VietQR mà chưa thanh toán) -->
          <div
            v-if="isQrPayment(order) && order.trangThaiThanhToan !== 'paid'"
            class="rounded-4 p-3 mb-4 shadow-sm"
            style="border: 1.5px dashed #fbd38d; background: #fffdfa"
          >
            <div class="d-flex align-items-start justify-content-between flex-wrap gap-2 mb-3">
              <div class="d-flex align-items-center gap-2.5">
                <div
                  class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                  style="
                    width: 44px;
                    height: 44px;
                    background: #fff7ed;
                    border: 1.5px solid #ffedd5;
                    color: #ea580c;
                  "
                >
                  <DollarSign :size="24" stroke-width="2.5" />
                </div>
                <div>
                  <div class="fw-bold" style="font-size: 0.95rem; color: #0f172a">
                    Đang chờ thanh toán qua mã VietQR
                  </div>
                  <div class="text-secondary small" style="font-size: 0.78rem">
                    Ngân hàng <strong>Timo</strong> · STK: <strong>0338861232</strong> ·
                    <strong>LE HUY DO</strong>
                  </div>
                </div>
              </div>
              <div class="text-end">
                <div class="fw-extrabold" style="font-size: 1.35rem; color: #ea580c">
                  {{ formatPrice(orderTotalAmount) }}
                </div>
              </div>
            </div>

            <div
              class="d-flex align-items-center justify-content-between flex-wrap gap-2 pt-2 border-top"
              style="border-color: #ffedd5 !important"
            >
              <div
                class="text-muted small"
                style="font-size: 0.75rem; max-width: 320px; line-height: 1.35"
              >
                Quý khách quét mã và thanh toán, cửa hàng sẽ kiểm tra & duyệt đơn sang bước
                <strong>Chờ xử lý</strong>.
              </div>
              <!-- NÚT THANH TOÁN NGAY MÀU CAM NỔI BẬT (Chỉ cho đơn QR) -->
              <button
                type="button"
                class="btn btn-warning fw-extrabold text-white px-3.5 py-2 rounded-pill shadow-sm d-inline-flex align-items-center gap-2"
                style="
                  background: linear-gradient(135deg, #ea580c 0%, #f97316 100%);
                  border: none;
                  font-size: 0.88rem;
                  letter-spacing: 0.3px;
                  box-shadow: 0 6px 16px rgba(234, 88, 12, 0.3);
                "
                @click="openQrPaymentModal"
              >
                <CreditCard :size="16" />
                <span>THANH TOÁN NGAY</span>
              </button>
            </div>
          </div>

          <!-- BANNER 2: THÔNG BÁO ĐÃ THANH TOÁN QR & ĐANG CHỜ XỬ LÝ -->
          <div
            v-else-if="
              isQrPayment(order) &&
              order.trangThaiThanhToan === 'paid' &&
              order.trangThaiDonHang === 'pending'
            "
            class="rounded-3 p-3 mb-4 d-flex align-items-center gap-3"
            style="background: #f0fdf4; border: 1px solid #bbf7d0"
          >
            <div
              class="rounded-circle d-flex align-items-center justify-content-center text-white flex-shrink-0"
              style="width: 36px; height: 36px; background: #16a34a"
            >
              <Check :size="18" stroke-width="3" />
            </div>
            <div>
              <div class="fw-bold" style="font-size: 0.9rem; color: #15803d">
                Đã thanh toán VietQR thành công · Đang chờ xử lý
              </div>
              <div class="text-secondary small mt-0.5" style="font-size: 0.76rem">
                Cửa hàng đã xác nhận thanh toán. Nhân viên đang kiểm tra đơn hàng và chuẩn bị xuất
                kho.
              </div>
            </div>
          </div>

          <!-- BANNER 3: HƯỚNG DẪN ĐƠN THANH TOÁN SAU (COD / TIỀN MẶT) -->
          <div
            v-else-if="
              !isQrPayment(order) &&
              order.trangThaiDonHang !== 'delivered' &&
              !['cancelled', 'returned'].includes(order.trangThaiDonHang)
            "
            class="rounded-3 p-3 mb-4 shadow-sm"
            style="background: #f8fafc; border: 1.5px solid #cbd5e1"
          >
            <div class="d-flex align-items-start justify-content-between flex-wrap gap-2 mb-2">
              <div class="d-flex align-items-center gap-2.5">
                <div
                  class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                  style="width: 40px; height: 40px; background: #e2e8f0; color: #0f172a"
                >
                  <Banknote :size="22" stroke-width="2.2" />
                </div>
                <div>
                  <div class="fw-bold" style="font-size: 0.92rem; color: #0f172a">
                    Thanh toán khi nhận hàng (COD)
                  </div>
                  <div class="text-secondary small" style="font-size: 0.76rem">
                    Phương thức: <strong>Tiền mặt</strong> · Trả tiền cho Shipper khi giao hàng
                  </div>
                </div>
              </div>
              <div class="text-end">
                <div class="text-secondary small" style="font-size: 0.72rem">
                  Số tiền cần thanh toán
                </div>
                <div class="fw-extrabold" style="font-size: 1.25rem; color: #0f172a">
                  {{ formatPrice(orderTotalAmount) }}
                </div>
              </div>
            </div>
            <div
              class="text-secondary small pt-2 border-top"
              style="border-color: #e2e8f0 !important; font-size: 0.76rem; line-height: 1.45"
            >
              <template v-if="order.trangThaiDonHang === 'pending'">
                Đơn hàng đang chờ shop xác nhận và chuẩn bị sản phẩm. Quý khách vui lòng chuẩn bị
                tiền mặt khi nhận hàng (không cần chuyển khoản trước).
              </template>
              <template v-else-if="order.trangThaiDonHang === 'confirmed'">
                Đơn hàng đã được duyệt và lên đơn thành công. Shop đang chuẩn bị sản phẩm để chuyển
                sang đóng gói.
              </template>
              <template v-else-if="order.trangThaiDonHang === 'processing'">
                Kho đang chuẩn bị và đóng gói sản phẩm cẩn thận để bàn giao cho shipper.
              </template>
              <template
                v-else-if="
                  ['shipping', 'out_for_delivery', 'awaiting_confirmation'].includes(
                    order.trangThaiDonHang,
                  )
                "
              >
                Shipper đang trên đường giao hàng đến bạn. Quý khách vui lòng đồng kiểm và thanh
                toán tiền mặt cho nhân viên giao hàng.
              </template>
              <template v-else>
                Quý khách vui lòng thanh toán bằng tiền mặt cho nhân viên giao hàng khi nhận và đồng
                kiểm sản phẩm.
              </template>
            </div>
          </div>

          <!-- BANNER 4: ĐƠN HÀNG ĐÃ GIAO THÀNH CÔNG -->
          <div
            v-else-if="order.trangThaiDonHang === 'delivered'"
            class="rounded-3 p-3 mb-4 d-flex align-items-center gap-3"
            style="background: #f0fdf4; border: 1px solid #bbf7d0"
          >
            <div
              class="rounded-circle d-flex align-items-center justify-content-center text-white flex-shrink-0"
              style="width: 36px; height: 36px; background: #16a34a"
            >
              <Check :size="18" stroke-width="3" />
            </div>
            <div>
              <div class="fw-bold" style="font-size: 0.9rem; color: #15803d">
                Đơn hàng đã hoàn tất giao hàng thành công
              </div>
              <div class="text-secondary small mt-0.5" style="font-size: 0.76rem">
                Cảm ơn bạn đã tin tưởng và mua sắm tại SAOClub!
              </div>
            </div>
          </div>

          <!-- Danh sách sản phẩm -->
          <div class="mb-4">
            <div
              class="text-secondary fw-bold mb-2.5 text-uppercase"
              style="font-size: 0.72rem; letter-spacing: 0.05em"
            >
              SẢN PHẨM TRONG ĐƠN ({{ displayItems.length }})
            </div>

            <div v-if="loading" class="text-center py-4 text-secondary">
              <div class="spinner-border spinner-border-sm me-2 text-warning" role="status"></div>
              Đang tải danh sách sản phẩm...
            </div>

            <div v-else class="d-flex flex-column gap-2">
              <div
                v-for="item in displayItems"
                :key="item.id || item.chiTietId || item.bienTheId"
                class="d-flex align-items-start gap-3 p-2.5 rounded-3"
                style="background: #ffffff; border: 1px solid #e2e8f0"
              >
                <!-- Ảnh sản phẩm -->
                <div
                  style="
                    width: 54px;
                    height: 50px;
                    flex-shrink: 0;
                    background: #f8fafc;
                    border-radius: 8px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    overflow: hidden;
                    border: 1px solid #e2e8f0;
                  "
                >
                  <img
                    v-if="
                      getProduct(item.bienTheId)?.hinhAnhChinh || item.hinhAnh || item.hinhAnhChinh
                    "
                    :src="
                      getProduct(item.bienTheId)?.hinhAnhChinh || item.hinhAnh || item.hinhAnhChinh
                    "
                    style="max-width: 48px; max-height: 44px; object-fit: contain"
                  />
                  <Laptop v-else :size="24" style="color: #94a3b8" />
                </div>

                <!-- Thông tin SP -->
                <div class="flex-grow-1 min-w-0">
                  <div class="fw-bold text-truncate" style="font-size: 0.88rem; color: #0f172a">
                    {{
                      getProduct(item.bienTheId)?.tenSanPham ||
                      item.tenSanPham ||
                      "Sản phẩm SAOClub"
                    }}
                  </div>
                  <div
                    v-if="getItemSpecs(item)"
                    class="mt-0.5"
                    style="font-size: 0.74rem; color: #64748b"
                  >
                    {{ getItemSpecs(item) }}
                  </div>
                  <div
                    class="d-flex align-items-center gap-2 mt-1"
                    style="font-size: 0.72rem; color: #94a3b8"
                  >
                    <span v-if="item.maSku"
                      >SKU: <code style="color: #475569">{{ item.maSku }}</code></span
                    >
                    <span v-if="order?.trangThaiDonHang !== 'pending' && item.soSerial"
                      >· Serial:
                      <strong style="color: #ea580c; font-family: monospace">{{
                        item.soSerial
                      }}</strong></span
                    >
                  </div>
                </div>

                <!-- Giá + Số lượng -->
                <div class="text-end flex-shrink-0" style="min-width: 90px">
                  <div class="fw-bold font-monospace" style="font-size: 0.92rem; color: #ea580c">
                    {{ formatPrice(item.thanhTien ?? (item.donGia || 0) * (item.soLuong || 1)) }}
                  </div>
                  <div class="text-secondary small" style="font-size: 0.72rem">
                    {{ formatPrice(item.donGia) }} × {{ item.soLuong || 1 }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Tổng kết tài chính -->
          <div class="p-3 rounded-3 mb-4" style="background: #f8fafc; border: 1px solid #e2e8f0">
            <div
              class="d-flex justify-content-between mb-1"
              style="font-size: 0.82rem; color: #64748b"
            >
              <span>Tiền hàng:</span>
              <span class="text-dark">{{ formatPrice(order.tongTien) }}</span>
            </div>
            <div
              v-if="order.giamGia > 0"
              class="d-flex justify-content-between mb-1"
              style="font-size: 0.82rem; color: #16a34a"
            >
              <span>Giảm giá voucher:</span>
              <span>− {{ formatPrice(order.giamGia) }}</span>
            </div>
            <div
              v-if="order.kenhBan !== 'in_store'"
              class="d-flex justify-content-between mb-1"
              style="font-size: 0.82rem; color: #64748b"
            >
              <span>Phí vận chuyển:</span>
              <span :class="order.phiVanChuyen === 0 ? 'text-success fw-semibold' : 'text-dark'">
                {{ order.phiVanChuyen === 0 ? "Miễn phí" : formatPrice(order.phiVanChuyen) }}
              </span>
            </div>
            <div
              class="d-flex justify-content-between fw-bold pt-2 mt-2"
              style="border-top: 1px dashed #cbd5e1; font-size: 1rem"
            >
              <span style="color: #0f172a">Tổng thanh toán:</span>
              <span style="color: #ea580c; font-size: 1.2rem">
                {{ formatPrice(order.thanhTien ?? order.tongTien) }}
              </span>
            </div>
          </div>

          <!-- Lịch sử giao hàng (tracking) -->
          <div v-if="order.maVanDon || history.length" class="mb-3">
            <OrderTrackingLog :ma-van-don="order.maVanDon || ''" :history="history" />
          </div>

          <!-- Nút thao tác của khách hàng -->
          <div class="d-flex gap-2 flex-wrap justify-content-end pt-2">
            <button
              v-if="order.trangThaiDonHang === 'awaiting_confirmation'"
              class="btn btn-sm btn-success fw-bold d-inline-flex align-items-center gap-1.5 px-3 py-2"
              style="font-size: 0.85rem"
              @click="$emit('confirm-received', order)"
            >
              <CheckCircle2 :size="15" /> Xác nhận đã nhận hàng
            </button>

            <button
              v-if="order.trangThaiDonHang === 'delivered'"
              class="btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-1 px-3 py-2"
              style="font-size: 0.82rem"
              @click="$emit('buy-again', order)"
            >
              <RefreshCw :size="13" /> Mua lại đơn này
            </button>

            <button
              v-if="canReturn"
              class="btn btn-sm btn-outline-danger d-inline-flex align-items-center gap-1 px-3 py-2"
              style="font-size: 0.82rem"
              @click="$emit('request-return', order)"
            >
              <Undo2 :size="13" /> Yêu cầu đổi trả
            </button>
          </div>
        </div>

        <!-- Cột phải: 8 BƯỚC TIẾN TRÌNH THANH TOÁN QR (Hoặc quy trình chuẩn nếu không phải QR) -->
        <div
          class="customer-order-right p-3 p-md-4 overflow-y-auto"
          style="
            width: 320px;
            min-width: 320px;
            flex-shrink: 0;
            background: #f8fafc;
            border-left: 1px solid #e2e8f0;
          "
        >
          <!-- Trạng thái đơn -->
          <div class="mb-4">
            <div
              class="text-secondary fw-bold mb-2 text-uppercase"
              style="font-size: 0.7rem; letter-spacing: 0.06em"
            >
              {{ t("admin.orderDetailModal.orderStatus") }}
            </div>
            <span
              class="badge d-inline-flex align-items-center gap-1.5 px-2.5 py-1.5 rounded-pill"
              style="font-size: 0.82rem"
              :style="{
                background: effectiveStatusStyle.bg,
                color: effectiveStatusStyle.text,
                border:
                  isQrPayment(order) && order.trangThaiThanhToan === 'unpaid'
                    ? '1px solid #fed7aa'
                    : 'none',
              }"
            >
              <component :is="effectiveStatusIcon" :size="14" />
              {{ effectiveStatusLabel }}
            </span>
          </div>

          <!-- Tiến trình đơn hàng (5 Bước COD / 7 Bước QR) -->
          <div class="mb-4">
            <div class="d-flex align-items-center justify-content-between mb-3">
              <div
                class="text-secondary fw-bold text-uppercase"
                style="font-size: 0.7rem; letter-spacing: 0.06em"
              >
                {{ isQrPayment(order) ? "TIẾN TRÌNH THANH TOÁN QR" : "TIẾN TRÌNH THANH TOÁN SAU" }}
              </div>
            </div>

            <div class="d-flex flex-column gap-0" style="position: relative">
              <div
                v-for="(step, index) in orderTimelineSteps"
                :key="step.id"
                class="d-flex align-items-start gap-3 position-relative"
              >
                <!-- Đường kẻ nối dọc liền mạch giữa các bước -->
                <div
                  v-if="index < orderTimelineSteps.length - 1"
                  style="
                    position: absolute;
                    left: 15px;
                    top: 28px;
                    bottom: -2px;
                    width: 2px;
                    z-index: 0;
                  "
                  :style="
                    isStepDone(orderTimelineSteps[index + 1].id) ||
                    isStepNext(orderTimelineSteps[index + 1].id)
                      ? 'background:#16a34a;'
                      : 'background:#e2e8f0;'
                  "
                ></div>

                <!-- Cột icon tròn -->
                <div
                  class="d-flex flex-column align-items-center"
                  style="width: 32px; flex-shrink: 0; position: relative; z-index: 1"
                >
                  <div
                    class="rounded-circle d-flex align-items-center justify-content-center position-relative p-0"
                    style="width: 30px; height: 30px; z-index: 1"
                    :style="
                      isStepDone(step.id)
                        ? 'background:#16a34a; border:2px solid #16a34a; color:white;'
                        : isStepNext(step.id)
                          ? 'background:#ffffff; border:2.5px solid #ea580c; color:#ea580c; box-shadow:0 0 0 3px rgba(234, 88, 12, 0.2);'
                          : isStepReached(step.id) && !isStepDone(step.id) && !isStepNext(step.id)
                            ? 'background:#fff7ed; border:2px solid #fb923c; color:#ea580c;'
                            : 'background:#ffffff; border:2px solid #cbd5e1; color:#64748b;'
                    "
                  >
                    <Check v-if="isStepDone(step.id)" :size="14" stroke-width="3" color="white" />
                    <component
                      v-else
                      :is="step.icon"
                      :size="13"
                      :style="{
                        opacity:
                          isStepNext(step.id) || (isStepReached(step.id) && !isStepDone(step.id))
                            ? 1
                            : 0.35,
                        color: isStepNext(step.id)
                          ? '#ea580c'
                          : isStepReached(step.id) && !isStepDone(step.id)
                            ? '#ea580c'
                            : 'inherit',
                      }"
                    />
                  </div>
                </div>

                <!-- Label & mô tả -->
                <div class="flex-grow-1 pb-3" style="padding-top: 3px">
                  <div
                    class="fw-semibold"
                    style="font-size: 0.83rem; line-height: 1.3"
                    :style="
                      isStepNext(step.id)
                        ? 'color:#ea580c; font-weight:700;'
                        : isStepDone(step.id)
                          ? 'color:#0f172a;'
                          : isStepReached(step.id) && !isStepDone(step.id)
                            ? 'color:#ea580c;'
                            : 'color:#94a3b8;'
                    "
                  >
                    {{ step.title }}
                  </div>
                  <div
                    style="font-size: 0.71rem; color: #64748b; line-height: 1.35; margin-top: 2px"
                  >
                    {{ step.desc }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Thanh toán -->
          <div class="pt-3 mb-3" style="border-top: 1px solid #e2e8f0">
            <div
              class="text-secondary fw-bold mb-2 text-uppercase"
              style="font-size: 0.7rem; letter-spacing: 0.06em"
            >
              {{ t("admin.orderDetailModal.paymentStatus") }}
            </div>
            <div
              class="badge d-inline-flex align-items-center gap-1 px-2.5 py-1.5 rounded-pill"
              style="font-size: 0.8rem"
              :style="{
                background: paymentStatusColor(order.trangThaiThanhToan).bg,
                color: paymentStatusColor(order.trangThaiThanhToan).text,
              }"
            >
              <component :is="paymentStatusIcon(order.trangThaiThanhToan)" :size="13" />
              {{ order.trangThaiThanhToan ? paymentStatusLabel(order.trangThaiThanhToan) : "—" }}
            </div>

            <!-- Phương thức thanh toán -->
            <div class="mt-2 small text-secondary">
              <span class="d-block mb-1" style="font-size: 0.72rem">Phương thức:</span>
              <span style="color: #0f172a; font-size: 0.8rem; font-weight: 600">
                <template v-if="isQrPayment(order)">
                  <QrCode :size="13" style="vertical-align: -2px; color: #ea580c" /> Quét mã VietQR
                  (Timo)
                </template>
                <template v-else-if="paymentMethodSummary.length">
                  <template v-for="(g, idx) in paymentMethodSummary" :key="g.method">
                    <component
                      :is="paymentMethodIcon(g.method)"
                      :size="13"
                      style="vertical-align: -2px; color: #475569"
                    />
                    {{
                      g.method === "tien_mat"
                        ? "Tiền mặt (COD khi nhận hàng)"
                        : paymentMethodLabel(g.method)
                    }}
                    <span v-if="idx < paymentMethodSummary.length - 1">, </span>
                  </template>
                </template>
                <template v-else>
                  <Banknote :size="13" style="vertical-align: -2px; color: #475569" /> Tiền mặt (COD
                  khi nhận hàng)
                </template>
              </span>
            </div>
          </div>

          <!-- Thông tin khách hàng & Giao hàng -->
          <div class="pt-3" style="border-top: 1px solid #e2e8f0">
            <div
              class="text-secondary fw-bold mb-2 text-uppercase"
              style="font-size: 0.7rem; letter-spacing: 0.06em"
            >
              Thông tin nhận hàng
            </div>
            <div class="d-flex flex-column gap-2" style="font-size: 0.78rem">
              <div class="p-2 rounded-2" style="background: #ffffff; border: 1px solid #e2e8f0">
                <div
                  class="d-flex align-items-center gap-1.5 fw-semibold"
                  style="color: #0f172a; font-size: 0.82rem"
                >
                  <User :size="13" style="color: #ea580c" class="flex-shrink-0" />
                  <span>{{ customerName }}</span>
                </div>
                <div
                  v-if="customerPhone"
                  class="d-flex align-items-center gap-1.5 text-secondary mt-1"
                  style="font-size: 0.76rem"
                >
                  <Phone :size="11" class="flex-shrink-0" />
                  <span>{{ customerPhone }}</span>
                </div>
              </div>

              <div class="p-2 rounded-2" style="background: #ffffff; border: 1px solid #e2e8f0">
                <div
                  class="text-secondary fw-semibold mb-1 d-flex align-items-center gap-1"
                  style="font-size: 0.68rem; text-transform: uppercase"
                >
                  <MapPin :size="11" style="color: #ea580c" /> Địa chỉ nhận:
                </div>
                <div style="line-height: 1.4; color: #0f172a; font-size: 0.78rem">
                  {{ deliveryAddress }}
                </div>
              </div>
            </div>

            <!-- Khu vực hành động Hủy đơn hàng -->
            <div class="mt-3 pt-3 border-top" style="border-color: #e2e8f0 !important">
              <!-- Trường hợp 1: Đã gửi yêu cầu hủy và đang chờ admin duyệt -->
              <div
                v-if="order.yeuCauHuy"
                class="p-2.5 rounded-3 d-flex align-items-start gap-2"
                style="background: #fffbeb; border: 1px solid #fde68a; color: #92400e"
              >
                <Clock :size="16" class="flex-shrink-0 mt-0.5" style="color: #b45309" />
                <div style="font-size: 0.78rem">
                  <div class="fw-bold text-amber-900" style="color: #92400e">
                    Đang chờ cửa hàng duyệt hủy
                  </div>
                  <div v-if="order.lyDoHuy" class="mt-0.5 text-secondary" style="font-size: 0.74rem">
                    Lý do: {{ order.lyDoHuy }}
                  </div>
                </div>
              </div>

              <!-- Trường hợp 2: Đang ở trạng thái pending hoặc confirmed -> Được phép gửi yêu cầu hủy -->
              <template v-else-if="['pending', 'confirmed'].includes(order.trangThaiDonHang)">
                <button
                  type="button"
                  class="btn btn-outline-danger btn-sm w-100 py-2 d-inline-flex align-items-center justify-content-center gap-1.5 fw-semibold"
                  style="border-radius: 10px; font-size: 0.82rem"
                  @click="openCancelDialog"
                >
                  <XCircle :size="15" />
                  <span>Yêu cầu hủy đơn hàng</span>
                </button>
              </template>

              <!-- Trường hợp 3: Đã từ processing trở đi -> Nút mờ (disabled) theo đúng yêu cầu -->
              <template
                v-else-if="
                  ['processing', 'shipping', 'out_for_delivery', 'awaiting_confirmation', 'delivered'].includes(
                    order.trangThaiDonHang
                  )
                "
              >
                <button
                  type="button"
                  class="btn btn-outline-secondary btn-sm w-100 py-2 d-inline-flex align-items-center justify-content-center gap-1.5 fw-semibold opacity-50"
                  style="border-radius: 10px; font-size: 0.82rem; cursor: not-allowed"
                  disabled
                  title="Đơn hàng đã bắt đầu đóng gói / giao hàng, không thể hủy"
                >
                  <XCircle :size="15" />
                  <span>Hủy đơn hàng (Đang xử lý/giao)</span>
                </button>
                <div class="text-muted text-center mt-1" style="font-size: 0.7rem">
                  Đơn đã đóng gói/vận chuyển không thể hủy
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Yêu cầu hủy đơn hàng (Customer) -->
    <div
      v-if="showCancelModal"
      class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
      style="background: rgba(15, 23, 42, 0.65); z-index: 1075; backdrop-filter: blur(4px);"
      @click.self="closeCancelDialog"
    >
      <div
        class="bg-white rounded-4 shadow-xl overflow-hidden d-flex flex-column"
        style="width: 480px; max-width: 95vw; border: 1px solid #fed7aa;"
      >
        <!-- Header -->
        <div class="d-flex align-items-center justify-content-between px-4 py-3" style="background:#fff7ed; border-bottom:1px solid #ffedd5;">
          <div class="d-flex align-items-center gap-2">
            <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:36px; height:36px; background:#fee2e2; color:#ef4444;">
              <AlertTriangle :size="18" />
            </div>
            <div>
              <div class="fw-bold" style="color:#0f172a; font-size:0.95rem;">Yêu cầu hủy đơn hàng</div>
              <div class="text-secondary small" style="font-size:0.75rem;">
                Mã đơn: #{{ displayOrderCode }}
              </div>
            </div>
          </div>
          <button type="button" class="btn-close btn-sm" @click="closeCancelDialog"></button>
        </div>

        <!-- Body -->
        <div class="p-4">
          <div class="small text-secondary mb-3" style="font-size:0.82rem; line-height:1.45;">
            Lưu ý: Đơn hàng chỉ có thể hủy khi đang ở trạng thái <strong>Chờ xác nhận</strong> hoặc <strong>Đã lên đơn</strong>. Khi đơn chuyển sang <strong>Đang đóng gói</strong>, bạn sẽ không thể hủy nữa.
          </div>

          <label class="form-label fw-semibold small mb-2" style="color:#1e293b;">
            Vui lòng chọn lý do hủy đơn:
          </label>
          <div class="d-flex flex-column gap-2 mb-3">
            <label
              v-for="r in CANCEL_REASONS"
              :key="r.id"
              class="d-flex align-items-center gap-2.5 p-2.5 rounded-3 border"
              :style="cancelReasonType === r.id ? 'border-color:#ea580c; background:#fff7ed;' : 'border-color:#e2e8f0; cursor:pointer;'"
              @click="cancelReasonType = r.id"
            >
              <input type="radio" :value="r.id" v-model="cancelReasonType" style="accent-color:#ea580c;" />
              <span style="font-size:0.83rem; color:#0f172a;">{{ r.label }}</span>
            </label>
          </div>

          <div v-if="cancelReasonType === 'other'" class="mb-3">
            <label class="form-label small text-secondary mb-1">Nhập lý do chi tiết:</label>
            <textarea
              v-model="customCancelReason"
              class="form-control form-control-sm"
              rows="3"
              placeholder="Nhập lý do bạn muốn hủy đơn..."
              style="font-size:0.82rem; resize:none;"
            ></textarea>
          </div>
        </div>

        <!-- Footer -->
        <div class="d-flex align-items-center justify-content-end gap-2 px-4 py-3 bg-light border-top">
          <button type="button" class="btn btn-sm btn-outline-secondary px-3" @click="closeCancelDialog">
            Đóng
          </button>
          <button
            type="button"
            class="btn btn-sm btn-danger px-3.5 fw-bold d-inline-flex align-items-center gap-1.5"
            :disabled="cancelSubmitting"
            @click="submitCancelOrder"
          >
            <Loader2 v-if="cancelSubmitting" :size="14" class="spin" />
            <XCircle v-else :size="14" />
            {{ cancelSubmitting ? "Đang gửi..." : "Gửi yêu cầu hủy" }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Thanh Toán QR SePay (khi bấm THANH TOÁN NGAY từ đơn Pending) -->
    <QrPaymentModal
      v-model="showQrModal"
      :order="order"
      :items="displayItems"
      @close="showQrModal = false"
      @paid="onPaymentSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { AuthStore } from "../../stores/index.js";
import { t, I18nStore } from "../../i18n/index.js";
import {
  orderStatusLabel,
  orderStatusColor,
  orderStatusIcon,
  channelLabel,
  channelColor,
  paymentStatusLabel,
  paymentStatusColor,
  paymentStatusIcon,
  paymentMethodLabel,
  paymentMethodIcon,
  isQrPayment,
  QR_TIMELINE_STEPS,
  isQrStepReached,
  isQrStepDone,
  isQrStepCurrent,
  isQrStepNext,
  getQrEffectiveStatus,
  COD_TIMELINE_STEPS,
  isCodStepReached,
  isCodStepDone,
  isCodStepCurrent,
  isCodStepNext,
  getCodEffectiveStatus,
} from "../../utils/orderStatus.js";
import { formatPrice as formatPriceRaw } from "../../utils/formatPrice.js";
import * as ChiTietDonHangService from "../../services/ChiTietDonHangService.js";
import * as ThanhToanService from "../../services/ThanhToanService.js";
import * as DonHangService from "../../services/DonHangService.js";
import OrderTrackingLog from "./OrderTrackingLog.vue";
import QrPaymentModal from "../checkout/QrPaymentModal.vue";
import {
  Package,
  Laptop,
  CheckCircle2,
  RefreshCw,
  Undo2,
  Check,
  Clock,
  Truck,
  Bike,
  Inbox,
  User,
  Phone,
  MapPin,
  Mail,
  DollarSign,
  CreditCard,
  X,
  ChevronDown,
  ChevronUp,
  FileText,
  QrCode,
  Banknote,
  XCircle,
  AlertTriangle,
  Loader2,
} from "@lucide/vue";

const props = defineProps({
  order: { type: Object, required: true },
  items: { type: Array, default: () => [] },
  products: { type: Array, default: () => [] },
  history: { type: Array, default: () => [] },
});

const emit = defineEmits([
  "close",
  "confirm-received",
  "buy-again",
  "request-return",
  "order-updated",
]);

const showQrModal = ref(false);
const localItems = ref([]);
const payments = ref([]);
const loading = ref(false);
let customerOrderEventSource = null;

// ── Yêu cầu hủy đơn hàng ──────────────────────────────────────────
const showCancelModal = ref(false);
const cancelReasonType = ref("change_mind");
const customCancelReason = ref("");
const cancelSubmitting = ref(false);

const CANCEL_REASONS = [
  { id: "change_mind", label: "Tôi không còn nhu cầu mua nữa" },
  { id: "wrong_product", label: "Tôi đặt nhầm sản phẩm hoặc số lượng" },
  { id: "change_address", label: "Tôi muốn thay đổi thông tin nhận hàng" },
  { id: "change_payment", label: "Tôi muốn thay đổi phương thức thanh toán" },
  { id: "better_price", label: "Tìm thấy giá tốt hơn ở nơi khác" },
  { id: "other", label: "Lý do khác" }
];

const openCancelDialog = () => {
  showCancelModal.value = true;
  cancelReasonType.value = "change_mind";
  customCancelReason.value = "";
};

const closeCancelDialog = () => {
  showCancelModal.value = false;
  cancelReasonType.value = "change_mind";
  customCancelReason.value = "";
};

const submitCancelOrder = async () => {
  const oId = props.order?.donHangId || props.order?.id;
  if (!oId) return;

  let reason = "";
  if (cancelReasonType.value === "other") {
    reason = customCancelReason.value.trim();
    if (!reason) {
      alert("Vui lòng nhập lý do hủy đơn hàng");
      return;
    }
  } else {
    const found = CANCEL_REASONS.find(r => r.id === cancelReasonType.value);
    reason = found ? found.label : "Khách yêu cầu hủy đơn";
  }

  cancelSubmitting.value = true;
  try {
    const res = await DonHangService.yeuCauHuy(oId, reason);
    if (res?.error) {
      alert(res.error);
      return;
    }
    showCancelModal.value = false;
    emit("order-updated", {
      ...props.order,
      yeuCauHuy: true,
      lyDoHuy: reason,
      ngayYeuCauHuy: new Date().toISOString()
    });
  } catch (err) {
    alert(err.message || "Gửi yêu cầu hủy đơn thất bại");
  } finally {
    cancelSubmitting.value = false;
  }
};

const displayOrderCode = computed(() => {
  return (
    props.order?.maDonHang ||
    props.order?.maDon ||
    (props.order?.id ? `as_${props.order.id}` : "11622")
  );
});

const orderTotalAmount = computed(() => {
  return Number(props.order?.thanhTien ?? props.order?.tongTien ?? 0);
});

// Định dạng thời gian hiển thị: "LÚC 15:26 24 THÁNG 9, 2026"
const formatPendingTime = (dateStr) => {
  if (!dateStr) return "LÚC 15:26 24 THÁNG 9, 2026";
  try {
    const d = new Date(dateStr);
    const hours = String(d.getHours()).padStart(2, "0");
    const minutes = String(d.getMinutes()).padStart(2, "0");
    const day = d.getDate();
    const month = d.getMonth() + 1;
    const year = d.getFullYear();
    return `LÚC ${hours}:${minutes} ${day} THÁNG ${month}, ${year}`;
  } catch {
    return String(dateStr);
  }
};

const customerName = computed(() => {
  return (
    props.order.nguoiNhan || props.order.khachHangHoTen || AuthStore.user?.hoTen || "Khách hàng"
  );
});

const customerPhone = computed(() => {
  return props.order.sdtNguoiNhan || props.order.khachHangSdt || AuthStore.user?.soDienThoai || "";
});

const customerEmail = computed(() => {
  return props.order.khachHangEmail || AuthStore.user?.email || "";
});

const deliveryAddress = computed(() => {
  if (props.order.diaChiGiaoHangText) return props.order.diaChiGiaoHangText;
  if (props.order.khachHangDiaChi) return props.order.khachHangDiaChi;
  if (AuthStore.user?.diaChi) return AuthStore.user.diaChi;
  if (props.order.kenhBan === "in_store") return "Khách nhận trực tiếp tại quầy";
  return "Chưa có địa chỉ giao hàng";
});

const displayItems = computed(() => {
  return props.items.length ? props.items : localItems.value;
});

const getProduct = (bienTheId) => {
  return props.products.find((p) => p.bienTheId === bienTheId);
};

const getItemSpecs = (item) => {
  const p = getProduct(item.bienTheId);
  const parts = [
    p?.cpu || item.cpu,
    p?.ram || item.ram,
    p?.oCung || item.oCung,
    p?.mauSac || item.mauSac,
  ].filter(Boolean);
  return parts.length ? parts.join(" · ") : "";
};

const orderTimelineSteps = computed(() => {
  if (props.order?.kenhBan === "in_store") {
    return [
      {
        id: "delivered",
        title: t("orderStatus.timeline.deliveredTitle"),
        desc:
          t("orderStatus.timeline.inStoreDeliveredDesc") || t("orderStatus.timeline.deliveredDesc"),
        icon: CheckCircle2,
      },
    ];
  }
  // Nếu là đơn thanh toán qua mã QR: hiển thị 7 bước đặc thù
  if (isQrPayment(props.order)) {
    return QR_TIMELINE_STEPS;
  }
  // Đơn thanh toán sau (COD): hiển thị 5 bước
  return COD_TIMELINE_STEPS;
});

const isStepReached = (stepId) => {
  if (props.order?.kenhBan === "in_store") {
    return !["cancelled", "returned"].includes(props.order.trangThaiDonHang);
  }
  if (isQrPayment(props.order)) {
    return isQrStepReached(props.order, stepId);
  }
  return isCodStepReached(props.order, stepId);
};

const isStepDone = (stepId) => {
  if (props.order?.kenhBan === "in_store") {
    return !["cancelled", "returned"].includes(props.order.trangThaiDonHang);
  }
  if (isQrPayment(props.order)) {
    return isQrStepDone(props.order, stepId);
  }
  return isCodStepDone(props.order, stepId);
};

const isStepCurrent = (stepId) => {
  if (props.order?.kenhBan === "in_store") return stepId === "delivered";
  if (isQrPayment(props.order)) {
    return isQrStepCurrent(props.order, stepId);
  }
  return isCodStepCurrent(props.order, stepId);
};

const isStepNext = (stepId) => {
  if (props.order?.kenhBan === "in_store") return false;
  if (isQrPayment(props.order)) {
    return isQrStepNext(props.order, stepId);
  }
  return isCodStepNext(props.order, stepId);
};

const effectiveStatus = computed(() => {
  if (
    props.order?.kenhBan === "in_store" &&
    !["cancelled", "returned"].includes(props.order?.trangThaiDonHang)
  ) {
    return "delivered";
  }
  return props.order?.trangThaiDonHang || props.order?.trangThai;
});

const effectiveStatusLabel = computed(() => {
  if (isQrPayment(props.order)) {
    return getQrEffectiveStatus(props.order).label;
  }
  return getCodEffectiveStatus(props.order).label;
});

const effectiveStatusStyle = computed(() => {
  if (isQrPayment(props.order)) {
    return getQrEffectiveStatus(props.order).color;
  }
  return getCodEffectiveStatus(props.order).color;
});

const effectiveStatusIcon = computed(() => {
  return orderStatusIcon(props.order?.trangThaiDonHang);
});

const paymentMethodSummary = computed(() => {
  if (!payments.value.length) {
    if (props.order?.phuongThucThanhToan) {
      return [{ method: props.order.phuongThucThanhToan, count: 1 }];
    }
    return [];
  }
  const map = new Map();
  for (const p of payments.value) {
    if (!p.phuongThucThanhToan) continue;
    const cur = map.get(p.phuongThucThanhToan) ?? { method: p.phuongThucThanhToan, count: 0 };
    cur.count += 1;
    map.set(p.phuongThucThanhToan, cur);
  }
  return [...map.values()];
});

const canReturn = computed(() => {
  if (props.order?.trangThaiDonHang !== "delivered" || !props.order?.ngayGiaoThucTe) return false;
  return Date.now() <= new Date(props.order.ngayGiaoThucTe).getTime() + 7 * 24 * 60 * 60 * 1000;
});

const formatPrice = (v) => (v == null ? "—" : formatPriceRaw(v));
const formatDate = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleString(I18nStore.locale);
  } catch {
    return d;
  }
};

const openQrPaymentModal = () => {
  showQrModal.value = true;
};

const onPaymentSuccess = () => {
  // Không tự đặt trangThaiThanhToan = "paid" ở client: nhân viên sẽ xác nhận khi nhận được tiền,
  // trạng thái thật được đồng bộ lại từ server.
  emit("order-updated", props.order);
};

onMounted(async () => {
  const orderId = props.order?.id || props.order?.donHangId;
  if (!props.items.length && orderId) {
    loading.value = true;
    try {
      localItems.value = await ChiTietDonHangService.getByDonHang(orderId).catch(() => []);
    } finally {
      loading.value = false;
    }
  }
  if (orderId) {
    payments.value = await ThanhToanService.getByDonHang(orderId).catch(() => []);
  }

  // SSE realtime: khi đơn được cập nhật (admin duyệt, chuyển trạng thái...) → reload props.order
  if (orderId) {
    customerOrderEventSource = new EventSource("/api/don-hang/events");
    customerOrderEventSource.addEventListener("order-updated", async (e) => {
      // Chỉ refresh nếu event chứa id đơn đang xem
      const data = e?.data;
      try {
        const eventId = data ? Number(data) : null;
        if (eventId && eventId !== orderId) return;
      } catch (_) {
        /* nếu payload không phải số, refresh luôn */
      }
      try {
        const khId = props.order?.khachHangId;
        if (!khId) return;
        const list = await DonHangService.getByKhachHang(khId);
        const updated = (Array.isArray(list) ? list : []).find(
          (o) => (o.id || o.donHangId) === orderId,
        );
        if (updated) emit("order-updated", updated);
        localItems.value = await ChiTietDonHangService.getByDonHang(orderId).catch(
          () => localItems.value,
        );
      } catch (_) {
        /* bỏ qua */
      }
    });
  }
});

onUnmounted(() => {
  if (customerOrderEventSource) {
    customerOrderEventSource.close();
    customerOrderEventSource = null;
  }
});
</script>

<style scoped>
.btn-close-modal:hover {
  background: #fee2e2 !important;
  color: #ef4444 !important;
  transform: rotate(90deg);
}
@media (max-width: 768px) {
  .customer-order-modal-body {
    flex-direction: column !important;
  }
  .customer-order-right {
    width: 100% !important;
    min-width: 100% !important;
    border-left: none !important;
    border-top: 1px solid #e2e8f0;
  }
}
</style>
