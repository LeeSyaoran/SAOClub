// warrantyStore.js — Pinia-style reactive store cho module bảo hành
import { reactive } from 'vue';
import * as PhieuBaoHanhService from '../services/PhieuBaoHanhService.js';
import * as HinhAnhBaoHanhService from '../services/HinhAnhBaoHanhService.js';
import * as BinhLuanBaoHanhService from '../services/BinhLuanBaoHanhService.js';

export const WarrantyStore = reactive({
  // Phiếu BH của khách đang đăng nhập
  myClaims: [],
  myClaimsLoading: false,
  myClaimsLoaded: false,
  // Sản phẩm còn hạn BH của khách (kèm serial)
  myEligibleProducts: [],
  myEligibleLoading: false,
  // Map[baoHanhId] = Array<{binhLuanId, ...}>
  commentsByClaim: {},
});

let myClaimsPromise = null;
let myEligiblePromise = null;

export const resetWarranty = () => {
  myClaimsPromise = null;
  myEligiblePromise = null;
  WarrantyStore.myClaims = [];
  WarrantyStore.myClaimsLoaded = false;
  WarrantyStore.myEligibleProducts = [];
  WarrantyStore.commentsByClaim = {};
};

// Lấy phiếu BH của khách
export const ensureMyClaims = (khachHangId, force = false) => {
  if (!khachHangId) return Promise.resolve([]);
  if (myClaimsPromise && !force) return myClaimsPromise;
  WarrantyStore.myClaimsLoading = true;
  myClaimsPromise = PhieuBaoHanhService.getByKhachHang(khachHangId)
    .then((list) => {
      WarrantyStore.myClaims = list || [];
      WarrantyStore.myClaimsLoaded = true;
      return WarrantyStore.myClaims;
    })
    .catch(() => {
      WarrantyStore.myClaims = [];
      WarrantyStore.myClaimsLoaded = true;
      return [];
    })
    .finally(() => {
      WarrantyStore.myClaimsLoading = false;
    });
  return myClaimsPromise;
};

// Lấy sản phẩm còn hạn BH (từ ChiTietSanPhamService.getUnderWarranty)
export const ensureMyEligibleProducts = (force = false) => {
  if (myEligiblePromise && !force) return myEligiblePromise;
  WarrantyStore.myEligibleLoading = true;
  // Lazy import để tránh cycle
  return import('../services/ChiTietSanPhamService.js').then(({ getUnderWarranty }) => {
    myEligiblePromise = getUnderWarranty()
      .then((list) => {
        WarrantyStore.myEligibleProducts = list || [];
        return WarrantyStore.myEligibleProducts;
      })
      .catch(() => {
        WarrantyStore.myEligibleProducts = [];
        return [];
      })
      .finally(() => {
        WarrantyStore.myEligibleLoading = false;
      });
    return myEligiblePromise;
  });
};

// Reload phiếu của khách sau khi tạo mới
export const refreshMyClaims = (khachHangId) => ensureMyClaims(khachHangId, true);

// Lấy danh sách ảnh của 1 phiếu
export const loadClaimImages = (baoHanhId) =>
  HinhAnhBaoHanhService.getByBaoHanh(baoHanhId);

// Lấy danh sách bình luận của 1 phiếu (cached)
export const loadClaimComments = async (baoHanhId) => {
  if (WarrantyStore.commentsByClaim[baoHanhId]) {
    return WarrantyStore.commentsByClaim[baoHanhId];
  }
  const list = await BinhLuanBaoHanhService.getByBaoHanh(baoHanhId);
  WarrantyStore.commentsByClaim[baoHanhId] = list;
  return list;
};

export const sendComment = async (baoHanhId, noiDung) => {
  const res = await BinhLuanBaoHanhService.send(baoHanhId, noiDung);
  // invalidate cache để load lại
  delete WarrantyStore.commentsByClaim[baoHanhId];
  return res;
};
