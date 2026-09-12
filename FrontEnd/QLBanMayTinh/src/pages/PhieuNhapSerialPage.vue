<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import { Package, ClipboardList, Building2, User, Calendar, ExternalLink, Printer } from "@lucide/vue";
import * as PhieuNhapKhoService from "../services/PhieuNhapKhoService.js";
import * as ChiTietPhieuNhapService from "../services/ChiTietPhieuNhapService.js";
import * as ChiTietSanPhamService from "../services/ChiTietSanPhamService.js";
import { formatPrice, formatDate, statusLabel } from "../utils/adminFormat.js";
import { t } from "../i18n/index.js";

// Dùng lại theme hồng giống InventoryPanel / HangHoa — chỉ trang này dùng, nhúng thẳng vì
// nó độc lập với AdminPage (mở tab riêng).
const route = useRoute();
const phieuNhapId = computed(() => Number(route.params.id));

const loading = ref(true);
const phieu = ref(null);
const chiTietList = ref([]);
const serialList = ref([]);
const supplier = ref(null);
const staff = ref(null);

// Status chip — dùng chung bảng màu với InventoryPanel.phieuNhapStatusColor.
const phieuNhapStatusColor = (s) => {
  if (s === 'hoan_thanh') return { bg: 'rgba(34,197,94,0.15)',  text: '#22c55e' };
  if (s === 'huy')        return { bg: 'rgba(239,68,68,0.15)',  text: '#f87171' };
  return                         { bg: 'rgba(250,204,21,0.15)', text: '#facc15' };
};

// Nhóm serial theo bienTheId để render 1 bảng/dòng cho mỗi SKU.
const serialsByBienThe = computed(() => {
  const map = new Map();
  for (const s of serialList.value) {
    if (!map.has(s.bienTheId)) map.set(s.bienTheId, []);
    map.get(s.bienTheId).push(s);
  }
  return map;
});

const itemsWithSerials = computed(() => chiTietList.value.map((c) => ({
  ...c,
  serials: serialsByBienThe.value.get(c.bienTheId) ?? [],
})));

const totalSerials = computed(() => serialList.value.length);

const loadAll = async () => {
  loading.value = true;
  try {
    // getAll() đã đủ — service chưa có getById, lọc từ list là đủ cho 1 trang chi tiết.
    const [pnAll, ctAll, suppliers, staffList, serials] = await Promise.all([
      PhieuNhapKhoService.getAll().catch(() => []),
      ChiTietPhieuNhapService.getAll().catch(() => []),
      fetch('/api/nha-cung-cap').then((r) => (r.ok ? r.json() : [])).catch(() => []),
      fetch('/api/nhan-vien').then((r) => (r.ok ? r.json() : [])).catch(() => []),
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

// Bảo đảm khi đổi id qua router (ví dụ mở lại cùng tab với phiếu khác) cũng tải lại.
watch(phieuNhapId, () => loadAll());
onMounted(() => loadAll());

const printEsc = (v) => String(v ?? '').replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]));

const printSerials = () => {
  if (!phieu.value) return;
  const rows = itemsWithSerials.value.map((c, i) =>
    `<tr>
      <td class="center">${i + 1}</td>
      <td>${printEsc(c.maSku)}</td>
      <td class="center">${c.soLuong}</td>
      <td class="center">${c.serials.length}</td>
      <td>${c.serials.map((s) => printEsc(s.soSerial)).join('<br>') || '<span class="muted">—</span>'}</td>
    </tr>`
  ).join('');
  const html = `<!doctype html><html><head><meta charset="utf-8"><title>Serial ${printEsc(phieu.value.maPhieuNhap)}</title>
    <style>
      body{font-family:Arial,sans-serif;padding:24px;color:#111;}
      h1{font-size:18px;margin:0 0 4px;}
      .sub{font-size:12px;color:#555;margin-bottom:18px;}
      table{width:100%;border-collapse:collapse;font-size:13px;}
      th,td{border:1px solid #999;padding:6px 10px;text-align:left;vertical-align:top;}
      th{background:#eee;}
      .center{text-align:center;} .muted{color:#888;}
    </style></head><body>
    <h1>DANH SÁCH SERIAL — ${printEsc(phieu.value.maPhieuNhap)}</h1>
    <div class="sub">${printEsc(supplier.value?.tenNhaCungCap || '')} · ${printEsc(formatDate(phieu.value.ngayNhap))}</div>
    <table>
      <thead><tr><th style="width:36px;">#</th><th>SKU</th><th class="center" style="width:70px;">SL phiếu</th><th class="center" style="width:70px;">SL serial</th><th>Danh sách serial</th></tr></thead>
      <tbody>${rows || `<tr><td colspan="5" class="center muted">Chưa có serial</td></tr>`}</tbody>
    </table>
    </body></html>`;
  const iframe = document.createElement('iframe');
  iframe.style.cssText = 'position:fixed;width:0;height:0;border:0;visibility:hidden;';
  document.body.appendChild(iframe);
  iframe.contentDocument.write(html);
  iframe.contentDocument.close();
  iframe.onload = () => {
    iframe.contentWindow.print();
    setTimeout(() => document.body.removeChild(iframe), 500);
  };
};

const backToList = () => {
  // Đóng tab nếu mở qua window.open — để user quay lại danh sách.
  window.close();
};
</script>

<template>
  <div class="pnser">
    <header class="pnser-head">
      <div class="pnser-head__left">
        <div class="pnser-icon"><ClipboardList :size="18" /></div>
        <div>
          <div class="pnser-title">
            Serial — <span class="pnser-mono">{{ phieu?.maPhieuNhap || '…' }}</span>
          </div>
          <div class="pnser-sub" v-if="phieu">
            {{ supplier?.tenNhaCungCap || '—' }} · {{ formatDate(phieu.ngayNhap) }}
          </div>
        </div>
      </div>
      <div class="pnser-head__right">
        <button class="pnser-btn" @click="printSerials" :disabled="!phieu"><Printer :size="14" /> In danh sách serial</button>
        <button class="pnser-btn pnser-btn--ghost" @click="backToList">Đóng tab</button>
      </div>
    </header>

    <div v-if="loading" class="pnser-empty">Đang tải…</div>
    <div v-else-if="!phieu" class="pnser-empty">Không tìm thấy phiếu nhập.</div>
    <template v-else>
      <div class="pnser-chips">
        <span class="pnser-chip"><Building2 :size="13" /> NCC: <b>{{ supplier?.tenNhaCungCap || '—' }}</b></span>
        <span class="pnser-chip"><User :size="13" /> NV: <b>{{ staff?.hoTen || '—' }}</b></span>
        <span class="pnser-chip"><Calendar :size="13" /> Ngày: <b>{{ formatDate(phieu.ngayNhap) }}</b></span>
        <span class="pnser-tag" :style="{ background: phieuNhapStatusColor(phieu.trangThai).bg, color: phieuNhapStatusColor(phieu.trangThai).text }">
          {{ statusLabel(phieu.trangThai) }}
        </span>
        <span class="pnser-chip" style="margin-left:auto;"><Package :size="13" /> Tổng: <b>{{ totalSerials }} serial</b></span>
      </div>

      <section class="pnser-card">
        <table class="pnser-table">
          <thead>
            <tr>
              <th>SKU</th>
              <th class="ta-c">SL phiếu</th>
              <th class="ta-c">SL serial</th>
              <th class="ta-r">Đơn giá</th>
              <th class="ta-r">Thành tiền</th>
              <th>Danh sách serial</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in itemsWithSerials" :key="c.id">
              <td class="pnser-code">{{ c.maSku }}</td>
              <td class="ta-c" style="font-weight:700;">{{ c.soLuong }}</td>
              <td class="ta-c">
                <span :class="c.serials.length < c.soLuong ? 'text-warning' : 'text-success'" style="font-weight:700;">
                  {{ c.serials.length }}
                </span>
              </td>
              <td class="ta-r pnser-muted">{{ formatPrice(c.donGiaNhap) }}</td>
              <td class="ta-r pnser-price">{{ formatPrice(c.thanhTien) }}</td>
              <td>
                <div v-if="c.serials.length === 0" class="pnser-muted">Chưa có serial.</div>
                <div v-else class="pnser-chips-list">
                  <span v-for="s in c.serials" :key="s.chiTietId" class="pnser-serial" :class="{ 'pnser-serial--dangling': s.trangThai !== 'trong_kho' }">
                    {{ s.soSerial }}
                  </span>
                </div>
              </td>
            </tr>
            <tr v-if="itemsWithSerials.length === 0">
              <td colspan="6" class="pnser-empty">Phiếu chưa có dòng hàng.</td>
            </tr>
          </tbody>
        </table>
      </section>

      <footer class="pnser-foot">
        <span class="pnser-muted">{{ itemsWithSerials.length }} SKU</span>
        <div class="pnser-foot__right">
          <span class="pnser-muted">Tổng tiền:</span>
          <span class="pnser-price">{{ formatPrice(phieu.tongTien) }}</span>
        </div>
      </footer>
    </template>
  </div>
</template>

<style scoped>
.pnser {
  --pink-50:  #fff5f9;
  --pink-100: #ffe6f0;
  --pink-200: #ffcfe1;
  --pink-300: #f7a8c8;
  --pink-500: #ec4899;
  --pink-600: #db2777;
  --pink-700: #a81b5d;
  --ink:   #1f2937;
  --muted: #6b7280;
  --line:  #f1dbe6;
  --field: #d9b3c6;
  font-family: inherit;
  font-size: 14px;
  color: var(--ink);
  background: #fffafc;
  min-height: 100vh;
  padding: 20px 24px;
}
.pnser-ta-r { text-align: right; }
.ta-r { text-align: right; }
.ta-c { text-align: center; }
.pnser-muted { color: var(--muted); }
.pnser-mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; color: var(--pink-700); }
.pnser-price { font-weight: 700; font-variant-numeric: tabular-nums; }
.text-warning { color: #d97706; }
.text-success { color: #059669; }

.pnser-head {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  background: #fff; border: 1px solid var(--line); border-radius: 14px;
  padding: 14px 18px; margin-bottom: 14px; box-shadow: 0 4px 6px rgba(168, 27, 93, .08);
}
.pnser-head__left { display: flex; align-items: center; gap: 12px; }
.pnser-head__right { display: flex; gap: 8px; }
.pnser-icon {
  width: 40px; height: 40px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: var(--pink-100); color: var(--pink-700);
}
.pnser-title { font-weight: 700; font-size: 1rem; }
.pnser-sub { font-size: 0.78rem; color: var(--muted); margin-top: 2px; }

.pnser-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 14px; border-radius: 999px; border: 1px solid var(--pink-200);
  font-size: 13px; font-weight: 600; font-family: inherit; cursor: pointer; white-space: nowrap;
  background: var(--pink-600); color: #fff;
}
.pnser-btn--ghost { background: #fff; color: var(--pink-700); }
.pnser-btn--ghost:hover { background: var(--pink-50); }
.pnser-btn:disabled { opacity: .45; cursor: not-allowed; }

.pnser-chips {
  display: flex; flex-wrap: wrap; gap: 8px; padding: 12px 16px;
  background: #fff; border: 1px solid var(--line); border-radius: 14px;
  margin-bottom: 12px;
}
.pnser-chip {
  display: inline-flex; align-items: center; gap: 6px;
  background: var(--pink-50); border-radius: 999px; padding: 4px 12px;
  font-size: 12.5px; color: var(--muted);
}
.pnser-chip b { color: var(--ink); font-weight: 700; }
.pnser-tag {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 4px 10px; border-radius: 999px; font-size: 12.5px; font-weight: 700;
}

.pnser-card { background: #fff; border: 1px solid var(--line); border-radius: 14px; overflow: hidden; }
.pnser-table { width: 100%; border-collapse: collapse; }
.pnser-table th {
  background: var(--pink-50); color: var(--pink-700);
  font-size: 11.5px; font-weight: 800; text-align: left; text-transform: uppercase; letter-spacing: .4px;
  padding: 11px 12px; white-space: nowrap; border-bottom: 1px solid var(--line);
}
.pnser-table td { padding: 11px 12px; border-bottom: 1px solid var(--line); vertical-align: top; }
.pnser-table tbody tr:last-child td { border-bottom: none; }
.pnser-code { color: var(--pink-700); font-weight: 700; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
.pnser-chips-list { display: flex; flex-wrap: wrap; gap: 4px; }
.pnser-serial {
  padding: 2px 8px; border-radius: 4px; font-size: 11.5px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  background: var(--pink-50); color: var(--pink-700); border: 1px solid var(--pink-200);
  white-space: nowrap;
}
.pnser-serial--dangling { background: #fef3c7; color: #92400e; border-color: #fde68a; }
.pnser-empty { padding: 40px 20px; text-align: center; color: var(--muted); font-size: 13.5px; }

.pnser-foot {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 14px 18px; margin-top: 12px; background: var(--pink-50);
  border: 1px solid var(--line); border-radius: 14px;
}
.pnser-foot__right { display: flex; align-items: center; gap: 8px; font-size: 0.95rem; }
</style>
