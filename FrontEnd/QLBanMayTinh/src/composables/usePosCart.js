// usePosCart — cart state + serial-aware mutations
import { ref, computed, watch } from "vue";
import { syncPosCart } from "../stores/posCart.js";
import { bumpSerialEvent } from "../stores/serialEvents.js";
import * as ChiTietSanPhamService from "../services/ChiTietSanPhamService.js";
import { refreshProducts } from "../stores/products.js";
import { refreshInventory } from "../stores/inventory.js";

const cart = ref([]);
watch(cart, (v) => syncPosCart(v), { deep: true });

export function usePosCart() {
  const cartTotal = computed(() =>
    cart.value.reduce((s, i) => s + i.giaBan * (i.soLuong ?? 1), 0)
  );

  const cartGroups = computed(() => {
    const map = new Map();
    cart.value.forEach((item) => {
      if (!map.has(item.sanPhamId)) map.set(item.sanPhamId, { ...item, items: [] });
      map.get(item.sanPhamId).items.push(item);
    });
    return [...map.values()];
  });

  const formatPriceShort = (v) => {
    if (v == null) return "—";
    if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(v % 1_000_000 === 0 ? 0 : 2)}tr`;
    if (v >= 1_000) return `${(v / 1_000).toFixed(0)}k`;
    return String(v);
  };

  const specLine = (item) =>
    [item.cpu, item.ram, item.oCung, item.mauSac].filter(Boolean).join(" · ");

  const setSerialTrangThai = async (item, trangThai) => {
    await ChiTietSanPhamService.update(item.chiTietId, {
      bienTheId: item.bienTheId,
      soSerial: item.soSerial,
      trangThai,
      ngayNhapKho: item.ngayNhapKho,
    }).catch(() => {});
    refreshProducts();
    refreshInventory();
    bumpSerialEvent();
  };

  const upsertItem = async (item, swapChiTietId = null) => {
    const oldItem = swapChiTietId != null
      ? cart.value.find((i) => i.chiTietId === swapChiTietId)
      : null;

    if (swapChiTietId != null) {
      cart.value = cart.value.map((i) =>
        i.chiTietId === swapChiTietId ? item : i
      );
    } else {
      cart.value = [...cart.value, item];
    }

    await setSerialTrangThai(item, "giu_hang");
    if (oldItem) await setSerialTrangThai(oldItem, "trong_kho");
  };

  const addMany = async (items) => {
    cart.value = [...cart.value, ...items];
    await Promise.all(items.map((item) => setSerialTrangThai(item, "giu_hang")));
  };

  const decrementGroup = async (g) => {
    if (!g.items.length) return;
    const lastItem = g.items[g.items.length - 1];
    cart.value = cart.value.filter((i) => i.chiTietId !== lastItem.chiTietId);
    await setSerialTrangThai(lastItem, "trong_kho");
  };

  const removeGroup = async (g) => {
    const ids = new Set(g.items.map((i) => i.chiTietId));
    cart.value = cart.value.filter((i) => !ids.has(i.chiTietId));
    await Promise.all(g.items.map((i) => setSerialTrangThai(i, "trong_kho")));
  };

  const releaseAll = async () => {
    await Promise.all(cart.value.map((item) => setSerialTrangThai(item, "trong_kho")));
    cart.value = [];
  };

  const buildCartItem = (product, serial) => ({
    sanPhamId: product.sanPhamId,
    bienTheId: product.bienTheId,
    tenSanPham: product.tenSanPham,
    maSku: product.maSku,
    giaBan: product.giaBan,
    hinhAnhChinh: product.hinhAnhChinh,
    cpu: product.cpu ?? null,
    ram: product.ram ?? null,
    oCung: product.oCung ?? null,
    mauSac: product.mauSac ?? null,
    chiTietId: serial.chiTietId,
    soSerial: serial.soSerial,
    ngayNhapKho: serial.ngayNhapKho,
    soLuong: 1,
  });

  return {
    cart,
    cartTotal,
    cartGroups,
    cartCount: computed(() => cart.value.length),
    formatPriceShort,
    specLine,
    setSerialTrangThai,
    upsertItem,
    addMany,
    decrementGroup,
    removeGroup,
    releaseAll,
    buildCartItem,
  };
}
