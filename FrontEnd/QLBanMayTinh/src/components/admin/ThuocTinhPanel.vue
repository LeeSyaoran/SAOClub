<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import { showToast } from "../../stores/toast.js";
import { ThuocTinhService } from "../../services/ThuocTinhService.js";
import * as DmService from "../../services/DmService.js";
import {
  ChiTietCpuService,
  ChiTietRamService,
  ChiTietGpuService,
  ChiTietOCungService,
} from "../../services/ChiTietLinhKienService.js";
import DmCategoryTable from "./DmCategoryTable.vue";
import {
  Settings,
  Palette,
  Battery,
  Scale,
  Monitor,
  Plus,
  Pencil,
  Trash2,
  Check,
  X,
  ChevronDown,
  ChevronUp,
  Cpu,
  MemoryStick,
  HardDrive,
  SlidersHorizontal,
  Sparkles,
  Layers,
  HelpCircle,
  Tag,
  ListFilter,
  CheckCircle2,
} from "@lucide/vue";

// ── Tab Management ─────────────────────────────────────────────────────────
const THUOC_TINH_TAB_KEY = "admin.thuocTinh.tab";
const currentTab = ref(sessionStorage.getItem(THUOC_TINH_TAB_KEY) || "general");
const visitedTabs = ref({ general: true, [currentTab.value]: true });

const setTab = (tab) => {
  currentTab.value = tab;
  visitedTabs.value[tab] = true;
  sessionStorage.setItem(THUOC_TINH_TAB_KEY, tab);
};

// ── Color Swatch Helper for "Màu sắc" ──────────────────────────────────────
const COLOR_HEX_MAP = {
  "đen": "#18181b",
  "trắng": "#f8fafc",
  "bạc": "#cbd5e1",
  "xám": "#64748b",
  "xanh dương": "#3b82f6",
  "xanh lá": "#22c55e",
  "đỏ": "#ef4444",
  "vàng": "#eab308",
  "hồng": "#ec4899",
  "tím": "#a855f7",
  "cam": "#f97316",
  "nâu": "#78350f",
  "gold": "#d4af37",
  "midnight": "#1e293b",
  "starlight": "#f1f5f9",
  "space gray": "#475569"
};

const getColorHex = (name) => {
  if (!name) return null;
  const lower = name.toLowerCase().trim();
  for (const [k, v] of Object.entries(COLOR_HEX_MAP)) {
    if (lower.includes(k)) return v;
  }
  return null;
};

// ── Theme / Icon config per attribute ──────────────────────────────────────
const ATTR_THEME = {
  mau_sac: {
    icon: Palette,
    bg: "rgba(225, 29, 72, 0.12)",
    color: "#e11d48",
  },
  man_hinh: {
    icon: Monitor,
    bg: "rgba(2, 132, 199, 0.12)",
    color: "#0284c7",
  },
  pin: {
    icon: Battery,
    bg: "rgba(22, 163, 74, 0.12)",
    color: "#16a34a",
  },
  trong_luong: {
    icon: Scale,
    bg: "rgba(217, 119, 6, 0.12)",
    color: "#d97706",
  },
  he_dieu_hanh: {
    icon: Settings,
    bg: "rgba(124, 58, 237, 0.12)",
    color: "#7c3aed",
  },
};

const getAttrTheme = (tenTruong) => {
  return ATTR_THEME[tenTruong] || {
    icon: SlidersHorizontal,
    bg: "rgba(14, 116, 144, 0.12)",
    color: "#0e7490",
  };
};

// ── Load data ──────────────────────────────────────────────────────────────
const attributes = ref([]);
const loading = ref(false);

const loadAttributes = async () => {
  loading.value = true;
  try {
    attributes.value = await ThuocTinhService.getAll();
  } catch (err) {
    showToast(t("admin.errors.loadFailed", { error: err.message }));
  } finally {
    loading.value = false;
  }
};
onMounted(loadAttributes);

// ── Add/Edit Attribute Modal ────────────────────────────────────────────────
const showAddModal = ref(false);
const editingAttr = ref(null);
const attrForm = ref({ tenTruong: "", tenHienThi: "", loaiDuLieu: "select", batBuoc: true, thuTuHienThi: 0 });
const attrError = ref("");
const savingAttr = ref(false);

const openAddModal = () => {
  editingAttr.value = null;
  attrForm.value = {
    tenTruong: "",
    tenHienThi: "",
    loaiDuLieu: "select",
    batBuoc: false,
    thuTuHienThi: attributes.value.length + 1,
  };
  attrError.value = "";
  showAddModal.value = true;
};

const openEditModal = (attr) => {
  editingAttr.value = attr;
  attrForm.value = {
    tenTruong: attr.tenTruong,
    tenHienThi: attr.tenHienThi,
    loaiDuLieu: attr.loaiDuLieu,
    batBuoc: attr.batBuoc,
    thuTuHienThi: attr.thuTuHienThi,
  };
  attrError.value = "";
  showAddModal.value = true;
};

const saveAttribute = async () => {
  attrError.value = "";
  if (!attrForm.value.tenTruong.trim()) {
    attrError.value = "Tên trường không được để trống";
    return;
  }
  if (!attrForm.value.tenHienThi.trim()) {
    attrError.value = "Tên hiển thị không được để trống";
    return;
  }
  // Validate tenTruong: lowercase, only a-z, 0-9, _
  if (!/^[a-z0-9_]+$/.test(attrForm.value.tenTruong)) {
    attrError.value = "Tên trường chỉ chứa chữ thường không dấu, số và gạch dưới (VD: ban_phim, cong_ket_noi)";
    return;
  }

  savingAttr.value = true;
  try {
    if (editingAttr.value) {
      const res = await ThuocTinhService.update(editingAttr.value.thuocTinhId, attrForm.value);
      if (!res.ok) throw new Error(await res.text());
      showToast(t("admin.messages.updateSuccess"));
    } else {
      const res = await ThuocTinhService.create(attrForm.value);
      if (!res.ok) throw new Error(await res.text());
      showToast(t("admin.messages.addSuccess"));
    }
    showAddModal.value = false;
    await loadAttributes();
  } catch (err) {
    attrError.value = err.message || "Lỗi khi lưu thuộc tính";
  } finally {
    savingAttr.value = false;
  }
};

const deleteAttribute = async (attr) => {
  if (!confirm(`Xóa thuộc tính "${attr.tenHienThi}" và tất cả giá trị của nó?`)) return;
  try {
    const res = await ThuocTinhService.delete(attr.thuocTinhId);
    if (!res.ok) throw new Error(await res.text());
    showToast("Xóa thuộc tính thành công");
    await loadAttributes();
  } catch (err) {
    showToast("Lỗi: " + (err.message || "Không thể xóa"));
  }
};

// ── Values Management per Attribute ───────────────────────────────────────
const expandedAttrs = ref({});
const toggleExpand = (id) => {
  expandedAttrs.value[id] = !expandedAttrs.value[id];
};

// Quick single value input (type + Enter to add)
const quickValueInput = ref({});
const addingQuickValue = ref({});

const handleQuickAdd = async (attr) => {
  const val = (quickValueInput.value[attr.thuocTinhId] || "").trim();
  if (!val) return;
  addingQuickValue.value[attr.thuocTinhId] = true;
  try {
    await ThuocTinhService.addGiaTri(attr.thuocTinhId, { giaTri: val });
    quickValueInput.value[attr.thuocTinhId] = "";
    await loadAttributes();
    showToast(`Đã thêm "${val}"`);
  } catch (err) {
    showToast("Lỗi: " + (err.message || "Không thể thêm giá trị"));
  } finally {
    addingQuickValue.value[attr.thuocTinhId] = false;
  }
};

const deleteValue = async (attr, giaTri) => {
  if (!confirm(`Xóa giá trị "${giaTri.giaTri}"?`)) return;
  try {
    await ThuocTinhService.deleteGiaTri(attr.thuocTinhId, giaTri.giaTriId);
    showToast("Đã xóa giá trị");
    await loadAttributes();
  } catch (err) {
    showToast("Lỗi: " + (err.message || "Không thể xóa"));
  }
};
</script>

<template>
  <div class="tt-page-container">
    <!-- ══ MODERN SEGMENTED TABS BAR ══ -->
    <div class="tt-tabs-bar">
      <button
        class="tt-tab-btn"
        :class="{ active: currentTab === 'general' }"
        @click="setTab('general')"
        type="button"
      >
        <SlidersHorizontal :size="16" />
        <span>Thuộc tính chung</span>
        <span v-if="attributes.length" class="tt-tab-badge">{{ attributes.length }}</span>
      </button>

      <button
        class="tt-tab-btn"
        :class="{ active: currentTab === 'cpu' }"
        @click="setTab('cpu')"
        type="button"
      >
        <Cpu :size="16" />
        <span>CPU</span>
      </button>

      <button
        class="tt-tab-btn"
        :class="{ active: currentTab === 'ram' }"
        @click="setTab('ram')"
        type="button"
      >
        <MemoryStick :size="16" />
        <span>RAM</span>
      </button>

      <button
        class="tt-tab-btn"
        :class="{ active: currentTab === 'gpu' }"
        @click="setTab('gpu')"
        type="button"
      >
        <Monitor :size="16" />
        <span>Card đồ họa (GPU)</span>
      </button>

      <button
        class="tt-tab-btn"
        :class="{ active: currentTab === 'oCung' }"
        @click="setTab('oCung')"
        type="button"
      >
        <HardDrive :size="16" />
        <span>Ổ cứng (Storage)</span>
      </button>
    </div>

    <!-- ══ TAB 1: THUỘC TÍNH CHUNG ══ -->
    <div v-show="currentTab === 'general'" class="tt-tab-body">
      <!-- Loading State -->
      <div v-if="loading" class="tt-loading-card">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
        <p class="tt-loading-text">Đang tải danh sách thuộc tính...</p>
      </div>

      <!-- Empty State -->
      <div v-else-if="attributes.length === 0" class="tt-empty-card">
        <div class="tt-empty-icon-box">
          <Layers :size="36" />
        </div>
        <h3>Chưa có thuộc tính nào</h3>
        <p>Bắt đầu tạo các thông số như Màu sắc, Màn hình, Dung lượng pin để áp dụng vào biến thể sản phẩm.</p>
        <button class="tt-btn-create" @click="openAddModal">
          <Plus :size="16" /> Thêm thuộc tính mới
        </button>
      </div>

      <!-- Attributes List Toolbar -->
      <div v-else class="tt-tab-toolbar">
        <div class="tt-tab-toolbar__left">
          <span class="tt-tab-toolbar__title">Danh sách thuộc tính</span>
          <span class="tt-tab-toolbar__badge">{{ attributes.length }} trường</span>
        </div>
        <button class="tt-btn-create" @click="openAddModal">
          <Plus :size="16" />
          <span>Thêm thuộc tính mới</span>
        </button>
      </div>

      <!-- Attributes List -->
      <div v-if="!loading && attributes.length > 0" class="tt-cards-stack">
        <div
          v-for="attr in attributes"
          :key="attr.thuocTinhId"
          class="tt-item-card"
          :class="{ 'is-expanded': expandedAttrs[attr.thuocTinhId] }"
        >
          <!-- Card Header Row -->
          <div class="tt-item-header" @click="toggleExpand(attr.thuocTinhId)">
            <div class="tt-item-info">
              <!-- Tailored Icon Badge -->
              <div
                class="tt-item-icon-box"
                :style="{ background: getAttrTheme(attr.tenTruong).bg, color: getAttrTheme(attr.tenTruong).color }"
              >
                <component :is="getAttrTheme(attr.tenTruong).icon" :size="20" />
              </div>

              <!-- Title & Key -->
              <div class="tt-item-meta">
                <div class="tt-item-title-row">
                  <span class="tt-item-name">{{ attr.tenHienThi }}</span>
                  <span class="tt-pill-type" :class="attr.loaiDuLieu">
                    {{ attr.loaiDuLieu === 'select' ? 'Danh sách chọn' : 'Văn bản tự do' }}
                  </span>
                  <span v-if="attr.batBuoc" class="tt-pill-required">
                    ● Bắt buộc
                  </span>
                  <span v-if="attr.loaiDuLieu === 'select'" class="tt-pill-count">
                    {{ attr.giaTriList?.length || 0 }} giá trị
                  </span>
                </div>
                <div class="tt-item-key">
                  Key định danh: <code>{{ attr.tenTruong }}</code>
                </div>
              </div>
            </div>

            <!-- Header Actions -->
            <div class="tt-item-actions" @click.stop>
              <button
                class="tt-btn-icon"
                @click="openEditModal(attr)"
                title="Chỉnh sửa thuộc tính"
              >
                <Pencil :size="15" />
              </button>
              <button
                class="tt-btn-icon tt-btn-icon--danger"
                @click="deleteAttribute(attr)"
                title="Xóa thuộc tính"
              >
                <Trash2 :size="15" />
              </button>
              <div class="tt-chevron-box" :class="{ 'is-open': expandedAttrs[attr.thuocTinhId] }">
                <ChevronDown :size="18" />
              </div>
            </div>
          </div>

          <!-- Expanded Body (Values & Quick Add) -->
          <div v-if="expandedAttrs[attr.thuocTinhId]" class="tt-item-body">
            <!-- For Select Type -->
            <div v-if="attr.loaiDuLieu === 'select'" class="tt-values-section">
              <div class="tt-values-header">
                <span class="tt-section-label">Danh sách tùy chọn khả dụng:</span>
                <span class="tt-values-hint">Bấm vào dấu × trên từng thẻ để xóa</span>
              </div>

              <!-- Chips Grid -->
              <div v-if="attr.giaTriList?.length" class="tt-chips-cloud">
                <div
                  v-for="val in attr.giaTriList"
                  :key="val.giaTriId"
                  class="tt-chip"
                >
                  <!-- Optional Color Dot for Colors -->
                  <span
                    v-if="attr.tenTruong === 'mau_sac' && getColorHex(val.giaTri)"
                    class="tt-color-dot"
                    :style="{ background: getColorHex(val.giaTri) }"
                  ></span>
                  <span class="tt-chip-text">{{ val.giaTri }}</span>
                  <button
                    class="tt-chip-remove"
                    @click="deleteValue(attr, val)"
                    title="Xóa giá trị này"
                  >
                    <X :size="13" />
                  </button>
                </div>
              </div>
              <div v-else class="tt-no-values">
                Chưa có giá trị nào. Hãy thêm giá trị bên dưới để người dùng có thể chọn.
              </div>

              <!-- Modern Quick Add Input Bar -->
              <div class="tt-quick-add-bar">
                <div class="tt-input-wrapper">
                  <input
                    v-model="quickValueInput[attr.thuocTinhId]"
                    type="text"
                    class="tt-quick-input"
                    :placeholder="`Thêm giá trị cho ${attr.tenHienThi} (ví dụ: ${attr.tenTruong === 'mau_sac' ? 'Titan Tự Nhiên' : attr.tenTruong === 'pin' ? '99Wh' : 'Nhập giá trị mới...'})`"
                    @keydown.enter.prevent="handleQuickAdd(attr)"
                  />
                  <button
                    class="tt-btn-quick-add"
                    @click="handleQuickAdd(attr)"
                    :disabled="addingQuickValue[attr.thuocTinhId] || !quickValueInput[attr.thuocTinhId]?.trim()"
                  >
                    <Plus :size="15" />
                    <span>{{ addingQuickValue[attr.thuocTinhId] ? 'Đang thêm...' : 'Thêm' }}</span>
                  </button>
                </div>
                <span class="tt-quick-help">Mẹo: Nhập xong nhấn <strong>Enter</strong> để thêm nhanh.</span>
              </div>
            </div>

            <!-- For Text Type -->
            <div v-else class="tt-text-info-box">
              <div class="tt-text-info-content">
                <HelpCircle :size="18" class="text-primary" />
                <div>
                  <strong>Trường nhập liệu văn bản tự do:</strong>
                  <p>Khi tạo sản phẩm hoặc biến thể, nhân viên có thể gõ giá trị tùy ý trực tiếp vào ô nhập (ví dụ: cân nặng 1.83kg, mô tả riêng...).</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Add / Edit Modal -->
      <div v-if="showAddModal" class="tt-modal-backdrop" @click.self="showAddModal = false">
        <div class="tt-modal-card">
          <div class="tt-modal-header">
            <div class="tt-modal-title-box">
              <div class="tt-modal-icon-badge">
                <SlidersHorizontal :size="18" />
              </div>
              <h4 class="tt-modal-title">
                {{ editingAttr ? 'Chỉnh sửa thuộc tính' : 'Thêm thuộc tính mới' }}
              </h4>
            </div>
            <button class="tt-btn-close" @click="showAddModal = false">
              <X :size="18" />
            </button>
          </div>

          <div class="tt-modal-body">
            <!-- Tên hiển thị -->
            <div class="tt-form-group">
              <label class="tt-form-label">
                Tên hiển thị <span class="text-danger">*</span>
              </label>
              <input
                v-model="attrForm.tenHienThi"
                type="text"
                class="tt-form-input"
                placeholder="VD: Bàn phím, Cổng kết nối, Chất liệu..."
              />
              <span class="tt-form-hint">Tên sẽ hiển thị cho khách hàng và nhân viên xem.</span>
            </div>

            <!-- Tên trường (Key) -->
            <div class="tt-form-group">
              <label class="tt-form-label">
                Mã định danh (Key CSDL) <span class="text-danger">*</span>
              </label>
              <input
                v-model="attrForm.tenTruong"
                type="text"
                class="tt-form-input"
                placeholder="VD: ban_phim, cong_ket_noi"
                :disabled="!!editingAttr"
              />
              <span class="tt-form-hint">Viết thường không dấu, dùng gạch dưới để phân cách (VD: <code>ban_phim</code>).</span>
            </div>

            <!-- Loại dữ liệu Selector Cards -->
            <div class="tt-form-group">
              <label class="tt-form-label">Kiểu dữ liệu</label>
              <div class="tt-type-grid">
                <label
                  class="tt-type-card"
                  :class="{ selected: attrForm.loaiDuLieu === 'select' }"
                >
                  <input
                    type="radio"
                    v-model="attrForm.loaiDuLieu"
                    value="select"
                    class="d-none"
                  />
                  <div class="tt-type-card-header">
                    <ListFilter :size="18" />
                    <strong>Danh sách chọn (Select)</strong>
                  </div>
                  <p>Tạo các giá trị định sẵn để chọn nhanh từ menu xổ xuống.</p>
                </label>

                <label
                  class="tt-type-card"
                  :class="{ selected: attrForm.loaiDuLieu === 'text' }"
                >
                  <input
                    type="radio"
                    v-model="attrForm.loaiDuLieu"
                    value="text"
                    class="d-none"
                  />
                  <div class="tt-type-card-header">
                    <Tag :size="18" />
                    <strong>Văn bản tự do (Text)</strong>
                  </div>
                  <p>Nhập tay chuỗi tự do bất kỳ khi tạo sản phẩm.</p>
                </label>
              </div>
            </div>

            <!-- Switch: Bắt buộc -->
            <div class="tt-form-group">
              <label class="tt-toggle-row">
                <input
                  v-model="attrForm.batBuoc"
                  type="checkbox"
                  class="tt-checkbox"
                />
                <div class="tt-toggle-text">
                  <strong>Bắt buộc nhập</strong>
                  <span>Yêu cầu nhân viên phải chọn hoặc nhập thuộc tính này khi thêm biến thể.</span>
                </div>
              </label>
            </div>

            <!-- Thứ tự hiển thị -->
            <div class="tt-form-group">
              <label class="tt-form-label">Thứ tự ưu tiên hiển thị</label>
              <input
                v-model.number="attrForm.thuTuHienThi"
                type="number"
                class="tt-form-input"
                style="max-width: 140px;"
                min="0"
              />
            </div>

            <!-- Error banner -->
            <div v-if="attrError" class="tt-alert-danger">
              {{ attrError }}
            </div>
          </div>

          <div class="tt-modal-footer">
            <button type="button" class="tt-btn-secondary" @click="showAddModal = false">
              Hủy bỏ
            </button>
            <button
              type="button"
              class="tt-btn-primary"
              @click="saveAttribute"
              :disabled="savingAttr"
            >
              {{ savingAttr ? 'Đang lưu...' : 'Lưu thuộc tính' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ══ TAB 2: CPU ══ -->
    <div v-show="currentTab === 'cpu'" v-if="visitedTabs.cpu" class="tt-tab-body">
      <DmCategoryTable
        :service="DmService.DmCpuService"
        id-field="cpuId"
        name-field="tenCpu"
        :label="t('admin.productsTabs.cpu') || 'CPU'"
        :name-label="t('admin.productsTabs.cpu') || 'CPU'"
        :header-icon="Cpu"
        :serial-service="ChiTietCpuService"
        serial-field-name="cpuId"
        :advanced-filter-config="{
          filters: [
            { key: 'hang', label: 'Hãng' },
            { key: 'dong', label: 'Dòng CPU' },
          ]
        }"
      />
    </div>

    <!-- ══ TAB 3: RAM ══ -->
    <div v-show="currentTab === 'ram'" v-if="visitedTabs.ram" class="tt-tab-body">
      <DmCategoryTable
        :service="DmService.DmRamService"
        id-field="ramId"
        name-field="dungLuong"
        :label="t('admin.productsTabs.ram') || 'RAM'"
        :name-label="t('admin.productsTabs.ram') || 'RAM'"
        :header-icon="MemoryStick"
        :serial-service="ChiTietRamService"
        serial-field-name="ramId"
        :advanced-filter-config="{
          filters: [
            { key: 'loai', label: 'Loại RAM' },
            { key: 'dungluong', label: 'Dung lượng' },
          ]
        }"
      />
    </div>

    <!-- ══ TAB 4: GPU ══ -->
    <div v-show="currentTab === 'gpu'" v-if="visitedTabs.gpu" class="tt-tab-body">
      <DmCategoryTable
        :service="DmService.DmGpuService"
        id-field="gpuId"
        name-field="tenGpu"
        :label="t('admin.productsTabs.gpu') || 'GPU'"
        :name-label="t('admin.productsTabs.gpu') || 'GPU'"
        :header-icon="Monitor"
        :serial-service="ChiTietGpuService"
        serial-field-name="gpuId"
        :advanced-filter-config="{
          filters: [
            { key: 'hang', label: 'Hãng' },
            { key: 'vram', label: 'VRAM' },
          ]
        }"
      />
    </div>

    <!-- ══ TAB 5: Ổ CỨNG ══ -->
    <div v-show="currentTab === 'oCung'" v-if="visitedTabs.oCung" class="tt-tab-body">
      <DmCategoryTable
        :service="DmService.DmOCungService"
        id-field="oCungId"
        name-field="loaiOcung"
        :label="t('admin.productsTabs.oCung') || 'Ổ cứng'"
        :name-label="t('admin.productsTabs.oCung') || 'Ổ cứng'"
        :header-icon="HardDrive"
        :serial-service="ChiTietOCungService"
        serial-field-name="oCungId"
        :advanced-filter-config="{
          filters: [
            { key: 'loai', label: 'Loại ổ cứng' },
            { key: 'dungluong', label: 'Dung lượng' },
          ]
        }"
      />
    </div>
  </div>
</template>

<style scoped>
.tt-page-container {
  padding: 0.25rem 0.5rem 2.5rem;
  max-width: 100%;
}

/* ── Toolbar Header ── */
.tt-tab-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
  padding: 0.25rem 0.25rem;
}

.tt-tab-toolbar__left {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.tt-tab-toolbar__title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-heading);
}

.tt-tab-toolbar__badge {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.6rem;
  font-size: 0.76rem;
  font-weight: 600;
  border-radius: 9999px;
  background: rgba(14, 116, 144, 0.1);
  color: var(--accent-2);
}

.tt-btn-create {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.2rem;
  background: var(--gradient-brand);
  color: #ffffff;
  border: none;
  border-radius: 0.6rem;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(225, 29, 72, 0.25);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.tt-btn-create:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(225, 29, 72, 0.35);
}

.tt-btn-create:active {
  transform: translateY(0);
}

/* ── Segmented Tabs Bar ── */
.tt-tabs-bar {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.45rem;
  background: var(--bg-card-inset);
  border: 1px solid var(--border-color);
  border-radius: 0.85rem;
  margin-bottom: 1.25rem;
  overflow-x: auto;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.03);
}

.tt-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.55rem 1.15rem;
  border: 1px solid transparent;
  border-radius: 0.65rem;
  background: transparent;
  color: var(--text-muted);
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
  white-space: nowrap;
}

.tt-tab-btn:hover {
  color: var(--text-primary);
  background: var(--bg-hover);
}

.tt-tab-btn.active {
  background: var(--bg-card);
  color: var(--accent);
  border-color: var(--border-color-soft);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.tt-tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.25rem;
  height: 1.25rem;
  padding: 0 0.4rem;
  font-size: 0.72rem;
  font-weight: 700;
  border-radius: 9999px;
  background: rgba(225, 29, 72, 0.1);
  color: var(--accent);
}

.tt-tab-btn.active .tt-tab-badge {
  background: var(--accent);
  color: #ffffff;
}

/* ── Stack of Attribute Cards ── */
.tt-cards-stack {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.tt-item-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 0.85rem;
  overflow: hidden;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
}

.tt-item-card:hover {
  border-color: var(--border-color-strong);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
}

.tt-item-card.is-expanded {
  border-color: var(--accent);
  box-shadow: 0 6px 20px -2px rgba(225, 29, 72, 0.08);
}

.tt-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.1rem 1.4rem;
  cursor: pointer;
  user-select: none;
  background: var(--bg-card);
  transition: background 0.15s;
}

.tt-item-header:hover {
  background: var(--bg-hover);
}

.tt-item-info {
  display: flex;
  align-items: center;
  gap: 1.1rem;
}

.tt-item-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 11px;
  flex-shrink: 0;
}

.tt-item-meta {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.tt-item-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.tt-item-name {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-heading);
}

.tt-pill-type {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.2rem 0.55rem;
  border-radius: 20px;
  background: var(--bg-card-inset);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.tt-pill-required {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.2rem 0.6rem;
  border-radius: 20px;
  background: rgba(225, 29, 72, 0.1);
  color: var(--accent);
}

.tt-pill-count {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.2rem 0.6rem;
  border-radius: 20px;
  background: rgba(14, 116, 144, 0.1);
  color: var(--accent-2);
}

.tt-item-key {
  font-size: 0.78rem;
  color: var(--text-muted);
}

.tt-item-key code {
  font-size: 0.75rem;
  color: var(--accent-2);
  background: var(--bg-card-inset);
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.tt-item-actions {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.tt-btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.15s ease;
}

.tt-btn-icon:hover {
  color: var(--accent-2);
  border-color: var(--accent-2);
  background: var(--bg-hover);
}

.tt-btn-icon--danger:hover {
  color: var(--accent);
  border-color: var(--accent);
  background: rgba(225, 29, 72, 0.08);
}

.tt-chevron-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--text-muted);
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  margin-left: 0.2rem;
}

.tt-chevron-box.is-open {
  transform: rotate(180deg);
  color: var(--accent);
}

/* ── Item Body ── */
.tt-item-body {
  padding: 1.25rem 1.4rem 1.5rem;
  background: var(--bg-card-inset);
  border-top: 1px solid var(--border-color-soft);
  animation: ttFadeIn 0.2s ease-out;
}

@keyframes ttFadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.tt-values-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.85rem;
}

.tt-section-label {
  font-size: 0.84rem;
  font-weight: 700;
  color: var(--text-heading);
}

.tt-values-hint {
  font-size: 0.76rem;
  color: var(--text-muted);
}

/* Chips cloud */
.tt-chips-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
}

.tt-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.35rem 0.75rem;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 2rem;
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-primary);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  transition: all 0.15s ease;
}

.tt-chip:hover {
  border-color: var(--accent);
  transform: translateY(-1px);
}

.tt-color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.tt-chip-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-muted);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tt-chip-remove:hover {
  background: rgba(225, 29, 72, 0.12);
  color: var(--accent);
}

.tt-no-values {
  padding: 1.25rem;
  text-align: center;
  font-size: 0.85rem;
  color: var(--text-muted);
  background: var(--bg-card);
  border: 1px dashed var(--border-color);
  border-radius: 0.65rem;
  margin-bottom: 1.25rem;
}

/* Quick Add Bar */
.tt-quick-add-bar {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.tt-input-wrapper {
  display: flex;
  gap: 0.5rem;
}

.tt-quick-input {
  flex: 1;
  padding: 0.6rem 0.95rem;
  border-radius: 0.6rem;
  border: 1.5px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 0.88rem;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.tt-quick-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(225, 29, 72, 0.12);
}

.tt-btn-quick-add {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.6rem 1.2rem;
  border-radius: 0.6rem;
  border: none;
  background: var(--accent-2);
  color: #ffffff;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s, transform 0.15s;
}

.tt-btn-quick-add:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tt-btn-quick-add:not(:disabled):hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.tt-quick-help {
  font-size: 0.75rem;
  color: var(--text-muted);
}

/* Text Type Info Box */
.tt-text-info-box {
  padding: 1.1rem;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 0.65rem;
}

.tt-text-info-content {
  display: flex;
  gap: 0.75rem;
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.tt-text-info-content strong {
  display: block;
  margin-bottom: 0.25rem;
  color: var(--text-heading);
}

.tt-text-info-content p {
  margin: 0;
}

/* ── Loading & Empty states ── */
.tt-loading-card,
.tt-empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 4rem 2rem;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 1rem;
}

.tt-loading-text {
  margin-top: 1rem;
  font-size: 0.9rem;
  color: var(--text-muted);
}

.tt-empty-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--bg-card-inset);
  color: var(--text-muted);
  margin-bottom: 1.25rem;
}

.tt-empty-card h3 {
  font-size: 1.15rem;
  font-weight: 700;
  margin-bottom: 0.4rem;
  color: var(--text-heading);
}

.tt-empty-card p {
  font-size: 0.88rem;
  color: var(--text-secondary);
  max-width: 440px;
  margin-bottom: 1.5rem;
}

/* ── Modal Design ── */
.tt-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(8px);
  animation: ttModalFade 0.2s ease-out;
}

@keyframes ttModalFade {
  from { opacity: 0; }
  to { opacity: 1; }
}

.tt-modal-card {
  width: 100%;
  max-width: 520px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 1.1rem;
  box-shadow: 0 20px 40px -8px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  animation: ttModalScale 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes ttModalScale {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.tt-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.tt-modal-title-box {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.tt-modal-icon-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: rgba(225, 29, 72, 0.1);
  color: var(--accent);
}

.tt-modal-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text-heading);
}

.tt-btn-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.tt-btn-close:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.tt-modal-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.tt-form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.tt-form-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-heading);
}

.tt-form-input {
  width: 100%;
  padding: 0.65rem 0.95rem;
  border-radius: 0.6rem;
  border: 1.5px solid var(--border-color);
  background: var(--bg-card-inset);
  color: var(--text-primary);
  font-size: 0.88rem;
  outline: none;
  transition: all 0.18s;
}

.tt-form-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(225, 29, 72, 0.12);
  background: var(--bg-card);
}

.tt-form-hint {
  font-size: 0.74rem;
  color: var(--text-muted);
}

/* Type Grid Selector */
.tt-type-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.tt-type-card {
  padding: 0.9rem;
  border: 1.5px solid var(--border-color);
  border-radius: 0.75rem;
  background: var(--bg-card-inset);
  cursor: pointer;
  transition: all 0.18s;
}

.tt-type-card:hover {
  border-color: var(--border-color-strong);
}

.tt-type-card.selected {
  border-color: var(--accent);
  background: rgba(225, 29, 72, 0.05);
}

.tt-type-card-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
  color: var(--text-heading);
  font-size: 0.85rem;
}

.tt-type-card.selected .tt-type-card-header {
  color: var(--accent);
}

.tt-type-card p {
  margin: 0;
  font-size: 0.74rem;
  color: var(--text-muted);
}

/* Toggle Switch row */
.tt-toggle-row {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  cursor: pointer;
  padding: 0.75rem 0.9rem;
  background: var(--bg-card-inset);
  border: 1px solid var(--border-color);
  border-radius: 0.65rem;
}

.tt-checkbox {
  margin-top: 0.2rem;
  accent-color: var(--accent);
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.tt-toggle-text {
  display: flex;
  flex-direction: column;
}

.tt-toggle-text strong {
  font-size: 0.85rem;
  color: var(--text-heading);
}

.tt-toggle-text span {
  font-size: 0.74rem;
  color: var(--text-muted);
}

.tt-alert-danger {
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  background: rgba(220, 38, 38, 0.1);
  border: 1px solid rgba(220, 38, 38, 0.2);
  color: #dc2626;
  font-size: 0.84rem;
}

.tt-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1.15rem 1.5rem;
  border-top: 1px solid var(--border-color);
}

.tt-btn-secondary {
  padding: 0.6rem 1.25rem;
  border: 1px solid var(--border-color);
  background: var(--bg-card-inset);
  color: var(--text-secondary);
  border-radius: 0.6rem;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.tt-btn-secondary:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.tt-btn-primary {
  padding: 0.6rem 1.4rem;
  border: none;
  background: var(--gradient-brand);
  color: #ffffff;
  border-radius: 0.6rem;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(225, 29, 72, 0.25);
  transition: all 0.18s;
}

.tt-btn-primary:hover {
  opacity: 0.95;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(225, 29, 72, 0.35);
}

.tt-btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
</style>
