<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import { nowLocalIso } from "../../utils/datetime.js";
import * as DonHangService from "../../Service/DonHangService.js";
import * as ChiTietSanPhamService from "../../Service/ChiTietSanPhamService.js";
import { formatPrice, formatDate } from "../../utils/adminFormat.js";
import { ProductsStore, ensureProducts } from "../../stores/products.js";
import { CustomersStore, ensureCustomers } from "../../stores/customers.js";
import { PromotionsStore, ensurePromotions } from "../../stores/promotions.js";
import { refreshOrders } from "../../stores/orders.js";
import CustomerFormModal from "./CustomerFormModal.vue";

onMounted(() => { ensureProducts(); ensureCustomers(); ensurePromotions(); });

// ── POS / Ban hang ───────────────────────────────────────────────────────────
// Luong bat buoc: phai xac dinh khach hang (co san hoac tao moi) TRUOC khi duoc
// them san pham vao gio — tranh tao hoa don "vo danh" roi moi lo tim/tao khach sau.
// posStage: 'start' (chua bat dau) -> 'phone' (dang nhap SDT tim/tao khach) -> 'selling' (da co khach, duoc them SP)
const posStage = ref('start');
const posPhoneNotFound = ref(false); // da tim nhung khong thay khach ung voi SDT vua nhap
const posSearch = ref("");
const posCart = ref([]);
const posPhone = ref("");
const posFoundCust = ref(null);
const posError = ref("");
const posSuccess = ref(false);
const posPromoCode = ref("");
const posAppliedPromo = ref(null);
const posPromoMsg = ref("");

const posProducts = computed(() => {
  const q = posSearch.value.toLowerCase();
  return ProductsStore.items.filter(
    (p) =>
      p.trangThai === "active" &&
      (!q ||
        p.tenSanPham.toLowerCase().includes(q) ||
        (p.maSku ?? "").toLowerCase().includes(q)),
  );
});
const posCartTotal = computed(() =>
  posCart.value.reduce((s, i) => s + i.giaBan * i.soLuong, 0),
);
const posFee = computed(() => (posCartTotal.value >= 300000 ? 0 : 30000));
const posGiamGia = computed(() => {
  const p = posAppliedPromo.value;
  if (!p) return 0;
  if (p.loai === 'percent') {
    let d = posCartTotal.value * Number(p.giaTri) / 100;
    if (p.giaTriToiDa) d = Math.min(d, Number(p.giaTriToiDa));
    return d;
  }
  return Number(p.giaTri) || 0;
});
const posGrandTotal = computed(() => Math.max(0, posCartTotal.value + posFee.value - posGiamGia.value));

const posApplyPromo = () => {
  const code = posPromoCode.value.trim().toUpperCase();
  if (!code) { posAppliedPromo.value = null; posPromoMsg.value = ''; return; }
  const p = PromotionsStore.items.find(
    (x) => x.maKhuyenMai?.toUpperCase() === code && x.trangThai === 'active'
  );
  if (p) {
    posAppliedPromo.value = p;
    posPromoMsg.value = t('checkout.promoSuccess', { name: p.tenKhuyenMai });
  } else {
    posAppliedPromo.value = null;
    posPromoMsg.value = t('checkout.promoInvalid');
  }
};

// ── Hoa don cho (giu don POS) ─────────────────────────────────────────────────
// Luu tam gio hang dang ban do khach chua thanh toan xong / nhan vien can phuc vu
// khach khac — luu o localStorage (tinh nang tien loi cho nhan vien tai quay,
// khong can bang rieng trong DB vi don chua thuc su ton tai cho toi khi thanh toan).
const HELD_ORDERS_KEY = 'saophone_pos_held_orders';
const heldOrders = ref([]);
const loadHeldOrders = () => {
  try { heldOrders.value = JSON.parse(localStorage.getItem(HELD_ORDERS_KEY)) ?? []; }
  catch { heldOrders.value = []; }
};
const saveHeldOrders = () => {
  localStorage.setItem(HELD_ORDERS_KEY, JSON.stringify(heldOrders.value));
};
loadHeldOrders();

const showHeldOrders = ref(false);

const posHoldOrder = () => {
  if (!posCart.value.length) return;
  heldOrders.value.unshift({
    id: Date.now(),
    heldAt: new Date().toISOString(),
    cart: posCart.value,
    phone: posPhone.value,
    foundCust: posFoundCust.value,
    promoCode: posPromoCode.value,
    appliedPromo: posAppliedPromo.value,
  });
  saveHeldOrders();
  // Chi don sach form tai cho — KHONG goi posReset() vi no se tra serial ve trong_kho.
  // Cac serial trong gio nay van phai o trang thai "giu_hang" cho toi khi tiep tuc
  // ban (Tiep tuc) hoac huy han (Xoa o danh sach don dang giu).
  posCart.value = [];
  posPhone.value = "";
  posFoundCust.value = null;
  posError.value = "";
  posSuccess.value = false;
  posPromoCode.value = "";
  posAppliedPromo.value = null;
  posPromoMsg.value = "";
  posStage.value = 'start';
  posPhoneNotFound.value = false;
};

const posResumeHeld = (id) => {
  const held = heldOrders.value.find((h) => h.id === id);
  if (!held) return;
  posCart.value = held.cart;
  posPhone.value = held.phone;
  posFoundCust.value = held.foundCust;
  posPromoCode.value = held.promoCode;
  posAppliedPromo.value = held.appliedPromo;
  heldOrders.value = heldOrders.value.filter((h) => h.id !== id);
  saveHeldOrders();
  showHeldOrders.value = false;
  // Khach hang cua don nay da duoc xac dinh tu truoc (luc giu don) — vao thang man hinh ban
  posStage.value = 'selling';
};

const posDeleteHeld = async (id) => {
  const held = heldOrders.value.find((h) => h.id === id);
  heldOrders.value = heldOrders.value.filter((h) => h.id !== id);
  saveHeldOrders();
  // Huy han don giu -> tra lai tat ca serial trong don do ve trong_kho de ban duoc tiep
  if (held) await Promise.all(held.cart.map((item) => setSerialTrangThai(item, 'trong_kho')));
};

// Ban tai quay bat buoc chon serial cu the truoc khi cho vao gio — moi dong trong
// gio la 1 don vi vat ly rieng (chiTietId rieng), khong dung soLuong gop nhieu may
// lai vi moi may co IMEI khac nhau. Serial da o trong gio se khong hien lai de chon.
const showSerialPicker = ref(false);
const serialPickerProduct = ref(null);
const serialPickerList = ref([]);
const serialPickerLoading = ref(false);

const posOpenSerialPicker = async (p) => {
  // Chan them vao gio neu chua xac dinh khach hang — nhan vien duyet san pham
  // thoai mai, nhung phai qua cong "Tao hoa don" (o khu vuc gio hang) truoc.
  if (posStage.value !== 'selling') {
    if (posStage.value === 'start') posStartInvoice();
    posError.value = t('admin.pos.needCustomerFirst');
    return;
  }
  serialPickerProduct.value = p;
  serialPickerList.value = [];
  showSerialPicker.value = true;
  serialPickerLoading.value = true;
  const all = await ChiTietSanPhamService.getByBienThe(p.bienTheId).catch(() => []);
  // "trong_kho" la nguon duy nhat cho serial con ban duoc — 1 serial da bi danh dau
  // "giu_hang" (do dang nam trong gio cua BAT KY phien POS nao, ke ca giu don) se
  // tu dong bi loai o day, khong can biet gio do thuoc phien nao.
  serialPickerList.value = all.filter((s) => s.trangThai === 'trong_kho');
  serialPickerLoading.value = false;
};

// Doi trang thai 1 serial — dung khi chon vao gio (giu_hang) hoac tra lai kho (trong_kho).
// Phai truyen du bienTheId/soSerial/ngayNhapKho vi backend dung BeanUtils copy toan bo
// request len entity, thieu ngayNhapKho se lam mat ngay nhap kho goc.
const setSerialTrangThai = async (item, trangThai) => {
  await ChiTietSanPhamService.update(item.chiTietId, {
    bienTheId: item.bienTheId,
    soSerial: item.soSerial,
    trangThai,
    ngayNhapKho: item.ngayNhapKho,
  }).catch(() => {});
};

const posSelectSerial = async (serial) => {
  const p = serialPickerProduct.value;
  const item = {
    bienTheId: p.bienTheId,
    tenSanPham: p.tenSanPham,
    maSku: p.maSku,
    giaBan: p.giaBan,
    hinhAnhChinh: p.hinhAnhChinh,
    chiTietId: serial.chiTietId,
    soSerial: serial.soSerial,
    ngayNhapKho: serial.ngayNhapKho,
    soLuong: 1,
  };
  posCart.value.push(item);
  showSerialPicker.value = false;
  // Danh dau giu ngay khi chon — de phien POS khac (hoac don khac) khong the chon trung
  // serial nay, ke ca khi don nay chua duoc "giu don" chinh thuc.
  await setSerialTrangThai(item, 'giu_hang');
};

const posRemove = async (chiTietId) => {
  const item = posCart.value.find((i) => i.chiTietId === chiTietId);
  posCart.value = posCart.value.filter((i) => i.chiTietId !== chiTietId);
  if (item) await setSerialTrangThai(item, 'trong_kho');
};
const posReset = async () => {
  await Promise.all(posCart.value.map((item) => setSerialTrangThai(item, 'trong_kho')));
  posCart.value = [];
  posPhone.value = "";
  posFoundCust.value = null;
  posError.value = "";
  posSuccess.value = false;
  posPromoCode.value = "";
  posAppliedPromo.value = null;
  posPromoMsg.value = "";
  posStage.value = 'start';
  posPhoneNotFound.value = false;
};

const posStartInvoice = () => {
  posStage.value = 'phone';
  posPhoneNotFound.value = false;
  posError.value = '';
};

const posLookup = () => {
  const phone = posPhone.value.trim();
  if (!phone) return;
  const found = CustomersStore.items.find((c) => c.soDienThoai === phone) ?? null;
  posFoundCust.value = found;
  if (found) {
    posPhoneNotFound.value = false;
    posError.value = '';
    posStage.value = 'selling';
  } else {
    posPhoneNotFound.value = true;
  }
};

const posCancelCreateCustomer = () => {
  posPhoneNotFound.value = false;
  posPhone.value = '';
};

// ── Them khach hang nhanh tu luong POS ────────────────────────────────────────
// Modal rieng cua PosPanel (KHONG dung chung instance voi CustomersTable.vue — 2 noi
// mo modal doc lap nhau, khong cung luc hien thi). Mo san SDT vua nhap, luu xong gan
// thang khach hang vua tao lam khach hang cua hoa don POS dang tao qua su kien @saved.
const showQuickCustomerModal = ref(false);
const quickCustomerModalRef = ref(null);

const posConfirmCreateCustomer = () => {
  posPhoneNotFound.value = false;
  quickCustomerModalRef.value.openForCreate({ soDienThoai: posPhone.value.trim() });
};
const onQuickCustomerSaved = (customer) => {
  posFoundCust.value = customer;
  posStage.value = 'selling';
};

// Chuyen loi validate dang JSON {"field":"message"} tu backend thanh 1 dong text de doc
const parsePosApiError = async (res) => {
  const raw = await res.text();
  try {
    const obj = JSON.parse(raw);
    const messages = Object.values(obj).filter((v) => typeof v === 'string');
    if (messages.length) return messages.join(' · ');
  } catch { /* khong phai JSON, dung raw text */ }
  return raw;
};

const posPlaceOrder = async () => {
  if (!posCart.value.length) { posError.value = t('admin.pos.cartEmpty'); return; }
  // Khach hang bat buoc phai duoc xac dinh (co san hoac tao moi) TRUOC khi co san pham
  // trong gio (theo luong posStage) nen o day luon phai co san posFoundCust.
  if (!posFoundCust.value) { posError.value = t('admin.pos.phoneRequired'); return; }
  posError.value = "";
  posSuccess.value = false;
  try {
    const khachHangId = posFoundCust.value.khachHangId;
    const nguoiNhan = posFoundCust.value.hoTen;
    const orderRes = await DonHangService.create({
      khachHangId, nguoiNhan, sdtNguoiNhan: posFoundCust.value.soDienThoai,
      diaChiGiaoHangText: posFoundCust.value.diaChi ?? "Tai cua hang",
      khuyenMaiId: posAppliedPromo.value?.khuyenMaiId ?? null,
      tongTien: posCartTotal.value, giamGia: posGiamGia.value,
      phiVanChuyen: posFee.value, thanhTien: posGrandTotal.value,
      ngayDat: nowLocalIso(),
      trangThaiDonHang: "confirmed", trangThaiThanhToan: "paid", kenhBan: "in_store",
    });
    if (!orderRes.ok) throw new Error(t('admin.errors.createOrderError', { message: await parsePosApiError(orderRes) }));
    const created = await orderRes.json();
    const donHangId = created.id ?? created.donHangId;
    // Don vua tao da danh dau "paid"/"confirmed" ngay (xem DonHangService.create) — neu 1 dong
    // gan hang loi giua chung (vd het hang, serial vua bi don khac gianh mat), phai xoa luon don
    // vua tao thay vi de lai 1 don "da thanh toan" nhung thieu san pham. Mirror dung pattern da
    // dung o CheckoutModal.vue.
    try {
      for (const item of posCart.value) {
        const ctRes = await DonHangService.addChiTiet({
          donHangId, bienTheId: item.bienTheId, chiTietId: item.chiTietId, soLuong: item.soLuong, donGia: item.giaBan, giamGiaDong: 0,
        });
        if (!ctRes.ok) throw new Error(t('admin.errors.addProductError', { message: await ctRes.text() }));
      }
    } catch (e) {
      await DonHangService.remove(donHangId).catch(() => {});
      throw e;
    }
    posSuccess.value = true;
    posCart.value = []; posPhone.value = ""; posFoundCust.value = null;
    posPromoCode.value = ""; posAppliedPromo.value = null; posPromoMsg.value = "";
    posStage.value = 'start';
    await refreshOrders();
  } catch (e) {
    posError.value = e.message;
  }
};
</script>

<template>
  <div class="pos-grid-layout">
    <!-- LEFT: tim kiem + san pham — luon hien, nhan vien duyet duoc binh thuong;
         chi viec THEM VAO GIO moi bi chan neu chua xac dinh khach hang (xem posOpenSerialPicker) -->
    <div class="d-flex flex-column gap-3 overflow-hidden">
      <input v-model="posSearch" class="form-control form-control-sm"
             style="background:var(--bg-hover); border-color:var(--border-color-strong); color:var(--text-primary);"
             :placeholder="t('admin.pos.searchPlaceholder')" />
      <div v-if="ProductsStore.loading" class="text-secondary small">{{ t('admin.pos.loading') }}</div>
      <div v-else class="row g-2 overflow-y-auto">
        <div v-for="p in posProducts" :key="p.bienTheId" class="col-6 col-xl-4">
          <div class="card h-100 border-secondary" style="background:var(--bg-hover);">
            <div class="d-flex align-items-center justify-content-center" style="height:88px;background:var(--bg-card-inset);">
              <img v-if="p.hinhAnhChinh" :src="p.hinhAnhChinh" :alt="p.tenSanPham" style="width:100%;height:100%;object-fit:contain;padding:6px;" />
              <span v-else style="font-size:1.8rem;">💻</span>
            </div>
            <div class="card-body p-2 d-flex flex-column gap-1">
              <div class="fw-semibold small text-light">{{ p.tenSanPham }}</div>
              <div class="text-secondary" style="font-size:0.76rem;">{{ p.maSku }}</div>
              <div class="text-secondary" style="font-size:0.75rem;">{{ p.tenThuongHieu }} · {{ p.tenDanhMuc }}</div>
              <div class="fw-bold text-warning" style="font-size:0.95rem;">{{ formatPrice(p.giaBan) }}</div>
              <button class="btn btn-sm btn-warning text-dark fw-bold mt-auto" @click="posOpenSerialPicker(p)">{{ t('admin.pos.addToCart') }}</button>
            </div>
          </div>
        </div>
        <div v-if="posProducts.length===0" class="col-12 text-center text-secondary small py-4">{{ t('admin.pos.noProductsFound') }}</div>
      </div>
    </div>

    <!-- RIGHT: gio hang POS — cong xac dinh khach hang nam o day -->
    <div class="card border-secondary d-flex flex-column overflow-hidden" style="background:var(--bg-hover);">
      <div class="card-header border-secondary d-flex justify-content-between align-items-center fw-bold">
        <span>🛒 {{ t('admin.pos.cart') }} <span class="text-secondary fw-normal small">{{ posCart.length }} {{ t('admin.pos.cartCountSuffix') }}</span></span>
        <button class="btn btn-sm btn-outline-info position-relative" style="font-size:0.72rem;padding:2px 8px;" @click="showHeldOrders = true">
          {{ t('admin.pos.heldOrders') }}
          <span v-if="heldOrders.length" class="badge rounded-pill bg-warning text-dark" style="font-size:0.62rem;">{{ heldOrders.length }}</span>
        </button>
      </div>

      <!-- Cong xac dinh khach hang: thay the phan gio hang cho toi khi co khach -->
      <div v-if="posStage !== 'selling'" class="d-flex flex-column align-items-center justify-content-center gap-3 flex-grow-1 text-center p-3">
        <div v-if="posError" class="small p-2 rounded-2 w-100" style="background:rgba(220,53,69,0.1);color:#e05252;">{{ posError }}</div>
        <template v-if="posStage === 'start'">
          <div style="font-size:2.4rem;">🧾</div>
          <div class="text-secondary small">{{ t('admin.pos.startHint') }}</div>
          <button class="btn btn-warning text-dark fw-bold px-4" @click="posStartInvoice">{{ t('admin.pos.startInvoice') }}</button>
        </template>
        <template v-else-if="posStage === 'phone'">
          <div class="fw-bold text-light">{{ t('admin.pos.enterPhoneTitle') }}</div>
          <div class="d-flex gap-2 w-100">
            <input v-model="posPhone" class="form-control form-control-sm" style="background:var(--bg-hover);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.pos.phonePlaceholder')" @keyup.enter="posLookup" />
            <button class="btn btn-sm btn-warning text-dark fw-bold flex-shrink-0" @click="posLookup">{{ t('admin.pos.find') }}</button>
          </div>
          <div v-if="posPhoneNotFound" class="d-flex flex-column align-items-center gap-2 w-100">
            <div class="small" style="color:#e05252;">{{ t('admin.pos.customerNotFound') }}</div>
            <div class="small text-secondary">{{ t('admin.pos.askCreateCustomer') }}</div>
            <div class="d-flex gap-2">
              <button class="btn btn-sm btn-outline-secondary" @click="posCancelCreateCustomer">{{ t('admin.pos.no') }}</button>
              <button class="btn btn-sm btn-warning text-dark fw-bold" @click="posConfirmCreateCustomer">{{ t('admin.pos.yesCreateCustomer') }}</button>
            </div>
          </div>
        </template>
      </div>

      <!-- Danh sach san pham trong gio: chi hien khi da xac dinh khach hang -->
      <template v-if="posStage === 'selling'">
      <div class="flex-grow-1 overflow-y-auto p-2 d-flex flex-column gap-1">
        <div v-if="posCart.length===0" class="text-secondary small text-center py-4">{{ t('admin.pos.cartEmptyList') }}</div>
        <div v-for="item in posCart" :key="item.chiTietId"
             class="d-flex align-items-center gap-2 p-2 rounded-2" style="background:var(--bg-hover);">
          <div class="d-flex align-items-center justify-content-center flex-shrink-0 rounded-2" style="width:36px;height:36px;background:var(--bg-card-inset);">
            <img v-if="item.hinhAnhChinh" :src="item.hinhAnhChinh" :alt="item.tenSanPham" style="width:100%;height:100%;object-fit:contain;padding:2px;" />
            <span v-else style="font-size:1rem;">💻</span>
          </div>
          <div class="flex-grow-1" style="min-width:0;">
            <div class="fw-semibold small text-light text-truncate">{{ item.tenSanPham }}</div>
            <div class="text-secondary" style="font-size:0.73rem;">{{ item.maSku }}</div>
            <div class="text-info" style="font-size:0.7rem;">S/N: {{ item.soSerial }}</div>
          </div>
          <div class="d-flex align-items-center gap-1 flex-shrink-0">
            <button class="btn btn-sm btn-outline-danger" style="width:20px;height:20px;padding:0;font-size:0.72rem;"
                    @click="posRemove(item.chiTietId)">✕</button>
          </div>
          <div class="text-warning fw-bold flex-shrink-0 text-end" style="font-size:0.8rem;min-width:72px;">{{ formatPrice(item.giaBan*item.soLuong) }}</div>
        </div>
      </div>
      <!-- Ma khuyen mai -->
      <div class="p-2 border-top border-secondary d-flex flex-column gap-1">
        <div class="d-flex gap-2">
          <input v-model="posPromoCode" class="form-control form-control-sm" style="background:var(--bg-hover);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('checkout.promoPlaceholder')" @keyup.enter="posApplyPromo" />
          <button class="btn btn-sm btn-outline-warning flex-shrink-0" @click="posApplyPromo">{{ t('checkout.apply') }}</button>
        </div>
        <div v-if="posPromoMsg" class="small" :class="posAppliedPromo ? 'text-success' : 'text-danger'">{{ posPromoMsg }}</div>
      </div>
      <!-- Tong tien -->
      <div class="p-2 border-top border-secondary d-flex flex-column gap-1">
        <div class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.subtotalLabel') }}</span><span>{{ formatPrice(posCartTotal) }}</span></div>
        <div v-if="posGiamGia > 0" class="d-flex justify-content-between text-success small"><span>{{ t('checkout.discount') }}</span><span>-{{ formatPrice(posGiamGia) }}</span></div>
        <div class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.shippingFeeLabel') }}</span><span>{{ posFee===0?t('admin.pos.free'):formatPrice(posFee) }}</span></div>
        <div class="d-flex justify-content-between fw-bold"><span>{{ t('admin.pos.totalLabel') }}</span><span>{{ formatPrice(posGrandTotal) }}</span></div>
      </div>
      <!-- Khach hang -->
      <div class="p-2 border-top border-secondary d-flex flex-column gap-2">
        <div class="text-uppercase text-secondary fw-bold" style="font-size:0.78rem;letter-spacing:0.04em;">{{ t('admin.pos.customerInfo') }}</div>
        <div v-if="posFoundCust" class="d-flex justify-content-between align-items-center gap-2 small p-2 rounded-2" style="background:rgba(72,199,142,0.1);color:#48c78e;">
          <span>✓ {{ posFoundCust.hoTen }} · {{ posFoundCust.soDienThoai }}</span>
          <button class="btn btn-sm btn-link text-secondary p-0" style="font-size:0.7rem;text-decoration:underline;" @click="posReset">{{ t('admin.pos.changeCustomer') }}</button>
        </div>
        <div v-else class="small text-secondary">{{ t('admin.pos.noCustomerYet') }}</div>
        <div v-if="posError" class="small p-2 rounded-2" style="background:rgba(220,53,69,0.1);color:#e05252;">{{ posError }}</div>
        <div v-if="posSuccess" class="small p-2 rounded-2" style="background:rgba(72,199,142,0.1);color:#48c78e;">{{ t('admin.pos.orderCreated') }}</div>
        <div class="d-flex gap-2">
          <button class="btn btn-sm btn-outline-secondary" @click="posReset">{{ t('admin.pos.reset') }}</button>
          <button class="btn btn-sm btn-outline-info" :disabled="!posCart.length" @click="posHoldOrder">{{ t('admin.pos.holdOrder') }}</button>
          <button class="btn btn-sm btn-warning text-dark fw-bold" style="flex:2;" :disabled="posStage !== 'selling' || !posCart.length" @click="posPlaceOrder">{{ t('admin.pos.createOrder') }}</button>
        </div>
      </div>
      </template>
    </div>
  </div>

  <!-- ══ MODAL CHON SERIAL (POS) ══ -->
  <div v-if="showSerialPicker" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showSerialPicker=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:480px;max-width:95vw;max-height:75vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <div>
          <div>{{ t('admin.pos.chooseSerial') }}</div>
          <div class="text-secondary fw-normal" style="font-size:0.75rem;">{{ serialPickerProduct?.tenSanPham }} — {{ serialPickerProduct?.maSku }}</div>
        </div>
        <button class="btn-close btn-close-white btn-sm" @click="showSerialPicker=false"></button>
      </div>
      <div class="overflow-y-auto p-3 d-flex flex-column gap-2">
        <div v-if="serialPickerLoading" class="text-secondary small text-center py-4">{{ t('admin.pos.loading') }}</div>
        <div v-else-if="serialPickerList.length===0" class="text-secondary small text-center py-4">{{ t('admin.pos.noSerialAvailable') }}</div>
        <button v-else v-for="s in serialPickerList" :key="s.chiTietId"
                class="btn btn-outline-warning d-flex justify-content-between align-items-center"
                style="font-family:monospace;font-size:0.85rem;"
                @click="posSelectSerial(s)">
          <span>{{ s.soSerial }}</span>
          <span class="text-secondary" style="font-size:0.7rem;">{{ formatDate(s.ngayNhapKho) }}</span>
        </button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL DON DANG GIU (POS) ══ -->
  <div v-if="showHeldOrders" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showHeldOrders=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:520px;max-width:95vw;max-height:80vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.pos.heldOrders') }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showHeldOrders=false"></button>
      </div>
      <div class="overflow-y-auto p-3 d-flex flex-column gap-2">
        <div v-if="heldOrders.length===0" class="text-secondary small text-center py-4">{{ t('admin.pos.noHeldOrders') }}</div>
        <div v-for="h in heldOrders" :key="h.id" class="rounded-3 p-2 d-flex justify-content-between align-items-center" style="background:var(--bg-hover);border:1px solid var(--border-color-soft);">
          <div>
            <div class="small text-light">{{ h.foundCust?.hoTen ?? (h.newName || h.phone || t('admin.pos.walkInCustomer')) }}</div>
            <div class="text-secondary" style="font-size:0.72rem;">{{ h.cart.length }} {{ t('admin.pos.cartCountSuffix') }} · {{ formatDate(h.heldAt) }}</div>
          </div>
          <div class="d-flex gap-1">
            <button class="btn btn-sm btn-warning text-dark fw-bold" style="font-size:0.72rem;padding:2px 8px;" @click="posResumeHeld(h.id)">{{ t('admin.pos.resume') }}</button>
            <button class="btn btn-sm btn-outline-danger" style="font-size:0.72rem;padding:2px 8px;" @click="posDeleteHeld(h.id)">{{ t('admin.products.delete') }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- ══ MODAL THEM KHACH HANG NHANH (POS) — instance rieng, khong dung chung voi
       CustomersTable.vue vi 2 noi mo modal doc lap nhau ══ -->
  <CustomerFormModal ref="quickCustomerModalRef" v-model="showQuickCustomerModal" @saved="onQuickCustomerSaved" />
</template>

<style scoped>
/* Layout POS: 2 cot, chiem toan bo chieu cao */
.pos-grid-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 18px;
  height: calc(100vh - 120px);
}

/* Bootstrap .text-light hardcode mau trang co dinh — ghi de theo theme hien tai (dung
   tren nen the/card, khong phai nen mau thuong hieu co dinh, nen an toan khi ghi de
   theo bien theme). Scoped rieng cho component nay vi CSS scoped khong ke thua qua bien
   gioi component. */
.text-light {
  color: var(--text-primary) !important;
}
</style>
