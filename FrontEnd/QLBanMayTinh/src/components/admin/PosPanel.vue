<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from "vue";
import { t } from "../../i18n/index.js";
import { nowLocalIso } from "../../utils/datetime.js";
import * as DonHangService from "../../services/DonHangService.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import { formatPrice, formatDate, boDauTiengViet } from "../../utils/adminFormat.js";
import { ProductsStore, ensureProducts, refreshProducts } from "../../stores/products.js";
import { refreshInventory } from "../../stores/inventory.js";
import { bumpSerialEvent } from "../../stores/serialEvents.js";
import { syncPosCart } from "../../stores/posCart.js";
import { CustomersStore, ensureCustomers } from "../../stores/customers.js";
import { PromotionsStore, refreshPromotions } from "../../stores/promotions.js";
import { refreshOrders } from "../../stores/orders.js";
import CustomerFormModal from "./CustomerFormModal.vue";
import ProductDetailModal from "./ProductDetailModal.vue";
import { groupBySanPham, variantCountBySanPham, configKey, configLabel, colorDot } from "../../utils/productGrouping.js";
import { POS_PAYMENT_METHODS, paymentMethodLabel, paymentMethodIcon } from "../../utils/orderStatus.js";
import * as ThanhToanService from "../../services/ThanhToanService.js";
import { SerialLockService } from "../../services/SerialLockService.js";
import { AuthStore } from "../../stores/index.js";
import { useToastStore } from "../../stores/toast.js";
const { showToast } = useToastStore();
import { askConfirm } from "../../stores/confirm.js";

// POS session ID — tao moi neu chua co, giu khi reload
const posSessionId = ref(
  localStorage.getItem('pos_session_id') || crypto.randomUUID()
);
if (!localStorage.getItem('pos_session_id')) {
  localStorage.setItem('pos_session_id', posSessionId.value);
}

// Theo doi serial dang lock boi session nay
const trackedSerialIds = ref(new Set());
import { Laptop, ShoppingCart, Receipt, Info, Hash, X, Check, ImageOff, Printer, Package, Search, Eye, Plus, Star, Shield, Cpu, HardDrive, MemoryStick, Flame } from '@lucide/vue';
import InvoiceModal from "./InvoiceModal.vue";

onMounted(async () => {
  ensureProducts();
  ensureCustomers();
  // Fetch luôn danh sách khuyến mãi khi vào POS — dropdown "Chọn mã giảm giá" cần có sẵn
  // dữ liệu trước khi nhân viên tick, không phụ thuộc AdminPage.fetchAll() đã chạy xong chưa
  // (nếu user vào thẳng POS hoặc AdminPage fetchAll bị lỗi nuốt thì dropdown sẽ trống).
  await refreshPromotions();
});

// ── Phí vận chuyển (POS) ──────────────────────────────────────────────────────
// Tính theo khoảng cách + miễn phí khi đơn đủ lớn. Tier đơn giản theo km — khớp với
// CheckoutModal.vue (cùng bảng, copy-paste trực tiếp để nhân viên tại quầy nhập km tay
// thay vì chọn địa chỉ như khách online).
const POS_FREE_SHIP_THRESHOLD = 300000;
const POS_SHIP_TIERS = [
  { maxKm: 2,  fee: 10000 },
  { maxKm: 5,  fee: 20000 },
  { maxKm: 10, fee: 30000 },
  { maxKm: 999, fee: 50000 },
];

// ── POS / Ban hang ───────────────────────────────────────────────────────────
// Luong bat buoc: phai xac dinh khach hang (co san hoac tao moi) TRUOC khi duoc
// them san pham vao gio — tranh tao hoa don "vo danh" roi moi lo tim/tao khach sau.
// posStage: 'start' (chua bat dau) -> 'phone' (dang nhap SDT tim/tao khach) -> 'selling' (da co khach, duoc them SP)
const posStage = ref('start');
const posPhoneNotFound = ref(false); // da tim nhung khong thay khach ung voi SDT vua nhap
const posSearch = ref("");
const posCart = ref([]);
// Đồng bộ posCartStore liên tục khi giỏ hàng thay đổi — InventoryPanel và SerialManager
// đọc store này để hiển thị trạng thái "đang lên đơn" tức thì (không cần API round-trip).
watch(posCart, (v) => syncPosCart(v), { deep: true });

const posPhone = ref("");
const posFoundCust = ref(null);
const posError = ref("");
const posPlacing = ref(false);
// Modal toan man hinh de nhan vien de nhin serial khi gio co nhieu may — modal rieng de
// khong bi gioi han chieu cao cua cart list.
const serialModalGroup = ref(null);
const showSerialModal = ref(false);
const openSerialModal = (g) => {
  serialModalGroup.value = g;
  showSerialModal.value = true;
};
const closeSerialModal = () => {
  showSerialModal.value = false;
  serialModalGroup.value = null;
};
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
  const cat = posCatalogCategory.value;
  return ProductsStore.items.filter(
    (p) =>
      p.trangThai === "active" &&
      (!cat || p.maDanhMuc === cat || p.danhMucId === cat) &&
      (!q ||
        boDauTiengViet(p.tenSanPham).includes(q) ||
        boDauTiengViet(p.maSku ?? "").includes(q) ||
        boDauTiengViet(p.barcode ?? "").includes(q)),
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
const openPosDetail = async (g) => {
  // Đảm bảo products đã loaded trước khi modal đọc ProductsStore.items
  await ensureProducts();
  posDetailSanPhamId.value = g.sanPhamId;
  posDetailSanPhamName.value = g.tenSanPham;
  // Từ catalog: g là biến thể phẳng (không có .items) → hiện tất cả biến thể cùng sanPhamId.
  // Từ cart: g.items chứa các serial khác biến thể → chỉ hiện các biến thể đang trong giỏ.
  posDetailOnlyBienTheIds.value = g.items
    ? [...new Set(g.items.map((i) => i.bienTheId))]
    : null;
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
// Gia ban 1 may (khong nhan so luong) — cart the hien cac serial trong cung 1 group,
// gia hien thi luon la gia cua 1 don vi de nhan vien khong bi nham voi tong tien (tong
// tien cua ca group hien o Subtotal phia duoi).
const formatPriceShort = (v) => {
  if (v == null) return '—';
  if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(v % 1_000_000 === 0 ? 0 : 2)}tr`;
  if (v >= 1_000) return `${(v / 1_000).toFixed(0)}k`;
  return String(v);
};
const posGroupPriceShort = (g) => formatPriceShort(g.items[0]?.giaBan);
// Ghép cpu + ram + oCung + mauSac thành 1 dòng spec rút gọn hiển thị trên card giỏ hàng.
const specLine = (item) => [item.cpu, item.ram, item.oCung, item.mauSac].filter(Boolean).join(' · ');
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

// Danh sach khuyen mai ap dung duoc cho don hien tai (dang active + dat don toi thieu).
// Dong thoi tra ve text hien thi ben option dropdown.
const promoConditionLabel = (p) => {
  if (!p.donHangToiThieu || Number(p.donHangToiThieu) === 0) return null;
  return `Áp dụng cho đơn từ ${formatPrice(Number(p.donHangToiThieu))}`;
};
const promoOptionLabel = (p) => {
  const val = p.loai === 'percent' ? `${p.giaTri}%` : formatPrice(p.giaTri);
  const cond = promoConditionLabel(p);
  return cond ? `${p.maKhuyenMai} — ${p.tenKhuyenMai} (${val}) · ${cond}` : `${p.maKhuyenMai} — ${p.tenKhuyenMai} (${val})`;
};
const posApplicablePromos = computed(() =>
  (PromotionsStore.items ?? [])
    .filter((p) => p.trangThai === 'active'
      && (!p.donHangToiThieu || posCartTotal.value >= Number(p.donHangToiThieu)))
);

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
const HELD_ORDERS_KEY = 'saoclub_pos_held_orders';
const CART_KEY = 'saoclub_pos_cart';
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
        // Đồng bộ posCartStore để tab Kho/Serial phản ánh ngay serial đang lên đơn
        syncPosCart(saved.cart);
        bumpSerialEvent();
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
const variantPickerBase = ref(null);

// ── POS Catalog: categories + barcode scan ─────────────────────────────────────
const posCategories = ref([]);
const posCatalogCategory = ref(null);
const posBarcodeInput = ref("");
const posBarcodeError = ref("");

onMounted(async () => {
  try {
    const { getActive } = await import("../../services/DanhMucService.js");
    posCategories.value = await getActive();
  } catch {}
});

const posHandleBarcode = async () => {
  const code = posBarcodeInput.value.trim();
  if (!code) return;
  posBarcodeError.value = "";
  try {
    const data = await ChiTietSanPhamService.scanBarcode(code);
    if (!data || data.error) {
      posBarcodeError.value = data?.error || "Không tìm thấy mã " + code;
      return;
    }
    // tim bien the trong ProductsStore theo bienTheId
    const variant = ProductsStore.items.find(v => v.bienTheId === data.bienTheId);
    if (!variant) {
      posBarcodeError.value = "Sản phẩm không có trong danh sách hàng hóa";
      return;
    }
    posBarcodeInput.value = "";
    posBarcodeError.value = "";
    // Dong catalog overlay (neu dang mo) roi mo variant picker voi bien the chinh xac
    showCatalog.value = false;
    posOpenVariantPicker(variant);
    // Auto-select trong modal picker: chon dung config + mau cua bien the
    await new Promise(r => setTimeout(r, 50));
    if (data.sanPhamId) {
      variantPickerActiveConfigKey.value = configKey(variant);
      variantPickerActiveColor.value = variant.mauSac ?? '';
    }
  } catch {
    posBarcodeError.value = "Lỗi khi quét mã vạch";
  }
};
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
  // Load ALL serials (including locked) — locked serials shown as disabled with lock badge.
  // Only 'trong_kho' serials can be picked (filter in template).
  const all = await ChiTietSanPhamService.getByBienThe(p.bienTheId).catch(() => []);
  serialPickerList.value = all;
  serialPickerLoading.value = false;
};

// Toggle serial trong picker — goi lock/unlock API de phien POS khac thay duoc lock status.
// Khi lock: chi goi khi serial trang thai = trong_kho va chua bi nguoi khac lock.
// Khi unlock: tra serial ve trong_kho ngay lap tuc de nguoi khac co the chon.
// Dong modal chon serial — unlock tat ca serial da tick (neu chua them vao gio).
// Vi serial da add vao gio se co trangThai=giu_hang roi, khong can unlock.
const posCloseSerialPicker = async () => {
  const chosen = [...serialPickerChosenIds.value];
  if (chosen.length > 0) {
    // Chi unlock nhung serial chua duoc them vao gio (van con trang thai trong_kho)
    const toUnlock = serialPickerList.value
      .filter(s => chosen.includes(s.chiTietId) && s.trangThai === 'trong_kho')
      .map(s => s.chiTietId);
    if (toUnlock.length > 0) {
      await SerialLockService.unlock(toUnlock, posSessionId.value);
    }
  }
  serialPickerChosenIds.value = new Set();
  showSerialPicker.value = false;
  serialPickerSwapChiTietId.value = null;
};

const posToggleSerial = async (serial) => {
  // Serial dang o trong_kho -> tick chon -> lock
  if (!serialPickerChosenIds.value.has(serial.chiTietId)) {
    // Kiem tra serial co bi lock boi nguoi khac chua (lockedByTen se co gia tri)
    if (serial.lockedBy && serial.lockedByTen) {
      showToast(`Serial đang được ${serial.lockedByTen} giữ`);
      return;
    }
    // Lock serial
    const result = await SerialLockService.lock(
      [serial.chiTietId],
      posSessionId.value,
      AuthStore.user?.id
    );
    if (!result.success) {
      showToast(`Không thể chọn serial này — đang được ai đó giữ`);
      return;
    }
    const next = new Set(serialPickerChosenIds.value);
    next.add(serial.chiTietId);
    serialPickerChosenIds.value = next;
  } else {
    // Bo tick -> unlock
    const next = new Set(serialPickerChosenIds.value);
    next.delete(serial.chiTietId);
    serialPickerChosenIds.value = next;
    await SerialLockService.unlock([serial.chiTietId], posSessionId.value);
  }
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
    cpu: p.cpu ?? null,
    ram: p.ram ?? null,
    oCung: p.oCung ?? null,
    mauSac: p.mauSac ?? null,
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
    cpu: p.cpu ?? null,
    ram: p.ram ?? null,
    oCung: p.oCung ?? null,
    mauSac: p.mauSac ?? null,
    chiTietId: serial.chiTietId,
    soSerial: serial.soSerial,
    ngayNhapKho: serial.ngayNhapKho,
    soLuong: 1,
  }));
  posCart.value = [...posCart.value, ...items];
  showSerialPicker.value = false;
  await Promise.all(items.map((item) => setSerialTrangThai(item, 'giu_hang')));
};

const posDecrementGroup = async (g) => {
  if (g.items.length === 0) return;
  const lastItem = g.items[g.items.length - 1];
  posCart.value = posCart.value.filter((i) => i.chiTietId !== lastItem.chiTietId);
  await setSerialTrangThai(lastItem, 'trong_kho');
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
          <div v-if="posCart.length===0" class="pos-cart-empty">
            <div class="pos-cart-empty__icon"><Package :size="48" /></div>
            <div class="pos-cart-empty__title">Giỏ hàng trống</div>
            <div class="pos-cart-empty__hint">Bấm <strong>+ Sản phẩm</strong> để bắt đầu</div>
          </div>
          <div
            v-for="g in posCartGroups" :key="g.sanPhamId"
            class="pos-cart-item"
          >
            <div class="d-flex align-items-center gap-2">
              <!-- Ảnh sản phẩm -->
              <div class="pos-cart-img">
                <img v-if="g.hinhAnhChinh" :src="g.hinhAnhChinh" :alt="g.tenSanPham" />
                <span v-else><Laptop :size="18" color="var(--text-muted)" /></span>
              </div>

              <!-- Tên + spec -->
              <div class="flex-grow-1" style="min-width:0;">
                <div class="fw-semibold small text-light text-truncate">{{ g.tenSanPham }}</div>
                <div v-if="specLine(g.items[0])" class="pos-cart-spec">{{ specLine(g.items[0]) }}</div>
              </div>

              <!-- Nút - số + serial-info product-detail -->
              <div class="d-flex align-items-center gap-1 flex-shrink-0">
                <button
                  class="pos-cart-qty-btn"
                  :disabled="g.items.length <= 1"
                  @click="posDecrementGroup(g)"
                >−</button>
                <span class="fw-bold text-center" style="min-width:28px;font-size:0.88rem;color:var(--accent-fg);">{{ g.items.length }}</span>
                <button
                  class="pos-cart-qty-btn pos-cart-qty-btn--add"
                  @click="posOpenSerialPicker(g.items[0], null)"
                >+</button>
                <button
                  class="pos-cart-icon-btn"
                  :title="t('admin.pos.showSerials')"
                  @click="openSerialModal(g)"
                >
                  <Hash :size="13" />
                </button>
                <button
                  class="pos-cart-icon-btn"
                  :title="t('admin.products.detail')"
                  @click="openPosDetail(g)"
                >
                  <Info :size="13" />
                </button>
              </div>

              <!-- Giá 1 máy -->
              <div class="fw-bold flex-shrink-0 text-end" style="font-size:0.85rem;min-width:100px;color:var(--accent-fg);">{{ formatPrice(g.items[0]?.giaBan) }}</div>

              <!-- Xóa toàn bộ group -->
              <button class="pos-cart-remove-btn" @click="posRemoveGroup(g)">
                <X :size="14" />
              </button>
            </div>
            <!-- Serial chip row ẩn — user bấm # để xem serial -->
            <div class="pos-cart-chips">
              <span class="pos-chip-warranty"><Shield :size="10" />12 tháng</span>
            </div>
          </div>
        </div>
      </div>

      <!-- PHAI (1/3): nut + thong tin don hang -->
      <div class="alt-card pos-side-card">
        <div class="pos-side-toolbar">
          <div class="pos-side-toolbar__icon"><Receipt :size="16" /></div>
          <span class="fw-bold">{{ t('admin.pos.orderInfo') }}</span>
          <span v-if="posFoundCust" class="pos-customer-avatar">{{ posFoundCust.hoTen.charAt(0).toUpperCase() }}</span>
        </div>

        <div v-if="posStage !== 'selling'" class="pos-side-empty">
          <div class="pos-side-empty__icon"><Receipt :size="36" /></div>
          <div>{{ t('admin.pos.noOrderInfoYet') }}</div>
        </div>

        <div v-else class="pos-side-body">
          <!-- Ma khuyen mai -->
          <div class="pos-side-section">
            <div class="pos-section-label"><Package :size="13" />Mã giảm giá</div>
            <div class="d-flex gap-2 position-relative">
              <select v-model="posPromoCode" class="form-select form-select-sm pos-select" @change="posApplyPromo">
                <option value="">{{ t('admin.pos.choosePromo') }}</option>
                <option v-for="p in posApplicablePromos" :key="p.khuyenMaiId" :value="p.maKhuyenMai">{{ promoOptionLabel(p) }}</option>
              </select>
            </div>
            <div v-if="posPromoMsg" class="small mt-1" :class="posAppliedPromo ? 'text-success' : 'text-danger'">{{ posPromoMsg }}</div>
          </div>

          <!-- Tong tien -->
          <div class="pos-side-section pos-price-block">
            <div class="pos-price-row"><span>{{ t('admin.pos.subtotalLabel') }}</span><span>{{ formatPrice(posCartTotal) }}</span></div>
            <div v-if="posGiamGia > 0" class="pos-price-row pos-price-discount"><span>{{ t('checkout.discount') }}</span><span>-{{ formatPrice(posGiamGia) }}</span></div>
            <div class="pos-price-divider"></div>
            <div class="pos-price-row pos-price-total"><span>{{ t('admin.pos.totalLabel') }}</span><span>{{ formatPrice(posGrandTotal) }}</span></div>
          </div>

          <!-- Phuong thuc thanh toan -->
          <div class="pos-side-section">
            <div class="pos-section-label"><Receipt :size="13" />Phương thức thanh toán</div>
            <div class="d-flex gap-1">
              <button
                v-for="m in POS_PAYMENT_METHODS" :key="m"
                class="pos-pay-btn"
                :class="{ active: posPaymentMethod === m }"
                @click="posPaymentMethod = m; posQrScanned = false"
              >
                <component :is="paymentMethodIcon(m)" :size="16" />
                <span>{{ paymentMethodLabel(m) }}</span>
              </button>
            </div>
            <div v-if="posPaymentMethod === 'chuyen_khoan'" class="pos-qr-block">
              <img
                v-if="!posQrImageFailed" :src="posQrImageUrl" alt="VietQR" class="pos-qr-img"
                @error="posQrImageFailed = true"
              />
              <div v-else class="pos-qr-fallback"><ImageOff :size="24" /><span>{{ t('checkout.qrImageFailed') }}</span></div>
              <button class="pos-qr-confirm-btn" :class="{ confirmed: posQrScanned }" @click="posQrScanned = !posQrScanned">
                <Check v-if="posQrScanned" :size="13" />
                {{ posQrScanned ? t('admin.pos.qrScannedConfirmed') : t('admin.pos.simulateQrScan') }}
              </button>
            </div>
          </div>

          <!-- Khach hang -->
          <div class="pos-side-section">
            <div class="pos-section-label"><Laptop :size="13" />Khách hàng</div>
            <div v-if="posFoundCust" class="pos-customer-card">
              <div class="pos-customer-info">
                <div class="pos-customer-name"><Check :size="13" /> {{ posFoundCust.hoTen }}</div>
                <div class="pos-customer-phone">{{ posFoundCust.soDienThoai }}</div>
              </div>
              <button class="pos-change-btn" @click="posReset">{{ t('admin.pos.changeCustomer') }}</button>
            </div>
            <div v-else class="small text-secondary">{{ t('admin.pos.noCustomerYet') }}</div>
            <div v-if="posError" class="pos-error-msg">{{ posError }}</div>
            <div v-if="posSuccess" class="pos-success-msg">
              <div><Check :size="15" /> {{ t('admin.pos.orderCreated') }}</div>
              <button class="pos-invoice-btn" @click="showInvoiceModal = true">
                <Printer :size="13" /> In hóa đơn
              </button>
            </div>
          </div>

          <!-- Nut hanh dong -->
          <div class="pos-side-section pos-side-actions">
            <div class="d-flex gap-2">
              <button class="pos-action-secondary" @click="posReset">{{ t('admin.pos.reset') }}</button>
              <button class="pos-action-secondary" :disabled="!posCart.length" @click="posHoldOrder">{{ t('admin.pos.holdOrder') }}</button>
            </div>
            <button
              class="pos-pay-submit"
              :disabled="posStage !== 'selling' || !posCart.length || !posPaymentMethod || posPlacing || (posPaymentMethod === 'chuyen_khoan' && !posQrScanned)"
              @click="posPlaceOrder"
            >
              <Star :size="15" /> {{ t('admin.pos.createOrder') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ══ TOAN MAN HINH: Danh sach hang hoa ══ -->
    <div v-if="showCatalog" class="pos-catalog-overlay">
      <!-- Header -->
      <div class="pos-catalog-header">
        <div class="pos-catalog-search">
          <Search :size="16" class="pos-catalog-search__icon" />
          <input v-model="posSearch" :placeholder="t('admin.pos.searchPlaceholder')" />
        </div>
        <!-- Barcode scan: quet ma vach nhanh -->
        <div class="pos-barcode-scan">
          <input
            v-model="posBarcodeInput"
            :placeholder="t('admin.pos.barcodePlaceholder')"
            class="pos-barcode-input"
            @keyup.enter="posHandleBarcode"
          />
          <button class="pos-barcode-btn" @click="posHandleBarcode">
            <Search :size="14" />
          </button>
        </div>
        <div class="d-flex align-items-center gap-2">
          <span class="pos-cart-badge"><ShoppingCart :size="14" />{{ posCart.length }}</span>
          <button class="alt-btn alt-btn--primary" @click="showCatalog = false">{{ t('admin.pos.doneAdding') }}</button>
          <button class="btn-close" :aria-label="t('common.close')" @click="showCatalog = false"></button>
        </div>
      </div>

      <!-- Category tabs -->
      <div class="pos-catalog-tabs">
        <button
          class="pos-tab"
          :class="{ active: posCatalogCategory === null }"
          @click="posCatalogCategory = null"
        >Tất cả</button>
        <button
          v-for="cat in posCategories"
          :key="cat.id"
          class="pos-tab"
          :class="{ active: posCatalogCategory === cat.id }"
          @click="posCatalogCategory = cat.id"
        >{{ cat.tenDanhMuc }}</button>
      </div>

      <div class="pos-catalog-body">
        <div v-if="ProductsStore.loading" class="text-secondary small">{{ t('admin.pos.loading') }}</div>
        <div v-if="posBarcodeError" class="pos-barcode-error">{{ posBarcodeError }}</div>

        <template v-else>
          <!-- Product grid -->
          <div class="pos-product-grid">
            <div v-for="p in posProductGroups" :key="p.sanPhamId" class="pos-product-card">
              <div class="pos-product-img-wrap">
                <img v-if="p.hinhAnhChinh" :src="p.hinhAnhChinh" :alt="p.tenSanPham" class="pos-product-img" />
                <span v-else class="pos-product-placeholder"><Laptop :size="32" /></span>
                <!-- Hot badge -->
                <span v-if="p.banChay" class="pos-hot-badge"><Flame :size="10" /> Bán chạy</span>
              </div>
              <div class="pos-product-body">
                <div class="pos-product-name">{{ p.tenSanPham }}</div>
                <!-- Spec chips -->
                <div class="pos-spec-chips">
                  <span v-if="p.cpu"><Cpu :size="10" />{{ p.cpu }}</span>
                  <span v-if="p.ram"><MemoryStick :size="10" />{{ p.ram }}</span>
                  <span v-if="p.oCung"><HardDrive :size="10" />{{ p.oCung }}</span>
                </div>
                <div class="pos-product-price">
                  <span class="pos-price-from" v-if="(posVariantCountMap.get(p.sanPhamId) || 0) > 1">Từ </span>{{ formatPrice(p.giaBan) }}
                </div>
              </div>
              <!-- Hover actions -->
              <div class="pos-product-actions">
                <button class="pos-action-btn pos-action-btn--view" :title="'Xem chi tiết'" @click.stop="openPosDetail(p)">
                  <Eye :size="14" />
                </button>
                <button class="pos-action-btn pos-action-btn--add" :title="'Thêm vào giỏ'" @click.stop="catalogAddToCart(p)">
                  <Plus :size="14" /> Thêm
                </button>
              </div>
            </div>
            <div v-if="posProductGroups.length===0" class="pos-no-results">
              <Package :size="40" />
              <div>{{ t('admin.pos.noProductsFound') }}</div>
            </div>
          </div>
        </template>
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
  <div v-if="showSerialPicker" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="posCloseSerialPicker">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:480px;max-width:95vw;max-height:75vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <div>
          <div>{{ t('admin.pos.chooseSerial') }}</div>
          <div class="text-secondary fw-normal" style="font-size:0.75rem;">{{ serialPickerProduct?.tenSanPham }} — {{ serialPickerProduct?.maSku }}</div>
        </div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="posCloseSerialPicker"></button>
      </div>
      <div class="overflow-y-auto p-3 d-flex flex-column gap-2">
        <div v-if="serialPickerLoading" class="text-secondary small text-center py-4">{{ t('admin.pos.loading') }}</div>
        <div v-else-if="serialPickerList.length===0" class="text-secondary small text-center py-4">{{ t('admin.pos.noSerialAvailable') }}</div>
        <button
          v-for="s in serialPickerList" v-else :key="s.chiTietId"
          class="btn d-flex justify-content-between align-items-center"
          :class="[
            s.trangThai !== 'trong_kho' || (s.lockedBy && s.lockedByTen) ? 'btn-secondary opacity-50' :
              (serialPickerSwapChiTietId == null && serialPickerChosenIds.has(s.chiTietId) ? 'btn-warning text-dark' : 'btn-outline-warning'),
            s.lockedBy && s.lockedByTen ? 'text-decoration-line-through' : ''
          ]"
          :disabled="s.trangThai !== 'trong_kho' || (s.lockedBy && s.lockedByTen)"
          :title="s.lockedByTen ? `Đang được ${s.lockedByTen} giữ` : (s.trangThai !== 'trong_kho' ? `Đã ${s.trangThai === 'giu_hang' ? 'được chọn' : s.trangThai}` : '')"
          style="font-family:monospace;font-size:0.85rem;"
          @click="serialPickerSwapChiTietId != null ? posSelectSerial(s) : posToggleSerial(s)"
        >
          <span>
            <span v-if="s.lockedBy && s.lockedByTen" class="me-1">🔒</span>
            {{ s.soSerial }}
          </span>
          <span class="text-secondary" style="font-size:0.7rem;">
            <span v-if="s.lockedBy && s.lockedByTen" class="text-warning">{{ s.lockedByTen }}</span>
            <span v-else>{{ formatDate(s.ngayNhapKho) }}</span>
          </span>
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
       chi hien dung cac bien the dang co trong nhom do (onlyBienTheIds). z-index 1080 cao hon
       catalog overlay (1060) de khong bi che. ══ -->
  <div v-if="showPosDetailModal" style="position:fixed;inset:0;z-index:1080;">
    <ProductDetailModal
      v-model="showPosDetailModal"
      :san-pham-id="posDetailSanPhamId"
      :san-pham-name="posDetailSanPhamName"
      :only-bien-the-ids="posDetailOnlyBienTheIds"
    />
  </div>

  <!-- ══ MODAL DANH SÁCH SERIAL (BARCODE) — mở từ nút # trên cart item, hiện mỗi serial
       dạng mã vạch text lớn để nhân viên dễ nhìn/đối chiếu khi giao hàng ══ -->
  <div
    v-if="showSerialModal"
    class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background:var(--bg-overlay);z-index:1070;"
    @click.self="closeSerialModal"
  >
    <div class="alt-card" style="max-width:680px;width:90%;max-height:85vh;display:flex;flex-direction:column;overflow:hidden;">
      <div class="alt-toolbar">
        <div class="d-flex align-items-center gap-2">
          <Hash :size="16" color="var(--accent-fg)" />
          <span class="fw-bold">{{ serialModalGroup?.tenSanPham }}</span>
          <span class="text-secondary small">({{ serialModalGroup?.items.length }} {{ t('admin.pos.serialCountSuffix') }})</span>
        </div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="closeSerialModal"></button>
      </div>
      <div class="overflow-y-auto p-3 d-flex flex-column gap-2">
        <div
          v-for="(item, idx) in serialModalGroup?.items ?? []" :key="item.chiTietId"
          class="d-flex align-items-center gap-3 px-3 py-2 rounded-3"
          style="background:var(--bg-card-inset);border:1px solid var(--border-color-soft);"
        >
          <span class="text-secondary" style="min-width:24px;text-align:right;font-size:0.8rem;">{{ idx + 1 }}</span>
          <span
            style="font-family:'Courier New',monospace;font-size:0.92rem;letter-spacing:0.08em;color:var(--accent-fg);flex-grow:1;"
          >{{ item.soSerial }}</span>
        </div>
      </div>
    </div>
  </div>

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
.pos-catalog-tabs {
  display: flex; gap: 6px; padding: 10px 16px;
  border-bottom: 1px solid var(--border-color-soft);
  overflow-x: auto; flex-shrink: 0;
}
.pos-tab {
  padding: 5px 14px; border-radius: 999px; font-size: 0.78rem;
  border: 1.5px solid var(--border-color-strong); cursor: pointer; white-space: nowrap;
  background: transparent; color: var(--text-secondary);
  transition: all 0.15s;
}
.pos-tab:hover { border-color: var(--accent-bg); color: var(--accent-fg); }
.pos-tab.active { background: var(--accent-bg); color: var(--accent-fg); border-color: var(--accent-bg); font-weight: 600; }
.pos-barcode-scan {
  display: flex; align-items: center; gap: 4px;
  background: var(--bg-input); border: 1.5px solid var(--border-color-strong);
  border-radius: 999px; padding: 3px 6px 3px 12px;
}
.pos-barcode-input {
  background: transparent; border: none; outline: none;
  color: var(--text-primary); font-size: 0.82rem; width: 140px;
}
.pos-barcode-input::placeholder { color: var(--text-muted); }
.pos-barcode-btn {
  background: var(--accent-bg); color: var(--accent-fg); border: none;
  border-radius: 999px; padding: 4px 10px; cursor: pointer; display: flex; align-items: center;
}
.pos-barcode-btn:hover { opacity: 0.85; }
.pos-barcode-error {
  padding: 8px 16px; color: #e05252; font-size: 0.8rem; background: rgba(220,53,69,0.1);
  border-radius: 8px; margin: 8px 0;
}
.pos-catalog-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color-soft);
  border-radius: 12px;
  overflow: hidden;
}

/* ─── Catalog Header ─── */
.pos-catalog-search {
  display: flex; align-items: center; gap: 8px;
  background: var(--bg-input); border: 1.5px solid var(--border-color-strong);
  border-radius: 999px; padding: 6px 14px; width: 280px;
}
.pos-catalog-search__icon { color: var(--text-muted); display: flex; }
.pos-catalog-search input {
  background: transparent; border: none; outline: none; color: var(--text-primary);
  font-size: 0.88rem; width: 100%;
}
.pos-catalog-search input::placeholder { color: var(--text-muted); }
.pos-cart-badge {
  display: inline-flex; align-items: center; gap: 5px;
  background: rgba(244,63,94,0.12); color: var(--accent-fg);
  border-radius: 999px; padding: 4px 12px; font-size: 0.78rem; font-weight: 700;
}

/* ─── Category Pills ─── */
.pos-catalog-categories {
  display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 18px;
}
.pos-cat-pill {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 5px 14px; border-radius: 999px; font-size: 0.8rem; font-weight: 600;
  border: 1.5px solid var(--border-color-soft);
  background: var(--bg-card); color: var(--text-secondary);
  cursor: pointer; transition: all .15s;
}
.pos-cat-pill:hover { border-color: var(--accent); color: var(--accent-fg); }
.pos-cat-pill.active {
  background: var(--gradient-brand); color: #fff;
  border-color: transparent; box-shadow: 0 3px 10px rgba(244,63,94,0.3);
}

/* ─── Trending ─── */
.pos-trending-section { margin-bottom: 24px; }
.pos-trending-header {
  display: flex; align-items: center; gap: 6px;
  font-size: 0.88rem; font-weight: 700; color: var(--text-primary);
  margin-bottom: 12px;
}
.pos-trending-carousel { display: flex; align-items: center; gap: 10px; }
.pos-carousel-btn {
  flex-shrink: 0; width: 30px; height: 30px; border-radius: 50%;
  border: 1px solid var(--border-color-soft); background: var(--bg-card);
  color: var(--text-secondary); cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all .15s;
}
.pos-carousel-btn:hover { background: var(--accent); color: #fff; border-color: var(--accent); }
.pos-trending-cards { display: flex; gap: 10px; flex: 1; overflow: hidden; }
.pos-trending-card {
  flex: 0 0 calc(25% - 8px); background: var(--bg-card);
  border: 1px solid var(--border-color-soft); border-radius: 12px; padding: 10px;
  cursor: pointer; transition: all .15s; text-align: center;
}
.pos-trending-card:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0,0,0,0.12); border-color: var(--accent); }
.pos-trending-img {
  position: relative; height: 80px; display: flex; align-items: center; justify-content: center;
  margin-bottom: 6px;
}
.pos-trending-img img { width: 100%; height: 100%; object-fit: contain; }
.pos-trending-placeholder { color: var(--text-muted); }
.pos-trending-badge {
  position: absolute; top: 2px; right: 2px;
  background: rgba(72,199,142,0.15); color: #2f9e6e;
  border-radius: 999px; padding: 1px 5px; font-size: 0.65rem; font-weight: 700;
  display: flex; align-items: center; gap: 2px;
}
.pos-trending-name {
  font-size: 0.72rem; font-weight: 600; color: var(--text-primary);
  line-height: 1.3; margin-bottom: 4px;
  overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
.pos-trending-price { font-size: 0.82rem; font-weight: 700; color: var(--accent-fg); }

/* ─── Product Grid ─── */
.pos-product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 14px;
}
.pos-product-card {
  background: var(--bg-card); border: 1px solid var(--border-color-soft);
  border-radius: 14px; overflow: hidden; cursor: pointer;
  transition: all .18s; position: relative;
  display: flex; flex-direction: column;
}
.pos-product-card:hover {
  transform: translateY(-3px) scale(1.01);
  box-shadow: 0 8px 22px rgba(244,63,94,0.15);
  border-color: var(--accent);
}
.pos-product-img-wrap {
  position: relative; height: 130px; background: var(--bg-card-inset);
  display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.pos-product-img { width: 100%; height: 100%; object-fit: contain; padding: 10px; }
.pos-product-placeholder { color: var(--text-muted); display: flex; }
.pos-hot-badge {
  position: absolute; top: 6px; left: 6px;
  background: linear-gradient(135deg, #f97316, #ef4444);
  color: #fff;
  border-radius: 999px; padding: 2px 8px; font-size: 0.68rem; font-weight: 700;
  display: flex; align-items: center; gap: 3px;
  box-shadow: 0 2px 8px rgba(249,115,22,0.4);
}
.pos-product-body { padding: 10px 10px 8px; flex: 1; display: flex; flex-direction: column; gap: 5px; }
.pos-product-name {
  font-size: 0.8rem; font-weight: 600; color: var(--text-primary);
  line-height: 1.3; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
.pos-spec-chips { display: flex; flex-wrap: wrap; gap: 4px; }
.pos-spec-chips span {
  display: inline-flex; align-items: center; gap: 3px;
  background: var(--pink-100); color: var(--pink-700);
  border-radius: 999px; padding: 2px 7px; font-size: 0.68rem; font-weight: 600;
}
.pos-product-price {
  font-size: 0.9rem; font-weight: 700; color: var(--accent-fg);
  margin-top: auto;
}
.pos-price-from { font-size: 0.7rem; color: var(--text-muted); font-weight: 400; }
.pos-product-actions {
  display: flex; gap: 6px; padding: 8px 10px;
  border-top: 1px solid var(--border-color-soft);
  background: var(--bg-card);
}
.pos-action-btn {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 5px;
  border-radius: 8px; padding: 6px 8px; font-size: 0.78rem; font-weight: 600;
  cursor: pointer; border: 1.5px solid; transition: all .15s;
}
.pos-action-btn--view {
  background: var(--bg-card-inset); border-color: var(--border-color-soft); color: var(--text-secondary);
}
.pos-action-btn--view:hover { background: var(--pink-100); border-color: var(--accent); color: var(--accent-fg); }
.pos-action-btn--add {
  background: rgba(244,63,94,0.08); border-color: var(--accent); color: var(--accent-fg);
}
.pos-action-btn--add:hover { background: var(--accent); color: #fff; }

.pos-no-results {
  grid-column: 1 / -1; text-align: center; color: var(--text-muted);
  padding: 50px 20px; display: flex; flex-direction: column; align-items: center; gap: 12px;
}

.text-light {
  color: var(--text-primary) !important;
}

/* ─── Cart Item ─── */
.pos-cart-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 10px; padding: 50px 20px; text-align: center; flex: 1;
}
.pos-cart-empty__icon { color: var(--text-muted); margin-bottom: 4px; }
.pos-cart-empty__title { font-size: 1rem; font-weight: 700; color: var(--text-secondary); }
.pos-cart-empty__hint { font-size: 0.8rem; color: var(--text-muted); }
.pos-cart-empty__hint strong { color: var(--accent-fg); }

.pos-cart-item {
  background: var(--bg-card-alt); border: 1px solid var(--border-color-soft);
  border-radius: 12px; padding: 10px 12px;
  transition: box-shadow .15s, border-color .15s;
}
.pos-cart-item:hover {
  box-shadow: 0 4px 14px rgba(0,0,0,0.08); border-color: rgba(244,63,94,0.3);
}
.pos-cart-img {
  width: 42px; height: 42px; background: var(--bg-card-inset);
  border-radius: 8px; overflow: hidden; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.pos-cart-img img { width: 100%; height: 100%; object-fit: contain; }
.pos-cart-spec {
  font-size: 0.68rem; color: var(--text-secondary); line-height: 1.3;
  display: flex; flex-wrap: wrap; gap: 3px; margin-top: 2px;
}
.pos-cart-qty-btn {
  width: 26px; height: 26px; border-radius: 6px;
  border: 1.5px solid var(--border-color-strong);
  background: var(--bg-card-inset); color: var(--text-secondary);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  font-size: 0.9rem; transition: all .12s;
}
.pos-cart-qty-btn:not(:disabled):hover { border-color: var(--accent); color: var(--accent-fg); }
.pos-cart-qty-btn--add {
  background: rgba(244,63,94,0.1); border-color: rgba(244,63,94,0.4); color: var(--accent-fg);
}
.pos-cart-qty-btn--add:hover { background: var(--accent); color: #fff; border-color: var(--accent); }
.pos-cart-icon-btn {
  width: 26px; height: 26px; border-radius: 6px;
  border: 1px solid var(--border-color-soft); background: transparent; color: var(--text-muted);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all .12s;
}
.pos-cart-icon-btn:hover { background: var(--pink-100); color: var(--accent-fg); border-color: var(--accent); }
.pos-cart-remove-btn {
  width: 26px; height: 26px; border-radius: 6px;
  border: none; background: transparent; color: var(--text-muted);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all .12s;
}
.pos-cart-remove-btn:hover { background: rgba(220,53,69,0.12); color: var(--danger); }
.pos-cart-chips { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 8px; }
.pos-chip-serial {
  display: inline-flex; align-items: center; gap: 3px;
  background: var(--bg-card-inset); border: 1px solid var(--border-color-soft);
  border-radius: 999px; padding: 2px 8px; font-size: 0.65rem; font-weight: 600;
  color: var(--text-secondary); font-family: ui-monospace, SFMono-Regular, monospace;
}
.pos-chip-serial--more { background: rgba(244,63,94,0.1); color: var(--accent-fg); border-color: rgba(244,63,94,0.3); }
.pos-chip-warranty {
  display: inline-flex; align-items: center; gap: 3px;
  background: rgba(99,102,241,0.1); border: 1px solid rgba(99,102,241,0.3);
  border-radius: 999px; padding: 2px 8px; font-size: 0.65rem; font-weight: 600;
  color: #6366f1;
}

/* ─── Order Info Panel (Right) ─── */
.pos-side-toolbar {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 16px; background: var(--bg-card-alt);
  border-bottom: 1px solid var(--border-color-soft);
}
.pos-side-toolbar__icon { color: var(--accent-fg); display: flex; }
.pos-customer-avatar {
  margin-left: auto;
  width: 26px; height: 26px; border-radius: 50%;
  background: var(--gradient-brand); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.72rem; font-weight: 700;
}
.pos-side-empty {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 10px; color: var(--text-muted); font-size: 0.8rem; padding: 40px;
}
.pos-side-empty__icon { color: var(--text-muted); margin-bottom: 4px; }
.pos-section-label {
  display: flex; align-items: center; gap: 5px;
  font-size: 0.72rem; font-weight: 700; color: var(--text-secondary);
  text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 8px;
}
.pos-select {
  background: var(--bg-input); border: 1.5px solid var(--border-color-strong);
  color: var(--text-primary); font-size: 0.82rem;
}

/* Price block */
.pos-price-block {
  background: var(--bg-card-inset); border-radius: 10px;
  padding: 12px 14px; display: flex; flex-direction: column; gap: 6px;
  margin: 4px 0;
}
.pos-price-row { display: flex; justify-content: space-between; font-size: 0.82rem; color: var(--text-secondary); }
.pos-price-discount { color: #2f9e6e; }
.pos-price-divider { height: 1px; background: var(--border-color-soft); margin: 2px 0; }
.pos-price-total { font-size: 1rem; font-weight: 800; color: var(--accent-fg); }

/* Payment buttons */
.pos-pay-btn {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 8px 4px; border-radius: 10px; font-size: 0.65rem; font-weight: 600;
  border: 1.5px solid var(--border-color-soft); background: var(--bg-card-inset); color: var(--text-secondary);
  cursor: pointer; transition: all .15s;
}
.pos-pay-btn:hover { border-color: var(--accent); color: var(--accent-fg); }
.pos-pay-btn.active {
  background: rgba(244,63,94,0.1); border-color: var(--accent);
  color: var(--accent-fg); box-shadow: 0 2px 8px rgba(244,63,94,0.2);
}
.pos-qr-block { display: flex; flex-direction: column; align-items: center; gap: 8px; margin-top: 8px; }
.pos-qr-img { width: 130px; height: 130px; border-radius: 10px; background: #fff; padding: 4px; }
.pos-qr-fallback {
  width: 130px; height: 130px; border-radius: 10px;
  background: var(--bg-card-alt); display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 6px; color: var(--text-muted); font-size: 0.75rem;
}
.pos-qr-confirm-btn {
  display: flex; align-items: center; gap: 5px; padding: 6px 16px; border-radius: 999px;
  font-size: 0.78rem; font-weight: 600; cursor: pointer; transition: all .15s;
  border: 1.5px solid; width: 100%; justify-content: center;
  background: rgba(251,191,36,0.1); border-color: rgba(251,191,36,0.5); color: #d97706;
}
.pos-qr-confirm-btn.confirmed { background: rgba(72,199,142,0.15); border-color: #2f9e6e; color: #2f9e6e; }

/* Customer card */
.pos-customer-card {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  background: rgba(72,199,142,0.08); border: 1px solid rgba(72,199,142,0.3);
  border-radius: 10px; padding: 10px 12px;
}
.pos-customer-info { display: flex; flex-direction: column; gap: 2px; }
.pos-customer-name { display: flex; align-items: center; gap: 5px; font-size: 0.85rem; font-weight: 700; color: #2f9e6e; }
.pos-customer-phone { font-size: 0.72rem; color: var(--text-secondary); }
.pos-change-btn {
  font-size: 0.72rem; color: var(--text-muted); text-decoration: underline; cursor: pointer;
  background: transparent; border: none; transition: color .12s;
}
.pos-change-btn:hover { color: var(--accent-fg); }
.pos-error-msg {
  font-size: 0.78rem; padding: 8px 10px; border-radius: 8px; margin-top: 6px;
  background: rgba(220,53,69,0.1); color: #e05252; border: 1px solid rgba(220,53,69,0.3);
}
.pos-success-msg {
  display: flex; flex-direction: column; align-items: center; gap: 8px; margin-top: 6px;
  padding: 10px; border-radius: 10px;
  background: rgba(72,199,142,0.08); border: 1px solid rgba(72,199,142,0.3); color: #2f9e6e;
  font-size: 0.82rem; font-weight: 700;
}
.pos-invoice-btn {
  display: flex; align-items: center; gap: 5px;
  background: #2f9e6e; color: #fff; border: none; border-radius: 999px;
  padding: 6px 16px; font-size: 0.78rem; font-weight: 600; cursor: pointer;
  transition: all .15s;
}
.pos-invoice-btn:hover { background: #27a862; }

/* Action buttons */
.pos-action-secondary {
  flex: 1; padding: 8px 12px; border-radius: 10px;
  background: var(--bg-card-inset); border: 1.5px solid var(--border-color-soft);
  color: var(--text-secondary); font-size: 0.8rem; font-weight: 600;
  cursor: pointer; transition: all .15s;
}
.pos-action-secondary:hover:not(:disabled) { border-color: var(--accent); color: var(--accent-fg); }
.pos-action-secondary:disabled { opacity: 0.4; cursor: not-allowed; }
.pos-pay-submit {
  width: 100%; padding: 12px;
  background: var(--gradient-brand); color: #fff;
  border: none; border-radius: 12px;
  font-size: 0.95rem; font-weight: 700;
  cursor: pointer; transition: all .18s;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  box-shadow: 0 4px 14px rgba(244,63,94,0.3);
}
.pos-pay-submit:hover:not(:disabled) { filter: brightness(1.08); transform: translateY(-1px); box-shadow: 0 6px 20px rgba(244,63,94,0.4); }
.pos-pay-submit:disabled { opacity: 0.5; cursor: not-allowed; filter: grayscale(0.3); transform: none; }
</style>
