/**
 * useBarcodeScanner — composable quet barcode/QR tu camera.
 *
 * Su dung @zxing/browser BrowserMultiFormatReader, ho tro:
 *   CODE_128, CODE_39, EAN_13, EAN_8, UPC_A, UPC_E,
 *   ITF, CODABAR, QR_CODE, DATA_MATRIX, PDF_417, AZTEC ...
 *
 * Usage:
 *   const scanner = useBarcodeScanner()
 *   scanner.onScan(data => console.log('Scanned:', data))
 *   await scanner.start()
 *   scanner.stop()
 */
import { ref, onUnmounted } from 'vue';

export function useBarcodeScanner() {
  const isOpen = ref(false);
  const isScanning = ref(false);
  const error = ref('');

  let reader = null;
  let controls = null;
  let scanCallback = null;

  // Khoi tao BrowserMultiFormatReader 1 lan
  const getReader = async () => {
    if (reader) return reader;
    const { BrowserMultiFormatReader } = await import('@zxing/browser');
    reader = new BrowserMultiFormatReader();
    return reader;
  };

  /**
   * Bat camera va bat dau quet.
   * @param {string|null} deviceId — null = camera mac dinh (thuong la camera sau)
   */
  const start = async (deviceId = null) => {
    error.value = '';
    isScanning.value = false;
    try {
      const r = await getReader();
      const hints = new Map();
      //Uu tien thu tu decode de quet nhanh hon
      const decodeFormats = [
        r.BarcodeFormat.QR_CODE,
        r.BarcodeFormat.CODE_128,
        r.BarcodeFormat.EAN_13,
        r.BarcodeFormat.EAN_8,
        r.BarcodeFormat.UPC_A,
        r.BarcodeFormat.UPC_E,
        r.BarcodeFormat.CODE_39,
        r.BarcodeFormat.ITF,
        r.BarcodeFormat.DATA_MATRIX,
        r.BarcodeFormat.PDF_417,
        r.BarcodeFormat.AZTEC,
      ];
      hints.set(r.HINT_DECODE_FORMATS, decodeFormats);
      r.hints = hints;

      if (deviceId) {
        // Quet tu camera cu the
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { deviceId: { exact: deviceId }, facingMode: undefined },
        });
        // eslint-disable-next-line no-unused-vars
        const [track, _] = stream.getVideoTracks();
        controls = await r.decodeFromVideoDevice(deviceId, null, (result, err) => {
          if (result) {
            isScanning.value = true;
            if (scanCallback) scanCallback(result.getText());
          }
        });
        isOpen.value = true;
      } else {
        // Quet tu camera mac dinh (camera sau tren dien thoai)
        controls = await r.decodeFromVideoDevice(undefined, null, (result, err) => {
          if (result) {
            isScanning.value = true;
            if (scanCallback) scanCallback(result.getText());
          }
        });
        isOpen.value = true;
      }
    } catch (e) {
      if (e.name === 'NotAllowedError' || e.name === 'PermissionDeniedError') {
        error.value = 'Không có quyền truy cập camera. Vui lòng cho phép trong cài đặt trình duyệt.';
      } else if (e.name === 'NotFoundError' || e.name === 'DevicesNotFoundError') {
        error.value = 'Không tìm thấy camera trên thiết bị.';
      } else {
        error.value = 'Không thể mở camera: ' + (e.message || e.name);
      }
      isOpen.value = false;
    }
  };

  /**
   * Tat camera.
   */
  const stop = () => {
    try {
      if (controls) {
        controls.stop();
        controls = null;
      }
      if (reader) {
        reader.reset();
      }
    } catch {
      // ignore
    }
    isOpen.value = false;
    isScanning.value = false;
  };

  /**
   * Dang ky callback khi quet duoc ma.
   * @param {function} cb — nhan text da quet
   */
  const onScan = (cb) => {
    scanCallback = cb;
  };

  onUnmounted(() => stop());

  return { isOpen, isScanning, error, start, stop, onScan };
}
