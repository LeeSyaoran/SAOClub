<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from "vue";
import { t } from "../../i18n/index.js";
import { CustomersStore, refreshCustomers } from "../../stores/customers.js";
import { OrdersStore } from "../../stores/orders.js";
import * as PhieuGiamGiaCaNhanService from "../../services/PhieuGiamGiaCaNhanService.js";
import * as KhachHangService from "../../services/KhachHangService.js";
import * as YeuThichService from "../../services/YeuThichService.js";
import * as ChatService from "../../services/ChatService.js";
import { useChatWebSocket } from "../../composables/useChatWebSocket.js";
import { showToast } from "../../stores/toast.js";
import { formatPrice, formatDate, formatDateTime, statusLabel } from "../../utils/adminFormat.js";
import { orderStatusLabel, orderStatusColor } from "../../utils/orderStatus.js";
import CustomerFormModal from "./CustomerFormModal.vue";
import TangDiemModal from "./TangDiemModal.vue";
import TangVoucherModal from "./TangVoucherModal.vue";
import { Heart } from "@lucide/vue";

const props = defineProps({
  customerId: { type: Number, required: true },
});
const emit = defineEmits(["back"]);

const customer = computed(
  () => (CustomersStore.items ?? []).find((c) => c.khachHangId === props.customerId) ?? null,
);

const isBusiness = computed(() => customer.value?.loaiKhach === "doanh_nghiep");

const customerOrders = computed(() =>
  (OrdersStore.items ?? [])
    .filter((o) => o.khachHangId === props.customerId)
    .sort((a, b) => new Date(b.ngayDat) - new Date(a.ngayDat)),
);

const totalSpent = computed(() =>
  customerOrders.value
    .filter((o) => o.trangThaiDonHang !== "cancelled")
    .reduce((sum, o) => sum + (o.thanhTien || 0), 0),
);

const vouchers = ref([]);
const vouchersLoading = ref(true);
const pointHistory = ref([]);
const pointHistoryLoading = ref(true);
const wishlist = ref([]);
const wishlistLoading = ref(true);

const loadVouchers = async () => {
  vouchersLoading.value = true;
  try {
    vouchers.value = await PhieuGiamGiaCaNhanService.getByKhachHang(props.customerId);
  } catch (e) {
    console.error("Không tải được danh sách voucher:", e);
    vouchers.value = [];
  } finally {
    vouchersLoading.value = false;
  }
};

const loadPointHistory = async () => {
  pointHistoryLoading.value = true;
  try {
    pointHistory.value = await KhachHangService.getLichSuDiem(props.customerId);
  } catch (e) {
    console.error("Không tải được lịch sử tặng điểm:", e);
    pointHistory.value = [];
  } finally {
    pointHistoryLoading.value = false;
  }
};

const loadWishlist = async () => {
  wishlistLoading.value = true;
  try {
    wishlist.value = await YeuThichService.getByCustomer(props.customerId);
  } catch (e) {
    console.error("Không tải được wishlist:", e);
    wishlist.value = [];
  } finally {
    wishlistLoading.value = false;
  }
};

onMounted(() => {
  loadVouchers();
  loadPointHistory();
  loadWishlist();
});

const voucherStatus = (v) => {
  if (v.daSuDung) return { key: "voucherStatusUsed", cls: "bg-secondary" };
  if (new Date(v.ngayHetHan) < new Date()) return { key: "voucherStatusExpired", cls: "bg-danger" };
  return { key: "voucherStatusActive", cls: "bg-success" };
};

const activeDetailTab = ref("info");
const chatInput = ref("");
const chatMessages = ref([]);
const chatLoading = ref(false);
const chatSending = ref(false);
const chatConversationId = ref(null);
const chatTrangThai = ref("HOI_DAP_AI");
const chatBodyRef = ref(null);

const { connect: connectChatWs, subscribeChat, disconnect: disconnectChatWs } = useChatWebSocket();
let currentChatSub = null;

const scrollChatToBottom = async () => {
  await nextTick();
  if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
};

const mapChatMessage = (m) => {
  const isCustomer = m.nguoiGui === "KHACH" || m.vaiTro === "KHACH";
  const isBot = m.nguoiGui === "AI" || m.loaiNguoiGui === "AI";
  return {
    id: m.id || m.tinNhanId,
    side: isCustomer ? "left" : "right",
    name: isCustomer
      ? customer.value?.hoTen || "Khách hàng"
      : isBot
        ? "🤖 SAOClub Bot"
        : m.tenNguoiGui || "CSKH",
    text: m.noiDung || m.content || "",
    time:
      m.thoiGian || m.createdAt
        ? new Date(m.thoiGian || m.createdAt).toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          })
        : "",
  };
};

const loadChatMessages = async (ctcId) => {
  chatLoading.value = true;
  try {
    const res = await ChatService.layTinNhan(ctcId, 0, 100);
    const list = res?.content || res || [];
    chatMessages.value = list.map(mapChatMessage);
    scrollChatToBottom();
  } catch {
    chatMessages.value = [];
  } finally {
    chatLoading.value = false;
  }
};

const subscribeToCustomerChat = (ctcId) => {
  if (currentChatSub) {
    try {
      currentChatSub.sub1?.unsubscribe?.();
    } catch {}
    try {
      currentChatSub.sub2?.unsubscribe?.();
    } catch {}
    currentChatSub = null;
  }
  currentChatSub = subscribeChat(
    ctcId,
    (msg) => {
      const msgId = msg.id || msg.tinNhanId;
      if (msg.trangThai) chatTrangThai.value = msg.trangThai;
      const optMatch = chatMessages.value.find(
        (m) => !m.id && m.side === "right" && m.text === (msg.noiDung || msg.content),
      );
      if (optMatch) {
        optMatch.id = msgId;
        return;
      }
      if (!chatMessages.value.some((m) => m.id && m.id === msgId)) {
        chatMessages.value.push(mapChatMessage(msg));
        scrollChatToBottom();
      }
    },
    (status) => {
      if (status?.trangThai) chatTrangThai.value = status.trangThai;
    },
  );
};

const loadCustomerChatHistory = async () => {
  if (!props.customerId) return;
  chatLoading.value = true;
  try {
    const res = await ChatService.layLichSuChatKhach(props.customerId, 0, 20);
    let list = res?.content || res || [];
    if (list.length === 0) {
      const created = await ChatService.taoPhienChat({
        sessionId: null,
        khachHangId: props.customerId,
        hoTen: customer.value?.hoTen || null,
      });
      if (created) list = [created];
    }
    if (list.length > 0) {
      const active =
        list.find((c) => c.trangThai === "CHAT_NHAN_VIEN" || c.trangThai === "HOI_DAP_AI") ||
        list[0];
      chatConversationId.value = active.id || active.cuocTroChuyenId;
      chatTrangThai.value = active.trangThai || "HOI_DAP_AI";
      await loadChatMessages(chatConversationId.value);
      subscribeToCustomerChat(chatConversationId.value);
    } else {
      chatMessages.value = [];
    }
  } catch {
    chatMessages.value = [];
  } finally {
    chatLoading.value = false;
  }
};

const sendChat = async () => {
  const text = chatInput.value.trim();
  if (!text || chatSending.value || !chatConversationId.value) return;
  chatSending.value = true;
  const optimistic = {
    id: null,
    side: "right",
    name: "CSKH",
    text,
    time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
  };
  chatMessages.value.push(optimistic);
  chatInput.value = "";
  scrollChatToBottom();
  try {
    const saved = await ChatService.guiTinNhanTuNhanVien(chatConversationId.value, text);
    if (saved?.id) optimistic.id = saved.id;
    if (saved?.trangThai) chatTrangThai.value = saved.trangThai;
  } catch (e) {
    showToast("Gửi tin nhắn thất bại: " + e.message, "error");
    chatMessages.value = chatMessages.value.filter((m) => m !== optimistic);
  } finally {
    chatSending.value = false;
  }
};

watch(activeDetailTab, (tab) => {
  if (tab === "chat") {
    connectChatWs(null, null, null);
    loadCustomerChatHistory();
  }
});

onUnmounted(() => {
  if (currentChatSub) {
    try {
      currentChatSub.sub1?.unsubscribe?.();
    } catch {}
    try {
      currentChatSub.sub2?.unsubscribe?.();
    } catch {}
  }
  disconnectChatWs();
});

const customerFormModalRef = ref(null);
const showCustomerModal = ref(false);
const showGiftPointsModal = ref(false);
const showGiftVoucherModal = ref(false);
</script>

<template>
  <div v-if="!customer" class="text-secondary small">{{ t("admin.customerDetail.notFound") }}</div>
  <div v-else class="customer-detail-showcase">
    <button class="btn btn-sm btn-outline-secondary mb-3" @click="emit('back')">
      {{ t("admin.customerDetail.back") }}
    </button>

    <div class="customer-detail-head">
      <div class="customer-detail-head-left">
        <div class="customer-avatar-wrap">
          <img
            class="customer-avatar"
            :src="
              customer.avatarUrl ||
              customer.hinhAnh ||
              '/images/Gemini_Generated_Image_kbekzokbekzokbek.png'
            "
            :alt="customer.hoTen"
          />
        </div>
        <div>
          <div class="customer-detail-name">
            {{ isBusiness ? customer.tenCongTy || customer.hoTen : customer.hoTen }}
          </div>
          <div class="customer-detail-contact">
            {{ customer.soDienThoai }} · {{ customer.email || "—" }}
          </div>
          <div class="customer-detail-tags">
            <span
              class="badge"
              :class="customer.trangThai === 'active' ? 'bg-success' : 'bg-secondary'"
              >{{ statusLabel(customer.trangThai) }}</span
            >
            <span class="badge bg-light text-dark ms-2">{{
              isBusiness ? "Doanh nghiệp" : "Cá nhân"
            }}</span>
          </div>
        </div>
      </div>
      <div class="customer-detail-head-actions">
        <button
          class="btn btn-sm btn-outline-warning"
          @click="customerFormModalRef.openForEdit(customer)"
        >
          {{ t("admin.customerDetail.edit") }}
        </button>
        <button
          class="btn btn-sm btn-warning text-dark fw-bold"
          @click="showGiftPointsModal = true"
        >
          {{ t("admin.customerDetail.giftPoints") }}
        </button>
        <button
          class="btn btn-sm btn-warning text-dark fw-bold"
          @click="showGiftVoucherModal = true"
        >
          {{ t("admin.customerDetail.giftVoucher") }}
        </button>
      </div>
    </div>

    <div class="customer-detail-tabs">
      <button
        class="customer-detail-tab"
        :class="{ 'is-active': activeDetailTab === 'info' }"
        @click="activeDetailTab = 'info'"
      >
        Thông tin
      </button>
      <button
        class="customer-detail-tab"
        :class="{ 'is-active': activeDetailTab === 'wishlist' }"
        @click="activeDetailTab = 'wishlist'"
      >
        <Heart :size="14" style="vertical-align: -2px; margin-right: 4px" />Yêu thích
        <span v-if="wishlist.length" class="badge bg-danger ms-1" style="font-size: 0.65rem">{{
          wishlist.length
        }}</span>
      </button>
      <button
        class="customer-detail-tab"
        :class="{ 'is-active': activeDetailTab === 'chat' }"
        @click="activeDetailTab = 'chat'"
      >
        Chat
      </button>
    </div>

    <div v-if="activeDetailTab === 'info'" class="customer-info-panel">
      <div class="customer-detail-kpis">
        <div class="kpi-tile">
          <span class="kpi-label">Tổng tiền đã chi</span>
          <span class="kpi-value">{{ formatPrice(totalSpent) }}</span>
        </div>
        <div class="kpi-tile">
          <span class="kpi-label">Số đơn hàng</span>
          <span class="kpi-value">{{ customerOrders.length }}</span>
        </div>
        <div class="kpi-tile">
          <span class="kpi-label">Điểm tích lũy</span>
          <span class="kpi-value">{{ customer.diemTichLuy ?? 0 }}</span>
        </div>
        <div class="kpi-tile">
          <span class="kpi-label">Đơn gần nhất</span>
          <span class="kpi-value small-value">{{
            customerOrders[0]
              ? formatDate(customerOrders[0].ngayDat)
              : t("admin.customerDetail.noLastOrder")
          }}</span>
        </div>
      </div>

      <div class="customer-detail-content-grid">
        <section class="customer-slab customer-slab-left">
          <div class="customer-slab-title">
            {{ isBusiness ? "Thông tin doanh nghiệp" : "Thông tin cá nhân" }}
          </div>
          <div class="customer-facts">
            <template v-if="isBusiness">
              <div class="fact-row">
                <span class="fact-label">Tên công ty</span
                ><span class="fact-value">{{ customer.tenCongTy || customer.hoTen }}</span>
              </div>
              <div class="fact-row">
                <span class="fact-label">Mã số thuế</span
                ><span class="fact-value">{{ customer.maSoThue || "—" }}</span>
              </div>
              <div class="fact-row">
                <span class="fact-label">Người đại diện</span
                ><span class="fact-value">{{ customer.hoTen || "—" }}</span>
              </div>
              <div class="fact-row">
                <span class="fact-label">Số điện thoại</span
                ><span class="fact-value">{{ customer.soDienThoai }}</span>
              </div>
              <div class="fact-row">
                <span class="fact-label">Email</span
                ><span class="fact-value">{{ customer.email || "—" }}</span>
              </div>
              <div class="fact-row">
                <span class="fact-label">Địa chỉ trụ sở</span
                ><span class="fact-value">{{ customer.diaChi || "—" }}</span>
              </div>
              <div class="fact-row">
                <span class="fact-label">Loại khách</span
                ><span class="fact-value">Doanh nghiệp</span>
              </div>
            </template>
            <template v-else>
              <div class="fact-row">
                <span class="fact-label">Họ tên</span
                ><span class="fact-value">{{ customer.hoTen }}</span>
              </div>
              <div class="fact-row">
                <span class="fact-label">Số điện thoại</span
                ><span class="fact-value">{{ customer.soDienThoai }}</span>
              </div>
              <div class="fact-row">
                <span class="fact-label">Email</span
                ><span class="fact-value">{{ customer.email || "—" }}</span>
              </div>
              <div class="fact-row">
                <span class="fact-label">Địa chỉ</span
                ><span class="fact-value">{{ customer.diaChi || "—" }}</span>
              </div>
              <div class="fact-row">
                <span class="fact-label">Loại khách</span><span class="fact-value">Cá nhân</span>
              </div>
            </template>
          </div>
        </section>

        <section class="customer-slab customer-slab-right">
          <div class="customer-slab-title">Đơn hàng của khách</div>
          <div v-if="customerOrders.length === 0" class="text-secondary small">
            {{ t("admin.customerDetail.ordersEmpty") }}
          </div>
          <div v-else class="customer-order-list">
            <div
              v-for="o in customerOrders.slice(0, 8)"
              :key="o.donHangId"
              class="customer-order-row"
            >
              <div class="customer-order-left">
                <span class="customer-order-code">{{
                  o.maDonHang || o.maDon || "#" + o.donHangId
                }}</span>
                <span class="customer-order-date">{{ formatDate(o.ngayDat) }}</span>
              </div>
              <div class="customer-order-right">
                <span class="customer-order-total">{{ formatPrice(o.thanhTien) }}</span>
                <span
                  class="customer-order-status"
                  :style="{
                    background: orderStatusColor(o.trangThaiDonHang).bg,
                    color: orderStatusColor(o.trangThaiDonHang).text,
                  }"
                  >{{ orderStatusLabel(o.trangThaiDonHang) }}</span
                >
              </div>
            </div>
          </div>
        </section>
      </div>

      <div class="customer-proof-grid">
        <section class="customer-slab">
          <div class="customer-slab-title">Voucher</div>
          <div v-if="vouchersLoading" class="text-secondary small">
            {{ t("admin.customers.loading") }}
          </div>
          <div v-else-if="vouchers.length === 0" class="text-secondary small">
            {{ t("admin.customerDetail.vouchersEmpty") }}
          </div>
          <div v-else class="customer-mini-list">
            <div v-for="v in vouchers.slice(0, 5)" :key="v.phieuId" class="customer-mini-item">
              <span>{{ v.maPhieu }}</span>
              <span>{{ v.loai === "percent" ? `${v.giaTri}%` : formatPrice(v.giaTri) }}</span>
            </div>
          </div>
        </section>

        <section class="customer-slab">
          <div class="customer-slab-title">Lịch sử điểm</div>
          <div v-if="pointHistoryLoading" class="text-secondary small">
            {{ t("admin.customers.loading") }}
          </div>
          <div v-else-if="pointHistory.length === 0" class="text-secondary small">
            {{ t("admin.customerDetail.pointHistoryEmpty") }}
          </div>
          <div v-else class="customer-mini-list">
            <div v-for="p in pointHistory.slice(0, 5)" :key="p.id" class="customer-mini-item">
              <span>+{{ p.soDiem }} điểm</span>
              <span class="text-secondary">{{ p.lyDo || "—" }}</span>
            </div>
          </div>
        </section>
      </div>
    </div>

    <div v-else-if="activeDetailTab === 'wishlist'" class="customer-wishlist-panel">
      <div v-if="wishlistLoading" class="text-secondary small">
        {{ t("admin.customers.loading") }}
      </div>
      <div v-else-if="wishlist.length === 0" class="text-secondary small">
        {{ t("admin.customerDetail.wishlistEmpty", "Khách hàng chưa có sản phẩm yêu thích nào.") }}
      </div>
      <div v-else class="wishlist-grid">
        <div v-for="item in wishlist" :key="item.yeuThichId" class="wishlist-card">
          <div class="wishlist-card__img-wrap">
            <img
              :src="item.hinhAnhChinh || '/images/placeholder.png'"
              class="wishlist-card__img"
              alt=""
              @error="$event.target.style.display = 'none'"
            />
            <span
              v-if="item.trangThai === 'inactive'"
              class="wishlist-card__badge badge bg-secondary"
              >Ngừng bán</span
            >
          </div>
          <div class="wishlist-card__body">
            <div class="wishlist-card__name">{{ item.tenSanPham }}</div>
            <div class="wishlist-card__sku text-secondary small">{{ item.maSku }}</div>
            <div class="wishlist-card__price">{{ formatPrice(item.giaBan) }}</div>
            <div class="wishlist-card__stock">
              <span v-if="(item.soLuongTon ?? 0) > 0" class="text-success small"
                >Còn {{ item.soLuongTon }} trong kho</span
              >
              <span v-else class="text-danger small">Hết hàng</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="customer-chat-panel">
      <div class="chat-window">
        <div class="chat-header">
          <span class="chat-avatar-mini"
            ><img
              :src="
                customer.avatarUrl ||
                customer.hinhAnh ||
                '/images/Gemini_Generated_Image_kbekzokbekzokbek.png'
              "
          /></span>
          <span class="chat-title">Chat với {{ customer.hoTen }}</span>
        </div>
        <div ref="chatBodyRef" class="chat-body">
          <div v-if="chatLoading" class="text-secondary small text-center py-3">Đang tải...</div>
          <div v-else-if="chatMessages.length === 0" class="text-secondary small text-center py-3">
            Chưa có tin nhắn nào.
          </div>
          <div
            v-for="(m, idx) in chatMessages"
            :key="m.id || idx"
            class="chat-message"
            :class="m.side"
          >
            <div class="chat-bubble">
              <span class="chat-bubble-name">{{ m.name }}</span>
              <span class="chat-bubble-text">{{ m.text }}</span>
              <span class="chat-bubble-time">{{ m.time }}</span>
            </div>
          </div>
        </div>
        <div class="chat-composer">
          <textarea
            v-model="chatInput"
            rows="2"
            placeholder="Nhắn với khách hàng..."
            :disabled="chatSending"
            @keyup.enter.exact.prevent="sendChat"
          ></textarea>
          <button
            class="btn btn-sm btn-warning text-dark fw-bold"
            :disabled="chatSending || !chatInput.trim()"
            @click="sendChat"
          >
            Gửi
          </button>
        </div>
      </div>
    </div>

    <CustomerFormModal ref="customerFormModalRef" v-model="showCustomerModal" />
    <TangDiemModal
      v-model="showGiftPointsModal"
      :customer-id="props.customerId"
      @gifted="
        () => {
          loadPointHistory();
          refreshCustomers();
        }
      "
    />
    <TangVoucherModal
      v-model="showGiftVoucherModal"
      :customer-id="props.customerId"
      @gifted="loadVouchers"
    />
  </div>
</template>

<style scoped>
.customer-detail-showcase {
  background: var(--bg-card);
  border: 1px solid var(--border-color-soft);
  border-radius: 20px;
  padding: 20px;
}
.customer-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color-soft);
}
.customer-detail-head-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.customer-avatar-wrap {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: var(--bg-card-soft);
  border: 2px solid var(--accent);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}
.customer-avatar {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 50%;
}
.customer-detail-name {
  font-size: 1.7rem;
  font-weight: 800;
  color: var(--text-heading);
}
.customer-detail-contact {
  color: var(--text-secondary);
  font-size: 0.9rem;
}
.customer-detail-tags {
  margin-top: 8px;
}
.customer-detail-head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.customer-detail-tabs {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  border-bottom: 1px solid var(--border-color-soft);
}
.customer-detail-tab {
  border: none;
  background: transparent;
  padding: 10px 16px;
  border-radius: 10px 10px 0 0;
  color: var(--text-secondary);
  font-weight: 700;
}
.customer-detail-tab.is-active {
  background: var(--primary);
  color: #fff;
}
.customer-info-panel {
  margin-top: 16px;
}
.customer-detail-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.kpi-tile {
  background: var(--bg-card-soft);
  border: 1px solid var(--border-color-soft);
  border-radius: 16px;
  padding: 14px;
}
.kpi-label {
  color: var(--text-secondary);
  display: block;
  font-size: 0.78rem;
}
.kpi-value {
  color: var(--text-heading);
  font-size: 1.2rem;
  font-weight: 800;
  display: block;
  margin-top: 6px;
}
.small-value {
  font-size: 0.84rem;
}
.customer-detail-content-grid {
  display: grid;
  grid-template-columns: 1fr 1.25fr;
  gap: 16px;
}
.customer-slab {
  background: var(--bg-card-soft);
  border: 1px solid var(--border-color-soft);
  border-radius: 18px;
  padding: 16px;
}
.customer-proof-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 16px;
  margin-top: 16px;
}
.customer-slab-title {
  font-weight: 800;
  font-size: 1rem;
  color: var(--text-heading);
  margin-bottom: 12px;
}
.customer-facts {
  display: grid;
  gap: 12px;
}
.fact-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px dotted var(--border-color-soft);
  padding: 7px 0;
}
.fact-label {
  color: var(--text-secondary);
  font-size: 0.82rem;
}
.fact-value {
  color: var(--text-heading);
  font-weight: 700;
}
.customer-order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.customer-order-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-color-soft);
}
.customer-order-left {
  display: flex;
  flex-direction: column;
}
.customer-order-code {
  font-weight: 800;
  color: var(--text-heading);
}
.customer-order-date {
  color: var(--text-secondary);
  font-size: 0.78rem;
}
.customer-order-right {
  display: flex;
  flex-direction: column;
  align-items: end;
  gap: 5px;
}
.customer-order-total {
  font-weight: 800;
}
.customer-order-status {
  font-size: 0.72rem;
  padding: 4px 8px;
  border-radius: 999px;
}
.customer-mini-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.customer-mini-item {
  background: var(--bg-card);
  padding: 8px 12px;
  border-radius: 10px;
  border: 1px solid var(--border-color-soft);
  display: flex;
  gap: 12px;
}
.customer-chat-panel {
  margin-top: 12px;
}
.chat-window {
  background: var(--bg-card-soft);
  border: 1px solid var(--border-color-soft);
  border-radius: 18px;
  overflow: hidden;
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color-soft);
}
.chat-avatar-mini {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--bg-card-soft);
  border: 1px solid var(--border-color-soft);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-avatar-mini img {
  width: 40px;
  height: 40px;
  object-fit: cover;
}
.chat-title {
  font-weight: 800;
  color: var(--text-heading);
}
.chat-body {
  min-height: 280px;
  max-height: 420px;
  overflow: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.chat-message {
  display: flex;
}
.chat-message.right {
  justify-content: flex-end;
}
.chat-message.left {
  justify-content: flex-start;
}
.chat-bubble {
  max-width: 70%;
  color: var(--text-heading);
  background: var(--bg-card);
  border: 1px solid var(--border-color-soft);
  padding: 10px 12px;
  border-radius: 16px;
}
.chat-message.right .chat-bubble {
  background: var(--primary-soft, #ffdce9);
}
.chat-bubble-name {
  display: block;
  font-size: 0.72rem;
  font-weight: 800;
  margin-bottom: 4px;
  color: var(--text-secondary);
}
.chat-bubble-text {
  display: block;
  font-size: 0.86rem;
}
.chat-bubble-time {
  display: block;
  margin-top: 5px;
  color: var(--text-secondary);
  font-size: 0.72rem;
}
.chat-composer {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid var(--border-color-soft);
}
.chat-composer textarea {
  flex: 1;
  border-radius: 12px;
  background: var(--bg-input);
  color: var(--text-primary);
  border: 1px solid var(--border-color-soft);
}
@media (max-width: 900px) {
  .customer-detail-kpis,
  .customer-detail-content-grid,
  .customer-proof-grid {
    grid-template-columns: 1fr;
  }
  .customer-detail-head {
    align-items: flex-start;
    flex-wrap: wrap;
  }
}

.wishlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}
.wishlist-card {
  background: var(--bg-card-soft);
  border: 1px solid var(--border-color-soft);
  border-radius: 14px;
  overflow: hidden;
}
.wishlist-card__img-wrap {
  position: relative;
  aspect-ratio: 4/3;
  background: var(--bg-card);
}
.wishlist-card__img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.wishlist-card__badge {
  position: absolute;
  top: 6px;
  right: 6px;
}
.wishlist-card__body {
  padding: 10px 12px;
}
.wishlist-card__name {
  font-size: 0.84rem;
  font-weight: 700;
  color: var(--text-heading);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.wishlist-card__sku {
  margin-top: 3px;
}
.wishlist-card__price {
  font-weight: 800;
  font-size: 0.95rem;
  color: var(--text-heading);
  margin-top: 6px;
}
.wishlist-card__stock {
  margin-top: 4px;
}
</style>
