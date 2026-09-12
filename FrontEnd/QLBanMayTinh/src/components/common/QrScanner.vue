<template>
  <div class="d-flex flex-column align-items-center gap-3">
    <!-- Camera source selector -->
    <div class="d-flex gap-2 mb-2" style="flex-wrap:wrap;justify-content:center;">
      <button
        v-for="src in cameraSources" :key="src.id"
        class="btn btn-sm rounded-pill px-3"
        :class="activeSource === src.id ? 'btn-warning fw-bold' : 'btn-outline-secondary'"
        style="font-size:0.78rem;"
        @click="selectSource(src)"
      >
        {{ src.label }}
      </button>
    </div>

    <!-- Custom MJPEG URL input -->
    <div v-if="showCustomUrl" class="w-100" style="max-width:320px;">
      <div class="input-group input-group-sm">
        <input
          v-model="customUrl"
          class="form-control"
          style="font-size:0.78rem;"
          placeholder="http://localhost:4747/video"
          @keyup.enter="startCustomStream"
        />
        <button class="btn btn-outline-warning" style="font-size:0.78rem;" @click="startCustomStream">OK</button>
      </div>
      <div v-if="customError" class="small mt-1" style="color:#f87171;">{{ customError }}</div>
    </div>

    <!-- MJPEG stream video (for proxy / USB camera) -->
    <div
      v-if="(activeSource === 'usb' || activeSource === 'custom') && mjpegReady && !error"
      class="position-relative rounded-3 overflow-hidden"
      style="width:280px;height:280px;background:#000;"
    >
      <img
        v-if="activeSource === 'usb' || activeSource === 'custom'"
        :src="mjpegSrc"
        style="width:100%;height:100%;object-fit:cover;"
        alt="Camera"
      />
      <!-- Scanning frame overlay -->
      <div
        class="position-absolute top-50 start-50 translate-middle"
        style="width:180px;height:180px;border:2px solid rgba(255,255,255,0.6);border-radius:12px;box-shadow:0 0 0 9999px rgba(0,0,0,0.4);"
      >
        <div
          class="position-absolute top-0 start-0 w-100 h-100 rounded-2"
          style="animation:qr-scan-line 2s linear infinite;"
        >
          <div style="height:2px;background:linear-gradient(90deg,transparent,#48c78e,transparent);" />
        </div>
      </div>
      <div
        v-if="isScanning"
        class="position-absolute bottom-0 start-0 w-100 p-2 small text-center"
        style="background:rgba(0,0,0,0.6);color:#fff;font-size:11px;"
      >
        {{ t('qrScanner.scanning') }}
      </div>
    </div>

    <!-- Native camera preview (for direct camera) -->
    <div
      v-if="activeSource === 'direct' && isOpen && !error"
      class="position-relative rounded-3 overflow-hidden"
      style="width:280px;height:280px;background:#000;"
    >
      <video
        ref="videoEl"
        class="w-100 h-100"
        style="object-fit:cover;"
        autoplay
        muted
        playsinline
      />
      <!-- Scanning frame overlay -->
      <div
        class="position-absolute top-50 start-50 translate-middle"
        style="width:180px;height:180px;border:2px solid rgba(255,255,255,0.6);border-radius:12px;box-shadow:0 0 0 9999px rgba(0,0,0,0.4);"
      >
        <div
          class="position-absolute top-0 start-0 w-100 h-100 rounded-2"
          style="animation:qr-scan-line 2s linear infinite;"
        >
          <div style="height:2px;background:linear-gradient(90deg,transparent,#48c78e,transparent);" />
        </div>
      </div>
      <div
        v-if="isScanning"
        class="position-absolute bottom-0 start-0 w-100 p-2 small text-center"
        style="background:rgba(0,0,0,0.6);color:#fff;font-size:11px;"
      >
        {{ t('qrScanner.scanning') }}
      </div>
    </div>

    <!-- Error state -->
    <div
      v-if="error"
      class="d-flex flex-column align-items-center justify-content-center gap-2 rounded-3"
      style="width:280px;height:280px;background:var(--bg-card-alt);"
    >
      <CameraOff :size="40" color="var(--text-muted)" />
      <p class="small text-center mb-0 px-3" style="color:var(--text-secondary);">{{ error }}</p>
      <button class="btn btn-sm btn-outline-warning" @click="retryCurrentSource">{{ t('qrScanner.retry') }}</button>
    </div>

    <!-- Controls -->
    <div v-if="!error && (activeSource === 'direct' || activeSource === 'custom')" class="d-flex gap-2">
      <button
        v-if="!isOpen"
        class="btn btn-sm btn-warning fw-bold rounded-pill px-4"
        style="font-size:0.8rem;"
        @click="startCurrentSource"
      >
        <Camera :size="14" style="vertical-align:-2px;" /> {{ t('qrScanner.openCamera') }}
      </button>
      <button
        v-else
        class="btn btn-sm btn-outline-secondary rounded-pill px-4"
        style="font-size:0.8rem;"
        @click="stopCamera"
      >
        <CameraOff :size="14" style="vertical-align:-2px;" /> {{ t('qrScanner.closeCamera') }}
      </button>
    </div>

    <!-- USB auto — no manual controls, starts automatically -->
    <div v-if="activeSource === 'usb' && !error" class="d-flex align-items-center gap-2 small text-secondary">
      <div v-if="!mjpegReady" class="spinner-border spinner-border-sm text-warning" role="status"></div>
      <span v-if="!mjpegReady">{{ t('qrScanner.scanning') }}</span>
    </div>

    <!-- Result -->
    <div v-if="result" class="text-center small px-3">
      <div class="fw-bold text-success mb-1" style="font-size:0.85rem;">{{ t('qrScanner.found') }}</div>
      <div class="p-2 rounded-2" style="background:var(--bg-card-alt);font-size:0.78rem;word-break:break-all;color:var(--text-primary);">{{ result }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted, onMounted, computed } from 'vue';
import { Camera, CameraOff } from '@lucide/vue';
import { t } from '../../i18n/index.js';

/**
 * QrScanner — quet barcode/QR tu camera.
 *
 * Ho tro 3 nguon:
 *   - 'direct'  : camera tich hop (getUserMedia)
 *   - 'usb'     : USB Camera Proxy (MJPEG stream)
 *   - 'custom'  : URL MJPEG tuy chinh
 *
 * Props:
 *   autoClose  — tu dong tat camera sau khi quet thanh cong (default: true)
 *   defaultSource — nguon mac dinh: 'direct' | 'usb' | 'custom'
 *   usbProxyUrl  — URL proxy MJPEG mac dinh (default: 'http://localhost:4747/video')
 *
 * Emits:
 *   scanned(data) — khi quet duoc ma vach
 *   close       — khi camera duoc dong
 */
const props = defineProps({
  autoClose: { type: Boolean, default: true },
  defaultSource: { type: String, default: 'direct' },
  usbProxyUrl: { type: String, default: 'http://localhost:4747/video' },
});
const emit = defineEmits(['scanned', 'close']);

// ── State ──────────────────────────────────────────────────────────────────
const videoEl = ref(null);
const isOpen = ref(false);
const isScanning = ref(false);
const error = ref('');
const result = ref('');

const activeSource = ref(props.defaultSource);
const showCustomUrl = computed(() => activeSource.value === 'custom');
const customUrl = ref(props.usbProxyUrl);
const customError = ref('');
const mjpegReady = ref(false);
const mjpegSrc = ref('');

const cameraSources = [
  { id: 'direct', label: '📷 Camera tich hop' },
  { id: 'usb',    label: '🔌 USB Camera' },
  { id: 'custom', label: '🌐 URL tuy chinh' },
];

let stream = null;         // native getUserMedia stream
let mjpegTimer = null;    // interval for scanning MJPEG img
let animationId = null;    // requestAnimationFrame for direct camera
let controls = null;      // zxing controls

// ── Source selection ────────────────────────────────────────────────────────
const selectSource = (src) => {
  stopCamera();
  activeSource.value = src.id;
  if (src.id === 'custom') {
    showCustomUrl.value = true;
  }
};

const startCurrentSource = () => {
  if (activeSource.value === 'direct') startDirectCamera();
  else if (activeSource.value === 'custom') startCustomStream();
};

const retryCurrentSource = () => {
  error.value = '';
  startCurrentSource();
};

// ── Direct camera (getUserMedia + zxing) ──────────────────────────────────
const startDirectCamera = async () => {
  error.value = '';
  result.value = '';
  isScanning.value = false;
  await new Promise(r => setTimeout(r, 50));
  if (!videoEl.value) { error.value = 'Camera chua san sang.'; return; }

  try {
    const { BrowserMultiFormatReader } = await import('@zxing/browser');
    const reader = new BrowserMultiFormatReader();
    reader.hints = new Map();
    reader.hints.set(reader.HINT_DECODE_FORMATS, [
      reader.BarcodeFormat.QR_CODE,
      reader.BarcodeFormat.CODE_128,
      reader.BarcodeFormat.EAN_13,
      reader.BarcodeFormat.EAN_8,
      reader.BarcodeFormat.UPC_A,
      reader.BarcodeFormat.UPC_E,
      reader.BarcodeFormat.CODE_39,
      reader.BarcodeFormat.ITF,
      reader.BarcodeFormat.DATA_MATRIX,
      reader.BarcodeFormat.PDF_417,
      reader.BarcodeFormat.AZTEC,
    ]);

    controls = await reader.decodeFromVideoDevice(undefined, videoEl.value, (barcodeResult) => {
      if (barcodeResult) {
        const text = barcodeResult.getText();
        isScanning.value = true;
        result.value = text;
        emit('scanned', text);
        if (props.autoClose) setTimeout(() => { stopCamera(); emit('close'); }, 800);
      }
    });
    isOpen.value = true;
  } catch (e) {
    if (e.name === 'NotAllowedError') error.value = 'Không có quyền truy cập camera.';
    else if (e.name === 'NotFoundError') error.value = 'Không tìm thấy camera.';
    else error.value = 'Lỗi: ' + (e.message || e.name);
  }
};

// ── USB Camera / MJPEG stream ──────────────────────────────────────────────
const startUsbCamera = () => {
  mjpegSrc.value = props.usbProxyUrl;
  mjpegReady.value = true;
  isOpen.value = true;
  startMjpegScanner();
};

const startCustomStream = () => {
  customError.value = '';
  const url = customUrl.value.trim();
  if (!url) { customError.value = 'Vui long nhap URL.'; return; }
  mjpegSrc.value = url;
  mjpegReady.value = true;
  isOpen.value = true;
  startMjpegScanner();
};

// Scan MJPEG by re-decoding the img src
let lastScanTime = 0;
const startMjpegScanner = async () => {
  let lastDataUrl = '';
  let consecutiveFails = 0;

  const scan = async () => {
    if (!mjpegReady.value) return;

    const now = Date.now();
    // Scan at most every 200ms
    if (now - lastScanTime < 200) {
      mjpegTimer = requestAnimationFrame(scan);
      return;
    }
    lastScanTime = now;

    try {
      // Get the current img src
      const imgSrc = mjpegSrc.value;
      if (!imgSrc || imgSrc === lastDataUrl) {
        mjpegTimer = requestAnimationFrame(scan);
        return;
      }
      lastDataUrl = imgSrc;

      // Load image and scan with jsQR on canvas
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.src = imgSrc + '?t=' + now; // cache bust

      await new Promise((resolve, reject) => {
        img.onload = resolve;
        img.onerror = () => { consecutiveFails++; reject(new Error('img error')); };
        setTimeout(resolve, 1500); // timeout
      });

      const canvas = document.createElement('canvas');
      canvas.width = img.naturalWidth || 640;
      canvas.height = img.naturalHeight || 480;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(img, 0, 0);

      const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);

      // Try jsQR (QR only) for quick fallback
      const { default: jsQR } = await import('https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.min.js')
        .catch(() => ({ default: null }));

      if (jsQR) {
        const code = jsQR(imageData.data, canvas.width, canvas.height);
        if (code && code.data) {
          isScanning.value = true;
          result.value = code.data;
          emit('scanned', code.data);
          if (props.autoClose) {
            setTimeout(() => { stopCamera(); emit('close'); }, 800);
          }
          return;
        }
      }

      // Also try zxing for 1D barcodes
      try {
        const { BrowserMultiFormatReader } = await import('@zxing/browser');
        const reader = new BrowserMultiFormatReader();
        reader.hints = new Map();
        reader.hints.set(reader.HINT_DECODE_FORMATS, [
          reader.BarcodeFormat.CODE_128,
          reader.BarcodeFormat.EAN_13,
          reader.BarcodeFormat.EAN_8,
          reader.BarcodeFormat.UPC_A,
          reader.BarcodeFormat.CODE_39,
          reader.BarcodeFormat.ITF,
        ]);

        const tempCanvas = document.createElement('canvas');
        tempCanvas.width = canvas.width;
        tempCanvas.height = canvas.height;
        const tempCtx = tempCanvas.getContext('2d');
        tempCtx.drawImage(img, 0, 0);

        const result = await reader.decodeFromCanvasElement(tempCanvas);
        if (result) {
          isScanning.value = true;
          const text = result.getText();
          result.value = text;
          emit('scanned', text);
          if (props.autoClose) {
            setTimeout(() => { stopCamera(); emit('close'); }, 800);
          }
          return;
        }
      } catch { /* zxing failed, continue scanning */ }

      consecutiveFails = 0;
    } catch {
      consecutiveFails++;
    }

    if (consecutiveFails > 50) {
      error.value = 'Mat ket noi camera. Kiem tra dien thoai van con mo.';
      mjpegReady.value = false;
      return;
    }

    mjpegTimer = requestAnimationFrame(scan);
  };

  mjpegTimer = requestAnimationFrame(scan);
};

// ── Stop ──────────────────────────────────────────────────────────────────
const stopCamera = () => {
  if (controls) { try { controls.stop(); } catch { /* ignore */ } controls = null; }
  if (mjpegTimer) { cancelAnimationFrame(mjpegTimer); mjpegTimer = null; }
  if (animationId) { cancelAnimationFrame(animationId); animationId = null; }
  if (stream) { stream.getTracks().forEach(t => t.stop()); stream = null; }
  isOpen.value = false;
  isScanning.value = false;
  mjpegReady.value = false;
  mjpegSrc.value = '';
  result.value = '';
};

// ── Auto-start on mount for USB source ───────────────────────────────────
onMounted(() => {
  if (activeSource.value === 'usb') {
    setTimeout(() => startUsbCamera(), 100);
  } else if (activeSource.value === 'direct') {
    setTimeout(() => startDirectCamera(), 100);
  }
});

onUnmounted(() => stopCamera());

defineExpose({ startCamera: startCurrentSource, stopCamera, startUsbCamera, startDirectCamera });
</script>

<style scoped>
@keyframes qr-scan-line {
  0% { transform: translateY(0); }
  100% { transform: translateY(180px); }
}
</style>
