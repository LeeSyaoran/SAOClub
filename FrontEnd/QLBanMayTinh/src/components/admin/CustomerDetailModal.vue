<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as KhachHangService from "../../services/KhachHangService.js";
import * as DonHangService from "../../services/DonHangService.js";
import { refreshCustomers } from "../../stores/customers.js";
import { showToast } from "../../stores/toast.js";
import { formatPrice, formatDate, statusLabel } from "../../utils/adminFormat.js";
import { orderStatusLabel, orderStatusColor } from "../../utils/orderStatus.js";

const props = defineProps({
  customer: { type: Object, required: true },
});
const emit = defineEmits(["close", "view-order"]);

// ── Avatar helpers ────────────────────────────────────────────────────────────
const getAvatarUrl = (c) => c?.hinhAnh || c?.avatarUrl || null;
const getInitials = (c) => {
  const name = c?.hoTen || 'K';
  const parts = name.trim().split(' ');
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  return name.substring(0, 2).toUpperCase();
};
const getAvatarBgColor = (c) => {
  const colors = [
    '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
    '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9'
  ];
  const name = c?.hoTen || '';
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
};

// ── Tab state ────────────────────────────────────────────────────────────────
const activeTab = ref('info');
const TABS = [
  { id: 'info',   icon: 'fa-user',     label: 'Thông tin khách hàng' },
  { id: 'orders', icon: 'fa-shopping-bag', label: 'Đơn hàng' },
  { id: 'chat',   icon: 'fa-comments', label: 'Chat' },
];

// ── Info tab: edit mode ───────────────────────────────────────────────────────
const editingInfo = ref(false);
const infoForm = ref({});
const infoFormError = ref("");
const savingInfo = ref(false);

const emptyInfoForm = () => ({
  hoTen: "", soDienThoai: "", email: "", diaChi: "",
  loaiKhach: "ca_nhan", tenCongTy: "", maSoThue: "",
  diemTichLuy: 0, trangThai: "active",
});

const startEditInfo = () => {
  infoForm.value = { ...emptyInfoForm(), ...props.customer };
  infoFormError.value = "";
  editingInfo.value = true;
};
const cancelEditInfo = () => {
  editingInfo.value = false;
  infoFormError.value = "";
};
const saveInfo = async () => {
  infoFormError.value = "";
  if (!infoForm.value.hoTen?.trim()) { infoFormError.value = "Vui lòng nhập họ tên"; return; }
  if (!infoForm.value.soDienThoai?.trim()) { infoFormError.value = "Vui lòng nhập số điện thoại"; return; }
  if (!infoForm.value.diaChi?.trim()) { infoFormError.value = "Vui lòng nhập địa chỉ"; return; }
  if (savingInfo.value) return;
  savingInfo.value = true;
  const body = {
    hoTen: infoForm.value.hoTen, soDienThoai: infoForm.value.soDienThoai,
    email: infoForm.value.email ?? "", diaChi: infoForm.value.diaChi ?? "",
    loaiKhach: infoForm.value.loaiKhach ?? "ca_nhan",
    tenCongTy: infoForm.value.tenCongTy ?? "", maSoThue: infoForm.value.maSoThue ?? "",
    diemTichLuy: Number(infoForm.value.diemTichLuy ?? 0),
    trangThai: infoForm.value.trangThai ?? "active",
  };
  try {
    const res = await KhachHangService.save(props.customer.khachHangId, body);
    if (!res.ok) {
      infoFormError.value = `Lỗi ${res.status}: ${await res.text()}`;
      return;
    }
    await refreshCustomers();
    editingInfo.value = false;
    showToast("Cập nhật khách hàng thành công", "success");
  } catch (e) {
    infoFormError.value = e.message;
  } finally {
    savingInfo.value = false;
  }
};

// ── Orders tab ───────────────────────────────────────────────────────────────
const orders = ref([]);
const ordersLoading = ref(false);
const expandedOrder = ref(null); // id đơn đang mở inline detail

const loadOrders = async () => {
  ordersLoading.value = true;
  try {
    const list = await DonHangService.getByKhachHang(props.customer.khachHangId);
    orders.value = (list || []).sort((a, b) => new Date(b.ngayDat) - new Date(a.ngayDat));
  } catch (e) {
    orders.value = [];
  } finally {
    ordersLoading.value = false;
  }
};

// ── Chat tab (mock) ──────────────────────────────────────────────────────────
const chatMessages = ref([
  { side: 'left',  name: props.customer.hoTen || 'Khách', text: 'Xin chào shop, tôi muốn hỏi về đơn hàng gần đây.', time: '09:30' },
  { side: 'right', name: 'CSKH',                          text: 'Chào bạn, mình đang tra cứu đơn của bạn nhé!',     time: '09:31' },
]);
const chatInput = ref('');
const sendChat = () => {
  const text = chatInput.value.trim();
  if (!text) return;
  chatMessages.value.push({
    side: 'right',
    name: 'CSKH',
    text,
    time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
  });
  chatInput.value = '';
};

onMounted(() => { loadOrders(); });
</script>

<template>
  <div class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center cdm-backdrop" @click.self="emit('close')">
    <div class="cdm-shell">
      <!-- ── Header ── -->
      <div class="cdm-header">
        <div class="cdm-header-left">
          <div v-if="getAvatarUrl(customer)" class="cdm-avatar cdm-avatar--img">
            <img :src="getAvatarUrl(customer)" :alt="customer.hoTen" />
          </div>
          <div v-else class="cdm-avatar cdm-avatar--initials" :style="{ background: getAvatarBgColor(customer) }">
            {{ getInitials(customer) }}
          </div>
          <div>
            <div class="cdm-name">{{ customer.hoTen || 'Khách hàng' }}</div>
            <div class="cdm-contact">
              <i class="fa fa-phone"></i> {{ customer.soDienThoai || '—' }}
              <span class="mx-2">·</span>
              <i class="fa fa-envelope"></i> {{ customer.email || '—' }}
            </div>
            <div class="cdm-tags">
              <span class="cdm-badge" :class="customer.trangThai === 'active' ? 'is-active' : 'is-locked'">
                {{ statusLabel(customer.trangThai) }}
              </span>
              <span class="cdm-badge is-soft">{{ customer.loaiKhach || 'ca_nhan' }}</span>
              <span v-if="customer.diemTichLuy" class="cdm-badge is-soft">
                <i class="fa fa-star"></i> {{ customer.diemTichLuy }} điểm
              </span>
            </div>
          </div>
        </div>
        <button class="cdm-close" :aria-label="t('common.close')" @click="emit('close')">
          <i class="fa fa-times"></i>
        </button>
      </div>

      <!-- ── Tabs ── -->
      <div class="cdm-tabs">
        <button
          v-for="tab in TABS" :key="tab.id"
          class="cdm-tab"
          :class="{ 'is-active': activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          <i :class="['fa', tab.icon]"></i>
          <span>{{ tab.label }}</span>
          <span v-if="tab.id === 'orders' && orders.length" class="cdm-tab-badge">{{ orders.length }}</span>
        </button>
      </div>

      <!-- ── Tab Content ── -->
      <div class="cdm-body">
        <!-- ============ INFO ============ -->
        <div v-if="activeTab === 'info'" class="cdm-tab-pane">
          <!-- Read mode -->
          <div v-if="!editingInfo" class="info-view">
            <div class="info-grid">
              <div class="info-row"><span class="info-label">Họ tên</span><span class="info-value">{{ customer.hoTen || '—' }}</span></div>
              <div class="info-row"><span class="info-label">Số điện thoại</span><span class="info-value">{{ customer.soDienThoai || '—' }}</span></div>
              <div class="info-row"><span class="info-label">Email</span><span class="info-value">{{ customer.email || '—' }}</span></div>
              <div class="info-row"><span class="info-label">Loại khách</span><span class="info-value">{{ customer.loaiKhach === 'doanh_nghiep' ? 'Doanh nghiệp' : 'Cá nhân' }}</span></div>
              <div class="info-row"><span class="info-label">Địa chỉ</span><span class="info-value">{{ customer.diaChi || '—' }}</span></div>
              <div class="info-row"><span class="info-label">Tên công ty</span><span class="info-value">{{ customer.tenCongTy || '—' }}</span></div>
              <div class="info-row"><span class="info-label">Mã số thuế</span><span class="info-value">{{ customer.maSoThue || '—' }}</span></div>
              <div class="info-row"><span class="info-label">Điểm tích lũy</span><span class="info-value">{{ customer.diemTichLuy ?? 0 }}</span></div>
              <div class="info-row"><span class="info-label">Số dư ví</span><span class="info-value">{{ customer.soDuVi ? formatPrice(Number(customer.soDuVi)) : '0 ₫' }}</span></div>
              <div class="info-row"><span class="info-label">Trạng thái</span><span class="info-value">{{ statusLabel(customer.trangThai) }}</span></div>
            </div>
            <div class="info-actions">
              <button class="cdm-btn cdm-btn--primary" @click="startEditInfo">
                <i class="fa fa-pen"></i> Chỉnh sửa
              </button>
            </div>
          </div>

          <!-- Edit mode -->
          <div v-else class="info-edit">
            <div v-if="infoFormError" class="alert alert-danger small py-2 mb-3">{{ infoFormError }}</div>
            <div class="row g-3">
              <div class="col-md-6"><label class="form-label small text-secondary">Họ tên *</label><input v-model="infoForm.hoTen" class="form-control form-control-sm cdm-input" /></div>
              <div class="col-md-6"><label class="form-label small text-secondary">Số điện thoại *</label><input v-model="infoForm.soDienThoai" class="form-control form-control-sm cdm-input" /></div>
              <div class="col-md-6"><label class="form-label small text-secondary">Email</label><input v-model="infoForm.email" type="email" class="form-control form-control-sm cdm-input" /></div>
              <div class="col-md-6"><label class="form-label small text-secondary">Loại khách</label>
                <select v-model="infoForm.loaiKhach" class="form-select form-select-sm cdm-input">
                  <option value="ca_nhan">Cá nhân</option>
                  <option value="doanh_nghiep">Doanh nghiệp</option>
                </select>
              </div>
              <div class="col-12"><label class="form-label small text-secondary">Địa chỉ *</label><input v-model="infoForm.diaChi" class="form-control form-control-sm cdm-input" /></div>
              <div class="col-md-6"><label class="form-label small text-secondary">Tên công ty</label><input v-model="infoForm.tenCongTy" class="form-control form-control-sm cdm-input" /></div>
              <div class="col-md-6"><label class="form-label small text-secondary">Mã số thuế</label><input v-model="infoForm.maSoThue" class="form-control form-control-sm cdm-input" /></div>
              <div class="col-md-6"><label class="form-label small text-secondary">Điểm tích lũy</label><input v-model="infoForm.diemTichLuy" type="number" min="0" class="form-control form-control-sm cdm-input" /></div>
              <div class="col-md-6"><label class="form-label small text-secondary">Trạng thái</label>
                <select v-model="infoForm.trangThai" class="form-select form-select-sm cdm-input">
                  <option value="active">Hoạt động</option>
                  <option value="inactive">Bị khóa</option>
                </select>
              </div>
            </div>
            <div class="info-actions">
              <button class="cdm-btn cdm-btn--ghost" @click="cancelEditInfo">Hủy</button>
              <button class="cdm-btn cdm-btn--primary" :disabled="savingInfo" @click="saveInfo">
                <i class="fa fa-save"></i> {{ savingInfo ? 'Đang lưu...' : 'Lưu thay đổi' }}
              </button>
            </div>
          </div>
        </div>

        <!-- ============ ORDERS ============ -->
        <div v-if="activeTab === 'orders'" class="cdm-tab-pane">
          <div v-if="ordersLoading" class="text-secondary small text-center py-4">Đang tải đơn hàng...</div>
          <div v-else-if="orders.length === 0" class="cdm-empty">
            <i class="fa fa-shopping-bag"></i>
            <p>Khách hàng chưa có đơn hàng nào.</p>
          </div>
          <div v-else class="orders-list">
            <div
              v-for="o in orders" :key="o.donHangId"
              class="order-row"
              :class="{ 'is-open': expandedOrder === o.donHangId }"
              @click="expandedOrder = expandedOrder === o.donHangId ? null : o.donHangId"
            >
              <div class="order-head">
                <div class="order-head-left">
                  <span class="order-code">{{ o.maDonHang || o.maDon || '#' + o.donHangId }}</span>
                  <span class="order-date"><i class="fa fa-clock"></i> {{ formatDate(o.ngayDat) }}</span>
                </div>
                <div class="order-head-right">
                  <span class="order-total">{{ formatPrice(o.thanhTien) }}</span>
                  <span class="order-status" :style="{ background: orderStatusColor(o.trangThaiDonHang).bg, color: orderStatusColor(o.trangThaiDonHang).text }">
                    {{ orderStatusLabel(o.trangThaiDonHang) }}
                  </span>
                  <i class="fa" :class="expandedOrder === o.donHangId ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
                </div>
              </div>
              <div v-if="expandedOrder === o.donHangId" class="order-detail" @click.stop>
                <div v-if="o.tenSanPham || o.danhSachSanPham">
                  <div class="order-detail-title">Sản phẩm</div>
                  <div class="order-products">{{ o.tenSanPham || o.danhSachSanPham }}</div>
                </div>
                <div class="order-detail-grid">
                  <div><span class="info-label">Phương thức TT</span><span class="info-value">{{ o.phuongThucThanhToan || '—' }}</span></div>
                  <div><span class="info-label">Địa chỉ giao</span><span class="info-value">{{ o.diaChiGiaoHang || '—' }}</span></div>
                  <div><span class="info-label">Phí ship</span><span class="info-value">{{ formatPrice(o.phiVanChuyen || 0) }}</span></div>
                  <div><span class="info-label">Giảm giá</span><span class="info-value">{{ formatPrice(o.giamGia || 0) }}</span></div>
                </div>
                <div class="order-detail-actions">
                  <button class="cdm-btn cdm-btn--ghost cdm-btn--sm" @click.stop="expandedOrder = null">
                    <i class="fa fa-times"></i> Đóng
                  </button>
                  <button class="cdm-btn cdm-btn--primary cdm-btn--sm" @click.stop="emit('view-order', o)">
                    <i class="fa fa-external-link-alt"></i> Xem chi tiết
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ============ CHAT ============ -->
        <div v-if="activeTab === 'chat'" class="cdm-tab-pane chat-tab">
          <div class="chat-window">
            <div class="chat-header">
              <div class="chat-avatar-mini">
                <div v-if="getAvatarUrl(customer)" class="cdm-avatar-mini cdm-avatar-mini--img">
                  <img :src="getAvatarUrl(customer)" :alt="customer.hoTen" />
                </div>
                <div v-else class="cdm-avatar-mini cdm-avatar-mini--initials" :style="{ background: getAvatarBgColor(customer) }">
                  {{ getInitials(customer) }}
                </div>
              </div>
              <div>
                <div class="chat-title">Chat với {{ customer.hoTen || 'khách' }}</div>
                <div class="chat-sub">CSKH trực tuyến</div>
              </div>
            </div>
            <div class="chat-body">
              <div v-for="(m, idx) in chatMessages" :key="idx" class="chat-message" :class="m.side">
                <div class="chat-bubble">
                  <span class="chat-bubble-name">{{ m.name }}</span>
                  <span class="chat-bubble-text">{{ m.text }}</span>
                  <span class="chat-bubble-time">{{ m.time }}</span>
                </div>
              </div>
            </div>
            <div class="chat-composer">
              <textarea v-model="chatInput" rows="2" placeholder="Nhắn với khách hàng..." @keyup.enter="sendChat"></textarea>
              <button class="cdm-btn cdm-btn--primary cdm-btn--sm" @click="sendChat">
                <i class="fa fa-paper-plane"></i> Gửi
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ═══ Backdrop & Shell ═══ */
.cdm-backdrop {
  background: var(--bg-overlay, rgba(0,0,0,0.5));
  z-index: 1000;
}
.cdm-shell {
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color-strong);
  border-radius: 16px;
  width: 720px;
  max-width: 95vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}

/* ═══ Header ═══ */
.cdm-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border-color-soft, #f1dbe6);
  background: linear-gradient(180deg, var(--pink-50, #fff5f9) 0%, #fff 100%);
}
.cdm-header-left {
  display: flex;
  gap: 14px;
  align-items: center;
}
.cdm-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 3px 10px rgba(168, 27, 93, 0.25);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}
.cdm-avatar--img img { width: 100%; height: 100%; object-fit: cover; }
.cdm-name { font-size: 18px; font-weight: 700; color: var(--ink, #1f2937); }
.cdm-contact {
  font-size: 13px;
  color: var(--muted, #6b7280);
  margin-top: 2px;
}
.cdm-tags {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.cdm-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 10px;
}
.cdm-badge.is-active { background: var(--ok-bg, #ecfdf5); color: var(--ok-text, #047857); }
.cdm-badge.is-locked { background: #fee2e2; color: #b91c1c; }
.cdm-badge.is-soft   { background: var(--pink-100, #ffe6f0); color: var(--pink-700, #a81b5d); }
.cdm-close {
  background: transparent;
  border: none;
  font-size: 18px;
  color: var(--muted, #6b7280);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s ease;
}
.cdm-close:hover { background: var(--pink-100, #ffe6f0); color: var(--pink-700, #a81b5d); }

/* ═══ Tabs ═══ */
.cdm-tabs {
  display: flex;
  gap: 4px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-color-soft, #f1dbe6);
  background: #fff;
}
.cdm-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: none;
  background: transparent;
  padding: 12px 16px;
  font-weight: 600;
  font-size: 13px;
  color: var(--muted, #6b7280);
  cursor: pointer;
  position: relative;
  border-radius: 10px 10px 0 0;
  transition: color 0.15s ease;
}
.cdm-tab i { font-size: 13px; }
.cdm-tab:hover { color: var(--pink-600, #db2777); }
.cdm-tab.is-active {
  color: var(--pink-700, #a81b5d);
}
.cdm-tab.is-active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 8px;
  right: 8px;
  height: 3px;
  background: linear-gradient(90deg, var(--pink-400, #ec4899), var(--pink-600, #db2777));
  border-radius: 3px 3px 0 0;
}
.cdm-tab-badge {
  background: var(--pink-500, #db2777);
  color: #fff;
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 10px;
  font-weight: 700;
}

/* ═══ Body ═══ */
.cdm-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafafa;
}
.cdm-tab-pane { animation: fadeIn 0.2s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: none; } }

/* ═══ Info view ═══ */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  background: #fff;
  border: 1px solid var(--border-color-soft, #f1dbe6);
  border-radius: 12px;
  overflow: hidden;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px dashed var(--border-color-soft, #f1dbe6);
}
.info-row:nth-last-child(-n+2) { border-bottom: none; }
.info-label {
  color: var(--muted, #6b7280);
  font-size: 13px;
  font-weight: 500;
}
.info-value {
  color: var(--ink, #1f2937);
  font-size: 14px;
  font-weight: 600;
  text-align: right;
  max-width: 60%;
  word-break: break-word;
}
.info-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

/* ═══ Buttons (3D style) ═══ */
.cdm-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  border-radius: 10px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  background: var(--pink-600, #db2777);
  color: #fff;
  box-shadow: 0 3px 0 var(--pink-700, #a81b5d), 0 4px 8px rgba(168, 27, 93, 0.3);
  border-bottom: 3px solid var(--pink-700, #a81b5d);
}
.cdm-btn:hover {
  background: var(--pink-700, #a81b5d);
  transform: translateY(-1px);
  box-shadow: 0 4px 0 var(--pink-700, #a81b5d), 0 6px 12px rgba(168, 27, 93, 0.35);
}
.cdm-btn:active {
  transform: translateY(2px);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.2);
  border-bottom-width: 0;
}
.cdm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
.cdm-btn--primary {
  background: var(--pink-600, #db2777);
  color: #fff;
}
.cdm-btn--ghost {
  background: transparent;
  color: var(--muted, #6b7280);
  border: 1px solid var(--border-color-strong);
  box-shadow: none;
  border-bottom: 1px solid var(--border-color-strong);
}
.cdm-btn--ghost:hover {
  background: var(--pink-50, #fff5f9);
  color: var(--pink-700, #a81b5d);
  box-shadow: none;
  transform: translateY(-1px);
}
.cdm-btn--sm { padding: 6px 12px; font-size: 12px; }

/* ═══ Info edit ═══ */
.info-edit {
  background: #fff;
  border: 1px solid var(--border-color-soft, #f1dbe6);
  border-radius: 12px;
  padding: 16px;
}
.cdm-input {
  background: var(--bg-input, #fff);
  color: var(--text-primary);
  border-color: var(--border-color-strong);
}

/* ═══ Orders tab ═══ */
.orders-list { display: flex; flex-direction: column; gap: 8px; }
.order-row {
  background: #fff;
  border: 1px solid var(--border-color-soft, #f1dbe6);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.15s ease;
}
.order-row:hover { border-color: var(--pink-300, #f7a8c8); box-shadow: 0 2px 8px rgba(168,27,93,0.1); }
.order-row.is-open { border-color: var(--pink-400, #ec4899); }
.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
}
.order-head-left { display: flex; flex-direction: column; gap: 4px; }
.order-code { font-weight: 700; color: var(--ink, #1f2937); font-size: 14px; }
.order-date { color: var(--muted, #6b7280); font-size: 12px; }
.order-date i { font-size: 11px; margin-right: 4px; }
.order-head-right { display: flex; align-items: center; gap: 12px; }
.order-total { font-weight: 800; color: var(--pink-700, #a81b5d); font-size: 14px; }
.order-status {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 600;
}
.order-head-right .fa { color: var(--muted, #6b7280); font-size: 12px; }

.order-detail {
  padding: 0 16px 16px;
  border-top: 1px dashed var(--border-color-soft, #f1dbe6);
  background: var(--pink-50, #fff5f9);
}
.order-detail-title {
  font-weight: 700;
  font-size: 13px;
  color: var(--muted, #6b7280);
  margin: 12px 0 6px;
}
.order-products {
  font-size: 13px;
  color: var(--ink, #1f2937);
  background: #fff;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color-soft, #f1dbe6);
}
.order-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
  margin-top: 12px;
}
.order-detail-grid > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.order-detail-grid .info-label { font-size: 12px; }
.order-detail-grid .info-value { font-size: 13px; }
.order-detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}

.cdm-empty {
  text-align: center;
  padding: 40px 20px;
  color: var(--muted, #6b7280);
}
.cdm-empty i { font-size: 32px; margin-bottom: 8px; opacity: 0.5; display: block; }
.cdm-empty p { margin: 0; font-size: 13px; }

/* ═══ Chat tab ═══ */
.chat-tab { padding: 0; }
.chat-window {
  background: var(--pink-50, #fff5f9);
  border: 1px solid var(--border-color-soft, #f1dbe6);
  border-radius: 14px;
  overflow: hidden;
  height: 480px;
  display: flex;
  flex-direction: column;
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border-bottom: 1px solid var(--border-color-soft, #f1dbe6);
}
.chat-avatar-mini { width: 40px; height: 40px; }
.cdm-avatar-mini {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(168, 27, 93, 0.2);
}
.cdm-avatar-mini--img img { width: 100%; height: 100%; object-fit: cover; }
.chat-title { font-weight: 700; color: var(--ink, #1f2937); font-size: 14px; }
.chat-sub { font-size: 11px; color: var(--success, #059669); }
.chat-sub::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  background: var(--success, #059669);
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;
}
.chat-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.chat-message { display: flex; }
.chat-message.right { justify-content: flex-end; }
.chat-message.left { justify-content: flex-start; }
.chat-bubble {
  max-width: 70%;
  background: #fff;
  border: 1px solid var(--border-color-soft, #f1dbe6);
  padding: 8px 12px;
  border-radius: 14px;
}
.chat-message.right .chat-bubble {
  background: linear-gradient(135deg, var(--pink-400, #ec4899), var(--pink-600, #db2777));
  color: #fff;
  border-color: transparent;
}
.chat-message.right .chat-bubble-name,
.chat-message.right .chat-bubble-time { color: rgba(255,255,255,0.85); }
.chat-bubble-name { display: block; font-size: 11px; font-weight: 700; margin-bottom: 3px; color: var(--muted, #6b7280); }
.chat-bubble-text { display: block; font-size: 13px; line-height: 1.4; }
.chat-bubble-time { display: block; margin-top: 4px; font-size: 10px; color: var(--muted, #6b7280); text-align: right; }
.chat-composer {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid var(--border-color-soft, #f1dbe6);
  background: #fff;
}
.chat-composer textarea {
  flex: 1;
  border-radius: 10px;
  border: 1px solid var(--border-color-soft, #f1dbe6);
  background: var(--bg-input, #fff);
  color: var(--text-primary);
  padding: 8px 12px;
  font-size: 13px;
  resize: none;
}

/* ═══ Responsive ═══ */
@media (max-width: 600px) {
  .info-grid { grid-template-columns: 1fr; }
  .order-detail-grid { grid-template-columns: 1fr; }
  .cdm-name { font-size: 16px; }
  .cdm-avatar { width: 50px; height: 50px; font-size: 18px; }
}
</style>
