<script setup>
import { reactive, ref, watch } from "vue";
import { t } from "../../i18n/index.js";
import { nowLocalIso } from "../../utils/datetime.js";
import * as SanPhamService from "../../services/SanPhamService.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import * as DanhMucService from "../../services/DanhMucService.js";
import * as DmService from "../../services/DmService.js";
import { authHeaders } from "../../services/api.js";
import { ProductsStore } from "../../stores/products.js";
import { SuppliersStore, ensureSuppliers } from "../../stores/suppliers.js";
import { ThuocTinhService } from "../../services/ThuocTinhService.js";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: "create" },
  sanPhamId: { type: Number, default: null },
});
const emit = defineEmits(["update:modelValue", "saved"]);

const suppliers = ref([]);
const categories = ref([]);
const brands = ref([]);
const cpuList = ref([]);
const ramList = ref([]);
const oCungList = ref([]);
const gpuList = ref([]);
const dynamicAttrs = ref([]); // Thuộc tính động từ ThuocTinhService
const thuocTinhValues = reactive({}); // { [tenTruong]: giaTri }

let productRefDataPromise = null;
const ensureProductRefData = () => {
  if (productRefDataPromise) return productRefDataPromise;
  productRefDataPromise = Promise.all([
    DanhMucService.getAll().catch(() => []),
    DmService.getThuongHieu().catch(() => []),
    DmService.getCpu().catch(() => []),
    DmService.getRam().catch(() => []),
    DmService.getOCung().catch(() => []),
    DmService.getGpu().catch(() => []),
    ensureSuppliers(),
    ThuocTinhService.getAll().catch(() => []),
  ]).then(([cat, br, cpu, ram, oc, gpu, , attrs]) => {
    categories.value = cat;
    brands.value = br;
    cpuList.value = cpu;
    ramList.value = ram;
    oCungList.value = oc;
    gpuList.value = gpu;
    suppliers.value = SuppliersStore.items ?? [];
    const STANDARD_ATTR_FIELDS = new Set(["mau_sac", "man_hinh", "pin", "he_dieu_hanh", "trong_luong"]);
    dynamicAttrs.value = Array.isArray(attrs)
      ? attrs
          .filter((a) => !STANDARD_ATTR_FIELDS.has(a.tenTruong))
          .sort((a, b) => (a.thuTuHienThi ?? 0) - (b.thuTuHienThi ?? 0))
      : [];
    // Khởi tạo giá trị mặc định cho thuộc tính động
    dynamicAttrs.value.forEach((attr) => {
      if (!(attr.tenTruong in thuocTinhValues)) {
        thuocTinhValues[attr.tenTruong] = "";
      }
    });
  });
  return productRefDataPromise;
};

// Regex và helper gán/trích xuất metadata thuộc tính động vào moTa
const METADATA_TAG_REGEX = /<!--METADATA_THUOC_TINH:([\s\S]*?)-->/;
const tríchXuatThuocTinhTuMoTa = (moTaStr) => {
  if (!moTaStr) return {};
  const match = String(moTaStr).match(METADATA_TAG_REGEX);
  if (match && match[1]) {
    try {
      return JSON.parse(match[1]) || {};
    } catch (e) {
      console.warn("Lỗi parse metadata thuộc tính từ mô tả", e);
    }
  }
  return {};
};
const loaiBoMetadataTuMoTa = (moTaStr) => (moTaStr ? String(moTaStr).replace(METADATA_TAG_REGEX, "").trim() : "");
const ganMetadataVaoMoTa = (moTaStr, thuocTinhObj) => {
  const baseMoTa = loaiBoMetadataTuMoTa(moTaStr);
  const hasValues = thuocTinhObj && Object.values(thuocTinhObj).some((v) => v !== "" && v != null);
  return hasValues ? `${baseMoTa}\n<!--METADATA_THUOC_TINH:${JSON.stringify(thuocTinhObj)}-->` : baseMoTa;
};

// Reset promise để load lại thuộc tính động mỗi lần mở modal
const resetRefDataCache = () => { productRefDataPromise = null; };

const formError = ref("");
const saving = ref(false);
const soSerialMoi = ref("");
const imagePreview = ref("");
const imageFilePending = ref(null);

const PHAN_LOAI_TAG_OPTIONS = [
  { value: "gaming", label: "Gaming" },
  { value: "van_phong", label: "Văn phòng" },
  { value: "sinh_vien", label: "Sinh viên" },
  { value: "do_hoa", label: "Đồ họa" },
  { value: "ky_thuat", label: "Kỹ thuật" },
  { value: "macbook", label: "MacBook" },
  { value: "cu", label: "Cũ" },
  { value: "gia_re", label: "Giá rẻ" },
  { value: "linh_kien", label: "Linh kiện" },
];
const toggleTag = (value) => {
  const tags = form.phanLoaiTags
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
  const idx = tags.indexOf(value);
  if (idx === -1) tags.push(value);
  else tags.splice(idx, 1);
  form.phanLoaiTags = tags.join(",");
  form.phanLoaiTen = tags
    .map((v) => PHAN_LOAI_TAG_OPTIONS.find((o) => o.value === v)?.label)
    .filter(Boolean)
    .join(", ");
};
const isTagSelected = (value) =>
  form.phanLoaiTags
    .split(",")
    .map((s) => s.trim())
    .includes(value);

const emptyForm = () => ({
  bienTheId: null,
  tenSanPham: "",
  thuongHieuId: null,
  danhMucId: null,
  nhaCungCapId: null,
  loaiSanPham: "",
  maSku: "",
  cpuId: null,
  ramId: null,
  oCungId: null,
  gpuId: null,
  kichThuocManHinh: "",
  heDieuHanh: "",
  pin: "",
  trongLuongKg: "",
  mauSac: "",
  giaBan: "",
  giaNhap: "",
  baoHanhThang: "",
  moTa: "",
  hinhAnhChinh: "",
  trangThai: "active",
  phanLoaiTags: "",
  phanLoaiTen: "",
});
const form = reactive(emptyForm());

const resetImageState = () => {
  imagePreview.value = "";
  imageFilePending.value = null;
};

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return;
    // Reset cache để luôn tải lại thuộc tính động mới nhất
    resetRefDataCache();
    await ensureProductRefData();
    formError.value = "";
    soSerialMoi.value = "";
    resetImageState();
    // Reset tất cả thuocTinhValues
    Object.keys(thuocTinhValues).forEach((k) => delete thuocTinhValues[k]);
    dynamicAttrs.value.forEach((attr) => { thuocTinhValues[attr.tenTruong] = ""; });

    if (props.mode === "edit") {
      const variants = (ProductsStore.items ?? []).filter((p) => p.sanPhamId === props.sanPhamId);
      const base = variants[0];
      if (!base) {
        emit("update:modelValue", false);
        return;
      }
      // Khớp ID cấu hình nếu base chỉ có tên hiển thị từ SanPhamResponse
      const resolvedCpuId = base.cpuId ?? cpuList.value.find((c) => (c.tenCpu ?? "").trim().toLowerCase() === (base.cpu ?? "").trim().toLowerCase())?.cpuId ?? null;
      const resolvedRamId = base.ramId ?? ramList.value.find((r) => (r.dungLuong ?? "").trim().toLowerCase() === (base.ram ?? "").trim().toLowerCase())?.ramId ?? null;
      const ocName = (base.oCung ?? base.ocung ?? "").trim().toLowerCase();
      const matchedOc = oCungList.value.find((o) => (o.loaiOcung ?? o.loaiOCung ?? o.LoaiOcung ?? "").trim().toLowerCase() === ocName);
      const resolvedOcungId = base.oCungId ?? base.ocungId ?? (matchedOc?.oCungId ?? matchedOc?.ocungId ?? matchedOc?.id ?? null);
      const resolvedGpuId = base.gpuId ?? gpuList.value.find((g) => (g.tenGpu ?? "").trim().toLowerCase() === (base.gpu ?? "").trim().toLowerCase())?.gpuId ?? null;

      Object.assign(form, {
        bienTheId: null,
        tenSanPham: base.tenSanPham || "",
        thuongHieuId: base.thuongHieuId,
        danhMucId: base.danhMucId,
        nhaCungCapId: base.nhaCungCapId,
        loaiSanPham: base.loaiSanPham || "",
        maSku: base.maSku || "",
        cpuId: resolvedCpuId,
        ramId: resolvedRamId,
        oCungId: resolvedOcungId,
        gpuId: resolvedGpuId,
        kichThuocManHinh: base.kichThuocManHinh || "",
        heDieuHanh: base.heDieuHanh || "",
        pin: base.pin || "",
        trongLuongKg: base.trongLuongKg ?? "",
        mauSac: base.mauSac || "",
        giaBan: base.giaBan ?? "",
        giaNhap: base.giaNhap ?? "",
        baoHanhThang: base.baoHanhThang ?? "",
        moTa: loaiBoMetadataTuMoTa(base.moTa || ""),
        hinhAnhChinh: base.hinhAnhChinh || "",
        trangThai: base.trangThai || "active",
        phanLoaiTags: base.phanLoaiTags || "",
        phanLoaiTen: base.phanLoaiTen || "",
      });
      // Nạp giá trị thuộc tính động từ dữ liệu sản phẩm (nếu có)
      const savedAttrs = tríchXuatThuocTinhTuMoTa(base.moTa);
      dynamicAttrs.value.forEach((attr) => {
        thuocTinhValues[attr.tenTruong] = savedAttrs[attr.tenTruong] ?? base[attr.tenTruong] ?? base.thuocTinhValues?.[attr.tenTruong] ?? "";
      });
      imagePreview.value = base.hinhAnhChinh || "";
    } else {
      Object.assign(form, emptyForm());
    }
  },
);

const handleImageFile = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  imageFilePending.value = file;
  imagePreview.value = URL.createObjectURL(file);
};

const close = () => emit("update:modelValue", false);

const save = async () => {
  formError.value = "";
  if (saving.value) return;
  saving.value = true;
  try {
    if (imageFilePending.value) {
      const fd = new FormData();
      fd.append("file", imageFilePending.value);
      try {
        const upRes = await fetch("/api/upload/image", {
          method: "POST",
          headers: authHeaders(),
          body: fd,
        });
        if (upRes.ok) {
          const upData = await upRes.json();
          form.hinhAnhChinh = upData.url;
        } else {
          formError.value = t("admin.errors.uploadFailed", { status: upRes.status });
          return;
        }
      } catch (e) {
        formError.value = t("admin.errors.uploadError", { message: e.message });
        return;
      }
    }

    const body = {
      ...form,
      thuongHieuId: Number(form.thuongHieuId),
      danhMucId: Number(form.danhMucId),
      nhaCungCapId: form.nhaCungCapId ? Number(form.nhaCungCapId) : null,
      cpuId: form.cpuId ? Number(form.cpuId) : null,
      ramId: form.ramId ? Number(form.ramId) : null,
      oCungId: form.oCungId ? Number(form.oCungId) : null,
      ocungId: form.oCungId ? Number(form.oCungId) : null,
      gpuId: form.gpuId ? Number(form.gpuId) : null,
      giaBan: Number(form.giaBan),
      giaNhap: Number(form.giaNhap),
      trongLuongKg: form.trongLuongKg ? Number(form.trongLuongKg) : null,
      baoHanhThang: Number(form.baoHanhThang),
      moTa: ganMetadataVaoMoTa(form.moTa, thuocTinhValues) || "",
      ngayTao: props.mode === "edit" ? null : nowLocalIso(),
      // Gộp thuộc tính động vào body (flat fields + object thuocTinhValues)
      ...thuocTinhValues,
      thuocTinhValues: { ...thuocTinhValues },
    };
    if (props.mode === "edit") {
      body.bienTheId = null;
    }
    try {
      const res = await SanPhamService.save(props.mode === "edit" ? props.sanPhamId : null, body);
      if (!res.ok) {
        formError.value = t("admin.errors.saveFailed", { status: res.status, text: await res.text() });
        return;
      }

      if (soSerialMoi.value.trim()) {
        const newList = await SanPhamService.getAll().catch(() => []);
        const newVariant = [...newList].reverse().find((p) => p.maSku === form.maSku);
        if (newVariant) {
          await ChiTietSanPhamService.create({
            bienTheId: newVariant.bienTheId,
            soSerial: soSerialMoi.value.trim(),
            trangThai: "trong_kho",
            ngayNhapKho: nowLocalIso(),
          }).catch(() => {});
        }
      }

      resetImageState();
      emit("saved");
      emit("update:modelValue", false);
    } catch (e) {
      formError.value = e.message;
    }
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <div
    v-if="modelValue"
    class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background: var(--bg-overlay); z-index: 1000"
    @click.self="close()"
  >
    <div
      class="rounded-4 d-flex flex-column"
      style="
        background: var(--bg-card);
        border: 1px solid var(--border-color-strong);
        width: 860px;
        max-width: 96vw;
        max-height: 92vh;
      "
    >
      <div
        class="d-flex justify-content-between align-items-center px-4 py-3"
        style="border-bottom: 1px solid var(--border-color)"
      >
        <div>
          <div class="fw-bold text-light" style="font-size: 1rem">
            {{
              (mode === 'edit')
                ? t("admin.productModal.titleEdit")
                : t("admin.productModal.titleAdd")
            }}
          </div>
        </div>
      </div>

      <div class="overflow-y-auto px-4 py-3" style="gap: 0">
        <div v-if="formError" class="alert alert-danger small py-2 mb-3">
          {{ formError }}
        </div>
        <div v-if="(mode === 'edit')" class="alert alert-info small py-2 mb-3">
          Khi sửa sản phẩm, các trường biến thể (SKU, cấu hình, giá, màu, bảo
          hành...) sẽ bị khóa và không thể thay đổi.
        </div>

        <div
          class="text-uppercase fw-bold mb-2"
          style="font-size: 0.65rem; letter-spacing: 0.1em; color: #60a5fa"
        >
          {{ t("admin.productModal.sectionBasic") }}
        </div>
        <div
          class="rounded-3 p-3 mb-3"
          style="
            background: var(--bg-input);
            border: 1px solid var(--border-color);
          "
        >
          <div class="row g-3">
            <div class="col-8">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.nameLabel")
              }}</label>
              <input
                v-model="form.tenSanPham"
                class="form-control form-control-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
                :placeholder="t('admin.productModal.namePlaceholder')"
              />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.skuLabel")
              }}</label>
              <input
                v-model="form.maSku"
                :disabled="(mode === 'edit')"
                class="form-control form-control-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                  font-family: monospace;
                "
                :placeholder="t('admin.productModal.skuPlaceholder')"
              />
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.typeLabel")
              }}</label>
              <select
                v-model="form.loaiSanPham"
                :disabled="(mode === 'edit')"
                class="form-select form-select-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              >
                <option value="" disabled>
                  {{ t("admin.productModal.selectPlaceholder") }}
                </option>
                <option value="LAPTOP">
                  {{ t("admin.productModal.typeLaptop") }}
                </option>
                <option value="PHU_KIEN">
                  {{ t("admin.productModal.typeAccessory") }}
                </option>
              </select>
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.statusLabel")
              }}</label>
              <select
                v-model="form.trangThai"
                class="form-select form-select-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              >
                <option value="active">
                  {{ t("admin.productModal.statusActive") }}
                </option>
                <option value="inactive">
                  {{ t("admin.productModal.statusInactive") }}
                </option>
                <option value="ngung_kinh_doanh">
                  {{ t("admin.productModal.statusDiscontinued") }}
                </option>
              </select>
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.colorLabel")
              }}</label>
              <input
                v-model="form.mauSac"
                class="form-control form-control-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
                :placeholder="t('admin.productModal.colorPlaceholder')"
              />
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.warrantyLabel")
              }}</label>
              <input
                v-model="form.baoHanhThang"
                type="number"
                :disabled="(mode === 'edit')"
                class="form-control form-control-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.brandLabel")
              }}</label>
              <select
                v-model="form.thuongHieuId"
                class="form-select form-select-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              >
                <option :value="null" disabled>
                  {{ t("admin.productModal.selectPlaceholder") }}
                </option>
                <option
                  v-for="b in brands"
                  :key="b.thuongHieuId"
                  :value="b.thuongHieuId"
                >
                  {{ b.tenThuongHieu }}
                </option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.categoryLabel")
              }}</label>
              <select
                v-model="form.danhMucId"
                class="form-select form-select-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              >
                <option :value="null" disabled>
                  {{ t("admin.productModal.selectPlaceholder") }}
                </option>
                <option v-for="c in categories" :key="c.id" :value="c.id">
                  {{ c.tenDanhMuc }}
                </option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.supplierLabel")
              }}</label>
              <select
                v-model="form.nhaCungCapId"
                class="form-select form-select-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              >
                <option :value="null">
                  {{ t("admin.productModal.noneOption") }}
                </option>
                <option
                  v-for="s in suppliers"
                  :key="s.nhaCungCapId"
                  :value="s.nhaCungCapId"
                >
                  {{ s.tenNhaCungCap }}
                </option>
              </select>
            </div>
          </div>
        </div>

        <div
          class="text-uppercase fw-bold mb-2"
          style="font-size: 0.65rem; letter-spacing: 0.1em; color: #60a5fa"
        >
          {{ t("admin.productModal.sectionTech") }}
        </div>
        <div
          class="rounded-3 p-3 mb-3"
          style="
            background: var(--bg-input);
            border: 1px solid var(--border-color);
          "
        >
          <div class="row g-3">
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.cpuLabel")
              }}</label>
              <select
                v-model="form.cpuId"
                class="form-select form-select-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              >
                <option :value="null">
                  {{ t("admin.productModal.noneOption") }}
                </option>
                <option v-for="c in cpuList" :key="c.cpuId" :value="c.cpuId">
                  {{ c.tenCpu }}
                </option>
              </select>
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.gpuLabel")
              }}</label>
              <select
                v-model="form.gpuId"
                class="form-select form-select-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              >
                <option :value="null">
                  {{ t("admin.productModal.noneOption") }}
                </option>
                <option v-for="g in gpuList" :key="g.gpuId" :value="g.gpuId">
                  {{ g.tenGpu }}
                </option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.ramLabel")
              }}</label>
              <select
                v-model="form.ramId"
                class="form-select form-select-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              >
                <option :value="null">
                  {{ t("admin.productModal.noneOption") }}
                </option>
                <option v-for="r in ramList" :key="r.ramId" :value="r.ramId">
                  {{ r.dungLuong }}
                </option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.storageLabel")
              }}</label>
              <select
                v-model="form.oCungId"
                class="form-select form-select-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              >
                <option :value="null">
                  {{ t("admin.productModal.noneOption") }}
                </option>
                <option
                  v-for="o in oCungList"
                  :key="o.oCungId ?? o.ocungId ?? o.id"
                  :value="o.oCungId ?? o.ocungId ?? o.id"
                >
                  {{ o.loaiOcung ?? o.loaiOCung ?? o.LoaiOcung }}
                </option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.screenLabel")
              }}</label>
              <input
                v-model="form.kichThuocManHinh"
                class="form-control form-control-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
                :placeholder="t('admin.productModal.screenPlaceholder')"
              />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.osLabel")
              }}</label>
              <input
                v-model="form.heDieuHanh"
                class="form-control form-control-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
                :placeholder="t('admin.productModal.osPlaceholder')"
              />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.batteryLabel")
              }}</label>
              <input
                v-model="form.pin"
                class="form-control form-control-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
                :placeholder="t('admin.productModal.batteryPlaceholder')"
              />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.weightLabel")
              }}</label>
              <input
                v-model="form.trongLuongKg"
                type="number"
                step="0.1"
                class="form-control form-control-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              />
            </div>
          </div>
        </div>

        <div
          class="text-uppercase fw-bold mb-2"
          style="font-size: 0.65rem; letter-spacing: 0.1em; color: #60a5fa"
        >
          {{ t("admin.productModal.sectionPrice") }}
        </div>
        <div
          class="rounded-3 p-3 mb-3"
          style="
            background: var(--bg-input);
            border: 1px solid var(--border-color);
          "
        >
          <div class="row g-3">
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.priceSellLabel")
              }}</label>
              <input
                v-model="form.giaBan"
                type="number"
                class="form-control form-control-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              />
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.priceBuyLabel")
              }}</label>
              <input
                v-model="form.giaNhap"
                type="number"
                class="form-control form-control-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              />
            </div>
          </div>
        </div>

        <div
          class="text-uppercase fw-bold mb-2"
          style="font-size: 0.65rem; letter-spacing: 0.1em; color: #60a5fa"
        >
          {{ t("admin.productModal.sectionMedia") }}
        </div>
        <div
          class="rounded-3 p-3 mb-3"
          style="
            background: var(--bg-input);
            border: 1px solid var(--border-color);
          "
        >
          <div class="row g-3">
            <div class="col-12">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.imageLabel")
              }}</label>
              <div class="d-flex align-items-center gap-3">
                <label
                  class="d-flex flex-column align-items-center justify-content-center rounded-3 border border-secondary text-secondary"
                  style="
                    width: 110px;
                    height: 88px;
                    cursor: pointer;
                    flex-shrink: 0;
                    overflow: hidden;
                    background: var(--bg-card-inset);
                  "
                >
                  <img
                    v-if="imagePreview"
                    :src="imagePreview"
                    style="width: 110px; height: 88px; object-fit: contain"
                  />
                  <template v-else>
                    <span style="font-size: 1.4rem">&#128247;</span>
                    <span style="font-size: 0.68rem; margin-top: 4px">{{
                      t("admin.productModal.imageClickToChoose")
                    }}</span>
                  </template>
                  <input
                    type="file"
                    accept="image/*"
                    class="d-none"
                    @change="handleImageFile"
                  />
                </label>
                <div
                  v-if="imageFilePending"
                  class="text-warning"
                  style="font-size: 0.75rem"
                >
                  {{ imageFilePending.name }}
                </div>
                <div v-else class="text-secondary" style="font-size: 0.75rem">
                  {{ t("admin.productModal.imageFormats") }}
                </div>
              </div>
            </div>
            <div class="col-12">
              <label class="form-label small text-secondary mb-1">{{
                t("admin.productModal.descLabel")
              }}</label>
              <textarea
                v-model="form.moTa"
                rows="3"
                class="form-control form-control-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
              ></textarea>
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t("admin.productModal.tagsLabel") }}
                <span class="text-warning small">{{
                  t("admin.productModal.tagsHint")
                }}</span></label>
              <div class="d-flex flex-wrap gap-2">
                <button
                  v-for="opt in PHAN_LOAI_TAG_OPTIONS"
                  :key="opt.value"
                  type="button"
                  class="btn btn-sm"
                  :class="
                    isTagSelected(opt.value)
                      ? 'btn-warning text-dark fw-bold'
                      : 'btn-outline-secondary'
                  "
                  style="
                    font-size: 0.75rem;
                    padding: 3px 12px;
                    border-radius: 999px;
                  "
                  @click="toggleTag(opt.value)"
                >
                  {{ opt.label }}
                </button>
              </div>
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t("admin.productModal.tagNameLabel") }}
                <span class="text-muted small">{{
                  t("admin.productModal.tagNameHint")
                }}</span></label>
              <input
                v-model="form.phanLoaiTen"
                class="form-control form-control-sm"
                style="
                  background: var(--bg-input);
                  color: var(--text-primary);
                  border-color: var(--border-color-strong);
                "
                :placeholder="t('admin.productModal.tagNamePlaceholder')"
              />
            </div>
          </div>
        </div>

        <!-- ── Thuộc tính động ── -->
        <div v-if="dynamicAttrs.length > 0">
          <div
            class="text-uppercase fw-bold mb-2"
            style="font-size: 0.65rem; letter-spacing: 0.1em; color: #a78bfa"
          >
            Thuộc tính bổ sung
          </div>
          <div
            class="rounded-3 p-3 mb-3"
            style="
              background: var(--bg-input);
              border: 1px solid var(--border-color);
            "
          >
            <div class="row g-3">
              <div
                v-for="attr in dynamicAttrs"
                :key="attr.thuocTinhId"
                class="col-6"
              >
                <label class="form-label small text-secondary mb-1">
                  {{ attr.tenHienThi }}
                  <span v-if="attr.batBuoc" class="text-danger">*</span>
                </label>
                <!-- Select với danh sách giá trị -->
                <select
                  v-if="attr.loaiDuLieu === 'select' && attr.giaTriList && attr.giaTriList.length > 0"
                  v-model="thuocTinhValues[attr.tenTruong]"
                  class="form-select form-select-sm"
                  style="
                    background: var(--bg-input);
                    color: var(--text-primary);
                    border-color: var(--border-color-strong);
                  "
                >
                  <option value="">-- Chọn {{ attr.tenHienThi }} --</option>
                  <option
                    v-for="gv in attr.giaTriList"
                    :key="gv.giaTriId"
                    :value="gv.giaTri"
                  >
                    {{ gv.giaTri }}
                  </option>
                </select>
                <!-- Input text nếu không có giá trị hoặc loại text -->
                <input
                  v-else
                  v-model="thuocTinhValues[attr.tenTruong]"
                  class="form-control form-control-sm"
                  style="
                    background: var(--bg-input);
                    color: var(--text-primary);
                    border-color: var(--border-color-strong);
                  "
                  :placeholder="'Nhập ' + attr.tenHienThi"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- ── Serial (luon co, modal nay chi con tao moi) ── -->
        <div v-if="!(mode === 'edit')">
          <div
            class="text-uppercase fw-bold mb-2"
            style="
              font-size: 0.65rem;
              letter-spacing: 0.1em;
              color: var(--accent-fg);
            "
          >
            {{ t("admin.productModal.sectionSerial") }}
          </div>
          <div
            class="rounded-3 p-3"
            style="
              background: var(--bg-input);
              border: 1px solid var(--border-color);
            "
          >
            <label class="form-label small text-secondary mb-1">{{ t("admin.productModal.serialLabel") }}
              <span class="text-danger">*</span></label>
            <input
              v-model="soSerialMoi"
              class="form-control form-control-sm"
              style="
                background: var(--bg-input);
                color: var(--text-primary);
                border-color: var(--border-color-strong);
                font-family: monospace;
              "
              :placeholder="t('admin.productModal.serialPlaceholder')"
            />
            <div class="text-secondary mt-1" style="font-size: 0.72rem">
              {{ t("admin.productModal.serialHint") }}
            </div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div
        class="d-flex justify-content-end gap-2 px-4 py-3"
        style="border-top: 1px solid var(--border-color)"
      >
        <button
          class="btn btn-sm btn-outline-secondary px-3"
          @click="close()"
        >
          {{ t("admin.productModal.cancel") }}
        </button>
        <button
          class="btn btn-sm btn-warning text-dark fw-bold px-4"
          :disabled="saving"
          @click="save"
        >
          {{
            (mode === 'edit')
              ? t("admin.productModal.update")
              : t("admin.productModal.addNew")
          }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.text-light {
  color: var(--text-primary) !important;
}
</style>
