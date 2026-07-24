import { resetProducts } from "./products.js";
import { resetCustomers } from "./customers.js";
import { resetOrders } from "./orders.js";
import { resetInventory } from "./inventory.js";
import { resetPromotions } from "./promotions.js";
import { resetStaff } from "./staff.js";
import { resetSuppliers } from "./suppliers.js";
import { resetReturns } from "./returns.js";
import { resetBaoHanh } from "./baoHanh.js";
import { resetDoiThuong } from "./doiThuong.js";

// Gọi mỗi lần đăng xuất (chủ động hoặc do hết phiên — xem Service/api.js) — các store
// staff/admin dùng promise nhớ nhớ (ensureX, "gọi nhiều lần chỉ tải 1 lần"), nếu không reset
// thì đổi tài khoản khác cùng tab (không reload trang) sẽ thấy dữ liệu cũ của phiên trước
// cho tới khi tự bấm refresh ở từng tab.
export const resetAllStores = () => {
  resetProducts();
  resetCustomers();
  resetOrders();
  resetInventory();
  resetPromotions();
  resetStaff();
  resetSuppliers();
  resetReturns();
  resetBaoHanh();
  resetDoiThuong();
};
