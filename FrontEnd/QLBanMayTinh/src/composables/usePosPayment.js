import { ref, computed } from 'vue';

const BANK_CODE = 'VCB';
const ACCOUNT_NUMBER = '9876543210';
const ACCOUNT_NAME = 'SAO LAPTOP';
const TRANSFER_CONTENT = 'Thanh toan SAO LAPTOP';

export function usePosPayment() {
  const selectedPayment = ref('tien_mat');
  const qrImageFailed = ref(false);

  const qrImageUrl = computed(() => {
    const info = encodeURIComponent(TRANSFER_CONTENT);
    const name = encodeURIComponent(ACCOUNT_NAME);
    // amount se duoc truyen khi goi
    return `https://img.vietqr.io/image/${BANK_CODE}-${ACCOUNT_NUMBER}-compact2.png?addInfo=${info}&accountName=${name}`;
  });

  const bankInfo = {
    bank: 'Vietcombank (VCB)',
    accountNumber: ACCOUNT_NUMBER,
    accountName: ACCOUNT_NAME,
    transferContent: TRANSFER_CONTENT,
  };

  const selectPayment = (method) => {
    selectedPayment.value = method;
    if (method !== 'chuyen_khoan') qrImageFailed.value = false;
  };

  const resetQrFailed = () => {
    qrImageFailed.value = false;
  };

  return {
    selectedPayment,
    qrImageFailed,
    qrImageUrl,
    bankInfo,
    selectPayment,
    resetQrFailed,
  };
}
