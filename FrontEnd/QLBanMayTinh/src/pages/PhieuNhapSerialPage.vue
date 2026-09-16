<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Building2, User, Calendar, Printer, ArrowLeft, Hash, X, Check } from "@lucide/vue";
import * as PhieuNhapKhoService from "../services/PhieuNhapKhoService.js";
import * as ChiTietPhieuNhapService from "../services/ChiTietPhieuNhapService.js";
import * as ChiTietSanPhamService from "../services/ChiTietSanPhamService.js";
import { formatPrice, formatDate, statusLabel } from "../utils/adminFormat.js";
import { get } from "../services/api.js";
import { showToast } from "../stores/toast.js";
import { askConfirm } from "../stores/confirm.js";

const route = useRoute();
const router = useRouter();
const phieuNhapId = computed(() => Number(route.params.id));

const loading = ref(true);
const phieu = ref(null);
const chiTietList = ref([]);
const serialList = ref([]);
const supplier = ref(null);
const staff = ref(null);

// Modal chi tiết serial của dòng hàng (SKU)
const showSerialModal = ref(false);
const selectedItem = ref(null);

const openSerialModal = (item) => {
  selectedItem.value = item;
  showSerialModal.value = true;
};

const closeSerialModal = () => {
  showSerialModal.value = false;
  selectedItem.value = null;
};

const handleKeyDown = (e) => {
  if (e.key === "Escape" && showSerialModal.value) {
    closeSerialModal();
  }
};

// Status chip phiếu nhập — dùng chung bảng màu với InventoryPanel
const phieuNhapStatusColor = (s) => {
  if (s === "hoan_thanh") return { bg: "rgba(34,197,94,0.15)", text: "#22c55e" };
  if (s === "huy") return { bg: "rgba(239,68,68,0.15)", text: "#f87171" };
  return { bg: "rgba(250,204,21,0.15)", text: "#facc15" };
};

// Trạng thái từng serial trong modal
const serialStatusClass = (s) => {
  if (s === "trong_kho") return "pnser-status-badge--in-stock";
  if (s === "da_ban") return "pnser-status-badge--sold";
  if (s === "giu_hang") return "pnser-status-badge--reserved";
  return "pnser-status-badge--default";
};

const serialStatusText = (s) => {
  if (s === "trong_kho") return "Trong kho";
  if (s === "da_ban") return "Đã bán";
  if (s === "giu_hang") return "Đang đặt hàng";
  return statusLabel(s) || s || "Trong kho";
};

// Nhóm serial theo bienTheId để hiển thị tương ứng từng SKU
const serialsByBienThe = computed(() => {
  const map = new Map();
  for (const s of serialList.value) {
    if (!map.has(s.bienTheId)) map.set(s.bienTheId, []);
    map.get(s.bienTheId).push(s);
  }
  return map;
});

const itemsWithSerials = computed(() =>
  chiTietList.value.map((c) => ({
    ...c,
    serials: serialsByBienThe.value.get(c.bienTheId) ?? [],
  }))
);

const totalSerials = computed(() => serialList.value.length);

const approving = ref(false);
const approveReceipt = async () => {
  if (!phieu.value) return;
  if (!(await askConfirm("Xác nhận duyệt phiếu nhập này?"))) return;
  approving.value = true;
  try {
    const res = await PhieuNhapKhoService.duyet(phieu.value.phieuNhapId);
    if (!res.ok) {
      showToast(await res.text().catch(() => "Duyệt phiếu thất bại"), "error");
      return;
    }
    showToast("Duyệt phiếu thành công!", "success");
    await loadAll();
  } catch (e) {
    showToast(e.message || "Duyệt phiếu thất bại", "error");
  } finally {
    approving.value = false;
  }
};

const loadAll = async () => {
  loading.value = true;
  try {
    const [pnAll, ctAll, suppliers, staffList, serials] = await Promise.all([
      PhieuNhapKhoService.getAll().catch(() => []),
      ChiTietPhieuNhapService.getAll().catch(() => []),
      get("/api/nha-cung-cap").catch(() => []),
      get("/api/nhan-vien?page=0&size=200").then((r) => r?.content || r || []).catch(() => []),
      ChiTietSanPhamService.getByPhieuNhap(phieuNhapId.value).catch(() => []),
    ]);
    phieu.value = (pnAll || []).find((p) => p.phieuNhapId === phieuNhapId.value) ?? null;
    chiTietList.value = (ctAll || []).filter((c) => c.phieuNhapId === phieuNhapId.value);
    serialList.value = serials || [];
    supplier.value = (suppliers || []).find((s) => s.nhaCungCapId === phieu.value?.nhaCungCapId) ?? null;
    staff.value = (staffList || []).find((s) => s.nhanVienId === phieu.value?.nhanVienId) ?? null;
  } finally {
    loading.value = false;
  }
};

watch(phieuNhapId, () => loadAll());

onMounted(() => {
  loadAll();
  window.addEventListener("keydown", handleKeyDown);
});

onUnmounted(() => {
  window.removeEventListener("keydown", handleKeyDown);
});

const printEsc = (v) =>
  String(v ?? "").replace(/[&<>]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;" }[c]));

// In danh sách serial theo layout chuẩn (không có cột SL phiếu)
const printSerials = () => {
  if (!phieu.value) return;
  const rows = itemsWithSerials.value
    .map(
      (c, i) =>
        `<tr>
          <td class="center">${i + 1}</td>
          <td>${printEsc(c.maSku)}</td>
          <td class="center">${c.serials.length}</td>
          <td class="right">${formatPrice(c.donGiaNhap)}</td>
          <td class="right">${formatPrice(c.thanhTien)}</td>
          <td>${c.serials.map((s) => printEsc(s.soSerial)).join(", ") || '<span class="muted">—</span>'}</td>
        </tr>`
    )
    .join("");

  const html = `<!doctype html><html><head><meta charset="utf-8"><title>Danh sách serial ${printEsc(phieu.value.maPhieuNhap)}</title>
    <style>
      body{font-family:Arial,sans-serif;padding:24px;color:#111;}
      h1{font-size:18px;margin:0 0 4px;}
      .sub{font-size:12px;color:#555;margin-bottom:18px;}
      table{width:100%;border-collapse:collapse;font-size:13px;}
      th,td{border:1px solid #999;padding:6px 10px;text-align:left;vertical-align:top;}
      th{background:#eee;}
      .center{text-align:center;} .right{text-align:right;} .muted{color:#888;}
    </style></head><body>
    <h1>DANH SÁCH SERIAL — ${printEsc(phieu.value.maPhieuNhap)}</h1>
    <div class="sub">${printEsc(supplier.value?.tenNhaCungCap || "")} · ${printEsc(formatDate(phieu.value.ngayNhap))}</div>
    <table>
      <thead>
        <tr>
          <th style="width:36px;">#</th>
          <th>SKU</th>
          <th class="center" style="width:70px;">SL serial</th>
          <th class="right" style="width:110px;">Đơn giá</th>
          <th class="right" style="width:120px;">Thành tiền</th>
          <th>Danh sách serial</th>
        </tr>
      </thead>
      <tbody>${rows || `<tr><td colspan="6" class="center muted">Chưa có serial</td></tr>`}</tbody>
      <tfoot>
        <tr>
          <th colspan="4" class="right">Tổng tiền:</th>
          <th class="right">${formatPrice(phieu.value.tongTien)}</th>
          <th></th>
        </tr>
      </tfoot>
    </table>
    </body></html>`;

  const iframe = document.createElement("iframe");
  iframe.style.cssText = "position:fixed;width:0;height:0;border:0;visibility:hidden;";
  document.body.appendChild(iframe);
  iframe.contentDocument.write(html);
  iframe.contentDocument.close();
  iframe.onload = () => {
    iframe.contentWindow.print();
    setTimeout(() => document.body.removeChild(iframe), 500);
  };
};

const backToList = () => {
  if (window.opener && !window.opener.closed) {
    window.close();
  } else if (window.history.length > 1) {
    window.history.back();
  } else {
    const saved = (() => {
      try { return JSON.parse(sessionStorage.getItem("saoclub_session")); } catch { return null; }
    })();
    const target = saved?.role === "quan_kho" ? "/kho" : (saved?.role === "nhan_vien" ? "/staff" : "/admin");
    router.push(target);
  }
};
</script>

<template>
  <div class="pnser">
    <div class="pnser-container">
      <!-- ══ HEADER ĐIỀU HƯỚNG ══ -->
      <header class="pnser-topbar">
        <button class="pnser-btn pnser-btn--ghost" @click="backToList">
          <ArrowLeft :size="14" /> Quay lại
        </button>
        <div style="display:flex;gap:8px;align-items:center;">
          <button
            v-if="phieu?.trangThai === 'cho_duyet'"
            class="pnser-btn pnser-btn--ok"
            :disabled="approving"
            @click="approveReceipt"
          >
            <Check :size="14" /> Duyệt phiếu
          </button>
          <button class="pnser-btn pnser-btn--primary" @click="printSerials" :disabled="!phieu">
            <Printer :size="14" /> In danh sách serial
          </button>
        </div>
      </header>

      <div v-if="loading" class="pnser-empty">Đang tải…</div>
      <div v-else-if="!phieu" class="pnser-empty">Không tìm thấy phiếu nhập.</div>
      <template v-else>
        <!-- ══ THANH THÔNG TIN CHIPS ══ -->
        <div class="pnser-chips">
          <span class="pnser-chip">
            <Building2 :size="13" /> NCC: <b>{{ supplier?.tenNhaCungCap || "—" }}</b>
          </span>
          <span class="pnser-chip">
            <User :size="13" /> NV: <b>{{ staff?.hoTen || "—" }}</b>
          </span>
          <span class="pnser-chip">
            <Calendar :size="13" /> Ngày: <b>{{ formatDate(phieu.ngayNhap) }}</b>
          </span>
          <span class="pnser-chip">
            <Hash :size="13" /> Mã: <b>{{ phieu.maPhieuNhap }}</b>
          </span>
          <span
            class="pnser-tag"
            :style="{ background: phieuNhapStatusColor(phieu.trangThai).bg, color: phieuNhapStatusColor(phieu.trangThai).text }"
          >
            {{ statusLabel(phieu.trangThai) }}
          </span>
          <span class="pnser-chip pnser-chip--count">
            <Hash :size="13" /> <b>{{ totalSerials }} serial</b>
          </span>
        </div>

      <!-- ══ BẢNG CHI TIẾT HÀNG HÓA ══ -->
      <section class="pnser-card">
        <table class="pnser-table">
          <thead>
            <tr>
              <th class="col-sku">SKU</th>
              <th class="col-qty ta-c">SL SERIAL</th>
              <th class="col-price ta-r">ĐƠN GIÁ</th>
              <th class="col-total ta-r">THÀNH TIỀN</th>
              <th class="col-action ta-c">THAO TÁC</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in itemsWithSerials" :key="c.id">
              <td class="col-sku pnser-code">{{ c.maSku }}</td>
              <td class="col-qty ta-c">
                <span class="pnser-qty-num">{{ c.serials.length }}</span>
              </td>
              <td class="col-price ta-r pnser-muted">{{ formatPrice(c.donGiaNhap) }}</td>
              <td class="col-total ta-r pnser-price">{{ formatPrice(c.thanhTien) }}</td>
              <td class="col-action ta-c">
                <button class="pnser-action-btn" @click="openSerialModal(c)">
                  <Hash :size="12" /> Xem serial
                </button>
              </td>
            </tr>
            <tr v-if="itemsWithSerials.length === 0">
              <td colspan="5" class="pnser-empty">Phiếu chưa có dòng hàng.</td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td colspan="3" class="ta-r pnser-foot-label">Tổng tiền</td>
              <td class="ta-r pnser-foot-value">{{ formatPrice(phieu.tongTien) }}</td>
              <td class="col-action"></td>
            </tr>
          </tfoot>
        </table>
      </section>

      <!-- ══ KHUNG GHI CHÚ ══ -->
      <div class="pnser-note-card">
        <span class="pnser-note-label">GHI CHÚ</span>
        <span class="pnser-note-content">{{ phieu.ghiChu || "—" }}</span>
      </div>

      <!-- ══ TAB CON: MODAL XEM CHI TIẾT SERIAL ══ -->
      <div
        v-if="showSerialModal"
        class="pnser-modal-mask"
        @click.self="closeSerialModal"
      >
        <div class="pnser-modal">
          <header class="pnser-modal__head">
            <div>
              <div class="pnser-modal__title">
                <span class="pnser-modal__hash">#</span> Chi tiết serial —
                <span class="pnser-mono">{{ phieu?.maPhieuNhap }}</span>
              </div>
              <div class="pnser-modal__sub" v-if="selectedItem">
                {{ selectedItem.maSku }} · {{ selectedItem.serials.length }} serial
              </div>
            </div>
            <button
              class="pnser-close-btn"
              @click="closeSerialModal"
              aria-label="Đóng"
            >
              <X :size="16" />
            </button>
          </header>

          <div class="pnser-modal__body">
            <div
              v-if="!selectedItem || selectedItem.serials.length === 0"
              class="pnser-modal-empty"
            >
              Chưa có serial nào cho sản phẩm này.
            </div>
            <div v-else class="pnser-modal-table-wrap">
              <table class="pnser-modal-table">
                <thead>
                  <tr>
                    <th class="modal-col-idx ta-c">#</th>
                    <th class="modal-col-serial ta-c">SỐ SERIAL</th>
                    <th class="modal-col-date ta-c">NGÀY NHẬP</th>
                    <th class="modal-col-status ta-c">TRẠNG THÁI</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(s, idx) in selectedItem.serials"
                    :key="s.chiTietId || idx"
                  >
                    <td class="modal-col-idx ta-c pnser-muted">{{ idx + 1 }}</td>
                    <td class="modal-col-serial ta-c pnser-serial-text">{{ s.soSerial }}</td>
                    <td class="modal-col-date ta-c pnser-muted">
                      {{ formatDate(s.ngayNhapKho || phieu?.ngayNhap) }}
                    </td>
                    <td class="modal-col-status ta-c">
                      <span
                        class="pnser-status-badge"
                        :class="serialStatusClass(s.trangThai)"
                      >
                        {{ serialStatusText(s.trangThai) }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <footer class="pnser-modal__foot">
            <button
              class="pnser-btn pnser-btn--ghost pnser-btn--close"
              @click="closeSerialModal"
            >
              Đóng
            </button>
          </footer>
        </div>
      </div>
    </template>
  </div>
</div>
</template>

<style scoped>
.pnser {
  --pink-50:  #fff5f9;
  --pink-100: #ffe6f0;
  --pink-200: #ffcfe1;
  --pink-300: #f7a8c8;
  --pink-400: #f472b6;
  --pink-500: #ec4899;
  --pink-600: #db2777;
  --pink-700: #a81b5d;
  --ink:   #1f2937;
  --muted: #6b7280;
  --line:  #f1dbe6;
  font-family: inherit;
  font-size: 14px;
  color: var(--ink);
  background: #fffafc;
  min-height: 100vh;
  padding: 20px 24px;
}

.pnser-container {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.ta-r { text-align: right; }
.ta-c { text-align: center; }
.pnser-muted { color: var(--muted); }
.pnser-mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; color: var(--pink-700); font-weight: 700; }
.pnser-price { font-weight: 700; font-variant-numeric: tabular-nums; color: var(--ink); }

/* ══ TOPBAR ══ */
.pnser-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 12px 18px;
  margin-bottom: 14px;
  box-shadow: 0 2px 4px rgba(168, 27, 93, 0.04);
}

.pnser-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
  text-decoration: none;
}
.pnser-btn--primary {
  background: var(--pink-600);
  color: #fff;
  border: 1px solid var(--pink-600);
}
.pnser-btn--primary:hover:not(:disabled) {
  background: var(--pink-700);
  border-color: var(--pink-700);
}
.pnser-btn--ghost {
  background: #fff;
  color: var(--pink-600);
  border: 1px solid var(--pink-200);
}
.pnser-btn--ghost:hover {
  background: var(--pink-50);
  border-color: var(--pink-300);
  color: var(--pink-700);
}
.pnser-btn--ok {
  background: #059669;
  color: #fff;
  border: 1px solid #059669;
}
.pnser-btn--ok:hover:not(:disabled) {
  background: #047857;
  border-color: #047857;
}
.pnser-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* ══ CHIPS BAR ══ */
.pnser-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  margin-bottom: 14px;
  box-shadow: 0 2px 4px rgba(168, 27, 93, 0.04);
}
.pnser-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--pink-50);
  border-radius: 999px;
  padding: 4px 12px;
  font-size: 12.5px;
  color: var(--muted);
}
.pnser-chip b {
  color: var(--ink);
  font-weight: 700;
}
.pnser-chip--count {
  background: var(--pink-100);
  color: var(--pink-700);
}
.pnser-chip--count b {
  color: var(--pink-700);
}
.pnser-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 700;
}

/* ══ MAIN TABLE CARD ══ */
.pnser-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(168, 27, 93, 0.04);
}
.pnser-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
}
.pnser-table th {
  background: var(--pink-50);
  color: var(--pink-700);
  font-size: 11.5px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  padding: 13px 16px;
  border-bottom: 1px solid var(--line);
  white-space: nowrap;
}
.pnser-table td {
  padding: 13px 16px;
  border-bottom: 1px solid var(--line);
  vertical-align: middle;
}
.pnser-table tbody tr:hover {
  background: #fffafc;
}

/* Column Widths */
.col-sku    { width: 26%; text-align: left; padding-left: 20px; }
.col-qty    { width: 16%; text-align: center; }
.col-price  { width: 20%; text-align: right; padding-right: 24px; }
.col-total  { width: 22%; text-align: right; padding-right: 24px; }
.col-action { width: 16%; text-align: center; }

.pnser-table th.col-sku,
.pnser-table td.col-sku {
  padding-left: 20px;
}
.pnser-table th.col-price,
.pnser-table td.col-price,
.pnser-table th.col-total,
.pnser-table td.col-total {
  padding-right: 24px;
}

.pnser-code {
  color: var(--pink-700);
  font-weight: 700;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13.5px;
}
.pnser-qty-num {
  color: #059669;
  font-weight: 700;
  font-size: 14px;
}
.pnser-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 14px;
  border-radius: 999px;
  border: 1px solid var(--pink-200);
  background: #fff;
  color: var(--pink-700);
  font-size: 12.5px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s ease;
}
.pnser-action-btn:hover {
  background: var(--pink-50);
  border-color: var(--pink-300);
  color: var(--pink-600);
  transform: translateY(-1px);
}

/* Footer / Total Row */
.pnser-table tfoot td {
  background: var(--pink-50);
  border-bottom: none;
  padding: 14px 16px;
}
.pnser-foot-label {
  color: var(--muted);
  font-size: 13.5px;
  font-weight: 600;
  padding-right: 20px;
}
.pnser-foot-value {
  color: var(--ink);
  font-size: 15px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  padding-right: 24px;
}

/* ══ NOTE CARD ══ */
.pnser-note-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 14px 18px;
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 4px rgba(168, 27, 93, 0.04);
}
.pnser-note-label {
  font-size: 12px;
  font-weight: 800;
  color: var(--pink-700);
  letter-spacing: 0.4px;
  text-transform: uppercase;
}
.pnser-note-content {
  font-size: 13.5px;
  color: var(--muted);
}

.pnser-empty {
  padding: 40px 20px;
  text-align: center;
  color: var(--muted);
  font-size: 13.5px;
}

/* ══ TAB CON / MODAL SERIAL POPUP ══ */
.pnser-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  padding: 20px;
  animation: fadeIn 0.15s ease-out;
}

.pnser-modal {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  width: 580px;
  max-width: 95vw;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.15), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  animation: popIn 0.18s ease-out;
}

.pnser-modal__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 16px 22px 14px;
  border-bottom: 1px solid var(--line);
  background: #fff;
}
.pnser-modal__title {
  font-size: 15.5px;
  font-weight: 700;
  color: var(--ink);
  display: flex;
  align-items: center;
  gap: 4px;
}
.pnser-modal__hash {
  color: var(--pink-600);
  font-weight: 800;
  font-size: 16px;
  margin-right: 2px;
}
.pnser-modal__sub {
  font-size: 12.5px;
  color: var(--muted);
  margin-top: 3px;
}
.pnser-close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--pink-200);
  background: #fff;
  color: var(--pink-600);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}
.pnser-close-btn:hover {
  background: var(--pink-50);
  border-color: var(--pink-300);
  color: var(--pink-700);
}

.pnser-modal__body {
  padding: 0;
  overflow-y: auto;
  flex: 1;
  min-height: 120px;
  max-height: 400px;
}
.pnser-modal-table-wrap {
  width: 100%;
}
.pnser-modal-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
}
.pnser-modal-table th {
  position: sticky;
  top: 0;
  z-index: 2;
  background: #fff;
  color: var(--pink-700);
  font-size: 11.5px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 13px 14px;
  border-bottom: 1px solid var(--line);
}
.pnser-modal-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #fce7f1;
  vertical-align: middle;
  font-size: 13px;
}
.pnser-modal-table tbody tr:hover {
  background: #fff8fb;
}

/* Modal columns widths - evenly distributed and centered */
.modal-col-idx    { width: 46px; text-align: center; }
.modal-col-serial { width: calc((100% - 46px) / 3); text-align: center; }
.modal-col-date   { width: calc((100% - 46px) / 3); text-align: center; }
.modal-col-status { width: calc((100% - 46px) / 3); text-align: center; }

.pnser-serial-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-weight: 600;
  color: var(--pink-700);
}

.pnser-status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
}
.pnser-status-badge--in-stock {
  background: rgba(34, 197, 94, 0.12);
  color: #16a34a;
}
.pnser-status-badge--sold {
  background: rgba(148, 163, 184, 0.15);
  color: #64748b;
}
.pnser-status-badge--reserved {
  background: rgba(245, 158, 11, 0.15);
  color: #d97706;
}
.pnser-status-badge--default {
  background: var(--pink-50);
  color: var(--pink-700);
}

.pnser-modal-empty {
  padding: 36px 20px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}

.pnser-modal__foot {
  display: flex;
  justify-content: flex-end;
  padding: 12px 20px;
  border-top: 1px solid var(--line);
  background: #fff;
}
.pnser-btn--close {
  padding: 6px 22px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}
</style>
