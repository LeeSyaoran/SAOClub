<script setup>
import { ref, computed, onMounted } from "vue";
import { Search, Filter, ChevronDown, Plus } from "@lucide/vue";
import { Hash, FolderOpen, X, Cpu, MemoryStick, HardDrive, Monitor, Image as ImageIcon } from "@lucide/vue";
import { t } from "../../i18n/index.js";
import { showToast } from "../../stores/toast.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { formatDate } from "../../utils/adminFormat.js";
import * as XLSX from "xlsx";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";

const props = defineProps({
  service: { type: Object, required: true },
  idField: { type: String, required: true },
  nameField: { type: String, required: true },
  label: { type: String, required: true },
  nameLabel: { type: String, required: true },
  serialService: { type: Object, required: true },
  serialFieldName: { type: String, required: true },
  // Header icon - truyen component hoac string icon name
  headerIcon: { type: [Object, String, Function], default: null },
  // Advanced filter config: { filters: [{ key, label, type, options }] }
  advancedFilterConfig: { type: Object, default: null },
});

// ── Load data ──────────────────────────────────────────────────────────────
const items = ref([]);
const loading = ref(false);
const serials = ref([]);

const load = async () => {
  loading.value = true;
  try {
    [items.value, serials.value] = await Promise.all([
      props.service.getAll().catch(() => []),
      props.serialService.getAll().catch(() => []),
    ]);
  } finally {
    loading.value = false;
  }
};
onMounted(load);

const serialsOf = (item) => serials.value.filter((s) => s[props.serialFieldName] === item[props.idField]);
const stockCountOf = (item) => serialsOf(item).filter((s) => s.trangThai === "trong_kho").length;

// ── Search ────────────────────────────────────────────────────────────────
const search = ref("");

// ── Advanced filters ─────────────────────────────────────────────────────
const showAdvanced = ref(false);
const activeFilters = ref({});

const initFilters = () => {
  if (!props.advancedFilterConfig?.filters) return;
  const f = {};
  props.advancedFilterConfig.filters.forEach((filterDef) => {
    f[filterDef.key] = "";
  });
  activeFilters.value = f;
};
initFilters();

const activeFilterCount = computed(() =>
  Object.values(activeFilters.value).filter(Boolean).length
);

const clearFilters = () => {
  initFilters();
};

// Extract filter options tu data
const getFilterOptions = (filterDef) => {
  if (filterDef.options) return filterDef.options;
  // Auto-generate options from items
  const key = filterDef.key;
  const extractors = {
    // CPU
    hang: (name) => {
      const n = (name || "").toLowerCase();
      if (n.includes("amd")) return "AMD";
      if (n.includes("intel")) return "Intel";
      return null;
    },
    dong: (name) => {
      const n = (name || "").toLowerCase();
      if (n.includes("ryzen 9") || n.includes("r9")) return "Ryzen 9";
      if (n.includes("ryzen 7") || n.includes("r7")) return "Ryzen 7";
      if (n.includes("ryzen 5") || n.includes("r5")) return "Ryzen 5";
      if (n.includes("ryzen 3") || n.includes("r3")) return "Ryzen 3";
      if (n.includes("i9") || n.includes("core i9")) return "Core i9";
      if (n.includes("i7") || n.includes("core i7")) return "Core i7";
      if (n.includes("i5") || n.includes("core i5")) return "Core i5";
      if (n.includes("i3") || n.includes("core i3")) return "Core i3";
      return null;
    },
    // GPU
    hang: (name) => {
      const n = (name || "").toLowerCase();
      if (n.includes("nvidia") || n.includes("geforce") || n.includes("rtx") || n.includes("gtx"))
        return "NVIDIA";
      if (n.includes("amd") || n.includes("radeon") || n.includes("rx ")) return "AMD";
      if (n.includes("intel")) return "Intel";
      return null;
    },
    vram: (name) => {
      const n = (name || "");
      const match = n.match(/(\d+)\s*GB/i);
      return match ? `${match[1]}GB` : null;
    },
    // Ổ cứng
    loai: (name) => {
      const n = (name || "").toLowerCase();
      if (n.includes("ssd")) return "SSD";
      if (n.includes("hdd")) return "HDD";
      return null;
    },
    dungluong: (name) => {
      const n = (name || "");
      // Match "512GB", "1TB", "2 TB"
      const match = n.match(/(\d+)\s*(gb|tb)/i);
      if (!match) return null;
      const val = parseInt(match[1]);
      const unit = match[2].toUpperCase();
      if (unit === "TB") return `${val}TB`;
      if (val >= 1000) return `${val / 1000}TB`;
      return `${val}GB`;
    },
    // RAM
    loai: (name) => {
      const n = (name || "").toUpperCase();
      if (n.includes("DDR5")) return "DDR5";
      if (n.includes("DDR4")) return "DDR4";
      if (n.includes("DDR3")) return "DDR3";
      return null;
    },
    dungluong: (name) => {
      const n = (name || "");
      const match = n.match(/(\d+)\s*GB/i);
      if (!match) return null;
      const val = parseInt(match[1]);
      return `${val}GB`;
    },
  };
  const extractor = extractors[key];
  if (!extractor) return [];
  const set = new Set(items.value.map((i) => extractor(i[props.nameField])).filter(Boolean));
  return [...set].sort().map((v) => ({ value: v, label: v }));
};

// Filter logic
const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase();
  const all = items.value;

  return all.filter((item) => {
    // Text search
    if (q && !(item[props.nameField] || "").toLowerCase().includes(q)) return false;

    // Advanced filters
    if (!props.advancedFilterConfig?.filters) return true;
    for (const filterDef of props.advancedFilterConfig.filters) {
      const val = activeFilters.value[filterDef.key];
      if (!val) continue;
      const extractor = {
        hang: (name) => {
          const n = (name || "").toLowerCase();
          if (filterDef.key === "hang") {
            if (props.label === "CPU") {
              if (val === "AMD" && n.includes("amd")) return true;
              if (val === "Intel" && n.includes("intel")) return true;
            }
            if (props.label === "GPU") {
              if (val === "NVIDIA" && (n.includes("nvidia") || n.includes("geforce") || n.includes("rtx") || n.includes("gtx")))
                return true;
              if (val === "AMD" && (n.includes("amd") || n.includes("radeon"))) return true;
              if (val === "Intel" && n.includes("intel")) return true;
            }
          }
          return false;
        },
        dong: (name) => {
          const n = (name || "").toLowerCase();
          if (val === "Ryzen 9" && (n.includes("ryzen 9") || n.includes("r9"))) return true;
          if (val === "Ryzen 7" && (n.includes("ryzen 7") || n.includes("r7"))) return true;
          if (val === "Ryzen 5" && (n.includes("ryzen 5") || n.includes("r5"))) return true;
          if (val === "Ryzen 3" && (n.includes("ryzen 3") || n.includes("r3"))) return true;
          if (val === "Core i9" && n.includes("i9")) return true;
          if (val === "Core i7" && n.includes("i7")) return true;
          if (val === "Core i5" && n.includes("i5")) return true;
          if (val === "Core i3" && n.includes("i3")) return true;
          return false;
        },
        vram: (name) => {
          const n = name || "";
          const match = n.match(/(\d+)\s*GB/i);
          return match ? `${match[1]}GB` === val : false;
        },
        loai: (name) => {
          const n = (name || "").toUpperCase();
          if (val === "SSD" && n.includes("SSD")) return true;
          if (val === "HDD" && n.includes("HDD")) return true;
          if (val === "DDR5" && n.includes("DDR5")) return true;
          if (val === "DDR4" && n.includes("DDR4")) return true;
          if (val === "DDR3" && n.includes("DDR3")) return true;
          return false;
        },
        dungluong: (name) => {
          const n = name || "";
          // Ổ cứng
          const ocMatch = n.match(/(\d+)\s*(gb|tb)/i);
          if (ocMatch) {
            const val2 = parseInt(ocMatch[1]);
            const unit = ocMatch[2].toUpperCase();
            const normalized = unit === "TB" ? `${val2}TB` : `${val2}GB`;
            if (normalized === val) return true;
          }
          // RAM
          const ramMatch = n.match(/(\d+)\s*GB/i);
          if (ramMatch) {
            const ramVal = `${parseInt(ramMatch[1])}GB`;
            if (ramVal === val) return true;
          }
          return false;
        },
      }[filterDef.key];
      if (extractor && !extractor(item[props.nameField])) return false;
    }
    return true;
  });
});

const { currentPage, totalPages, pagedItems: pagedRows, pageSize } = usePagination(filteredItems);

// ── Modal them/sua ──────────────────────────────────────────────────────────
const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const formValue = ref({});
const saving = ref(false);

const newSerials = ref([""]);
const addSerialRow = () => newSerials.value.push("");
const removeSerialRow = (idx) => {
  if (newSerials.value.length > 1) newSerials.value.splice(idx, 1);
  else newSerials.value[idx] = "";
};
const importSerialsFromFile = async (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  const ext = file.name.split(".").pop()?.toLowerCase();
  let parsed;
  if (ext === "xlsx" || ext === "xls") {
    const buf = await file.arrayBuffer();
    const wb = XLSX.read(buf, { type: "array" });
    const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { header: 1 });
    parsed = rows.flat().map((v) => String(v ?? "").trim()).filter(Boolean);
  } else {
    const text = await file.text();
    parsed = text.split(/[\n,]+/).map((s) => s.trim()).filter(Boolean);
  }
  const existing = newSerials.value.filter(Boolean);
  newSerials.value = [...existing, ...parsed].length ? [...existing, ...parsed] : [""];
  e.target.value = "";
};

const openAdd = () => {
  editingId.value = null;
  formValue.value = { [props.nameField]: "", hinhAnh: "" };
  formError.value = "";
  newSerials.value = [""];
  showModal.value = true;
};
const openEdit = (item) => {
  editingId.value = item[props.idField];
  formValue.value = { ...item };
  formError.value = "";
  newSerials.value = [""];
  showModal.value = true;
};

const saveItem = async () => {
  formError.value = "";
  if (!formValue.value[props.nameField]?.trim()) {
    formError.value = t("admin.dmCategory.nameRequired", { label: props.nameLabel });
    return;
  }
  const serialList = newSerials.value.map((s) => s.trim()).filter(Boolean);
  if (!editingId.value && serialList.length === 0) {
    formError.value = t("admin.dmCategory.serialRequired", { label: props.nameLabel });
    return;
  }
  if (saving.value) return;
  saving.value = true;
  try {
    const payload = {
      [props.nameField]: formValue.value[props.nameField].trim(),
      hinhAnh: formValue.value.hinhAnh?.trim() || null
    };
    let res;
    if (editingId.value) {
      res = await props.service.update(editingId.value, payload);
    } else {
      res = await props.service.create(payload);
    }
    if (!res.ok) {
      formError.value = t("admin.errors.saveFailed", { status: res.status, text: await res.text() });
      return;
    }
    if (!editingId.value) {
      const created = await res.json();
      const newId = created[props.idField];
      for (const soSerial of serialList) {
        const sres = await props.serialService.create({
          [props.serialFieldName]: newId,
          soSerial,
          trangThai: "trong_kho",
          ngayNhapKho: nowLocalIso(),
        });
        if (!sres.ok) {
          showToast(await sres.text().catch(() => t("admin.errors.addSerialError")));
          return;
        }
      }
    }
    showModal.value = false;
    await load();
  } catch (e) {
    formError.value = e.message;
  } finally {
    saving.value = false;
  }
};

// ── Serial modal ────────────────────────────────────────────────────────────
const showSerialsModal = ref(false);
const serialsModalItem = ref(null);
const openSerials = (item) => {
  serialsModalItem.value = item;
  showSerialsModal.value = true;
};
</script>

<template>
  <div class="dm-card">
    <!-- Header icon + title -->
    <div v-if="props.headerIcon" class="dm-header">
      <div class="dm-header__icon">
        <component v-if="typeof props.headerIcon === 'object' || typeof props.headerIcon === 'function'" :is="props.headerIcon" :size="32" />
        <i v-else :class="props.headerIcon" style="font-size: 32px;"></i>
      </div>
      <div class="dm-header__text">
        <h2 class="dm-header__title">{{ label }}</h2>
        <p class="dm-header__sub">{{ t("admin.dmCategory.subtitle") }}</p>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="alt-toolbar">
      <span class="alt-toolbar__count">
        {{ filteredItems.length }}/{{ items.length }}
        {{ t("admin.dmCategory.countSuffix", { label: "" }) }}
        {{ label.toLowerCase() }}
      </span>
      <div class="alt-toolbar__actions">
        <div class="alt-search">
          <Search class="alt-search__icon" :size="14" />
          <input v-model="search" :placeholder="t('admin.dmCategory.searchPlaceholder', { label })" />
          <button v-if="search" class="dm-search__clear" @click="search = ''">
            <X :size="12" />
          </button>
        </div>

        <!-- Toggle bo loc nang cao -->
        <button
          v-if="props.advancedFilterConfig?.filters?.length"
          class="alt-btn alt-btn--ghost"
          :class="{ 'is-active': showAdvanced || activeFilterCount > 0 }"
          @click="showAdvanced = !showAdvanced"
        >
          <Filter :size="14" />
          <span>Bộ lọc</span>
          <span v-if="activeFilterCount > 0" class="dm-filter-chip">{{ activeFilterCount }}</span>
          <ChevronDown style="font-size:0.7rem;transition:transform 0.2s;" :size="14" />
        </button>

        <button class="alt-btn alt-btn--primary" @click="openAdd">
          <Plus :size="14" /> {{ t("admin.dmCategory.add", { label }) }}
        </button>
      </div>
    </div>

    <!-- Bo loc nang cao -->
    <div v-if="showAdvanced && props.advancedFilterConfig?.filters?.length" class="dm-advanced-filter">
      <div class="dm-filter-row">
        <div
          v-for="filterDef in props.advancedFilterConfig.filters"
          :key="filterDef.key"
          class="dm-filter-group"
        >
          <label class="dm-filter-label">{{ filterDef.label }}</label>
          <select v-model="activeFilters[filterDef.key]" class="dm-filter-select">
            <option value="">Tất cả</option>
            <option
              v-for="opt in getFilterOptions(filterDef)"
              :key="opt.value"
              :value="opt.value"
            >{{ opt.label }}</option>
          </select>
        </div>

        <button v-if="activeFilterCount > 0" class="dm-filter-clear" @click="clearFilters">
          <X :size="13" /> Xóa lọc
        </button>
      </div>
    </div>

    <!-- Bang -->
    <div v-if="loading" class="alt-empty">{{ t("admin.dmCategory.loading") }}</div>
    <div v-else class="alt-table-wrap">
      <table class="alt-table">
        <thead>
          <tr>
            <th style="width:40px;">{{ t("admin.common.stt") }}</th>
            <th style="width:80px;">Hình ảnh</th>
            <th>{{ nameLabel }}</th>
            <th style="width:150px;">{{ t("admin.dmCategory.colStock") }}</th>
            <th style="width:140px;">{{ t("admin.dmCategory.colAction") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, idx) in pagedRows" :key="item[idField]">
            <td class="dm-stt">{{ currentPage * pageSize + idx + 1 }}</td>
            <td>
              <div style="width: 40px; height: 40px; border-radius: 6px; overflow: hidden; background: var(--bg-input); display: flex; align-items: center; justify-content: center;">
                <img v-if="item.hinhAnh && !item.imgError" :src="item.hinhAnh" @error="item.imgError = true" style="width: 100%; height: 100%; object-fit: contain;" />
                <ImageIcon v-else class="text-secondary" :size="20" />
              </div>
            </td>
            <td class="dm-name">{{ item[nameField] }}</td>
            <td>
              <span class="dm-stock-badge" :class="{ 'dm-stock--zero': stockCountOf(item) === 0 }">
                {{ stockCountOf(item) }}
              </span>
            </td>
            <td>
              <div class="d-flex gap-1">
                <button class="alt-btn alt-btn--ghost" style="padding:4px 10px;" @click="openSerials(item)">
                  <Hash :size="12" style="vertical-align:-2px;" />
                  {{ t("admin.dmCategory.viewSerials", { count: stockCountOf(item) }) }}
                </button>
                <button class="alt-btn alt-btn--ghost" style="padding:4px 10px;" @click="openEdit(item)">
                  {{ t("admin.dmCategory.edit") }}
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredItems.length === 0">
            <td colspan="4" class="alt-empty">
              {{ search || activeFilterCount > 0
                ? (t("admin.dmCategory.noResult") || "Không có kết quả phù hợp")
                : (t("admin.dmCategory.empty", { label })) }}
            </td>
          </tr>
        </tbody>
      </table>
      <Pagination
        v-if="totalPages > 1"
        class="alt-pager"
        :current-page="currentPage"
        :total-pages="totalPages"
        @page-change="currentPage = $event"
      />
    </div>
  </div>

  <!-- Modal them/sua -->
  <div
    v-if="showModal"
    class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background:rgba(0,0,0,0.6);z-index:1000;backdrop-filter:blur(2px);"
    @click.self="showModal = false"
  >
    <div class="rounded-3 p-4" style="background:var(--bg-card);width:420px;max-width:94vw;box-shadow:0 24px 64px rgba(0,0,0,0.5);">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);font-size:1rem;">
          {{ editingId ? t("admin.dmCategory.titleEdit", { label }) : t("admin.dmCategory.titleAdd", { label }) }}
        </div>
        <button class="dm-modal-close" :aria-label="t('common.close')" @click="showModal = false">
          <X :size="16" />
        </button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-3">{{ formError }}</div>
      <div class="mb-4">
        <label class="form-label" style="font-size:0.8rem;color:var(--text-secondary);">
          Hình ảnh (URL)
        </label>
        <div class="d-flex gap-3 align-items-center">
          <div style="width: 60px; height: 60px; border-radius: 8px; overflow: hidden; background: var(--bg-input); flex-shrink: 0; display: flex; align-items: center; justify-content: center; border: 1px solid var(--border-color);">
            <img v-if="formValue?.hinhAnh && !formValue.imgError" :src="formValue.hinhAnh" @error="formValue.imgError = true" style="width:100%; height:100%; object-fit: contain;" />
            <ImageIcon v-else class="text-secondary" :size="28" />
          </div>
          <input
            v-model="formValue.hinhAnh"
            class="form-control admin-input"
            placeholder="Nhập đường dẫn ảnh..."
          />
        </div>
      </div>

      <div class="mb-4">
        <label class="form-label" style="font-size:0.8rem;color:var(--text-secondary);">
          {{ nameLabel }} <span class="text-danger">*</span>
        </label>
        <input
          v-model="formValue[nameField]"
          class="form-control admin-input"
          :placeholder="`Nhập ${nameLabel.toLowerCase()}...`"
          required
        />
      </div>
      <div v-if="!editingId" class="mb-3">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <label class="form-label small text-secondary mb-0">{{ t("admin.stockModal.newSerialsLabel") }}</label>
          <label class="btn btn-sm btn-outline-info" style="padding:2px 10px;font-size:0.72rem;cursor:pointer;">
            <FolderOpen :size="14" style="vertical-align:-2px;" /> {{ t("admin.stockModal.importFromFile") }}
            <input type="file" accept=".csv,.txt,.xlsx,.xls" class="d-none" @change="importSerialsFromFile" />
          </label>
        </div>
        <div class="d-flex flex-column gap-2">
          <div v-for="(s, idx) in newSerials" :key="idx" class="d-flex gap-2 align-items-center">
            <input v-model="newSerials[idx]" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" :placeholder="t('admin.stockModal.serialPlaceholder')" />
            <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;" :aria-label="t('common.remove')" @click="removeSerialRow(idx)">
              <X :size="14" />
            </button>
          </div>
        </div>
        <button class="btn btn-sm btn-outline-warning mt-2" @click="addSerialRow">{{ t("admin.stockModal.addSerialRow") }}</button>
        <div class="text-secondary mt-1" style="font-size:0.72rem;">{{ t("admin.stockModal.importHint") }}</div>
      </div>
      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showModal = false">{{ t("admin.dmCategory.cancel") }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="saveItem">{{ t("admin.dmCategory.save") }}</button>
      </div>
    </div>
  </div>

  <!-- Modal xem serial -->
  <div
    v-if="showSerialsModal"
    class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background:rgba(0,0,0,0.6);z-index:1000;backdrop-filter:blur(2px);"
    @click.self="showSerialsModal = false"
  >
    <div class="rounded-3 p-4 d-flex flex-column" style="background:var(--bg-card);width:460px;max-width:94vw;max-height:70vh;box-shadow:0 24px 64px rgba(0,0,0,0.5);">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">
          {{ t("admin.dmCategory.serialsModalTitle", { name: serialsModalItem?.[nameField] }) }}
          <span class="dm-serial-count">{{ serialsOf(serialsModalItem).length }} serial</span>
        </div>
        <button class="dm-modal-close" :aria-label="t('common.close')" @click="showSerialsModal = false">
          <X :size="16" />
        </button>
      </div>
      <div class="overflow-y-auto d-flex flex-column gap-1 flex-grow-1">
        <div v-if="serialsOf(serialsModalItem).length === 0" class="text-secondary small text-center py-4">
          {{ t("admin.serialManager.empty") }}
        </div>
        <div
          v-for="s in serialsOf(serialsModalItem)"
          :key="s.soSerial"
          class="dm-serial-row"
        >
          <span class="dm-serial-number">{{ s.soSerial }}</span>
          <span class="dm-serial-status" :class="`dm-serial-status--${s.trangThai}`">
            {{ t(`admin.statusLabel.${s.trangThai}`) }}
          </span>
          <span class="dm-serial-date">{{ formatDate(s.ngayNhapKho) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── Card ────────────────────────────────────────────────────────── */
.dm-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
}

/* ── Header ─────────────────────────────────────────────────────── */
.dm-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border-color);
}
.dm-header__icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: rgba(72, 199, 142, 0.12);
  color: #48c78e;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.dm-header__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-heading);
}
.dm-header__sub {
  margin: 2px 0 0;
  font-size: 0.78rem;
  color: var(--text-muted);
}

/* ── Search clear ────────────────────────────────────────────────── */
.dm-search__clear {
  background: none;
  border: none;
  padding: 0 6px;
  cursor: pointer;
  color: var(--text-muted);
  display: flex;
  align-items: center;
}
.dm-search__clear:hover { color: var(--text-primary); }

.dm-filter-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #48c78e;
  color: #fff;
  font-size: 0.65rem;
  font-weight: 700;
}

/* ── Advanced filter ─────────────────────────────────────────────── */
.dm-advanced-filter {
  padding: 12px 16px;
  background: var(--bg-card-inset);
  border-bottom: 1px solid var(--border-color);
}
.dm-filter-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}
.dm-filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 130px;
}
.dm-filter-label {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.dm-filter-select {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid var(--border-color-strong);
  background: var(--bg-input);
  color: var(--text-primary);
  font-size: 0.82rem;
}
.dm-filter-clear {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid var(--border-color-strong);
  background: transparent;
  color: var(--text-secondary);
  font-size: 0.8rem;
  cursor: pointer;
  white-space: nowrap;
  align-self: flex-end;
}
.dm-filter-clear:hover {
  background: var(--bg-card-alt);
  color: var(--text-primary);
}

/* ── Table cells ───────────────────────────────────────────────── */
.dm-stt {
  color: var(--text-muted);
  font-size: 0.78rem;
  text-align: center;
}
.dm-name {
  font-weight: 600;
  color: var(--text-primary);
}
.dm-stock-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 22px;
  padding: 0 8px;
  border-radius: 6px;
  background: rgba(72, 199, 142, 0.15);
  color: #48c78e;
  font-size: 0.78rem;
  font-weight: 600;
}
.dm-stock-badge.dm-stock--zero {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

/* ── Serial modal ───────────────────────────────────────────────── */
.dm-serial-count {
  font-size: 0.75rem;
  font-weight: 400;
  color: var(--text-muted);
  margin-left: 8px;
}
.dm-serial-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  background: var(--bg-hover);
  font-size: 0.82rem;
}
.dm-serial-number {
  font-family: monospace;
  flex: 1;
}
.dm-serial-status {
  font-size: 0.72rem;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--bg-card-alt);
  color: var(--text-secondary);
}
.dm-serial-status--trong_kho {
  background: rgba(72, 199, 142, 0.15);
  color: #48c78e;
}
.dm-serial-status--da_ban {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}
.dm-serial-status--giu_hang {
  background: rgba(139, 99, 210, 0.15);
  color: #8a63d2;
}
.dm-serial-date {
  font-size: 0.72rem;
  color: var(--text-muted);
  white-space: nowrap;
}

/* ── Modal close ────────────────────────────────────────────────── */
.dm-modal-close {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background: var(--bg-card-alt);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.dm-modal-close:hover {
  background: var(--bg-card-alt);
  color: var(--text-primary);
}
</style>
