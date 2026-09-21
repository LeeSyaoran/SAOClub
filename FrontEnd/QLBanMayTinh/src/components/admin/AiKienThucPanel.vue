<script setup>
/* Quản lý kiến thức AI chatbot */
import { ref, computed, onMounted } from "vue";
import * as ChatService from "../../services/ChatService.js";
import { showToast } from "../../stores/toast.js";
import { t } from "../../i18n/index.js";

const LOAI_OPTIONS = [
  { value: "", label: "Tất cả" },
  { value: "FAQ", label: "Câu hỏi thường gặp" },
  { value: "CHINH_SACH", label: "Chính sách" },
  { value: "SAN_PHAM", label: "Sản phẩm" },
  { value: "KHUYEN_MAI", label: "Khuyến mãi" },
  { value: "VAN_CHUYEN", label: "Vận chuyển" },
  { value: "BAO_HANH", label: "Bảo hành" },
  { value: "THANH_TOAN", label: "Thanh toán" },
  { value: "OTHER", label: "Khác" },
];

const LOAI_LABELS = Object.fromEntries(LOAI_OPTIONS.map((o) => [o.value, o.label]));
const LOAI_COLORS = {
  FAQ: "#8b5cf6",
  CHINH_SACH: "#0ea5e9",
  SAN_PHAM: "#10b981",
  KHUYEN_MAI: "#f59e0b",
  VAN_CHUYEN: "#06b6d4",
  BAO_HANH: "#ef4444",
  THANH_TOAN: "#a855f7",
  OTHER: "#6b7280",
};

// ─── State ────────────────────────────────────────────────────────────────────
const items = ref([]);
const loading = ref(false);
const page = ref(0);
const totalPages = ref(0);
const totalElements = ref(0);
const pageSize = 20;
const selectedLoai = ref("");
const searchQ = ref("");

// ─── Modal state ─────────────────────────────────────────────────────────────
const showModal = ref(false);
const editing = ref(null); // null = create, object = edit
const form = ref({ loai: "FAQ", tieuDe: "", noiDung: "", active: true });
const formErrors = ref({});
const saving = ref(false);

// ─── Delete state ────────────────────────────────────────────────────────────
const deletingId = ref(null);
const showDeleteConfirm = ref(false);
const deleteTarget = ref(null);

// ─── Preview ────────────────────────────────────────────────────────────────
const previewItem = ref(null);

// ─── Load data ─────────────────────────────────────────────────────────────
async function loadData(reset = true) {
  if (reset) page.value = 0;
  loading.value = true;
  try {
    let res;
    if (selectedLoai.value) {
      res = await ChatService.layKienThucTheoLoai(selectedLoai.value, page.value, pageSize);
    } else {
      res = await ChatService.layKienThucAI(page.value, pageSize);
    }

    // Filter client-side by search
    let list = res.content || res._embedded?.aiKienThucList || [];
    if (searchQ.value.trim()) {
      const q = searchQ.value.toLowerCase();
      list = list.filter(
        (i) =>
          i.tieuDe?.toLowerCase().includes(q) ||
          i.noiDung?.toLowerCase().includes(q)
      );
    }

    items.value = list;
    totalPages.value = res.totalPages || 0;
    totalElements.value = res.totalElements || 0;
  } catch (e) {
    showToast("Không tải được danh sách kiến thức", "error");
  } finally {
    loading.value = false;
  }
}

function onLoaiChange() {
  loadData(true);
}

function onSearch() {
  loadData(true);
}

function goPage(p) {
  page.value = p;
  loadData(false);
}

// ─── Modal helpers ────────────────────────────────────────────────────────────
function openCreate() {
  editing.value = null;
  form.value = { loai: "FAQ", tieuDe: "", noiDung: "", active: true };
  formErrors.value = {};
  showModal.value = true;
}

function openEdit(item) {
  editing.value = item;
  form.value = {
    loai: item.loai || "FAQ",
    tieuDe: item.tieuDe || "",
    noiDung: item.noiDung || "",
    active: item.active !== false,
  };
  formErrors.value = {};
  showModal.value = true;
}

function validateForm() {
  const errs = {};
  if (!form.value.loai) errs.loai = "Vui lòng chọn loại";
  if (!form.value.tieuDe?.trim()) errs.tieuDe = "Tiêu đề không được để trống";
  if (!form.value.noiDung?.trim()) errs.noiDung = "Nội dung không được để trống";
  if (form.value.noiDung?.trim().length < 10) errs.noiDung = "Nội dung phải có ít nhất 10 ký tự";
  formErrors.value = errs;
  return Object.keys(errs).length === 0;
}

async function saveForm() {
  if (!validateForm()) return;
  saving.value = true;
  try {
    const data = {
      loai: form.value.loai,
      tieuDe: form.value.tieuDe.trim(),
      noiDung: form.value.noiDung.trim(),
      active: form.value.active,
    };
    if (editing.value) {
      await ChatService.capNhatKienThuc(editing.value.id, data);
      showToast("Cập nhật thành công", "success");
    } else {
      await ChatService.taoKienThuc(data);
      showToast("Thêm mới thành công", "success");
    }
    showModal.value = false;
    loadData(false);
  } catch (e) {
    showToast("Lỗi: " + (e.message || "Không lưu được"), "error");
  } finally {
    saving.value = false;
  }
}

// ─── Delete ─────────────────────────────────────────────────────────────────
function confirmDelete(item) {
  deleteTarget.value = item;
  showDeleteConfirm.value = true;
}

async function doDelete() {
  if (!deleteTarget.value) return;
  deletingId.value = deleteTarget.value.id;
  try {
    await ChatService.xoaKienThuc(deleteTarget.value.id);
    showToast("Xóa thành công", "success");
    showDeleteConfirm.value = false;
    deleteTarget.value = null;
    loadData(false);
  } catch (e) {
    showToast("Lỗi xóa: " + (e.message || ""), "error");
  } finally {
    deletingId.value = null;
  }
}

// ─── Toggle active ───────────────────────────────────────────────────────────
async function toggleActive(item) {
  const newActive = !item.active;
  try {
    await ChatService.capNhatKienThuc(item.id, {
      ...item,
      active: newActive,
    });
    item.active = newActive;
    showToast(newActive ? "Đã kích hoạt" : "Đã tắt", "success");
  } catch (e) {
    showToast("Lỗi cập nhật", "error");
  }
}

// ─── Stats ────────────────────────────────────────────────────────────────────
const stats = computed(() => {
  const byLoai = {};
  for (const item of items.value) {
    const loai = item.loai || "OTHER";
    byLoai[loai] = (byLoai[loai] || 0) + 1;
  }
  return byLoai;
});

onMounted(() => loadData());
</script>

<template>
  <div class="ai-kien-thuc-panel">
    <!-- ── Header ── -->
    <div class="d-flex align-items-center justify-content-between mb-3 flex-wrap gap-2">
      <div>
        <h5 class="mb-0 fw-bold">📚 Kiến thức AI Chatbot</h5>
        <small class="text-secondary">{{ totalElements }} bài viết</small>
      </div>
      <button class="btn btn-primary btn-sm" @click="openCreate">
        <span class="fa fa-plus"></span> Thêm kiến thức
      </button>
    </div>

    <!-- ── Filters ── -->
    <div class="d-flex gap-2 mb-3 flex-wrap">
      <!-- Search -->
      <div class="input-group" style="max-width:300px;">
        <span class="input-group-text bg-white border-secondary">
          <span class="fa fa-search"></span>
        </span>
        <input
          v-model="searchQ"
          class="form-control form-control-sm"
          placeholder="Tìm kiếm tiêu đề, nội dung..."
          style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);"
          @input="onSearch"
        />
      </div>

      <!-- Loại filter -->
      <select
        v-model="selectedLoai"
        class="form-select form-select-sm"
        style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);"
        @change="onLoaiChange"
      >
        <option v-for="opt in LOAI_OPTIONS" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>

    <!-- ── Stats chips ── -->
    <div v-if="Object.keys(stats).length > 0" class="d-flex flex-wrap gap-1 mb-3">
      <span
        v-for="(count, loai) in stats"
        :key="loai"
        class="badge rounded-pill"
        :style="{ background: (LOAI_COLORS[loai] || '#6b7280') + '22', color: LOAI_COLORS[loai] || '#6b7280', border: '1px solid ' + (LOAI_COLORS[loai] || '#6b7280') + '44' }"
      >
        {{ LOAI_LABELS[loai] || loai }}: {{ count }}
      </span>
    </div>

    <!-- ── Table ── -->
    <div class="card border-secondary" style="background:var(--bg-card);">
      <div class="table-responsive">
        <table class="table table-sm table-hover align-middle mb-0">
          <thead>
            <tr>
              <th style="width:120px;">Loại</th>
              <th>Tiêu đề</th>
              <th style="width:80px;">Trạng thái</th>
              <th style="width:140px;">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading && items.length === 0">
              <td colspan="4" class="text-center py-4 text-secondary">
                <span class="fa fa-spinner fa-spin"></span> Đang tải...
              </td>
            </tr>
            <tr v-else-if="items.length === 0">
              <td colspan="4" class="text-center py-4 text-secondary">
                Chưa có kiến thức nào
              </td>
            </tr>
            <tr
              v-for="item in items"
              :key="item.id"
              :class="{ 'opacity-50': !item.active }"
            >
              <!-- Loại -->
              <td>
                <span
                  class="badge rounded-pill"
                  :style="{
                    background: (LOAI_COLORS[item.loai] || '#6b7280') + '22',
                    color: LOAI_COLORS[item.loai] || '#6b7280',
                    border: '1px solid ' + (LOAI_COLORS[item.loai] || '#6b7280') + '44',
                  }"
                >
                  {{ LOAI_LABELS[item.loai] || item.loai }}
                </span>
              </td>

              <!-- Tiêu đề + preview -->
              <td>
                <div
                  class="fw-semibold small cursor-pointer"
                  style="color:var(--text-primary);"
                  @click="previewItem = item"
                >
                  {{ item.tieuDe }}
                </div>
                <div
                  class="text-secondary small text-truncate"
                  style="max-width:400px;"
                >
                  {{ item.noiDung }}
                </div>
              </td>

              <!-- Trạng thái -->
              <td>
                <div class="form-check form-switch mb-0">
                  <input
                    class="form-check-input"
                    type="checkbox"
                    role="switch"
                    :checked="item.active !== false"
                    @change="toggleActive(item)"
                  />
                </div>
              </td>

              <!-- Thao tác -->
              <td>
                <div class="d-flex gap-1">
                  <button
                    class="btn btn-outline-secondary btn-sm py-1 px-2"
                    title="Xem trước"
                    @click="previewItem = item"
                  >
                    <span class="fa fa-eye"></span>
                  </button>
                  <button
                    class="btn btn-outline-primary btn-sm py-1 px-2"
                    title="Sửa"
                    @click="openEdit(item)"
                  >
                    <span class="fa fa-edit"></span>
                  </button>
                  <button
                    class="btn btn-outline-danger btn-sm py-1 px-2"
                    :disabled="deletingId === item.id"
                    title="Xóa"
                    @click="confirmDelete(item)"
                  >
                    <span :class="deletingId === item.id ? 'fa fa-spinner fa-spin' : 'fa fa-trash'"></span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="d-flex align-items-center justify-content-between px-3 py-2 border-top border-secondary">
        <small class="text-secondary">
          Trang {{ page + 1 }} / {{ totalPages }}
        </small>
        <div class="d-flex gap-1">
          <button
            class="btn btn-sm btn-outline-secondary"
            :disabled="page === 0"
            @click="goPage(page - 1)"
          >
            <span class="fa fa-chevron-left"></span>
          </button>
          <button
            v-for="p in Math.min(5, totalPages)"
            :key="p"
            class="btn btn-sm"
            :class="p - 1 === page ? 'btn-primary' : 'btn-outline-secondary'"
            @click="goPage(p - 1)"
          >
            {{ p }}
          </button>
          <button
            class="btn btn-sm btn-outline-secondary"
            :disabled="page >= totalPages - 1"
            @click="goPage(page + 1)"
          >
            <span class="fa fa-chevron-right"></span>
          </button>
        </div>
      </div>
    </div>

    <!-- ── Modal: Create / Edit ── -->
    <div
      v-if="showModal"
      class="modal d-block"
      tabindex="-1"
      style="background:rgba(0,0,0,0.5);"
      @click.self="showModal = false"
    >
      <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h6 class="modal-title fw-bold">
              <span class="fa fa-brain"></span>
              {{ editing ? "Sửa kiến thức" : "Thêm kiến thức mới" }}
            </h6>
            <button type="button" class="btn-close" @click="showModal = false"></button>
          </div>
          <div class="modal-body">
            <div class="row g-3">
              <!-- Loại -->
              <div class="col-md-4">
                <label class="form-label small text-secondary">Loại <span class="text-danger">*</span></label>
                <select
                  v-model="form.loai"
                  class="form-select form-select-sm"
                  :class="{ 'is-invalid': formErrors.loai }"
                  style="background:var(--bg-input);color:var(--text-primary);"
                >
                  <option v-for="opt in LOAI_OPTIONS.filter(o => o.value)" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </option>
                </select>
                <div v-if="formErrors.loai" class="invalid-feedback">{{ formErrors.loai }}</div>
              </div>

              <!-- Active -->
              <div class="col-md-4 d-flex align-items-end pb-1">
                <div class="form-check">
                  <input
                    id="formActive"
                    v-model="form.active"
                    class="form-check-input"
                    type="checkbox"
                    role="switch"
                  />
                  <label for="formActive" class="form-check-label small">Kích hoạt</label>
                </div>
              </div>

              <!-- Tiêu đề -->
              <div class="col-12">
                <label class="form-label small text-secondary">Tiêu đề <span class="text-danger">*</span></label>
                <input
                  v-model="form.tieuDe"
                  class="form-control form-control-sm"
                  :class="{ 'is-invalid': formErrors.tieuDe }"
                  placeholder="VD: Chính sách đổi trả trong 7 ngày"
                  style="background:var(--bg-input);color:var(--text-primary);"
                />
                <div v-if="formErrors.tieuDe" class="invalid-feedback">{{ formErrors.tieuDe }}</div>
              </div>

              <!-- Nội dung -->
              <div class="col-12">
                <label class="form-label small text-secondary">Nội dung <span class="text-danger">*</span></label>
                <textarea
                  v-model="form.noiDung"
                  class="form-control form-control-sm"
                  :class="{ 'is-invalid': formErrors.noiDung }"
                  rows="6"
                  placeholder="Nhập nội dung kiến thức mà chatbot sẽ sử dụng để trả lời câu hỏi của khách..."
                  style="background:var(--bg-input);color:var(--text-primary);resize:vertical;"
                ></textarea>
                <div v-if="formErrors.noiDung" class="invalid-feedback">{{ formErrors.noiDung }}</div>
                <div class="form-text small text-secondary">
                  {{ form.noiDung?.length || 0 }} ký tự
                </div>
              </div>

              <!-- Preview -->
              <div class="col-12">
                <label class="form-label small text-secondary">💬 Preview chatbot trả lời:</label>
                <div
                  class="p-3 rounded-3"
                  style="background:linear-gradient(135deg,#f3e8ff,#ede9fe);border:1px solid #c4b5fd;font-size:13px;line-height:1.5;"
                >
                  <div class="fw-bold mb-1 small">🤖 SAOClub Bot</div>
                  <template v-if="form.tieuDe">
                    <strong>{{ form.tieuDe }}</strong>
                  </template>
                  <template v-else>
                    <em class="text-secondary">Tiêu đề sẽ hiển thị ở đây...</em>
                  </template>
                  <br />
                  <span v-if="form.noiDung">{{ form.noiDung }}</span>
                  <span v-else class="text-secondary"><em>Nội dung trả lời sẽ hiển thị ở đây...</em></span>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary btn-sm" @click="showModal = false">
              Hủy
            </button>
            <button
              type="button"
              class="btn btn-primary btn-sm"
              :disabled="saving"
              @click="saveForm"
            >
              <span :class="saving ? 'fa fa-spinner fa-spin' : 'fa fa-save'"></span>
              {{ saving ? "Đang lưu..." : (editing ? "Cập nhật" : "Thêm mới") }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Preview Modal ── -->
    <div
      v-if="previewItem"
      class="modal d-block"
      tabindex="-1"
      style="background:rgba(0,0,0,0.5);"
      @click.self="previewItem = null"
    >
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h6 class="modal-title fw-bold">📖 Xem kiến thức</h6>
            <button type="button" class="btn-close" @click="previewItem = null"></button>
          </div>
          <div class="modal-body">
            <div class="mb-2">
              <span
                class="badge rounded-pill me-2"
                :style="{
                  background: (LOAI_COLORS[previewItem.loai] || '#6b7280') + '22',
                  color: LOAI_COLORS[previewItem.loai] || '#6b7280',
                }"
              >
                {{ LOAI_LABELS[previewItem.loai] || previewItem.loai }}
              </span>
              <span
                class="badge"
                :class="previewItem.active !== false ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'"
              >
                {{ previewItem.active !== false ? "Kích hoạt" : "Tắt" }}
              </span>
            </div>
            <h5 class="fw-bold mb-3">{{ previewItem.tieuDe }}</h5>
            <div
              class="p-3 rounded-3"
              style="background:var(--bg-hover);border:1px solid var(--border-color);white-space:pre-wrap;line-height:1.6;"
            >
              {{ previewItem.noiDung }}
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary btn-sm" @click="previewItem = null">
              Đóng
            </button>
            <button type="button" class="btn btn-primary btn-sm" @click="openEdit(previewItem); previewItem = null">
              <span class="fa fa-edit"></span> Sửa
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Delete Confirm ── -->
    <div
      v-if="showDeleteConfirm"
      class="modal d-block"
      tabindex="-1"
      style="background:rgba(0,0,0,0.5);"
      @click.self="showDeleteConfirm = false"
    >
      <div class="modal-dialog modal-dialog-centered modal-sm">
        <div class="modal-content">
          <div class="modal-header">
            <h6 class="modal-title fw-bold text-danger">
              <span class="fa fa-exclamation-triangle"></span> Xác nhận xóa
            </h6>
            <button type="button" class="btn-close" @click="showDeleteConfirm = false"></button>
          </div>
          <div class="modal-body">
            <p>Xóa bài "<strong>{{ deleteTarget?.tieuDe }}</strong>"? Hành động này không thể hoàn tác.</p>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary btn-sm" @click="showDeleteConfirm = false">
              Hủy
            </button>
            <button
              type="button"
              class="btn btn-danger btn-sm"
              :disabled="deletingId !== null"
              @click="doDelete"
            >
              <span :class="deletingId ? 'fa fa-spinner fa-spin' : 'fa fa-trash'"></span>
              {{ deletingId ? "Đang xóa..." : "Xóa" }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cursor-pointer { cursor: pointer; }
.bg-success-subtle { background: rgba(34,197,94,0.1); }
.bg-secondary-subtle { background: rgba(107,114,128,0.1); }
</style>
