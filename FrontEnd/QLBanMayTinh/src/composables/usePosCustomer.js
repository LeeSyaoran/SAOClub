import { ref, reactive, computed } from 'vue';
import * as KhachHangService from '../services/KhachHangService.js';
import { AuthStore } from '../stores/index.js';

export function usePosCustomer() {
  const customer = ref(null);
  const isLoading = ref(false);

  const form = reactive({
    hoTen: '',
    soDienThoai: '',
    email: '',
    diaChi: '',
  });

  const isLoggedInCustomer = computed(() =>
    AuthStore.user?.role === 'khach_hang' && !!AuthStore.user?.soDienThoai
  );

  const findByPhone = async (soDienThoai) => {
    if (!soDienThoai?.trim()) { customer.value = null; return; }
    isLoading.value = true;
    try {
      const found = await KhachHangService.findByPhone(soDienThoai).catch(() => null);
      customer.value = found || null;
      if (found) {
        form.hoTen = found.hoTen || '';
        form.email = found.email || '';
        form.diaChi = found.diaChi || '';
      }
    } finally {
      isLoading.value = false;
    }
  };

  const createGuest = async () => {
    isLoading.value = true;
    try {
      const body = {
        hoTen: form.hoTen,
        soDienThoai: form.soDienThoai,
        email: form.email || null,
        diaChi: form.diaChi || 'Chua cap nhat',
        loaiKhach: 'ca_nhan',
        diemTichLuy: 0,
        trangThai: 'active',
      };
      const res = await KhachHangService.createGuest(body);
      if (!res.ok) throw new Error(await res.text());
      const created = await res.json();
      customer.value = { khachHangId: created.khachHangId, ...form };
      return created;
    } finally {
      isLoading.value = false;
    }
  };

  const fillFromLoggedIn = () => {
    if (!isLoggedInCustomer.value) return;
    form.soDienThoai = AuthStore.user.soDienThoai;
    form.hoTen = AuthStore.user.hoTen || '';
    form.email = AuthStore.user.email || '';
    form.diaChi = AuthStore.user.diaChi || '';
    customer.value = { khachHangId: AuthStore.user.id, ...form };
  };

  const clearCustomer = () => {
    customer.value = null;
    Object.keys(form).forEach(k => { form[k] = ''; });
  };

  return {
    customer,
    form,
    isLoading,
    isLoggedInCustomer,
    findByPhone,
    createGuest,
    fillFromLoggedIn,
    clearCustomer,
  };
}
