<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from "vue";
import { t } from "../../i18n/index.js";
import { nowLocalIso } from "../../utils/datetime.js";
import * as DonHangService from "../../services/DonHangService.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import { formatPrice, formatDate, boDauTiengViet } from "../../utils/adminFormat.js";
import { ProductsStore, ensureProducts, refreshProducts } from "../../stores/products.js";
import { refreshInventory } from "../../stores/inventory.js";
import { bumpSerialEvent } from "../../stores/serialEvents.js";
import { CustomersStore, ensureCustomers } from "../../stores/customers.js";
import { PromotionsStore, ensurePromotions } from "../../stores/promotions.js";
import { refreshOrders } from "../../stores/orders.js";
import CustomerFormModal from "./CustomerFormModal.vue";
import ProductDetailModal from "./ProductDetailModal.vue";
import { groupBySanPham, variantCountBySanPham, configKey, configLabel, colorDot } from "../../utils/productGrouping.js";
import { POS_PAYMENT_METHODS, paymentMethodLabel, paymentMethodIcon } from "../../utils/orderStatus.js";
import * as ThanhToanService from "../../services/ThanhToanService.js";
import { askConfirm } from "../../stores/confirm.js";
import { Laptop, ShoppingCart, Receipt, Info, RefreshCw, X, Check, ExternalLink, ImageOff, Printer, Package, Search } from '@lucide/vue';
import InvoiceModal from "./InvoiceModal.vue";

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
const posPlacing = ref(false);
const posSuccess = ref(false);
const posLastOrder = ref(null);   // don hang vua tao thanh cong — de mo modal in hoa don
const showInvoiceModal = ref(false);
const posPromoCode = ref("");
const posAppliedPromo = ref(null);
const posPromoMsg = ref("");
const posPaymentMethod = ref(null); // 1 trong POS_PAYMENT_METHODS — bat buoc chon truoc khi tao don
// POS luon la ban tai quay — phi van chuyen luon = 0, khong co UI chon hinh thuc nhan hang.
const posDeliveryMode = ref('pickup');
const posDeliveryAddress = ref('');
const posDistanceKm = ref('');
// Xac nhan thu cong "da quet QR" — chua co webhook ngan hang that nen nhan vien tu bam
// sau khi (gia lap) thay khach quet xong. Reset ve false moi khi doi phuong thuc thanh toan.
const posQrScanned = ref(false);
const posQrImageFailed = ref(false);

const posProducts = computed(() => {
  const q = boDauTiengViet(posSearch.value.toLowerCase());
  return ProductsStore.items.filter(
    (p) =>
      p.trangThai === "active" &&
      (!q ||
        boDauTiengViet(p.tenSanPham).includes(q) ||
        boDauTiengViet(p.maSku ?? "").includes(q)),
  );
});

// Gộp posProducts (đã lọc active + tìm kiếm) còn 1 card/sản phẩm — dùng cho lưới hiển
// thị. Modal chọn cấu hình/màu bên dưới KHÔNG dùng posProducts làm pool (sẽ bị bó hẹp
// theo từ khóa tìm kiếm hiện tại) — nó tự lấy từ ProductsStore.items, xem
// variantPickerVariants.
const posProductGroups = computed(() => groupBySanPham(posProducts.value));
const posVariantCountMap = computed(() => variantCountBySanPham(posProducts.value));

// Modal "Chi tiet san pham" xem-thuan (khong qua cong khach hang, khong dinh vao luong
// them-vao-gio) — mo tu nut "Chi tiet" tren dong gio hang, dung lai ProductDetailModal.vue.
// onlyBienTheIds gioi han modal chi hien DUNG cac bien the dang co trong nhom gio hang nay
// (khong phai toan bo ho bien the trong catalogue) — them 1 bien the moi cung sanPhamId
// vao gio thi lan mo sau se tu dong hien them, vi tinh lai luc mo (khong luu snapshot).
const showPosDetailModal = ref(false);
const posDetailSanPhamId = ref(null);
const posDetailSanPhamName = ref('');
const posDetailOnlyBienTheIds = ref(null);
const openPosDetail = (g) => {
  posDetailSanPhamId.value = g.sanPhamId;
  posDetailSanPhamName.value = g.tenSanPham;
  posDetailOnlyBienTheIds.value = [...new Set(g.items.map((i) => i.bienTheId))];
  showPosDetailModal.value = true;
};

const posCartTotal = computed(() =>
  posCart.value.reduce((s, i) => s + i.giaBan * i.soLuong, 0),
);
// Gom posCart (van la mang phang 1 phan tu/serial — nguon su that cho posCartTotal/
// posPlaceOrder, khong doi) theo sanPhamId de hien thi — mua nhieu bien the KHAC NHAU
// (khac mau/cau hinh) cung 1 san pham van gom chung 1 dong "x N", giong dung cach lai
// san pham da gom o luoi ben trai.
const posCartGroups = computed(() => {
  const map = new Map();
  posCart.value.forEach((item) => {
    if (!map.has(item.sanPhamId)) map.set(item.sanPhamId, { ...item, items: [] });
    map.get(item.sanPhamId).items.push(item);
  });
  return [...map.values()];
});
const posGroupTotal = (g) => g.items.reduce((s, i) => s + i.giaBan, 0);
const posFee = computed(() => {
  if (posDeliveryMode.value !== 'delivery') return 0;
  if (posCartTotal.value >= POS_FREE_SHIP_THRESHOLD) return 0;
  const km = parseFloat(posDistanceKm.value) || 0;
  const tier = POS_SHIP_TIERS.find(t => km <= t.maxKm);
  return tier ? tier.fee : POS_SHIP_TIERS[POS_SHIP_TIERS.length - 1].fee;
});
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

// Tai dung dung cach CheckoutModal.vue tao QR — VietQR API, cung tai khoan VCB demo.
const posQrImageUrl = computed(() => {
  const bank    = 'VCB';
  const account = '9876543210';
  const info    = encodeURIComponent('Thanh toan SAO LAPTOP');
  const name    = encodeURIComponent('SAO LAPTOP');
  return `https://img.vietqr.io/image/${bank}-${account}-compact2.png?amount=${posGrandTotal.value}&addInfo=${info}&accountName=${name}`;
});

// Ap tu dong khi go xong (debounce), khong can bam nut "Ap dung" — giong luong online.
let posPromoDebounce = null;
const onPosPromoInput = () => {
  showPosPromoSuggestions.value = true;
  clearTimeout(posPromoDebounce);
  if (!posPromoCode.value.trim()) { posAppliedPromo.value = null; posPromoMsg.value = ''; return; }
  posPromoDebounce = setTimeout(posApplyPromo, 500);
};

// Goi y ma khuyen mai con hieu luc (du dieu kien don toi thieu) de bam chon thay vi phai
// nho/go dung ma — giong list voucher ca nhan tu dong hien o checkout online.
const showPosPromoSuggestions = ref(false);
const posPromoSuggestions = computed(() => {
  const q = posPromoCode.value.trim().toUpperCase();
  return (PromotionsStore.items ?? [])
    .filter((p) => p.trangThai === 'active'
      && (!p.donHangToiThieu || posCartTotal.value >= Number(p.donHangToiThieu))
      && (!q || p.maKhuyenMai?.toUpperCase().includes(q)))
    .slice(0, 8);
});
const onPosPromoFocus = () => { if (posPromoSuggestions.value.length) showPosPromoSuggestions.value = true; };
const selectPosPromoSuggestion = (p) => {
  posPromoCode.value = p.maKhuyenMai;
  posAppliedPromo.value = p;
  posPromoMsg.value = t('checkout.promoSuccess', { name: p.tenKhuyenMai });
  showPosPromoSuggestions.value = false;
};

const posApplyPromo = () => {
  clearTimeout(posPromoDebounce);
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
const CART_KEY = 'saophone_pos_cart';
const heldOrders = ref([]);

onMounted(() => {
  try {
    const raw = localStorage.getItem(HELD_ORDERS_KEY);
    if (raw) heldOrders.value = JSON.parse(raw);
  } catch { heldOrders.value = []; }

  // Khoi phuc cart neu co (dang ban bi cat ket noi)
  try {
    const cartRaw = localStorage.getItem(CART_KEY);
    if (cartRaw) {
      const saved = JSON.parse(cartRaw);
      if (saved.cart?.length) {
        posCart.value = saved.cart;
        posPhone.value = saved.phone || "";
        posFoundCust.value = saved.foundCust || null;
        posPromoCode.value = saved.promoCode || "";
        posAppliedPromo.value = saved.appliedPromo || null;
        posPaymentMethod.value = saved.paymentMethod || null;
        posDeliveryMode.value = saved.deliveryMode || 'pickup';
        posDeliveryAddress.value = saved.deliveryAddress || "";
        posStage.value = saved.cart.length ? 'selling' : 'start';
      }
    }
  } catch {}
});

// Tu dong luu cart khi reload/tab bi dong
const saveCart = () => {
  try {
    localStorage.setItem(CART_KEY, JSON.stringify({
      cart: posCart.value,
      phone: posPhone.value,
      foundCust: posFoundCust.value,
      promoCode: posPromoCode.value,
      appliedPromo: posAppliedPromo.value,
      paymentMethod: posPaymentMethod.value,
      deliveryMode: posDeliveryMode.value,
      deliveryAddress: posDeliveryAddress.value,
    }));
  } catch {}
};
onBeforeUnmount(() => { saveCart(); });
window.addEventListener('beforeunload', saveCart);

// Lang nghe storage event de dong bo khi tab khac thay doi localStorage
window.addEventListener('storage', (e) => {
  if (e.key === HELD_ORDERS_KEY) {
    try {
      const raw = localStorage.getItem(HELD_ORDERS_KEY);
      heldOrders.value = raw ? JSON.parse(raw) : [];
    } catch { heldOrders.value = []; }
  }
});

// Dong bo ref -> localStorage sau moi thao tac giu/huy don
const saveHeldOrders = () => {
  try {
    localStorage.setItem(HELD_ORDERS_KEY, JSON.stringify(heldOrders.value));
  } catch { /* quota exceeded hoac serializable fail */ }
};

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
    paymentMethod: posPaymentMethod.value,
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
  posPaymentMethod.value = null;
  posDeliveryMode.value = 'pickup';
  posDeliveryAddress.value = '';
  posDistanceKm.value = '';
  posQrScanned.value = false;
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
  posPaymentMethod.value = held.paymentMethod ?? null;
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

// ── Chon cau hinh/mau truoc khi vao modal chon serial ─────────────────────────
// Luon mo modal nay khi bam "Them vao gio", ke ca san pham chi co 1 bien the — dong
// nhat trai nghiem cho moi truong hop (khac voi trang khach hang, von bo qua buoc nay
// neu chi co 1 lua chon — xem App.vue handleQuickAdd). Bam "Tiep tuc chon serial" se
// goi thang posOpenSerialPicker() hien co, khong doi gi ben trong ham do.
const showVariantPicker = ref(false);
const variantPickerBase = ref(null); // san pham dai dien vua bam (tu posProductGroups)
const variantPickerActiveConfigKey = ref('');
const variantPickerActiveColor = ref('');

// Toan bo bien the active cung sanPhamId — lay tu ProductsStore.items (khong qua bo loc
// tim kiem cua posProducts) de modal luon hien DAY DU cac lua chon cua san pham, ke ca khi
// nhan vien dang go tim theo 1 SKU/cau hinh cu the. Chi loai bien the da het hang, giong
// y het hanh vi da co truoc khi co thay doi nay.
const variantPickerVariants = computed(() =>
  ProductsStore.items.filter(
    (v) => v.trangThai === 'active' && v.sanPhamId === variantPickerBase.value?.sanPhamId,
  ),
);

// Cau hinh duy nhat (deduplicate theo cpu+ram+oCung) — copy logic tu ProductDetail.vue
const variantPickerConfigs = computed(() => {
  const seen = new Set();
  return variantPickerVariants.value.filter((v) => {
    const k = configKey(v);
    if (seen.has(k)) return false;
    seen.add(k); return true;
  });
});

// Mau sac cua cau hinh dang chon (deduplicate theo mauSac)
const variantPickerColorsForConfig = computed(() => {
  const seen = new Set();
  return variantPickerVariants.value
    .filter((v) => configKey(v) === variantPickerActiveConfigKey.value)
    .filter((v) => {
      const c = v.mauSac ?? '';
      if (seen.has(c)) return false;
      seen.add(c); return true;
    });
});

// Bien the hien tai = giao cua cau hinh + mau da chon
const variantPickerActiveVariant = computed(() =>
  variantPickerVariants.value.find((v) =>
    configKey(v) === variantPickerActiveConfigKey.value &&
    (v.mauSac ?? '') === variantPickerActiveColor.value,
  ) ?? variantPickerBase.value,
);

const variantPickerSelectConfig = (v) => {
  variantPickerActiveConfigKey.value = configKey(v);
  const available = variantPickerVariants.value.filter(
    (vv) => configKey(vv) === variantPickerActiveConfigKey.value,
  );
  if (!available.find((vv) => (vv.mauSac ?? '') === variantPickerActiveColor.value))
    variantPickerActiveColor.value = available[0]?.mauSac ?? '';
};
const variantPickerSelectColor = (v) => { variantPickerActiveColor.value = v.mauSac ?? ''; };

// Mo modal chon cau hinh/mau — thay the diem goi cu tu nut "Them vao gio" tren card san
// pham. Giu nguyen dung guard dang co o dau posOpenSerialPicker (chan neu chua xac dinh
// khach hang), copy nguyen khong doi.
const posOpenVariantPicker = (p) => {
  if (posStage.value !== 'selling') {
    if (posStage.value === 'start') posStartInvoice();
    posError.value = t('admin.pos.needCustomerFirst');
    return;
  }
  variantPickerBase.value = p;
  variantPickerActiveConfigKey.value = configKey(p);
  variantPickerActiveColor.value = p.mauSac ?? '';
  showVariantPicker.value = true;
};

// ── Danh sach hang hoa (catalog) mo toan man hinh ──────────────────────────────
// Luoi san pham truoc day chiem thang ben trai man hinh POS, nay chuyen vao day de
// nhuong cho gio hang (2/3) + thong tin don hang (1/3). Mo qua nut FAB bieu tuong
// hop hang o canh man hinh. Neu chua xac dinh khach hang, dong overlay lai truoc de
// nhan vien thay ngay man hinh nhap SDT (posOpenVariantPicker se tu dat loi/chuyen stage).
const showCatalog = ref(false);
const catalogAddToCart = (p) => {
  if (posStage.value !== 'selling') showCatalog.value = false;
  posOpenVariantPicker(p);
};

// Chot bien the da chon -> dong modal nay, mo modal chon serial hien co (khong sua gi
// ben trong posOpenSerialPicker).
const posConfirmVariant = () => {
  showVariantPicker.value = false;
  posOpenSerialPicker(variantPickerActiveVariant.value);
};

// Ban tai quay bat buoc chon serial cu the truoc khi cho vao gio — moi dong trong
// gio la 1 don vi vat ly rieng (chiTietId rieng), khong dung soLuong gop nhieu may
// lai vi moi may co IMEI khac nhau. Serial da o trong gio se khong hien lai de chon.
const showSerialPicker = ref(false);
const serialPickerProduct = ref(null);
const serialPickerList = ref([]);
const serialPickerLoading = ref(false);
// Khac null khi mo picker de DOI serial cua 1 dong da co san trong gio (nut 🔄) — thay vi
// them dong moi. Serial cu se duoc tra ve "trong_kho" sau khi chon xong (xem posSelectSerial).
const serialPickerSwapChiTietId = ref(null);
// Cac serial da duoc tick chon trong lan mo picker nay (chi dung khi them-moi, khong
// dung khi doi-serial vi luong doi van la 1-doi-1).
const serialPickerChosenIds = ref(new Set());

const posOpenSerialPicker = async (p, swapChiTietId = null) => {
  // Chan them vao gio neu chua xac dinh khach hang — nhan vien duyet san pham
  // thoai mai, nhung phai qua cong "Tao hoa don" (o khu vuc gio hang) truoc.
  if (posStage.value !== 'selling') {
    if (posStage.value === 'start') posStartInvoice();
    posError.value = t('admin.pos.needCustomerFirst');
    return;
  }
  serialPickerProduct.value = p;
  serialPickerSwapChiTietId.value = swapChiTietId;
  serialPickerChosenIds.value = new Set();
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

const posToggleSerial = (serial) => {
  const next = new Set(serialPickerChosenIds.value);
  if (next.has(serial.chiTietId)) next.delete(serial.chiTietId);
  else next.add(serial.chiTietId);
  serialPickerChosenIds.value = next;
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
  // Cap nhat cache cua cac bang Kho/Serial ngay sau khi backend ghi xong
  refreshProducts();
  refreshInventory();
  bumpSerialEvent();
};

const posSelectSerial = async (serial) => {
  const p = serialPickerProduct.value;
  const item = {
    sanPhamId: p.sanPhamId,
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
  const swapId = serialPickerSwapChiTietId.value;
  const oldItem = swapId != null ? posCart.value.find((i) => i.chiTietId === swapId) : null;
  posCart.value = swapId != null
    ? posCart.value.map((i) => (i.chiTietId === swapId ? item : i))
    : [...posCart.value, item];
  showSerialPicker.value = false;
  serialPickerSwapChiTietId.value = null;
  // Danh dau giu ngay khi chon — de phien POS khac (hoac don khac) khong the chon trung
  // serial nay, ke ca khi don nay chua duoc "giu don" chinh thuc.
  await setSerialTrangThai(item, 'giu_hang');
  if (oldItem) await setSerialTrangThai(oldItem, 'trong_kho');
};

// Them nhieu serial cung luc vao gio — chi dung khi KHONG phai doi-serial (swapChiTietId
// null). Moi serial da chon tao 1 dong rieng trong posCart, giong het cau truc item cua
// posSelectSerial(), roi danh dau giu_hang tung cai.
const posAddChosenSerials = async () => {
  const p = serialPickerProduct.value;
  const chosen = serialPickerList.value.filter((s) => serialPickerChosenIds.value.has(s.chiTietId));
  const items = chosen.map((serial) => ({
    sanPhamId: p.sanPhamId,
    bienTheId: p.bienTheId,
    tenSanPham: p.tenSanPham,
    maSku: p.maSku,
    giaBan: p.giaBan,
    hinhAnhChinh: p.hinhAnhChinh,
    chiTietId: serial.chiTietId,
    soSerial: serial.soSerial,
    ngayNhapKho: serial.ngayNhapKho,
    soLuong: 1,
  }));
  posCart.value = [...posCart.value, ...items];
  showSerialPicker.value = false;
  await Promise.all(items.map((item) => setSerialTrangThai(item, 'giu_hang')));
};

const posRemove = async (chiTietId) => {
  const item = posCart.value.find((i) => i.chiTietId === chiTietId);
  if (!item) return;
  if (!(await askConfirm(t('admin.pos.confirmRemove', { name: item.tenSanPham, serial: item.soSerial })))) return;
  posCart.value = posCart.value.filter((i) => i.chiTietId !== chiTietId);
  await setSerialTrangThai(item, 'trong_kho');
};

// Xoa toan bo 1 group (cung bienTheId) trong 1 lan xac nhan — dung khi mua nhieu may cung
// bien the va muon huy ca lo, thay vi bam xoa tung serial rieng.
const posRemoveGroup = async (g) => {
  if (!(await askConfirm(t('admin.pos.confirmRemoveGroup', { name: g.tenSanPham, count: g.items.length })))) return;
  const ids = new Set(g.items.map((i) => i.chiTietId));
  posCart.value = posCart.value.filter((i) => !ids.has(i.chiTietId));
  await Promise.all(g.items.map((i) => setSerialTrangThai(i, 'trong_kho')));
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
  posPaymentMethod.value = null;
  posDeliveryMode.value = 'pickup';
  posDeliveryAddress.value = '';
  posDistanceKm.value = '';
  posQrScanned.value = false;
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

// ── Goi y khach hang theo SDT (tu 2 so dau) khi nhap tren POS ──────────────────
const showPosSuggestions = ref(false);
const posPhoneSuggestions = computed(() => {
  const q = posPhone.value.trim();
  if (q.length < 2) return [];
  return (CustomersStore.items ?? [])
    .filter((c) => c.soDienThoai?.startsWith(q))
    .slice(0, 8);
});
const onPosPhoneFocus = () => { if (posPhoneSuggestions.value.length) showPosSuggestions.value = true; };
const selectPosSuggestion = (c) => {
  posPhone.value = c.soDienThoai;
  posFoundCust.value = c;
  posPhoneNotFound.value = false;
  posError.value = '';
  showPosSuggestions.value = false;
  posStage.value = 'selling';
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
  if (!posPaymentMethod.value) { posError.value = t('admin.pos.paymentRequired'); return; }
  if (posPlacing.value) return;
  if (!(await askConfirm(t('admin.pos.confirmCheckout', { total: formatPrice(posGrandTotal.value) })))) return;
  posPlacing.value = true;
  posError.value = "";
  posSuccess.value = false;
  try {
    const khachHangId = posFoundCust.value.khachHangId;
    const nguoiNhan = posFoundCust.value.hoTen;
    const ngayDat = nowLocalIso();
    const diaChiGiao = posDeliveryMode.value === 'delivery'
      ? posDeliveryAddress.value.trim()
      : (posFoundCust.value.diaChi ?? "Tai cua hang");
    const orderRes = await DonHangService.create({
      khachHangId, nguoiNhan, sdtNguoiNhan: posFoundCust.value.soDienThoai,
      diaChiGiaoHangText: diaChiGiao,
      khuyenMaiId: posAppliedPromo.value?.khuyenMaiId ?? null,
      tongTien: posCartTotal.value, giamGia: posGiamGia.value,
      phiVanChuyen: posFee.value, thanhTien: posGrandTotal.value,
      ngayDat,
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
        // Serial dang o "giu_hang" (tu luc chon vao gio, xem posSelectSerial) — backend chi
        // nhan gan serial dang "trong_kho" (chong ban trung bang pessimistic lock), nen phai
        // tra ve "trong_kho" ngay truoc khi gui de backend tu khoa + gan lai. Neu nhan vien
        // khac vua nhanh tay gianh mat serial trong khe ho nay, backend se bao loi dung nhu
        // thiet ke — do la hanh vi dung, khong phai bug.
        await setSerialTrangThai(item, 'trong_kho');
        const ctRes = await DonHangService.addChiTiet({
          donHangId, bienTheId: item.bienTheId, chiTietId: item.chiTietId, soLuong: item.soLuong, donGia: item.giaBan, giamGiaDong: 0,
        });
        if (!ctRes.ok) throw new Error(t('admin.errors.addProductError', { message: await ctRes.text() }));
      }
      // Ghi nhan phuong thuc thanh toan — cung nam trong try nay nen loi cung duoc rollback
      // (xoa don) giong het loi 1 dong san pham, khong de lai don "da thanh toan" nhung
      // thieu record thanh toan.
      // DB bat buoc so_tien > 0 — don giam gia 100% (tong = 0) khong co gi de ghi nhan thanh toan, nen bo qua.
      if (posGrandTotal.value > 0) {
        const ttRes = await ThanhToanService.create({
          donHangId,
          ngayThanhToan: nowLocalIso(),
          phuongThucThanhToan: posPaymentMethod.value,
          soTien: posGrandTotal.value,
          maGiaoDich: null,
          trangThai: 'success',
          ghiChu: null,
        });
        if (!ttRes.ok) throw new Error(t('admin.errors.createPaymentError', { message: await parsePosApiError(ttRes) }));
      }
      // Don tai quay: khach nhan hang ngay luc thanh toan, khong qua cac buoc giao hang online
      // (processing/shipping/out_for_delivery) — chuyen thang sang "delivered" ngay sau khi
      // tao don + ghi nhan thanh toan xong. Backend chi cho phep nhay thang confirmed->delivered
      // voi kenhBan="in_store" (xem DonHangService.kiemTraChuyenTrangThai), don online van phai
      // di tuan tu nhu cu.
      if (posDeliveryMode.value === 'pickup') {
        const finalizeRes = await DonHangService.update(donHangId, {
          khachHangId, nguoiNhan, sdtNguoiNhan: posFoundCust.value.soDienThoai,
          diaChiGiaoHangText: diaChiGiao,
          khuyenMaiId: posAppliedPromo.value?.khuyenMaiId ?? null,
          tongTien: posCartTotal.value, giamGia: posGiamGia.value,
          phiVanChuyen: posFee.value, thanhTien: posGrandTotal.value,
          ngayDat,
          ngayGiaoThucTe: nowLocalIso(),
          trangThaiDonHang: "delivered", trangThaiThanhToan: "paid", kenhBan: "in_store",
        });
        if (!finalizeRes.ok) throw new Error(t('admin.errors.createOrderError', { message: await parsePosApiError(finalizeRes) }));
      }
    } catch (e) {
      await DonHangService.remove(donHangId).catch(() => {});
      // Xoa xong nhung khong refresh thi danh sach don hang tren UI (da tang truoc do qua
      // SSE "don moi") van con dong "ma" cua don vua bi xoa — refresh lai cho khop backend.
      await refreshOrders();
      throw e;
    }
    posSuccess.value = true;
    // Luu don va khach hang de mo modal in hoa don
    posLastOrder.value = { ...created, khachHangId, maDonHang: created.maDonHang, thanhTien: posGrandTotal.value, tongTien: posCartTotal.value, giamGia: posGiamGia.value, phiVanChuyen: posFee.value, ngayDat, kenhBan: 'in_store' };
    const _custSnapshot = posFoundCust.value ? { ...posFoundCust.value } : null;
    posCart.value = []; posPhone.value = ""; posFoundCust.value = null;
    posPromoCode.value = ""; posAppliedPromo.value = null; posPromoMsg.value = "";
    posPaymentMethod.value = null;
    posDeliveryMode.value = 'pickup';
    posDeliveryAddress.value = '';
    posDistanceKm.value = '';
    posQrScanned.value = false;
    posStage.value = 'start';
    await refreshOrders();
  } catch (e) {
    posError.value = e.message;
  } finally {
    posPlacing.value = false;
  }
};
</script>

<template>
  <div class="pos-page">
    <!-- FAB canh man hinh: mo danh sach hang hoa toan man hinh -->
    <button
      class="pos-catalog-fab" :aria-label="t('admin.pos.openCatalog')" :title="t('admin.pos.openCatalog')"
      @click="showCatalog = true"
    >
      <Package :size="22" />
    </button>

    <div class="pos-grid-layout">
      <!-- TRAI (2/3): Gio hang -->
      <div class="alt-card pos-cart-card">
        <div class="alt-toolbar">
          <div class="alt-toolbar__left">
            <span class="d-inline-flex align-items-center gap-2 fw-bold">
              <ShoppingCart :size="17" color="var(--accent-fg)" />
              {{ t('admin.pos.cart') }}
              <span class="alt-toolbar__count">{{ posCart.length }} {{ t('admin.pos.cartCountSuffix') }}</span>
            </span>
          </div>
          <div class="alt-toolbar__actions">
            <button class="alt-btn alt-btn--ghost position-relative" @click="showHeldOrders = true">
              {{ t('admin.pos.heldOrders') }}
              <span v-if="heldOrders.length" class="badge rounded-pill bg-warning text-dark ms-1" style="font-size:0.62rem;">{{ heldOrders.length }}</span>
            </button>
          </div>
        </div>

        <div v-if="posStage !== 'selling'" class="d-flex flex-column align-items-center justify-content-center gap-3 flex-grow-1 text-center p-4">
          <div v-if="posError" class="small p-2 rounded-2 w-100" style="max-width:360px;background:rgba(220,53,69,0.1);color:#e05252;">{{ posError }}</div>
          <template v-if="posStage === 'start'">
            <div><Receipt :size="42" color="var(--text-muted)" /></div>
            <div class="text-secondary small" style="max-width:360px;">{{ t('admin.pos.startHint') }}</div>
            <button class="alt-btn alt-btn--primary px-4" @click="posStartInvoice">{{ t('admin.pos.startInvoice') }}</button>
          </template>
          <template v-else-if="posStage === 'phone'">
            <div class="fw-bold text-light">{{ t('admin.pos.enterPhoneTitle') }}</div>
            <div class="d-flex gap-2 w-100 position-relative" style="max-width:340px;">
              <input v-model="posPhone" class="form-control form-control-sm w-100" style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.pos.phonePlaceholder')" @input="showPosSuggestions = true" @focus="onPosPhoneFocus" @blur="showPosSuggestions = false" @keyup.enter="posLookup" />
              <div v-if="showPosSuggestions && posPhoneSuggestions.length" class="position-absolute w-100 rounded-3 shadow-lg text-start" style="top:100%; left:0; z-index:20; background:var(--bg-card); border:1px solid var(--border-color-strong); max-height:220px; overflow-y:auto;">
                <div
                  v-for="c in posPhoneSuggestions" :key="c.khachHangId" class="px-3 py-2 small d-flex justify-content-between gap-2"
                  style="cursor:pointer;" @mousedown.prevent="selectPosSuggestion(c)"
                  @mouseenter="$event.currentTarget.style.background='var(--bg-hover)'"
                  @mouseleave="$event.currentTarget.style.background=''"
                >
                  <span class="text-light">{{ c.hoTen }}</span>
                  <span class="text-secondary">{{ c.soDienThoai }}</span>
                </div>
              </div>
            </div>
            <div v-if="posPhoneNotFound" class="d-flex flex-column align-items-center gap-2 w-100">
              <div class="small" style="color:#e05252;">{{ t('admin.pos.customerNotFound') }}</div>
              <div class="small text-secondary">{{ t('admin.pos.askCreateCustomer') }}</div>
              <div class="d-flex gap-2">
                <button class="alt-btn alt-btn--ghost" @click="posCancelCreateCustomer">{{ t('admin.pos.no') }}</button>
                <button class="alt-btn alt-btn--primary" @click="posConfirmCreateCustomer">{{ t('admin.pos.yesCreateCustomer') }}</button>
              </div>
            </div>
          </template>
        </div>

        <!-- Danh sach san pham trong gio: chi hien khi da xac dinh khach hang -->
        <div v-else class="flex-grow-1 overflow-y-auto p-3 d-flex flex-column gap-2">
          <div v-if="posCart.length===0" class="text-secondary small text-center py-5">
            <Package :size="34" color="var(--text-muted)" style="margin-bottom:8px;" /><br/>
            {{ t('admin.pos.cartEmptyList') }}
          </div>
          <div
            v-for="g in posCartGroups" :key="g.sanPhamId"
            class="d-flex flex-column gap-1 p-2 rounded-3" style="background:var(--bg-card-alt);border:1px solid var(--border-color-soft);"
          >
            <div class="d-flex align-items-center gap-2">
              <div class="d-flex align-items-center justify-content-center flex-shrink-0 rounded-2" style="width:40px;height:40px;background:var(--bg-card-inset);">
                <img v-if="g.hinhAnhChinh" :src="g.hinhAnhChinh" :alt="g.tenSanPham" style="width:100%;height:100%;object-fit:contain;padding:2px;" />
                <span v-else><Laptop :size="18" color="var(--text-muted)" /></span>
              </div>
              <div class="flex-grow-1" style="min-width:0;">
                <div class="fw-semibold small text-light text-truncate">{{ g.tenSanPham }}<span v-if="g.items.length>1" class="text-secondary fw-normal"> × {{ g.items.length }}</span></div>
                <template v-if="g.items.length===1">
                  <div class="text-secondary" style="font-size:0.73rem;">{{ g.items[0].maSku }}</div>
                  <div style="font-size:0.7rem;color:var(--accent-fg);">S/N: {{ g.items[0].soSerial }}</div>
                </template>
              </div>
              <button
                class="btn btn-sm btn-outline-secondary flex-shrink-0" style="width:22px;height:22px;padding:0;font-size:0.62rem;"
                :aria-label="t('admin.products.detail')" @click="openPosDetail(g)"
              >
                <Info :size="14" />
              </button>
              <div v-if="g.items.length===1" class="d-flex align-items-center gap-1 flex-shrink-0">
                <button
                  class="btn btn-sm btn-outline-secondary" style="width:22px;height:22px;padding:0;font-size:0.68rem;"
                  :aria-label="t('admin.pos.swapSerial')" @click="posOpenSerialPicker(g.items[0], g.items[0].chiTietId)"
                >
                  <RefreshCw :size="14" />
                </button>
                <button
                  class="btn btn-sm btn-outline-danger" style="width:22px;height:22px;padding:0;font-size:0.72rem;"
                  :aria-label="t('common.remove')" @click="posRemove(g.items[0].chiTietId)"
                >
                  <X :size="14" />
                </button>
              </div>
              <button v-else class="btn btn-sm btn-outline-danger flex-shrink-0" style="font-size:0.66rem;padding:2px 6px;" @click="posRemoveGroup(g)">{{ t('admin.pos.removeAll') }} ({{ g.items.length }})</button>
              <div class="fw-bold flex-shrink-0 text-end" style="font-size:0.85rem;min-width:80px;color:var(--accent-fg);">{{ formatPrice(posGroupTotal(g)) }}</div>
            </div>
            <div v-if="g.items.length>1" class="d-flex flex-column gap-1 ps-5">
              <div v-for="item in g.items" :key="item.chiTietId" class="d-flex align-items-center gap-2">
                <div class="flex-grow-1" style="min-width:0;">
                  <div class="text-secondary text-truncate" style="font-size:0.68rem;">{{ item.maSku }}</div>
                  <div style="font-size:0.7rem;color:var(--accent-fg);">S/N: {{ item.soSerial }}</div>
                </div>
                <button
                  class="btn btn-sm btn-outline-secondary" style="width:22px;height:22px;padding:0;font-size:0.68rem;"
                  :aria-label="t('admin.pos.swapSerial')" @click="posOpenSerialPicker(item, item.chiTietId)"
                >
                  <RefreshCw :size="14" />
                </button>
                <button
                  class="btn btn-sm btn-outline-danger" style="width:22px;height:22px;padding:0;font-size:0.72rem;"
                  :aria-label="t('common.remove')" @click="posRemove(item.chiTietId)"
                >
                  <X :size="14" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- PHAI (1/3): nut + thong tin don hang -->
      <div class="alt-card pos-side-card">
        <div class="alt-toolbar">
          <span class="fw-bold">{{ t('admin.pos.orderInfo') }}</span>
        </div>

        <div v-if="posStage !== 'selling'" class="d-flex flex-column align-items-center justify-content-center flex-grow-1 text-center p-4 text-secondary small">
          {{ t('admin.pos.noOrderInfoYet') }}
        </div>

        <div v-else class="pos-side-body">
          <!-- Ma khuyen mai -->
          <div class="pos-side-section">
            <div class="d-flex gap-2 position-relative">
              <input v-model="posPromoCode" class="form-control form-control-sm" style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('checkout.promoPlaceholder')" @input="onPosPromoInput" @focus="onPosPromoFocus" @blur="showPosPromoSuggestions = false" @keyup.enter="posApplyPromo" />
              <button class="alt-btn alt-btn--ghost flex-shrink-0" style="padding:6px 12px;" @click="posApplyPromo">{{ t('checkout.apply') }}</button>
              <button
                class="btn btn-sm btn-outline-secondary flex-shrink-0" style="padding:2px 8px;"
                :aria-label="t('admin.pos.viewPromotionsTab')" :title="t('admin.pos.viewPromotionsTab')"
                @click="() => window.open('/#/admin', '_blank')"
              ><ExternalLink :size="14" /></button>
              <div v-if="showPosPromoSuggestions && posPromoSuggestions.length" class="position-absolute w-100 rounded-3 shadow-lg" style="top:100%; left:0; z-index:20; background:var(--bg-card); border:1px solid var(--border-color-strong); max-height:220px; overflow-y:auto;">
                <div
                  v-for="p in posPromoSuggestions" :key="p.khuyenMaiId" class="px-3 py-2 small d-flex justify-content-between gap-2"
                  style="cursor:pointer;" @mousedown.prevent="selectPosPromoSuggestion(p)"
                  @mouseenter="$event.currentTarget.style.background='var(--bg-hover)'"
                  @mouseleave="$event.currentTarget.style.background=''"
                >
                  <span class="text-light">{{ p.maKhuyenMai }} <span class="text-secondary">· {{ p.tenKhuyenMai }}</span></span>
                  <span class="fw-bold flex-shrink-0" style="color:var(--accent-fg);">{{ p.loai === 'percent' ? `${p.giaTri}%` : formatPrice(p.giaTri) }}</span>
                </div>
              </div>
            </div>
            <div v-if="posPromoMsg" class="small mt-1" :class="posAppliedPromo ? 'text-success' : 'text-danger'">{{ posPromoMsg }}</div>
          </div>

          <!-- Tong tien -->
          <div class="pos-side-section d-flex flex-column gap-1">
            <div class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.subtotalLabel') }}</span><span>{{ formatPrice(posCartTotal) }}</span></div>
            <div v-if="posGiamGia > 0" class="d-flex justify-content-between text-success small"><span>{{ t('checkout.discount') }}</span><span>-{{ formatPrice(posGiamGia) }}</span></div>
            <div class="d-flex justify-content-between fw-bold pt-1" style="font-size:1.02rem;border-top:1px dashed var(--border-color-soft);"><span>{{ t('admin.pos.totalLabel') }}</span><span style="color:var(--accent-fg);">{{ formatPrice(posGrandTotal) }}</span></div>
          </div>

          <!-- Phuong thuc thanh toan -->
          <div class="pos-side-section d-flex flex-column gap-2">
            <div class="text-uppercase text-secondary fw-bold" style="font-size:0.72rem;letter-spacing:0.04em;">{{ t('admin.pos.paymentMethodLabel') }}</div>
            <div class="d-flex gap-1">
              <button
                v-for="m in POS_PAYMENT_METHODS" :key="m"
                class="btn btn-sm flex-fill d-flex flex-column align-items-center py-2"
                style="border-radius:8px;font-size:0.65rem;"
                :style="posPaymentMethod === m
                  ? 'background:rgba(225,29,72,0.1);border:1.5px solid var(--accent);color:var(--accent-fg);'
                  : 'background:var(--bg-input);border:1.5px solid var(--border-color-strong);color:var(--text-secondary);'"
                @click="posPaymentMethod = m; posQrScanned = false"
              >
                <component :is="paymentMethodIcon(m)" :size="18" />
                <span>{{ paymentMethodLabel(m) }}</span>
              </button>
            </div>
            <div v-if="posPaymentMethod === 'chuyen_khoan'" class="d-flex flex-column align-items-center gap-2 p-3 rounded-3" style="background:var(--bg-card-inset);">
              <img
                v-if="!posQrImageFailed" :src="posQrImageUrl" alt="VietQR" style="width:150px;height:150px;border-radius:10px;background:#fff;padding:4px;"
                @error="posQrImageFailed = true"
              />
              <div
                v-else class="d-flex flex-column align-items-center justify-content-center text-center small"
                style="width:150px;height:150px;border-radius:10px;background:var(--bg-card-alt);color:var(--text-secondary);gap:6px;"
              >
                <ImageOff :size="24" />{{ t('checkout.qrImageFailed') }}
              </div>
              <button
                class="btn btn-sm w-100" :class="posQrScanned ? 'btn-success' : 'btn-outline-warning'"
                @click="posQrScanned = !posQrScanned"
              >
                <Check v-if="posQrScanned" :size="14" style="vertical-align:-2px;" />
                {{ posQrScanned ? t('admin.pos.qrScannedConfirmed') : t('admin.pos.simulateQrScan') }}
              </button>
            </div>
          </div>

          <!-- Khach hang -->
          <div class="pos-side-section d-flex flex-column gap-2">
            <div class="text-uppercase text-secondary fw-bold" style="font-size:0.72rem;letter-spacing:0.04em;">{{ t('admin.pos.customerInfo') }}</div>
            <div v-if="posFoundCust" class="d-flex justify-content-between align-items-center gap-2 small p-2 rounded-2" style="background:rgba(72,199,142,0.1);color:#2f9e6e;">
              <span class="d-inline-flex align-items-center gap-1"><Check :size="13" /> {{ posFoundCust.hoTen }} · {{ posFoundCust.soDienThoai }}</span>
              <button class="btn btn-sm btn-link text-secondary p-0" style="font-size:0.7rem;text-decoration:underline;" @click="posReset">{{ t('admin.pos.changeCustomer') }}</button>
            </div>
            <div v-else class="small text-secondary">{{ t('admin.pos.noCustomerYet') }}</div>
            <div v-if="posError" class="small p-2 rounded-2" style="background:rgba(220,53,69,0.1);color:#e05252;">{{ posError }}</div>
            <div v-if="posSuccess" class="d-flex flex-column align-items-center gap-2 w-100 p-2 rounded-2" style="background:rgba(72,199,142,0.1);color:#2f9e6e;">
              <div>{{ t('admin.pos.orderCreated') }}</div>
              <button class="alt-btn alt-btn--primary" @click="showInvoiceModal = true">
                <Printer :size="13" /> In hóa đơn
              </button>
            </div>
          </div>

          <!-- Nut hanh dong -->
          <div class="pos-side-section pos-side-actions">
            <div class="d-flex gap-2">
              <button class="alt-btn alt-btn--ghost flex-fill" @click="posReset">{{ t('admin.pos.reset') }}</button>
              <button class="alt-btn alt-btn--ghost flex-fill" :disabled="!posCart.length" @click="posHoldOrder">{{ t('admin.pos.holdOrder') }}</button>
            </div>
            <button class="alt-btn alt-btn--primary w-100 justify-content-center" style="padding:10px;font-size:14px;" :disabled="posStage !== 'selling' || !posCart.length || !posPaymentMethod || posPlacing || (posPaymentMethod === 'chuyen_khoan' && !posQrScanned)" @click="posPlaceOrder">{{ t('admin.pos.createOrder') }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ══ TOAN MAN HINH: Danh sach hang hoa ══ -->
    <div v-if="showCatalog" class="pos-catalog-overlay">
      <div class="pos-catalog-header">
        <div class="alt-search" style="width:320px;">
          <span class="alt-search__icon"><Search :size="15" /></span>
          <input v-model="posSearch" :placeholder="t('admin.pos.searchPlaceholder')" />
        </div>
        <div class="d-flex align-items-center gap-2">
          <span class="text-secondary small">{{ t('admin.pos.cart') }}: {{ posCart.length }}</span>
          <button class="alt-btn alt-btn--primary" @click="showCatalog = false">{{ t('admin.pos.doneAdding') }}</button>
          <button class="btn-close" :aria-label="t('common.close')" @click="showCatalog = false"></button>
        </div>
      </div>
      <div class="pos-catalog-body">
        <div v-if="ProductsStore.loading" class="text-secondary small">{{ t('admin.pos.loading') }}</div>
        <div v-else class="row g-3">
          <div v-for="p in posProductGroups" :key="p.sanPhamId" class="col-6 col-md-4 col-xl-3 col-xxl-2">
            <div class="card h-100 pos-catalog-card">
              <div class="d-flex align-items-center justify-content-center" style="height:110px;background:var(--bg-card-inset);">
                <img v-if="p.hinhAnhChinh" :src="p.hinhAnhChinh" :alt="p.tenSanPham" style="width:100%;height:100%;object-fit:contain;padding:8px;" />
                <span v-else><Laptop :size="32" color="var(--text-muted)" /></span>
              </div>
              <div class="card-body p-2 d-flex flex-column gap-1">
                <div class="fw-semibold small text-light">{{ p.tenSanPham }}</div>
                <div class="fw-bold" style="font-size:0.95rem;color:var(--accent-fg);">
                  <span v-if="(posVariantCountMap.get(p.sanPhamId) || 0) > 1" class="fw-normal" style="font-size:0.7rem;color:var(--text-secondary);">{{ t('home.fromPrice') }} </span>{{ formatPrice(p.giaBan) }}
                </div>
                <button class="alt-btn alt-btn--primary mt-auto justify-content-center" @click="catalogAddToCart(p)">{{ t('admin.pos.addToCart') }}</button>
              </div>
            </div>
          </div>
          <div v-if="posProductGroups.length===0" class="col-12 text-center text-secondary small py-5">{{ t('admin.pos.noProductsFound') }}</div>
        </div>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHON CAU HINH/MAU (POS) ══ -->
  <div v-if="showVariantPicker" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showVariantPicker=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:480px;max-width:95vw;max-height:80vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <div>
          <div>{{ t('admin.pos.chooseVariant') }}</div>
          <div class="text-secondary fw-normal" style="font-size:0.75rem;">{{ variantPickerBase?.tenSanPham }} — {{ variantPickerActiveVariant?.maSku }}</div>
        </div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showVariantPicker=false"></button>
      </div>
      <div class="overflow-y-auto p-3 d-flex flex-column gap-3">
        <div class="fw-bold text-warning" style="font-size:1.1rem;">{{ formatPrice(variantPickerActiveVariant?.giaBan) }}</div>

        <div v-if="variantPickerConfigs.length > 1">
          <div class="fw-semibold mb-2" style="font-size:0.72rem;text-transform:uppercase;letter-spacing:0.06em;color:var(--text-secondary);">
            {{ t('productDetail.versions', { count: variantPickerConfigs.length }) }}
          </div>
          <div class="d-flex flex-wrap gap-2">
            <button
              v-for="v in variantPickerConfigs" :key="configKey(v)"
              class="btn btn-sm d-flex flex-column align-items-start text-start px-3 py-2"
              style="border-radius:10px;"
              :style="variantPickerActiveConfigKey === configKey(v)
                ? 'background:rgba(244,63,94,0.12);border:1.5px solid var(--accent);color:var(--accent-fg);'
                : 'background:var(--bg-input);border:1.5px solid var(--border-color-strong);color:var(--text-secondary);'"
              @click="variantPickerSelectConfig(v)"
            >
              <span class="fw-semibold" style="font-size:11px;line-height:1.5;">{{ configLabel(v).line1 }}</span>
              <span v-if="configLabel(v).line2" style="font-size:10px;opacity:0.75;">{{ configLabel(v).line2 }}</span>
            </button>
          </div>
        </div>

        <div v-if="variantPickerColorsForConfig.some(v => v.mauSac)">
          <div class="fw-semibold mb-2" style="font-size:0.72rem;text-transform:uppercase;letter-spacing:0.06em;color:var(--text-secondary);">
            {{ t('productDetail.colorHeading') }}
          </div>
          <div class="d-flex flex-wrap gap-2">
            <button
              v-for="v in variantPickerColorsForConfig" :key="v.bienTheId"
              class="btn btn-sm d-flex align-items-center gap-2 px-3 py-2"
              style="border-radius:10px;"
              :style="variantPickerActiveColor === v.mauSac
                ? 'background:rgba(244,63,94,0.12);border:1.5px solid var(--accent);color:var(--accent-fg);'
                : 'background:var(--bg-input);border:1.5px solid var(--border-color-strong);color:var(--text-secondary);'"
              @click="variantPickerSelectColor(v)"
            >
              <span class="rounded-circle flex-shrink-0" :style="`width:13px;height:13px;background:${colorDot(v.mauSac)};border:1.5px solid #666;display:inline-block;`"></span>
              <div class="d-flex flex-column align-items-start text-start">
                <span class="fw-semibold" style="font-size:11px;line-height:1.3;">{{ v.mauSac }}</span>
                <span style="font-size:10px;color:var(--accent-fg);">{{ formatPrice(v.giaBan) }}</span>
              </div>
            </button>
          </div>
        </div>

        <button class="btn btn-warning text-dark fw-bold mt-2" @click="posConfirmVariant">{{ t('admin.pos.continueToSerial') }}</button>
      </div>
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
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showSerialPicker=false"></button>
      </div>
      <div class="overflow-y-auto p-3 d-flex flex-column gap-2">
        <div v-if="serialPickerLoading" class="text-secondary small text-center py-4">{{ t('admin.pos.loading') }}</div>
        <div v-else-if="serialPickerList.length===0" class="text-secondary small text-center py-4">{{ t('admin.pos.noSerialAvailable') }}</div>
        <button
          v-for="s in serialPickerList" v-else :key="s.chiTietId"
          class="btn d-flex justify-content-between align-items-center"
          :class="serialPickerSwapChiTietId == null && serialPickerChosenIds.has(s.chiTietId) ? 'btn-warning text-dark' : 'btn-outline-warning'"
          style="font-family:monospace;font-size:0.85rem;"
          @click="serialPickerSwapChiTietId != null ? posSelectSerial(s) : posToggleSerial(s)"
        >
          <span>{{ s.soSerial }}</span>
          <span class="text-secondary" style="font-size:0.7rem;">{{ formatDate(s.ngayNhapKho) }}</span>
        </button>
      </div>
      <div v-if="serialPickerSwapChiTietId == null" class="p-3 border-top border-secondary">
        <button
          class="btn btn-warning text-dark fw-bold w-100" :disabled="serialPickerChosenIds.size === 0"
          @click="posAddChosenSerials"
        >
          {{ t('admin.pos.addChosenSerials', { count: serialPickerChosenIds.size }) }}
        </button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL DON DANG GIU (POS) ══ -->
  <div v-if="showHeldOrders" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showHeldOrders=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:520px;max-width:95vw;max-height:80vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.pos.heldOrders') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showHeldOrders=false"></button>
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

  <!-- ══ MODAL CHI TIET SAN PHAM (POS) — xem-thuan, mo tu nut "Chi tiet" tren dong gio hang,
       chi hien dung cac bien the dang co trong nhom do (onlyBienTheIds) ══ -->
  <ProductDetailModal
    v-model="showPosDetailModal"
    :san-pham-id="posDetailSanPhamId"
    :san-pham-name="posDetailSanPhamName"
    :only-bien-the-ids="posDetailOnlyBienTheIds"
  />

  <!-- Modal in hoa don POS -->
  <InvoiceModal
    :show="showInvoiceModal"
    :don-hang-id="posLastOrder?.donHangId ?? posLastOrder?.id"
    :order="posLastOrder"
    @close="showInvoiceModal = false"
  />
</template>

<style scoped>
@import "../../assets/admin-list-theme.css";

.pos-page {
  position: relative;
  height: calc(100vh - 120px);
}

/* 2/3 gio hang (trai) — 1/3 nut + thong tin (phai), dong bo card/toolbar voi cac tab
   quan tri khac (xem admin-list-theme.css: .alt-card/.alt-toolbar/.alt-btn). */
.pos-grid-layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 18px;
  height: 100%;
}

.pos-cart-card,
.pos-side-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pos-side-card {
  overflow-y: auto;
}
.pos-side-body { display: flex; flex-direction: column; }
.pos-side-section {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color-soft);
}
.pos-side-section:last-child { border-bottom: none; }
.pos-side-actions { display: flex; flex-direction: column; gap: 8px; }

/* Nut FAB canh man hinh mo danh sach hang hoa toan man hinh */
.pos-catalog-fab {
  position: fixed;
  right: 22px;
  bottom: 28px;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-brand);
  color: var(--accent-text);
  box-shadow: 0 6px 18px var(--shadow-color);
  z-index: 1055;
  cursor: pointer;
  transition: filter 0.15s, transform 0.15s;
}
.pos-catalog-fab:hover { filter: brightness(1.08); transform: translateY(-1px); }

.pos-catalog-overlay {
  position: fixed;
  inset: 0;
  z-index: 1060;
  background: var(--bg-page);
  display: flex;
  flex-direction: column;
}
.pos-catalog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 20px;
  background: var(--bg-card-alt);
  border-bottom: 1px solid var(--border-color-soft);
  flex-wrap: wrap;
}
.pos-catalog-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 20px;
}
.pos-catalog-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color-soft);
  border-radius: 12px;
  overflow: hidden;
}

.text-light {
  color: var(--text-primary) !important;
}
</style>
