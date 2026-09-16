<script setup>
import { ref, computed } from "vue";
import { AuthStore } from "../../stores/index.js";
import * as PhieuBaoHanhService from "../../services/PhieuBaoHanhService.js";
import * as HinhAnhBaoHanhService from "../../services/HinhAnhBaoHanhService.js";
import { ACCEPTED_UPLOAD, MAX_UPLOAD_SIZE } from "../../services/warrantyConstants.js";
import {
  X, ShieldPlus, Upload, Loader2, Package, Calendar, MapPin, Store,
  Truck, CheckCircle2, Image as ImageIcon, Video, Trash2, Send, AlertCircle,
} from '@lucide/vue';

const props = defineProps({
  product: { type: Object, required: true },
});
const emit = defineEmits(["close", "submitted", "toast"]);
const auth = AuthStore;

const moTaLoi = ref('');
const phuongThuc = ref('tai_cua_hang');
const diaChiLayHang = ref(auth.user?.diaChi || '');
const hinhAnhFiles = ref([]);
const submitting = ref(false);
const uploadError = ref('');

const errors = computed(() => {
  const e = {};
  if (!moTaLoi.value.trim()) e.moTa = 'Vui lòng mô tả lỗi';
  else if (moTaLoi.value.trim().length < 10) e.moTa = 'Mô tả lỗi cần ít nhất 10 ký tự';
  if (phuongThuc.value === 'giao_tan_noi' && !diaChiLayHang.value.trim()) e.diaChi = 'Vui lòng nhập địa chỉ lấy hàng';
  return e;
});

const canSubmit = computed(() => Object.keys(errors.value).length === 0);

const onFileChange = (e) => {
  uploadError.value = '';
  const files = Array.from(e.target.files || []);
  for (const file of files) {
    if (!['image/jpeg','image/png','image/webp','image/gif','video/mp4','video/webm','video/quicktime'].includes(file.type)) {
      uploadError.value = 'Chỉ chấp nhận file ảnh hoặc video';
      return;
    }
    if (file.size > MAX_UPLOAD_SIZE) {
      uploadError.value = `File "${file.name}" vượt quá 20MB`;
      return;
    }
    if (hinhAnhFiles.value.length >= 5) {
      uploadError.value = 'Tối đa 5 file';
      return;
    }
    const url = URL.createObjectURL(file);
    hinhAnhFiles.value.push({ file, previewUrl: url, tenFile: file.name, loai: file.type.startsWith('image/') ? 'image' : 'video' });
  }
  e.target.value = '';
};

const removeFile = (i) => { URL.revokeObjectURL(hinhAnhFiles.value[i].previewUrl); hinhAnhFiles.value.splice(i, 1); };

const formatDate = (d) => {
  if (!d) return '—';
  try { return new Date(d).toLocaleDateString('vi-VN'); } catch { return d; }
};

const submit = async () => {
  if (!canSubmit.value || submitting.value) return;
  submitting.value = true;
  try {
    const payload = {
      khachHangId: auth.user?.id,
      chiTietId: props.product.chiTietId,
      donHangId: props.product.donHangId,
      moTaLoi: moTaLoi.value.trim(),
      phuongThuc: phuongThuc.value,
      diaChiLayHang: phuongThuc.value === 'giao_tan_noi' ? diaChiLayHang.value.trim() : null,
      trangThai: 'cho_xu_ly',
    };
    const res = await PhieuBaoHanhService.create(payload);
    if (!res.ok) throw new Error(await res.text());
    const baoHanhId = await res.json();
    
    // Upload images
    for (const f of hinhAnhFiles.value) {
      const formData = new FormData();
      formData.append('file', f.file);
      formData.append('loai', f.loai);
      formData.append('tenFile', f.tenFile);
      await HinhAnhBaoHanhService.upload(baoHanhId, formData).catch(() => {});
    }
    emit('submitted');
  } catch (e) {
    emit('toast', e.message || 'Gửi yêu cầu thất bại', 'error');
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal">
      <div class="modal-header">
        <div class="modal-title">
          <ShieldPlus :size="20" class="modal-icon" />
          <div>
            <div class="modal-title-main">Yêu cầu bảo hành</div>
            <div class="modal-title-sub">Gửi phiếu bảo hành cho sản phẩm của bạn</div>
          </div>
        </div>
        <button class="modal-close-btn" @click="emit('close')"><X :size="18" /></button>
      </div>

      <div class="modal-body">
        <!-- Product info -->
        <div class="product-info-block">
          <div class="product-thumb">
            <img v-if="product.hinhAnh" :src="product.hinhAnh" />
            <Package v-else :size="24" />
          </div>
          <div class="product-detail">
            <div class="product-name">{{ product.tenSanPham || product.tenBienThe || '—' }}</div>
            <div class="product-meta">
              <span v-if="product.maSku">SKU: <strong>{{ product.maSku }}</strong></span>
              <span v-if="product.soSerial">Serial: <strong>{{ product.soSerial }}</strong></span>
            </div>
            <div class="product-warranty">
              <Calendar :size="12" />
              Hết hạn bảo hành: <strong>{{ formatDate(product.ngayHetBaoHanh) }}</strong>
              · Đơn hàng <strong>#{{ product.maDon || product.donHangId }}</strong>
            </div>
          </div>
        </div>

        <!-- Issue description -->
        <div class="form-group">
          <label class="form-label">Mô tả lỗi <span class="required">*</span></label>
          <textarea
            v-model="moTaLoi"
            class="form-textarea"
            :class="{ 'has-error': errors.moTa }"
            placeholder="Mô tả chi tiết lỗi bạn gặp phải (vd: máy không lên nguồn, sạc không vào, màn hình sọc,...)"
            rows="4"
          ></textarea>
          <div v-if="errors.moTa" class="field-error">
            <AlertCircle :size="12" /> {{ errors.moTa }}
          </div>
          <div class="char-count">{{ moTaLoi.length }} / 500 ký tự</div>
        </div>

        <!-- Upload -->
        <div class="form-group">
          <label class="form-label">Ảnh / Video minh chứng</label>
          <div class="upload-area">
            <input type="file" accept="image/*,video/*" multiple @change="onFileChange" class="file-input" id="claim-file-input" />
            <label for="claim-file-input" class="upload-trigger">
              <Upload :size="24" class="upload-icon" />
              <div class="upload-text">Click để chọn ảnh/video</div>
              <div class="upload-hint">JPG, PNG, WebP, MP4 · Tối đa 20MB mỗi file · Tối đa 5 file</div>
            </label>
          </div>
          <div v-if="uploadError" class="field-error"><AlertCircle :size="12" /> {{ uploadError }}</div>
          <div v-if="hinhAnhFiles.length > 0" class="preview-files">
            <div v-for="(f, i) in hinhAnhFiles" :key="i" class="preview-item">
              <img v-if="f.loai === 'image'" :src="f.previewUrl" class="preview-thumb" />
              <video v-else :src="f.previewUrl" class="preview-thumb" muted></video>
              <div class="preview-info">
                <div class="preview-name">{{ f.tenFile }}</div>
                <div class="preview-type">
                  <ImageIcon v-if="f.loai === 'image'" :size="10" /> 
                  <Video v-else :size="10" />
                  {{ f.loai === 'image' ? 'Ảnh' : 'Video' }}
                </div>
              </div>
              <button class="preview-remove" @click="removeFile(i)"><Trash2 :size="13" /></button>
            </div>
          </div>
        </div>

        <!-- Method -->
        <div class="form-group">
          <label class="form-label">Phương thức gửi <span class="required">*</span></label>
          <div class="method-options">
            <label class="method-option" :class="{ 'is-active': phuongThuc === 'tai_cua_hang' }">
              <input type="radio" v-model="phuongThuc" value="tai_cua_hang" />
              <div class="method-icon"><Store :size="20" /></div>
              <div class="method-text">
                <div class="method-name">Mang đến cửa hàng</div>
                <div class="method-desc">Bạn tự mang máy đến cửa hàng SAOClub gần nhất</div>
              </div>
              <CheckCircle2 v-if="phuongThuc === 'tai_cua_hang'" :size="18" class="method-check" />
            </label>
            <label class="method-option" :class="{ 'is-active': phuongThuc === 'giao_tan_noi' }">
              <input type="radio" v-model="phuongThuc" value="giao_tan_noi" />
              <div class="method-icon"><Truck :size="20" /></div>
              <div class="method-text">
                <div class="method-name">Nhân viên đến lấy</div>
                <div class="method-desc">Nhân viên đến tận nơi lấy máy (phí vận chuyển có thể phát sinh)</div>
              </div>
              <CheckCircle2 v-if="phuongThuc === 'giao_tan_noi'" :size="18" class="method-check" />
            </label>
          </div>
        </div>

        <!-- Address (if pickup) -->
        <Transition name="fade-in">
          <div v-if="phuongThuc === 'giao_tan_noi'" class="form-group">
            <label class="form-label">Địa chỉ lấy hàng <span class="required">*</span></label>
            <textarea
              v-model="diaChiLayHang"
              class="form-textarea"
              :class="{ 'has-error': errors.diaChi }"
              placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố"
              rows="2"
            ></textarea>
            <div v-if="errors.diaChi" class="field-error"><AlertCircle :size="12" /> {{ errors.diaChi }}</div>
          </div>
        </Transition>

        <!-- Note -->
        <div class="info-note">
          <AlertCircle :size="13" />
          Yêu cầu sẽ được nhân viên xác nhận trong vòng 24h làm việc. Bạn sẽ nhận thông báo khi có cập nhật.
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-cancel" @click="emit('close')" :disabled="submitting">Hủy</button>
        <button class="btn-submit" @click="submit" :disabled="!canSubmit || submitting">
          <Loader2 v-if="submitting" :size="15" class="spin" />
          <Send v-else :size="15" />
          {{ submitting ? 'Đang gửi...' : 'Gửi yêu cầu' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,.5);
  display: flex; align-items: center; justify-content: center;
  z-index: 1200; padding: 20px; backdrop-filter: blur(4px);
}
.modal {
  background: white; border-radius: 16px; width: 100%; max-width: 560px;
  max-height: 92vh; display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0,0,0,.25);
}
.modal-header {
  padding: 16px 20px; border-bottom: 1px solid #e5e7eb;
  display: flex; justify-content: space-between; align-items: center;
  background: linear-gradient(180deg, #fff5f9 0%, white 100%);
}
.modal-title { display: flex; gap: 10px; align-items: center; }
.modal-icon { color: #db2777; flex-shrink: 0; }
.modal-title-main { font-weight: 800; font-size: 1rem; color: #111827; }
.modal-title-sub { font-size: 12px; color: #6b7280; margin-top: 2px; }
.modal-close-btn { width: 32px; height: 32px; background: transparent; border: none; border-radius: 8px; color: #6b7280; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.modal-close-btn:hover { background: #f3f4f6; color: #111827; }
.modal-body { padding: 18px 20px; overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 16px; }
.product-info-block { display: flex; gap: 12px; padding: 12px; background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 12px; }
.product-thumb { width: 64px; height: 64px; background: white; border: 1px solid #e5e7eb; border-radius: 10px; display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0; }
.product-thumb img { width: 100%; height: 100%; object-fit: contain; padding: 4px; }
.product-thumb svg { color: #9ca3af; }
.product-detail { flex: 1; min-width: 0; }
.product-name { font-weight: 700; font-size: 13.5px; color: #111827; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.product-meta { display: flex; gap: 12px; font-size: 11.5px; color: #6b7280; margin-top: 4px; flex-wrap: wrap; }
.product-warranty { display: flex; align-items: center; gap: 4px; font-size: 11.5px; color: #6b7280; margin-top: 6px; flex-wrap: wrap; }
.product-warranty strong { color: #db2777; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 13px; font-weight: 700; color: #374151; }
.required { color: #db2777; }
.form-textarea { width: 100%; padding: 10px 12px; background: white; border: 1.5px solid #e5e7eb; border-radius: 10px; color: #111827; font-size: 13px; font-family: inherit; resize: vertical; transition: all .2s ease; line-height: 1.5; }
.form-textarea:focus { outline: none; border-color: #db2777; box-shadow: 0 0 0 3px rgba(219,39,119,.1); }
.form-textarea.has-error { border-color: #ef4444; }
.field-error { display: flex; align-items: center; gap: 4px; font-size: 11.5px; color: #ef4444; font-weight: 600; }
.char-count { font-size: 11px; color: #9ca3af; text-align: right; }
.upload-area { position: relative; }
.file-input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
.upload-trigger { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; padding: 24px; border: 2px dashed #e5e7eb; border-radius: 12px; cursor: pointer; transition: all .2s ease; text-align: center; }
.upload-trigger:hover { border-color: #db2777; background: #fff5f9; }
.upload-icon { color: #9ca3af; }
.upload-text { font-size: 13px; font-weight: 700; color: #374151; }
.upload-hint { font-size: 11px; color: #9ca3af; }
.preview-files { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
.preview-item { display: flex; align-items: center; gap: 10px; padding: 8px; background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 10px; }
.preview-thumb { width: 48px; height: 48px; object-fit: cover; border-radius: 6px; flex-shrink: 0; }
.preview-info { flex: 1; min-width: 0; }
.preview-name { font-size: 12px; font-weight: 600; color: #111827; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.preview-type { display: flex; align-items: center; gap: 4px; font-size: 11px; color: #9ca3af; margin-top: 2px; }
.preview-remove { width: 28px; height: 28px; background: transparent; border: 1px solid #e5e7eb; border-radius: 6px; color: #9ca3af; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all .15s ease; }
.preview-remove:hover { background: #fef2f2; border-color: #ef4444; color: #ef4444; }
.method-options { display: flex; flex-direction: column; gap: 8px; }
.method-option { display: flex; align-items: center; gap: 12px; padding: 12px; border: 1.5px solid #e5e7eb; border-radius: 12px; cursor: pointer; transition: all .2s ease; }
.method-option input { display: none; }
.method-option:hover { border-color: #db2777; background: #fff5f9; }
.method-option.is-active { border-color: #db2777; background: #fff5f9; }
.method-icon { width: 44px; height: 44px; background: #f3f4f6; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #6b7280; flex-shrink: 0; }
.method-option.is-active .method-icon { background: #db2777; color: white; }
.method-text { flex: 1; }
.method-name { font-weight: 700; font-size: 13px; color: #111827; }
.method-desc { font-size: 11.5px; color: #6b7280; margin-top: 2px; line-height: 1.4; }
.method-check { color: #db2777; flex-shrink: 0; }
.info-note { display: flex; gap: 8px; align-items: flex-start; background: #eff6ff; border: 1px solid rgba(37,99,235,.2); border-radius: 8px; padding: 10px 12px; font-size: 12px; color: #1e40af; line-height: 1.5; }
.info-note svg { flex-shrink: 0; margin-top: 1px; }
.modal-footer { padding: 14px 20px; border-top: 1px solid #e5e7eb; display: flex; justify-content: flex-end; gap: 8px; background: #f9fafb; }
.btn-cancel { padding: 9px 18px; background: white; border: 1.5px solid #e5e7eb; border-radius: 9999px; font-size: 13px; font-weight: 700; color: #6b7280; cursor: pointer; transition: all .2s ease; }
.btn-cancel:hover { background: #f3f4f6; }
.btn-cancel:disabled { opacity: .5; cursor: not-allowed; }
.btn-submit { display: inline-flex; align-items: center; gap: 6px; padding: 9px 20px; background: #db2777; color: white; border: none; border-radius: 9999px; font-size: 13px; font-weight: 700; cursor: pointer; transition: all .2s ease; box-shadow: 0 3px 0 #a82560; }
.btn-submit:hover:not(:disabled) { background: #a82560; transform: translateY(-1px); }
.btn-submit:disabled { opacity: .5; cursor: not-allowed; box-shadow: none; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.fade-in-enter-active, .fade-in-leave-active { transition: all .2s ease; }
.fade-in-enter-from, .fade-in-leave-to { opacity: 0; transform: translateY(-8px); }
@media (max-width: 575.98px) { .modal { max-height: 96vh; } .product-info-block { flex-direction: column; } }
</style>
