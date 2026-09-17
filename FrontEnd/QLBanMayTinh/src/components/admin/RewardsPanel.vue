<script setup>
import { ref, reactive, computed, onMounted, watch } from "vue";
import { Search, Filter, RotateCcw, X, Plus, ChevronDown, Trash2, Edit2, Gift } from "@lucide/vue";
import { t } from "../../i18n/index.js";
import { formatPrice, statusLabel, boDauTiengViet } from "../../utils/adminFormat.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { DoiThuongStore, ensureDoiThuong, refreshDoiThuong } from "../../stores/doiThuong.js";
import * as DmDoiThuongService from "../../services/DmDoiThuongService.js";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";

onMounted(() => {
  ensureDoiThuong();
});

const rewards = computed(() => DoiThuongStore?.items ?? []);

// ── Tìm kiếm & Bộ lọc nâng cao ────────────────────────────────────────────────
const search = ref("");
const isFilterOpen = ref(false);
const filters = reactive({
  loai: "",       // 'percent' | 'fixed' | ''
  trangThai: "",  // 'active' | 'inactive' | ''
  diemMin: "",
  diemMax: "",
  giaTriMin: "",
  giaTriMax: "",
});

const activeFilterCount = computed(() => {
  let count = 0;
  if (filters.loai) count++;
  if (filters.trangThai) count++;
  if (filters.diemMin !== "") count++;
  if (filters.diemMax !== "") count++;
  if (filters.giaTriMin !== "") count++;
  if (filters.giaTriMax !== "") count++;
  return count;
});

const resetFilters = () => {
  filters.loai = "";
  filters.trangThai = "";
  filters.diemMin = "";
  filters.diemMax = "";
  filters.giaTriMin = "";
  filters.giaTriMax = "";
  search.value = "";
};

const filteredRewards = computed(() => {
  const rawQ = search.value.trim();
  const q = boDauTiengViet(rawQ);

  return rewards.value.filter((r) => {
    // 1. Text search
    if (q) {
      const nameMatch = boDauTiengViet(r.ten ?? "").includes(q);
      const descMatch = boDauTiengViet(r.moTa ?? "").includes(q);
      if (!nameMatch && !descMatch) return false;
    }
    // 2. Loại ưu đãi
    if (filters.loai && r.loai !== filters.loai) return false;
    // 3. Trạng thái
    if (filters.trangThai && r.trangThai !== filters.trangThai) return false;
    // 4. Điểm cần đổi
    const diem = Number(r.diemCan ?? 0);
    if (filters.diemMin !== "" && diem < Number(filters.diemMin)) return false;
    if (filters.diemMax !== "" && diem > Number(filters.diemMax)) return false;
    // 5. Giá trị ưu đãi
    const giaTri = Number(r.giaTri ?? 0);
    if (filters.giaTriMin !== "" && giaTri < Number(filters.giaTriMin)) return false;
    if (filters.giaTriMax !== "" && giaTri > Number(filters.giaTriMax)) return false;

    return true;
  });
});

const { currentPage, totalPages, pagedItems: pagedRewards, pageSize } = usePagination(filteredRewards);

// Reset trang về 0 khi có thay đổi tìm kiếm hoặc bộ lọc
watch(
  [
    search,
    () => filters.loai,
    () => filters.trangThai,
    () => filters.diemMin,
    () => filters.diemMax,
    () => filters.giaTriMin,
    () => filters.giaTriMax,
  ],
  () => {
    currentPage.value = 0;
  }
);

// ── Modal Thêm / Sửa phần thưởng ─────────────────────────────────────────────
const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const emptyForm = () => ({
  ten: "",
  moTa: "",
  diemCan: "",
  loai: "percent",
  giaTri: "",
  giaTriToiDa: "",
  trangThai: "active",
});
const form = reactive(emptyForm());

const openAdd = () => {
  Object.assign(form, emptyForm());
  editingId.value = null;
  formError.value = "";
  showModal.value = true;
};

const openEdit = (r) => {
  Object.assign(form, {
    ten: r.ten,
    moTa: r.moTa ?? "",
    diemCan: r.diemCan ?? "",
    loai: r.loai ?? "percent",
    giaTri: r.giaTri ?? "",
    giaTriToiDa: r.giaTriToiDa ?? "",
    trangThai: r.trangThai ?? "active",
  });
  editingId.value = r.doiThuongId;
  formError.value = "";
  showModal.value = true;
};

const saveReward = async () => {
  formError.value = "";
  if (!form.ten.trim()) {
    formError.value = "Vui lòng nhập tên phần thưởng";
    return;
  }
  if (form.diemCan === "" || Number(form.diemCan) < 0) {
    formError.value = "Vui lòng nhập điểm đổi hợp lệ";
    return;
  }
  if (form.giaTri === "" || Number(form.giaTri) < 0) {
    formError.value = "Vui lòng nhập giá trị hợp lệ";
    return;
  }

  const body = {
    ...form,
    diemCan: form.diemCan ? Number(form.diemCan) : 0,
    giaTri: form.giaTri ? Number(form.giaTri) : 0,
    giaTriToiDa: form.giaTriToiDa ? Number(form.giaTriToiDa) : null,
  };

  try {
    const res = await DmDoiThuongService.save(editingId.value, body);
    if (!res.ok) {
      formError.value = t("admin.errors.saveFailedWithText", {
        status: res.status,
        text: await res.text(),
      });
      return;
    }
    showModal.value = false;
    await refreshDoiThuong();
    showToast(
      editingId.value
        ? "Cập nhật phần thưởng thành công"
        : "Thêm phần thưởng mới thành công",
      "success"
    );
  } catch (e) {
    formError.value = e.message;
  }
};

const deleteReward = async (id) => {
  const confirmed = await askConfirm(
    t("admin.confirm.deleteReward") || "Bạn có chắc muốn xóa phần thưởng này không?"
  );
  if (!confirmed) return;

  const res = await DmDoiThuongService.remove(id);
  if (!res.ok) {
    showToast(t("admin.errors.deleteFailed", { status: res.status }), "error");
    return;
  }
  showToast("Đã xóa phần thưởng thành công", "success");
  await refreshDoiThuong();
};
</script>

<template>
  <div class="rewards-wrapper">
    <div class="alt-card">
      <!-- ══ TOOLBAR ══ -->
      <div class="alt-toolbar">
        <span class="alt-toolbar__count">
          {{ filteredRewards.length }}/{{ rewards.length }} {{ t("admin.rewards.countSuffix") }}
        </span>

        <div class="alt-toolbar__actions">
          <!-- Thanh tìm kiếm -->
          <div class="alt-search">
            <Search class="alt-search__icon" :size="14" />
            <input v-model="search" placeholder="Tìm theo tên phần thưởng, mô tả..." />
            <button
              v-if="search"
              type="button"
              class="alt-search__clear"
              @click="search = ''"
              title="Xóa tìm kiếm"
            >
              <X :size="12" />
            </button>
          </div>

          <!-- Nút Bộ lọc nâng cao -->
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

          <!-- Nút Thêm mới -->
          <button class="alt-btn alt-btn--primary" @click="openAdd">
            <Plus :size="13" /> {{ t("admin.rewards.add") }}
          </button>
        </div>
      </div>

      <!-- ══ DRAWER BỘ LỌC NÂNG CAO ══ -->
      <div class="sm-filter-collapse" :class="{ 'is-open': isFilterOpen }">
        <div class="sm-filter-panel">
          <div class="sm-filter-grid">
            <!-- Loại giảm giá -->
            <div class="sm-filter-field">
              <label>{{ t("admin.rewards.colType") || "Hình thức" }}</label>
              <select v-model="filters.loai" class="alt-select">
                <option value="">-- Tất cả hình thức --</option>
                <option value="percent">{{ t("admin.rewards.typePercent") }} (%)</option>
                <option value="fixed">{{ t("admin.rewards.typeFixed") }} (VNĐ)</option>
              </select>
            </div>

            <!-- Trạng thái -->
            <div class="sm-filter-field">
              <label>{{ t("admin.rewards.colStatus") || "Trạng thái" }}</label>
              <select v-model="filters.trangThai" class="alt-select">
                <option value="">-- Tất cả trạng thái --</option>
                <option value="active">{{ t("admin.rewardModal.statusActive") || "Hoạt động" }}</option>
                <option value="inactive">{{ t("admin.rewardModal.statusStopped") || "Ngừng hoạt động" }}</option>
              </select>
            </div>

            <!-- Khoảng điểm cần đổi -->
            <div class="sm-filter-field">
              <label>{{ t("admin.rewards.colPoints") || "Điểm cần đổi" }}</label>
              <div class="range-inputs">
                <input
                  v-model="filters.diemMin"
                  type="number"
                  min="0"
                  placeholder="Từ điểm"
                  class="alt-input"
                />
                <span class="range-sep">–</span>
                <input
                  v-model="filters.diemMax"
                  type="number"
                  min="0"
                  placeholder="Đến điểm"
                  class="alt-input"
                />
              </div>
            </div>

            <!-- Khoảng giá trị ưu đãi -->
            <div class="sm-filter-field">
              <label>{{ t("admin.rewards.colValue") || "Giá trị ưu đãi" }}</label>
              <div class="range-inputs">
                <input
                  v-model="filters.giaTriMin"
                  type="number"
                  min="0"
                  placeholder="Từ giá trị"
                  class="alt-input"
                />
                <span class="range-sep">–</span>
                <input
                  v-model="filters.giaTriMax"
                  type="number"
                  min="0"
                  placeholder="Đến giá trị"
                  class="alt-input"
                />
              </div>
            </div>
          </div>

          <!-- Nút chân bộ lọc -->
          <div class="sm-filter-foot">
            <button
              type="button"
              class="alt-btn alt-btn--ghost alt-btn--sm"
              :disabled="activeFilterCount === 0"
              @click="resetFilters"
            >
              <RotateCcw :size="12" /> Xóa bộ lọc
            </button>
            <button
              type="button"
              class="alt-btn alt-btn--primary alt-btn--sm"
              @click="isFilterOpen = false"
            >
              Hoàn tất
            </button>
          </div>
        </div>
      </div>

      <!-- ══ CHIPS CÁC BỘ LỌC ĐANG ÁP DỤNG ══ -->
      <div v-if="activeFilterCount > 0" class="sm-active-chips">
        <span class="sm-chips-label">Đang lọc:</span>

        <span v-if="filters.loai" class="sm-chip">
          Loại: <b>{{ filters.loai === 'percent' ? 'Giảm %' : 'Giảm tiền' }}</b>
          <button type="button" @click="filters.loai = ''"><X :size="10" /></button>
        </span>

        <span v-if="filters.trangThai" class="sm-chip">
          Trạng thái: <b>{{ filters.trangThai === 'active' ? 'Hoạt động' : 'Ngừng' }}</b>
          <button type="button" @click="filters.trangThai = ''"><X :size="10" /></button>
        </span>

        <span v-if="filters.diemMin !== '' || filters.diemMax !== ''" class="sm-chip">
          Điểm: <b>{{ filters.diemMin || 0 }} – {{ filters.diemMax || '∞' }}</b>
          <button type="button" @click="filters.diemMin = ''; filters.diemMax = ''"><X :size="10" /></button>
        </span>

        <span v-if="filters.giaTriMin !== '' || filters.giaTriMax !== ''" class="sm-chip">
          Giá trị: <b>{{ filters.giaTriMin || 0 }} – {{ filters.giaTriMax || '∞' }}</b>
          <button type="button" @click="filters.giaTriMin = ''; filters.giaTriMax = ''"><X :size="10" /></button>
        </span>

        <button type="button" class="sm-chip-clear" @click="resetFilters">Xóa tất cả</button>
      </div>

      <!-- ══ BẢNG DANH SÁCH ══ -->
      <div v-if="DoiThuongStore.loading" class="alt-empty">{{ t("admin.rewards.loading") }}</div>
      <div v-else class="alt-table-wrap">
        <table class="alt-table">
          <thead>
            <tr>
              <th style="width: 40px;">{{ t("admin.common.stt") }}</th>
              <th>{{ t("admin.rewards.colName") }}</th>
              <th>Mô tả</th>
              <th>{{ t("admin.rewards.colPoints") }}</th>
              <th>{{ t("admin.rewards.colType") }}</th>
              <th>{{ t("admin.rewards.colValue") }}</th>
              <th>Giảm tối đa</th>
              <th>{{ t("admin.rewards.colStatus") }}</th>
              <th style="width: 140px;">{{ t("admin.rewards.colAction") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(r, idx) in pagedRewards" :key="r.doiThuongId">
              <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
              <td class="fw-bold" style="color: var(--text-primary);">{{ r.ten }}</td>
              <td class="text-secondary small" style="max-width: 200px;">
                {{ r.moTa || '—' }}
              </td>
              <td>
                <span class="badge-points">
                  <Gift :size="12" /> {{ r.diemCan?.toLocaleString() }} điểm
                </span>
              </td>
              <td>
                <span
                  class="alt-tag"
                  :style="
                    r.loai === 'percent'
                      ? 'background:rgba(236,72,153,0.12); color:#db2777;'
                      : 'background:rgba(59,130,246,0.12); color:#2563eb;'
                  "
                >
                  {{ r.loai === 'percent' ? t("admin.rewards.typePercent") : t("admin.rewards.typeFixed") }}
                </span>
              </td>
              <td class="fw-semibold" style="color: var(--accent-fg);">
                {{ r.loai === 'percent' ? `${r.giaTri}%` : formatPrice(r.giaTri) }}
              </td>
              <td class="text-secondary">
                {{ r.giaTriToiDa ? formatPrice(r.giaTriToiDa) : '—' }}
              </td>
              <td>
                <span
                  class="alt-tag"
                  :style="
                    r.trangThai === 'active'
                      ? 'background:rgba(22,163,74,0.14);color:var(--state-success);'
                      : 'background:var(--bg-card-alt);color:var(--text-secondary);'
                  "
                >
                  {{ statusLabel(r.trangThai) }}
                </span>
              </td>
              <td>
                <div class="d-flex gap-1">
                  <button
                    class="alt-btn alt-btn--ghost"
                    style="padding: 4px 10px;"
                    @click="openEdit(r)"
                    title="Chỉnh sửa"
                  >
                    <Edit2 :size="12" /> {{ t("admin.rewards.edit") }}
                  </button>
                  <button
                    class="alt-btn alt-btn--ghost"
                    style="padding: 4px 10px; color: var(--state-danger); border-color: var(--state-danger);"
                    @click="deleteReward(r.doiThuongId)"
                    title="Xóa"
                  >
                    <Trash2 :size="12" />
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredRewards.length === 0">
              <td colspan="9" class="alt-empty">{{ t("admin.rewards.empty") }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Phân trang -->
      <div v-if="totalPages > 1" class="alt-pager">
        <Pagination
          :current-page="currentPage"
          :total-pages="totalPages"
          @page-change="currentPage = $event"
        />
      </div>
    </div>

    <!-- ══ MODAL THÊM / SỬA PHẦN THƯỞNG ══ -->
    <div
      v-if="showModal"
      class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
      style="background: var(--bg-overlay); z-index: 1000;"
      @click.self="showModal = false"
    >
      <div
        class="alt-card d-flex flex-column"
        style="width: 620px; max-width: 95vw; max-height: 90vh; border-radius: 14px;"
      >
        <div class="alt-toolbar">
          <span>{{ editingId ? t("admin.rewardModal.titleEdit") : t("admin.rewardModal.titleAdd") }}</span>
          <button
            class="btn-close btn-sm ms-auto"
            :aria-label="t('common.close')"
            @click="showModal = false"
          ></button>
        </div>
        <div class="overflow-y-auto p-4">
          <div v-if="formError" class="alert alert-danger small py-2 mb-3">{{ formError }}</div>
          <div class="row g-3">
            <div class="col-8">
              <label class="form-label small">{{ t("admin.rewardModal.nameLabel") }}</label>
              <input v-model="form.ten" class="form-control form-control-sm admin-input" placeholder="Ví dụ: Voucher giảm 10%" />
            </div>
            <div class="col-4">
              <label class="form-label small">{{ t("admin.rewardModal.pointsLabel") }}</label>
              <input v-model="form.diemCan" type="number" min="0" class="form-control form-control-sm admin-input" />
            </div>
            <div class="col-12">
              <label class="form-label small">{{ t("admin.rewardModal.descLabel") }}</label>
              <textarea v-model="form.moTa" rows="2" class="form-control form-control-sm admin-input" placeholder="Mô tả chi tiết quyền lợi..."></textarea>
            </div>
            <div class="col-6">
              <label class="form-label small">{{ t("admin.rewardModal.typeLabel") }}</label>
              <select v-model="form.loai" class="form-select form-select-sm admin-input">
                <option value="percent">{{ t("admin.rewardModal.typePercent") }}</option>
                <option value="fixed">{{ t("admin.rewardModal.typeFixed") }}</option>
              </select>
            </div>
            <div class="col-6">
              <label class="form-label small">
                {{ form.loai === "percent" ? t("admin.rewardModal.valueLabelPercent") : t("admin.rewardModal.valueLabelFixed") }}
              </label>
              <input v-model="form.giaTri" type="number" min="0" class="form-control form-control-sm admin-input" />
            </div>
            <div class="col-6">
              <label class="form-label small">{{ t("admin.rewardModal.maxDiscountLabel") }}</label>
              <input v-model="form.giaTriToiDa" type="number" min="0" class="form-control form-control-sm admin-input" placeholder="Không bắt buộc nếu là số tiền" />
            </div>
            <div class="col-6">
              <label class="form-label small">{{ t("admin.rewardModal.statusLabel") }}</label>
              <select v-model="form.trangThai" class="form-select form-select-sm admin-input">
                <option value="active">{{ t("admin.rewardModal.statusActive") }}</option>
                <option value="inactive">{{ t("admin.rewardModal.statusStopped") }}</option>
              </select>
            </div>
          </div>
        </div>
        <div
          class="alt-toolbar"
          style="border-top: 1px solid var(--border-color); border-bottom: none; justify-content: flex-end; gap: 8px;"
        >
          <button class="alt-btn alt-btn--ghost" @click="showModal = false">{{ t("admin.rewardModal.cancel") }}</button>
          <button class="alt-btn alt-btn--primary" @click="saveReward">
            {{ editingId ? t("admin.rewardModal.update") : t("admin.rewardModal.addNew") }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ══ Toolbar & Tìm kiếm ══ */
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

/* ══ Drawer Bộ lọc ══ */
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
.sm-filter-field select,
.sm-filter-field input {
  width: 100%;
  padding: 7px 10px;
  font-size: 13px;
  border-radius: 8px;
  border: 1px solid var(--border-color-strong, #dba9c7);
  background: var(--bg-card, #fff);
  color: var(--text-primary, #1f2937);
  outline: none;
}
.sm-filter-field select:focus,
.sm-filter-field input:focus {
  border-color: var(--pink-500, #ec4899);
  box-shadow: 0 0 0 3px rgba(236, 72, 153, 0.15);
}

.range-inputs {
  display: flex;
  align-items: center;
  gap: 6px;
}
.range-inputs input {
  flex: 1;
}
.range-sep {
  color: var(--text-muted, #94a3b8);
  font-weight: 600;
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

/* ══ Badge điểm ══ */
.badge-points {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(234, 179, 8, 0.15);
  color: #b45309;
  font-weight: 600;
  font-size: 12px;
}
</style>
