<script setup>
import { ref, reactive, computed, onMounted, watch } from "vue";
import { Search } from "@lucide/vue";
import { t } from "../../i18n/index.js";
import * as KhuyenMaiService from "../../services/KhuyenMaiService.js";
import * as VongQuayService from "../../services/VongQuayService.js";
import { formatPrice, formatDate, statusLabel, toLocalDT, boDauTiengViet } from "../../utils/adminFormat.js";
import { showToast } from "../../stores/toast.js";
import { PromotionsStore, ensurePromotions, refreshPromotions } from "../../stores/promotions.js";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";
import { Filter, RotateCcw, X, Plus, Gift, Tag, Sparkles, ChevronDown, Hash, FileText, Percent, DollarSign, Calendar, CalendarCheck, Users, Activity, SlidersHorizontal } from '@lucide/vue';

onMounted(() => {
  ensurePromotions();
  loadWheelConfig();
});

const promotions = computed(() => PromotionsStore?.items ?? []);

// ── Cấu hình Vòng quay may mắn ───────────────────────────────────────────────
const wheelConfig = ref({ diemMoiLuot: 0, tyLeTruot: 0 });
const wheelConfigSaving = ref(false);
const wheelConfigError = ref("");

const loadWheelConfig = async () => {
  try {
    const res = await VongQuayService.getCauHinh();
    wheelConfig.value = { diemMoiLuot: res.diemMoiLuot, tyLeTruot: res.tyLeTruot };
  } catch (e) {
    wheelConfigError.value = e.message || t("admin.wheelConfig.loadError");
  }
};

const saveWheelConfig = async () => {
  wheelConfigSaving.value = true;
  wheelConfigError.value = "";
  try {
    const res = await VongQuayService.capNhatCauHinh(wheelConfig.value);
    if (!res.ok) throw new Error(await res.text().catch(() => res.statusText));
    showToast(t("admin.wheelConfig.saveSuccess"), "success");
  } catch (e) {
    wheelConfigError.value = e.message || t("admin.wheelConfig.saveError");
  } finally {
    wheelConfigSaving.value = false;
  }
};

// ── Bộ lọc nâng cao: Khuyến mại ──────────────────────────────────────────────
const search = ref("");
const isFilterOpen = ref(false);
const filters = reactive({
  loai: "",
  trangThai: "",
  validity: "", // 'valid' | 'upcoming' | 'expired'
  usage: "",    // 'available' | 'exhausted'
  dateStartFrom: "",
  dateEndTo: "",
});

const activeFilterCount = computed(() => {
  let count = 0;
  if (filters.loai) count++;
  if (filters.trangThai) count++;
  if (filters.validity) count++;
  if (filters.usage) count++;
  if (filters.dateStartFrom) count++;
  if (filters.dateEndTo) count++;
  return count;
});

const resetFilters = () => {
  filters.loai = "";
  filters.trangThai = "";
  filters.validity = "";
  filters.usage = "";
  filters.dateStartFrom = "";
  filters.dateEndTo = "";
  search.value = "";
};

const getPromoValidity = (p) => {
  const now = new Date();
  if (p.ngayBatDau && new Date(p.ngayBatDau) > now) return 'upcoming';
  if (p.ngayKetThuc && new Date(p.ngayKetThuc) < now) return 'expired';
  return 'valid';
};

const filteredPromotions = computed(() => {
  const rawQ = search.value.trim();
  const q = boDauTiengViet(rawQ);
  return promotions.value.filter((p) => {
    if (q) {
      const codeMatch = boDauTiengViet(p.maKhuyenMai ?? '').includes(q);
      const nameMatch = boDauTiengViet(p.tenKhuyenMai ?? '').includes(q);
      if (!codeMatch && !nameMatch) return false;
    }
    if (filters.loai && p.loai !== filters.loai) return false;
    if (filters.trangThai && p.trangThai !== filters.trangThai) return false;
    if (filters.validity) {
      const v = getPromoValidity(p);
      if (v !== filters.validity) return false;
    }
    if (filters.usage) {
      const used = p.soLanDaDung ?? 0;
      const max = p.soLuongToiDa;
      const hasLimit = max != null && max > 0;
      if (filters.usage === 'exhausted' && (!hasLimit || used < max)) return false;
      if (filters.usage === 'available' && hasLimit && used >= max) return false;
    }
    if (filters.dateStartFrom) {
      const d = (p.ngayBatDau || '').slice(0, 10);
      if (!d || d < filters.dateStartFrom) return false;
    }
    if (filters.dateEndTo) {
      const d = (p.ngayKetThuc || '').slice(0, 10);
      if (!d || d > filters.dateEndTo) return false;
    }
    return true;
  });
});

const { currentPage, totalPages, pagedItems: pagedPromotions, pageSize } = usePagination(filteredPromotions);

// Reset trang về 0 khi bộ lọc hoặc tìm kiếm thay đổi
watch(
  [search, () => filters.loai, () => filters.trangThai, () => filters.validity, () => filters.usage, () => filters.dateStartFrom, () => filters.dateEndTo],
  () => {
    currentPage.value = 0;
  }
);

// ── Modal Thêm / Sửa Khuyến mại ──────────────────────────────────────────────
const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const emptyForm = () => ({
  maKhuyenMai: "",
  tenKhuyenMai: "",
  loai: "percent",
  giaTri: "",
  giaTriToiDa: "",
  donHangToiThieu: "",
  ngayBatDau: "",
  ngayKetThuc: "",
  soLuongToiDa: "",
  trangThai: "active",
});
const form = reactive(emptyForm());

const openAdd = () => {
  Object.assign(form, emptyForm());
  editingId.value = null;
  formError.value = "";
  showModal.value = true;
};

const openEdit = (p) => {
  const dt = (d) => (d ? d.slice(0, 16) : "");
  Object.assign(form, {
    maKhuyenMai: p.maKhuyenMai,
    tenKhuyenMai: p.tenKhuyenMai,
    loai: p.loai ?? "percent",
    giaTri: p.giaTri ?? "",
    giaTriToiDa: p.giaTriToiDa ?? "",
    donHangToiThieu: p.donHangToiThieu ?? "",
    ngayBatDau: dt(p.ngayBatDau),
    ngayKetThuc: dt(p.ngayKetThuc),
    soLuongToiDa: p.soLuongToiDa ?? "",
    trangThai: p.trangThai ?? "active",
  });
  editingId.value = p.khuyenMaiId;
  formError.value = "";
  showModal.value = true;
};

const savePromo = async () => {
  formError.value = "";
  const body = {
    ...form,
    giaTri: form.giaTri ? Number(form.giaTri) : null,
    giaTriToiDa: form.giaTriToiDa ? Number(form.giaTriToiDa) : null,
    donHangToiThieu: form.donHangToiThieu ? Number(form.donHangToiThieu) : null,
    soLuongToiDa: form.soLuongToiDa ? Number(form.soLuongToiDa) : null,
    ngayBatDau: toLocalDT(form.ngayBatDau),
    ngayKetThuc: toLocalDT(form.ngayKetThuc),
  };
  try {
    const res = await KhuyenMaiService.save(editingId.value, body);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailedWithText', { status: res.status, text: await res.text() });
      return;
    }
    showModal.value = false;
    await refreshPromotions();
    showToast(editingId.value ? "Cập nhật khuyến mại thành công" : "Tạo khuyến mại mới thành công", "success");
  } catch (e) {
    formError.value = e.message;
  }
};
</script>

<template>
  <div class="promotions-wrapper">
    <!-- ══ CẤU HÌNH VÒNG QUAY ══ -->
    <div class="alt-toolbar mb-3 promo-wheel-toolbar" style="border-radius:14px; border:1px solid var(--border-color);">
      <div class="d-flex align-items-center gap-2">
        <Sparkles :size="16" class="text-warning" />
        <span class="fw-bold small">{{ t('admin.wheelConfig.title') }}</span>
      </div>
      <div class="d-flex align-items-center gap-2 flex-wrap ms-auto">
        <label class="small mb-0" style="color:var(--text-muted);">{{ t('admin.wheelConfig.pointsPerSpin') }}</label>
        <input
          v-model.number="wheelConfig.diemMoiLuot" type="number" min="1"
          class="form-control form-control-sm admin-input" style="width:90px;"
        />
        <label class="small mb-0" style="color:var(--text-muted);">{{ t('admin.wheelConfig.missRate') }}</label>
        <input
          v-model.number="wheelConfig.tyLeTruot" type="number" min="0" max="100"
          class="form-control form-control-sm admin-input" style="width:70px;"
        />
        <button class="alt-btn alt-btn--primary alt-btn--sm" :disabled="wheelConfigSaving" @click="saveWheelConfig">
          {{ t('admin.wheelConfig.save') }}
        </button>
        <span v-if="wheelConfigError" class="text-danger small">{{ wheelConfigError }}</span>
      </div>
    </div>

    <!-- ══ THẺ DANH SÁCH KHUYẾN MẠI ══ -->
    <div class="alt-card">
      <div class="alt-toolbar">
        <span class="alt-toolbar__count">{{ filteredPromotions.length }}/{{ promotions.length }} {{ t('admin.promotions.countSuffix') }}</span>
        <div class="alt-toolbar__actions">
          <div class="alt-search">
            <Search class="alt-search__icon" :size="14" />
            <input v-model="search" placeholder="Tìm theo mã, tên khuyến mại..." />
            <button v-if="search" type="button" class="alt-search__clear" @click="search = ''" title="Xóa tìm kiếm">
              <X :size="12" />
            </button>
          </div>
          <button
            type="button"
            class="alt-btn"
            :class="isFilterOpen || activeFilterCount > 0 ? 'alt-btn--filter-active' : 'alt-btn--ghost'"
            @click="isFilterOpen = !isFilterOpen"
          >
            <Filter :size="13" />
            <span>Lọc nâng cao</span>
            <span v-if="activeFilterCount > 0" class="alt-badge-count">{{ activeFilterCount }}</span>
            <ChevronDown :size="13" class="filter-caret" :class="{ 'is-open': isFilterOpen }" />
          </button>
          <button class="alt-btn alt-btn--primary" @click="openAdd">
            <Plus :size="13" /> {{ t('admin.promotions.add') }}
          </button>
        </div>
      </div>

      <!-- ══ HỘP LỌC NÂNG CAO ══ -->
      <div class="sm-filter-collapse" :class="{ 'is-open': isFilterOpen }">
        <div class="sm-filter-panel">
          <div class="sm-filter-grid">
            <div class="sm-filter-field">
              <label>Hình thức giảm giá</label>
              <select v-model="filters.loai" class="alt-select">
                <option value="">-- Tất cả hình thức --</option>
                <option value="percent">{{ t('admin.promotions.typePercent') }} (%)</option>
                <option value="fixed">{{ t('admin.promotions.typeFixed') }} (VNĐ)</option>
              </select>
            </div>
            <div class="sm-filter-field">
              <label>Trạng thái cấu hình</label>
              <select v-model="filters.trangThai" class="alt-select">
                <option value="">-- Tất cả trạng thái --</option>
                <option value="active">Đang kích hoạt</option>
                <option value="inactive">Tạm ngưng</option>
              </select>
            </div>
            <div class="sm-filter-field">
              <label>Hiệu lực thời gian</label>
              <select v-model="filters.validity" class="alt-select">
                <option value="">-- Tất cả hiệu lực --</option>
                <option value="valid">Đang có hiệu lực</option>
                <option value="upcoming">Sắp diễn ra (chưa đến ngày)</option>
                <option value="expired">Đã hết hạn</option>
              </select>
            </div>
            <div class="sm-filter-field">
              <label>Tình trạng sử dụng</label>
              <select v-model="filters.usage" class="alt-select">
                <option value="">-- Tất cả tình trạng --</option>
                <option value="available">Còn lượt sử dụng</option>
                <option value="exhausted">Đã hết lượt sử dụng</option>
              </select>
            </div>
            <div class="sm-filter-field">
              <label>Bắt đầu từ ngày</label>
              <input type="date" v-model="filters.dateStartFrom" class="alt-input" />
            </div>
            <div class="sm-filter-field">
              <label>Kết thúc đến ngày</label>
              <input type="date" v-model="filters.dateEndTo" class="alt-input" />
            </div>
          </div>
          <div class="sm-filter-foot">
            <button type="button" class="alt-btn alt-btn--ghost alt-btn--sm" :disabled="activeFilterCount === 0" @click="resetFilters">
              <RotateCcw :size="12" /> Xóa bộ lọc
            </button>
            <button type="button" class="alt-btn alt-btn--primary alt-btn--sm" @click="isFilterOpen = false">
              Hoàn tất
            </button>
          </div>
        </div>
      </div>

      <!-- ══ CHIPS BỘ LỌC ĐANG ÁP DỤNG ══ -->
      <div v-if="activeFilterCount > 0" class="sm-active-chips">
        <span class="sm-chips-label">Đang lọc:</span>
        <span v-if="filters.loai" class="sm-chip">
          Hình thức: <b>{{ filters.loai === 'percent' ? 'Giảm %' : 'Giảm cố định' }}</b>
          <button type="button" @click="filters.loai = ''"><X :size="10" /></button>
        </span>
        <span v-if="filters.trangThai" class="sm-chip">
          Trạng thái: <b>{{ filters.trangThai === 'active' ? 'Kích hoạt' : 'Tạm ngưng' }}</b>
          <button type="button" @click="filters.trangThai = ''"><X :size="10" /></button>
        </span>
        <span v-if="filters.validity" class="sm-chip">
          Hiệu lực: <b>{{ filters.validity === 'valid' ? 'Đang hiệu lực' : (filters.validity === 'upcoming' ? 'Chưa bắt đầu' : 'Đã hết hạn') }}</b>
          <button type="button" @click="filters.validity = ''"><X :size="10" /></button>
        </span>
        <span v-if="filters.usage" class="sm-chip">
          Lượt dùng: <b>{{ filters.usage === 'available' ? 'Còn lượt' : 'Hết lượt' }}</b>
          <button type="button" @click="filters.usage = ''"><X :size="10" /></button>
        </span>
        <span v-if="filters.dateStartFrom" class="sm-chip">
          Bắt đầu từ: <b>{{ filters.dateStartFrom }}</b>
          <button type="button" @click="filters.dateStartFrom = ''"><X :size="10" /></button>
        </span>
        <span v-if="filters.dateEndTo" class="sm-chip">
          Kết thúc đến: <b>{{ filters.dateEndTo }}</b>
          <button type="button" @click="filters.dateEndTo = ''"><X :size="10" /></button>
        </span>
        <button type="button" class="sm-chip-clear" @click="resetFilters">Xóa tất cả</button>
      </div>

      <div v-if="PromotionsStore.loading" class="alt-empty">{{ t('admin.promotions.loading') }}</div>
      <div v-else class="alt-table-wrap">
        <table class="alt-table">
          <thead>
            <tr>
              <th style="width:4%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Hash :size="12" /> {{ t('admin.common.stt') }}</span></th>
              <th style="width:10%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Tag :size="12" /> {{ t('admin.promotions.colCode') }}</span></th>
              <th style="width:20%;"><span class="d-inline-flex align-items-center gap-1.5"><FileText :size="12" /> {{ t('admin.promotions.colName') }}</span></th>
              <th style="width:10%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Percent :size="12" /> {{ t('admin.promotions.colType') }}</span></th>
              <th style="width:10%; text-align:right;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-end"><DollarSign :size="12" /> {{ t('admin.promotions.colValue') }}</span></th>
              <th style="width:9%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Calendar :size="12" /> {{ t('admin.promotions.colStart') }}</span></th>
              <th style="width:9%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><CalendarCheck :size="12" /> {{ t('admin.promotions.colEnd') }}</span></th>
              <th style="width:9%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Users :size="12" /> {{ t('admin.promotions.colUsed') }}</span></th>
              <th style="width:9%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Activity :size="12" /> {{ t('admin.promotions.colStatus') }}</span></th>
              <th style="width:10%; text-align:center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><SlidersHorizontal :size="12" /> {{ t('admin.promotions.colAction') }}</span></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(p, idx) in pagedPromotions" :key="p.khuyenMaiId">
              <td class="text-secondary text-center">{{ currentPage * pageSize + idx + 1 }}</td>
              <td class="fw-bold text-center" style="font-family:monospace; color:var(--pink-600, #db2777);">{{ p.maKhuyenMai }}</td>
              <td>{{ p.tenKhuyenMai }}</td>
              <td class="text-center">
                <span class="alt-tag" :style="p.loai==='percent' ? 'background:rgba(236,72,153,0.12); color:#db2777;' : 'background:rgba(59,130,246,0.12); color:#2563eb;'">
                  {{ p.loai === 'percent' ? t('admin.promotions.typePercent') : t('admin.promotions.typeFixed') }}
                </span>
              </td>
              <td class="fw-semibold text-end" style="color:var(--accent-fg);">
                {{ p.loai === 'percent' ? `${p.giaTri}%` : formatPrice(p.giaTri) }}
              </td>
              <td class="text-secondary text-center">{{ formatDate(p.ngayBatDau) }}</td>
              <td class="text-secondary text-center">{{ formatDate(p.ngayKetThuc) }}</td>
              <td class="text-center">
                <span :class="p.soLuongToiDa && p.soLanDaDung >= p.soLuongToiDa ? 'text-danger fw-bold' : ''">
                  {{ p.soLanDaDung ?? 0 }}/{{ p.soLuongToiDa ?? '∞' }}
                </span>
              </td>
              <td class="text-center">
                <span class="alt-tag" :style="p.trangThai === 'active' ? 'background:rgba(22,163,74,0.14);color:var(--state-success);' : 'background:var(--bg-card-alt);color:var(--text-secondary);'">
                  {{ statusLabel(p.trangThai) }}
                </span>
              </td>
              <td class="text-center">
                <div class="d-flex justify-content-center gap-1">
                  <button class="alt-btn alt-btn--ghost" style="padding:4px 12px;" @click="openEdit(p)">{{ t('admin.promotions.edit') }}</button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredPromotions.length === 0"><td colspan="10" class="alt-empty">{{ t('admin.promotions.empty') }}</td></tr>
          </tbody>
        </table>
      </div>
      <div v-if="totalPages > 1" class="alt-pager"><Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" /></div>
    </div>

    <!-- ══ MODAL KHUYẾN MẠI ══ -->
    <div v-if="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showModal=false">
      <div class="alt-card d-flex flex-column" style="width:620px;max-width:95vw;max-height:90vh;border-radius:14px;">
        <div class="alt-toolbar">
          <span>{{ editingId ? t('admin.promoModal.titleEdit') : t('admin.promoModal.titleAdd') }}</span>
          <button class="btn-close btn-sm ms-auto" :aria-label="t('common.close')" @click="showModal=false"></button>
        </div>
        <div class="overflow-y-auto p-4">
          <div v-if="formError" class="alert alert-danger small py-2 mb-3">{{ formError }}</div>
          <div class="row g-3">
            <div class="col-6"><label class="form-label small">{{ t('admin.promoModal.codeLabel') }}</label><input v-model="form.maKhuyenMai" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.promoModal.nameLabel') }}</label><input v-model="form.tenKhuyenMai" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.promoModal.typeLabel') }}</label><select v-model="form.loai" class="form-select form-select-sm admin-input"><option value="percent">{{ t('admin.promoModal.typePercent') }}</option><option value="fixed">{{ t('admin.promoModal.typeFixed') }}</option></select></div>
            <div class="col-6"><label class="form-label small">{{ form.loai==='percent'?t('admin.promoModal.valueLabelPercent'):t('admin.promoModal.valueLabelFixed') }}</label><input v-model="form.giaTri" type="number" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.promoModal.maxDiscountLabel') }}</label><input v-model="form.giaTriToiDa" type="number" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.promoModal.minOrderLabel') }}</label><input v-model="form.donHangToiThieu" type="number" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.promoModal.startDateLabel') }}</label><input v-model="form.ngayBatDau" type="datetime-local" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.promoModal.endDateLabel') }}</label><input v-model="form.ngayKetThuc" type="datetime-local" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.promoModal.maxUsageLabel') }}</label><input v-model="form.soLuongToiDa" type="number" class="form-control form-control-sm admin-input" /></div>
            <div class="col-6"><label class="form-label small">{{ t('admin.promoModal.statusLabel') }}</label><select v-model="form.trangThai" class="form-select form-select-sm admin-input"><option value="active">{{ t('admin.promoModal.statusActive') }}</option><option value="inactive">{{ t('admin.promoModal.statusStopped') }}</option></select></div>
          </div>
        </div>
        <div class="alt-toolbar" style="border-top:1px solid var(--border-color);border-bottom:none;justify-content:flex-end;gap:8px;">
          <button class="alt-btn alt-btn--ghost" @click="showModal=false">{{ t('admin.promoModal.cancel') }}</button>
          <button class="alt-btn alt-btn--primary" @click="savePromo">{{ editingId ? t('admin.promoModal.update') : t('admin.promoModal.addNew') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ══ Nút lọc & badge ══ */
.alt-search {
  position: relative;
  display: flex;
  align-items: center;
}
.alt-search__clear {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  padding: 0;
  color: var(--text-muted, #94a3b8);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s;
}
.alt-search__clear:hover {
  color: var(--text-primary, #0f172a);
}
.filter-caret {
  transition: transform 0.2s ease;
}
.filter-caret.is-open {
  transform: rotate(180deg);
}
.alt-btn--filter-active {
  background: rgba(236, 72, 153, 0.15) !important;
  color: var(--pink-600, #db2777) !important;
  border-color: var(--pink-400, #f472b6) !important;
  font-weight: 700 !important;
}
.alt-badge-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--pink-600, #db2777);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  margin-left: 4px;
}

/* ══ Khung sập mở bộ lọc ══ */
.sm-filter-collapse {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.24s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  background: var(--bg-card-alt, #fff5f9);
  border-bottom: 1px solid var(--border-color, #f1dbe6);
}
.sm-filter-collapse.is-open {
  grid-template-rows: 1fr;
}
.sm-filter-panel {
  min-height: 0;
  padding: 0 16px;
  transition: padding 0.24s ease;
}
.sm-filter-collapse.is-open .sm-filter-panel {
  padding: 16px;
}

.sm-filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 12px 16px;
  align-items: flex-end;
}
.sm-filter-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.sm-filter-field label {
  font-size: 12px;
  font-weight: 700;
  color: var(--pink-600, #db2777);
}
.sm-filter-field input[type="date"],
.sm-filter-field select {
  width: 100%;
  padding: 7px 10px;
  font-size: 13px;
  border-radius: 8px;
  border: 1px solid var(--border-color-strong, #dba9c7);
  background: var(--bg-card, #fff);
  color: var(--text-primary, #1f2937);
  outline: none;
}
.sm-filter-field input[type="date"]:focus,
.sm-filter-field select:focus {
  border-color: var(--pink-500, #ec4899);
  box-shadow: 0 0 0 3px rgba(236, 72, 153, 0.15);
}

.sm-filter-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-color, #f1dbe6);
}
.alt-btn--sm {
  padding: 5px 12px;
  font-size: 12px;
  border-radius: 999px;
}

/* ══ Thanh Chips ══ */
.sm-active-chips {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 8px 16px;
  background: var(--bg-card, #fff);
  border-bottom: 1px solid var(--border-color, #f1dbe6);
  font-size: 12.5px;
}
.sm-chips-label {
  color: var(--text-muted, #6b7280);
  font-weight: 600;
  font-size: 12px;
}
.sm-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--bg-card-alt, #fff5f9);
  border: 1px solid var(--pink-200, #ffcfe1);
  color: var(--pink-600, #db2777);
  font-size: 12px;
}
.sm-chip button {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  color: var(--pink-600, #db2777);
  display: inline-flex;
  align-items: center;
  opacity: 0.7;
}
.sm-chip button:hover {
  opacity: 1;
}
.sm-chip-clear {
  background: none;
  border: none;
  color: var(--pink-600, #db2777);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  margin-left: 4px;
}
</style>
