<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as ChiTietSanPhamService from "../../Service/ChiTietSanPhamService.js";
import * as PhieuBaoHanhService from "../../Service/PhieuBaoHanhService.js";
import { formatPrice, formatDate } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { CustomersStore, ensureCustomers } from "../../stores/customers.js";
import { BaoHanhStore, ensureBaoHanh, refreshBaoHanh } from "../../stores/baoHanh.js";

onMounted(() => {
  ensureWarrantyData();
  ensureBaoHanh();
  ensureCustomers();
});

// ── Bảng "Còn hạn bảo hành" — chuyển nguyên xi từ AdminPage.vue ────────────────
const warrantyList = ref([]);
const warrantyLoading = ref(false);
const warrantySearch = ref('');
let warrantyPromise = null;
const ensureWarrantyData = (force = false) => {
  if (warrantyPromise && !force) return warrantyPromise;
  warrantyLoading.value = true;
  warrantyPromise = ChiTietSanPhamService.getUnderWarranty().catch(() => []).then((list) => {
    warrantyList.value = list;
    warrantyLoading.value = false;
  });
  return warrantyPromise;
};
const filteredWarranty = computed(() => {
  const q = warrantySearch.value.trim().toLowerCase();
  if (!q) return warrantyList.value;
  return warrantyList.value.filter((w) =>
    [w.soSerial, w.maSku, w.tenSanPham, w.maDonHang, w.tenKhachHang, w.soDienThoaiKhachHang]
      .some((v) => (v || '').toLowerCase().includes(q)));
});
const daysUntilExpiry = (isoDate) => Math.ceil((new Date(isoDate) - new Date()) / 86400000);

// ── Helpers ───────────────────────────────────────────────────────────────────
const customerName = (id) => CustomersStore.items.find(c => c.khachHangId === id)?.hoTen ?? `KH#${id}`;
const statusLabel = (s) => t(`admin.warrantyClaimStatus.${s}`);
const STATUS_COLOR = {
  con_bao_hanh: { bg: '#bfdbfe', text: '#1e3a8a' },
  dang_xu_ly:   { bg: '#fde68a', text: '#92400e' },
  da_xu_ly:     { bg: '#bbf7d0', text: '#166534' },
  het_bao_hanh: { bg: '#fecaca', text: '#991b1b' },
  tu_choi:      { bg: '#e5e7eb', text: '#374151' },
};
const statusColor = (s) => STATUS_COLOR[s] ?? { bg: '#e5e7eb', text: '#374151' };

// ── Bảng "Phiếu bảo hành" (CRUD) ────────────────────────────────────────────────
const claimSearch = ref("");
const filteredClaims = computed(() => {
  const q = claimSearch.value.trim().toLowerCase();
  if (!q) return BaoHanhStore.items;
  return BaoHanhStore.items.filter((p) => {
    const name = customerName(p.khachHangId).toLowerCase();
    return String(p.baoHanhId).includes(q) || name.includes(q) || (p.soSerial ?? '').toLowerCase().includes(q);
  });
});

const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const saving = ref(false);
const lockedInfo = ref(null); // { tenSanPham, maSku, soSerial, tenKhachHang, maDonHang } — hien thi tinh, khong sua

const emptyForm = () => ({
  donHangId: null, bienTheId: null, chiTietId: null, khachHangId: null,
  ngayMua: '', ngayHetBh: '',
  ngayTiepNhan: '', ngayTraKhach: '',
  moTaLoi: '', ketQuaXuLy: '', trangThai: 'con_bao_hanh',
  chiPhiPhatSinh: 0, ghiChu: '',
});
const form = ref(emptyForm());

const openCreateFromWarranty = (w) => {
  editingId.value = null;
  form.value = {
    ...emptyForm(),
    donHangId: w.donHangId,
    bienTheId: w.bienTheId,
    chiTietId: w.chiTietId,
    khachHangId: w.khachHangId,
    ngayMua: (w.ngayGiaoThucTe || '').slice(0, 16),
    ngayHetBh: (w.ngayHetBaoHanh || '').slice(0, 16),
  };
  lockedInfo.value = {
    tenSanPham: w.tenSanPham, maSku: w.maSku, soSerial: w.soSerial,
    tenKhachHang: w.tenKhachHang, maDonHang: w.maDonHang,
  };
  formError.value = "";
  showModal.value = true;
};

const openEdit = (p) => {
  editingId.value = p.baoHanhId;
  form.value = {
    donHangId: p.donHangId, bienTheId: p.bienTheId, chiTietId: p.chiTietId, khachHangId: p.khachHangId,
    ngayMua: (p.ngayMua || '').slice(0, 16),
    ngayHetBh: (p.ngayHetBh || '').slice(0, 16),
    ngayTiepNhan: (p.ngayTiepNhan || '').slice(0, 16),
    ngayTraKhach: (p.ngayTraKhach || '').slice(0, 16),
    moTaLoi: p.moTaLoi || '',
    ketQuaXuLy: p.ketQuaXuLy || '',
    trangThai: p.trangThai,
    chiPhiPhatSinh: p.chiPhiPhatSinh ?? 0,
    ghiChu: p.ghiChu || '',
  };
  lockedInfo.value = {
    tenSanPham: null, maSku: p.maSku, soSerial: p.soSerial,
    tenKhachHang: customerName(p.khachHangId), maDonHang: `#${p.donHangId}`,
  };
  formError.value = "";
  showModal.value = true;
};

const saveClaim = async () => {
  formError.value = "";
  if (!form.value.donHangId || !form.value.bienTheId || !form.value.khachHangId) {
    formError.value = t('admin.warrantyClaimModal.missingInfo');
    return;
  }
  if (!form.value.moTaLoi.trim()) {
    formError.value = t('admin.warrantyClaimModal.faultRequired');
    return;
  }
  const ngayMuaValid = form.value.ngayMua && !isNaN(new Date(form.value.ngayMua).getTime());
  const ngayHetBhValid = form.value.ngayHetBh && !isNaN(new Date(form.value.ngayHetBh).getTime());
  if (!ngayMuaValid || !ngayHetBhValid) {
    formError.value = t('admin.warrantyClaimModal.datesRequired');
    return;
  }
  if (saving.value) return;
  saving.value = true;
  try {
    const body = {
      donHangId: form.value.donHangId,
      bienTheId: form.value.bienTheId,
      chiTietId: form.value.chiTietId,
      khachHangId: form.value.khachHangId,
      ngayMua: nowLocalIso(new Date(form.value.ngayMua)),
      ngayHetBh: nowLocalIso(new Date(form.value.ngayHetBh)),
      ngayTiepNhan: form.value.ngayTiepNhan ? nowLocalIso(new Date(form.value.ngayTiepNhan)) : null,
      ngayTraKhach: form.value.ngayTraKhach ? nowLocalIso(new Date(form.value.ngayTraKhach)) : null,
      moTaLoi: form.value.moTaLoi,
      ketQuaXuLy: form.value.ketQuaXuLy || null,
      trangThai: form.value.trangThai,
      chiPhiPhatSinh: form.value.chiPhiPhatSinh || 0,
      ghiChu: form.value.ghiChu || '—',
    };
    const res = await PhieuBaoHanhService.save(editingId.value, body);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }
    showModal.value = false;
    await refreshBaoHanh();
  } catch (e) {
    formError.value = e.message;
  } finally {
    saving.value = false;
  }
};

const deleteClaim = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteWarrantyClaim')))) return;
  const res = await PhieuBaoHanhService.remove(id);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status }))); return; }
  await refreshBaoHanh();
};
</script>

<template>
  <!-- ══ BANG CON HAN BAO HANH ══ -->
  <div class="d-flex align-items-center gap-2 mb-3 flex-wrap">
    <span class="text-secondary small">{{ filteredWarranty.length }} {{ t('admin.warranty.countSuffix') }}</span>
    <span class="badge" style="background:rgba(148,163,184,0.15);color:#94a3b8;font-size:0.72rem;">📅 {{ t('admin.warranty.today') }}: {{ formatDate(new Date()) }}</span>
    <input v-model="warrantySearch" class="form-control form-control-sm ms-auto" style="max-width:260px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);font-size:0.82rem;"
           :placeholder="t('admin.warranty.searchPlaceholder')" />
  </div>
  <div v-if="warrantyLoading" class="text-secondary small text-center py-5">{{ t('admin.warranty.loading') }}</div>
  <div v-else class="table-responsive mb-4">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
      <thead><tr>
        <th style="width:40px;">{{ t('admin.common.stt') }}</th>
        <th>{{ t('admin.warranty.colSerial') }}</th>
        <th>{{ t('admin.warranty.colProduct') }}</th>
        <th>{{ t('admin.warranty.colCustomer') }}</th>
        <th>{{ t('admin.warranty.colPhone') }}</th>
        <th>{{ t('admin.warranty.colOrder') }}</th>
        <th>{{ t('admin.warranty.colDelivered') }}</th>
        <th>{{ t('admin.warranty.colExpires') }}</th>
        <th>{{ t('admin.warranty.colRemaining') }}</th>
        <th>{{ t('admin.warranty.colAction') }}</th>
      </tr></thead>
      <tbody>
        <tr v-for="(w, idx) in filteredWarranty" :key="w.chiTietId">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td class="text-secondary" style="font-family:monospace;">{{ w.soSerial }}</td>
          <td>{{ w.tenSanPham }} <span class="text-secondary" style="font-size:0.75rem;">({{ w.maSku }})</span></td>
          <td>{{ w.tenKhachHang }}</td>
          <td class="text-secondary">{{ w.soDienThoaiKhachHang }}</td>
          <td class="text-secondary" style="font-family:monospace;">{{ w.maDonHang }}</td>
          <td>{{ formatDate(w.ngayGiaoThucTe) }}</td>
          <td>{{ formatDate(w.ngayHetBaoHanh) }}</td>
          <td>
            <span class="badge" :style="daysUntilExpiry(w.ngayHetBaoHanh) <= 30
              ? { background: 'rgba(248,113,113,0.15)', color: '#f87171' }
              : daysUntilExpiry(w.ngayHetBaoHanh) <= 90
                ? { background: 'rgba(250,204,21,0.15)', color: '#facc15' }
                : { background: 'rgba(34,197,94,0.15)', color: '#22c55e' }">
              {{ t('admin.warranty.daysLeft', { count: daysUntilExpiry(w.ngayHetBaoHanh) }) }}
            </span>
          </td>
          <td>
            <button class="btn btn-sm btn-outline-warning" style="font-size:0.72rem;padding:2px 8px;" @click="openCreateFromWarranty(w)">🛡️ {{ t('admin.warranty.createClaim') }}</button>
          </td>
        </tr>
        <tr v-if="filteredWarranty.length===0"><td colspan="10" class="text-center text-secondary">{{ t('admin.warranty.empty') }}</td></tr>
      </tbody>
    </table>
  </div>

  <!-- ══ BANG PHIEU BAO HANH ══ -->
  <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
    <span class="text-secondary small">{{ filteredClaims.length }}/{{ BaoHanhStore.items.length }} {{ t('admin.warrantyClaims.countSuffix') }}</span>
    <input v-model="claimSearch" class="form-control form-control-sm" style="width:240px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.warrantyClaims.searchPlaceholder')" />
  </div>
  <div v-if="BaoHanhStore.loading" class="text-secondary small">{{ t('admin.warrantyClaims.loading') }}</div>
  <div v-else class="table-responsive">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
      <thead><tr>
        <th style="width:40px;">{{ t('admin.common.stt') }}</th>
        <th>{{ t('admin.warrantyClaims.colId') }}</th><th>{{ t('admin.warrantyClaims.colProduct') }}</th><th>{{ t('admin.warrantyClaims.colSerial') }}</th>
        <th>{{ t('admin.warrantyClaims.colCustomer') }}</th><th>{{ t('admin.warrantyClaims.colOrder') }}</th>
        <th>{{ t('admin.warrantyClaims.colCost') }}</th><th>{{ t('admin.warrantyClaims.colStatus') }}</th><th>{{ t('admin.warrantyClaims.colAction') }}</th>
      </tr></thead>
      <tbody>
        <tr v-for="(p, idx) in filteredClaims" :key="p.baoHanhId">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td class="text-secondary" style="font-family:monospace;">#{{ p.baoHanhId }}</td>
          <td class="text-secondary" style="font-family:monospace;">{{ p.maSku }}</td>
          <td class="text-secondary" style="font-family:monospace;">{{ p.soSerial || '—' }}</td>
          <td>{{ customerName(p.khachHangId) }}</td>
          <td class="text-secondary">#{{ p.donHangId }}</td>
          <td class="text-warning fw-semibold">{{ formatPrice(p.chiPhiPhatSinh) }}</td>
          <td><span class="badge" :style="{ background: statusColor(p.trangThai).bg, color: statusColor(p.trangThai).text }">{{ statusLabel(p.trangThai) }}</span></td>
          <td>
            <div class="d-flex gap-1">
              <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem;padding:2px 8px;" @click="openEdit(p)">{{ t('admin.warrantyClaims.edit') }}</button>
              <button class="btn btn-sm btn-outline-danger" style="font-size:0.78rem;padding:2px 8px;" @click="deleteClaim(p.baoHanhId)">{{ t('admin.warrantyClaims.delete') }}</button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredClaims.length===0"><td colspan="9" class="text-center text-secondary">{{ t('admin.warrantyClaims.empty') }}</td></tr>
      </tbody>
    </table>
  </div>

  <!-- ══ MODAL PHIEU BAO HANH ══ -->
  <div v-if="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:560px;max-width:96vw;max-height:90vh;overflow-y:auto;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ editingId ? t('admin.warrantyClaimModal.titleEdit') : t('admin.warrantyClaimModal.titleAdd') }}</div>
        <button class="btn-close btn-close-white btn-sm" @click="showModal=false"></button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-2">{{ formError }}</div>

      <div class="p-2 mb-3 rounded-2" style="background:var(--bg-input);">
        <div v-if="lockedInfo?.tenSanPham" class="small">{{ t('admin.warrantyClaimModal.productLabel') }}: <strong>{{ lockedInfo.tenSanPham }}</strong> ({{ lockedInfo.maSku }})</div>
        <div v-else class="small">{{ t('admin.warrantyClaimModal.productLabel') }}: <strong>{{ lockedInfo?.maSku }}</strong></div>
        <div class="small">{{ t('admin.warrantyClaimModal.serialLabel') }}: <strong>{{ lockedInfo?.soSerial || '—' }}</strong></div>
        <div class="small">{{ t('admin.warrantyClaimModal.customerLabel') }}: <strong>{{ lockedInfo?.tenKhachHang }}</strong></div>
        <div class="small">{{ t('admin.warrantyClaimModal.orderLabel') }}: <strong>{{ lockedInfo?.maDonHang }}</strong></div>
      </div>

      <div class="row g-2 mb-2">
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.purchaseDateLabel') }}</label>
          <input type="datetime-local" v-model="form.ngayMua" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.expiryDateLabel') }}</label>
          <input type="datetime-local" v-model="form.ngayHetBh" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.faultLabel') }} *</label>
        <input v-model="form.moTaLoi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.statusLabel') }}</label>
        <select v-model="form.trangThai" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option value="con_bao_hanh">{{ t('admin.warrantyClaimStatus.con_bao_hanh') }}</option>
          <option value="dang_xu_ly">{{ t('admin.warrantyClaimStatus.dang_xu_ly') }}</option>
          <option value="da_xu_ly">{{ t('admin.warrantyClaimStatus.da_xu_ly') }}</option>
          <option value="het_bao_hanh">{{ t('admin.warrantyClaimStatus.het_bao_hanh') }}</option>
          <option value="tu_choi">{{ t('admin.warrantyClaimStatus.tu_choi') }}</option>
        </select>
      </div>

      <div class="row g-2 mb-2">
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.receivedDateLabel') }}</label>
          <input type="datetime-local" v-model="form.ngayTiepNhan" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.returnedDateLabel') }}</label>
          <input type="datetime-local" v-model="form.ngayTraKhach" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.resultLabel') }}</label>
        <input v-model="form.ketQuaXuLy" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.costLabel') }}</label>
        <input type="number" min="0" v-model.number="form.chiPhiPhatSinh" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.noteLabel') }}</label>
        <input v-model="form.ghiChu" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showModal=false">{{ t('admin.warrantyClaimModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="saveClaim">{{ t('admin.warrantyClaimModal.save') }}</button>
      </div>
    </div>
  </div>
</template>
